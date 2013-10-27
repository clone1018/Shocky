import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import com.sun.script.javascript.RhinoScriptEngine;

import pl.shockah.Helper;
import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.interfaces.ILua;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.threads.*;

public class ModuleLua extends ScriptModule implements ResourceFinder {

	public static final File binary = new File(Data.lastSave, "luastate.bin").getAbsoluteFile();
	public static final File scripts = new File("data", "lua").getAbsoluteFile();
	public static final String luaHash = "lua";
	public static final String cmdFuncHash = "cmdfunc";
	public static final String channelHash = "luachannel";

	protected Command cmd, reset;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("lua");
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);

	LuaTable env = null;
	LuaTable envMeta = null;

	@Override
	public String name() {
		return "lua";
	}

	@Override
	public String identifier() {
		return "lua";
	}

	@Override
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdLua(), reset = new CmdLuaReset());
		initLua();
	}

	@Override
	public void onDisable() {
		Command.removeCommands(cmd, reset);
	}

	public File[] getReadableFiles() {
		return new File[] { binary, scripts };
	}

	@Override
	public void onDataSave(File dir) {
		File file = new File(dir, "luastate.bin");
		LuaValue value = env.get("irc");
		if (!value.istable())
			return;
		
		File temp = null;
		try {
			try {
				temp = File.createTempFile("shocky", ".tmp");
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			
			System.out.printf("File: %s Temp: %s", file.getAbsolutePath(), temp.getAbsolutePath()).println();
			
			DataOutputStream os = new DataOutputStream(new FileOutputStream(temp));
			writeValue(os, value);
			os.close();
			
			if (file.exists())
				file.delete();
			temp.renameTo(file);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (temp != null && temp.exists())
				temp.delete();
		}
	}

	private void initLua() {
		BaseLib.FINDER = this;
		env = new LuaTable();
		env.load(new JseBaseLib());
		//env.load(new PCall());
		env.load(new PackageLib());
		env.load(new TableLib());
		env.load(new StringLib());
		env.load(new JseMathLib());
		env.load(new JseOsLib());

		env.load(new BotLib());
		env.load(new JSONLib());
		
		env.rawset("print", LuaValue.NIL);
		env.rawset("pcall", LuaValue.NIL);
		env.rawset("xpcall", LuaValue.NIL);

		env.set("cmd", new CmdData());
		
		for (Module module : Module.getModules()) {
			if (module instanceof ILua)
				((ILua)module).setupLua(env);
		}

		try {
			Class.forName("org.luaj.vm2.lib.LuaState");
			Class.forName("ModuleLua$CmdFunction");
			new RhinoScriptEngine().eval("0");
			if (!scripts.exists())
				scripts.mkdirs();
			if (binary.exists()) {
				FileInputStream fs = new FileInputStream(binary);
				DataInputStream is = new DataInputStream(fs);
				env.set("irc", readValue(is));
				is.close();
				fs.close();
			} else {
				env.set("irc", new LuaTable());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		LuaThread.setGlobals(env);
		LuaC.install();

		BaseLib.instance.STDIN = new ZeroInputStream();

		envMeta = new LuaTable();
		envMeta.rawset(LuaValue.INDEX, env);
	}

	@Override
	public InputStream findResource(String filename) {
		File[] files = scripts.listFiles();
		for (int i = 0; i < files.length; i++) {
			try {
				String name = files[i].getName();
				if (filename.indexOf('.') == -1)
					name = name.split("\\.")[0];
				if (name.equalsIgnoreCase(filename)) {
					return new FileInputStream(files[i]);
				}
			} catch (FileNotFoundException e) {
			}
		}
		return null;
	}

	public static void writeTable(DataOutputStream os, LuaValue value)
			throws IOException {
		LuaTable table = value.checktable();
		LuaValue[] keys = table.keys();
		writeEncodedInt(os, keys.length);
		for (LuaValue value2 : keys) {
			writeValue(os, value2);
			writeValue(os, table.get(value2));
		}
	}

	public static void writeValue(DataOutputStream os, LuaValue value)
			throws IOException {
		if (value.type() == LuaValue.TFUNCTION && !value.isclosure()) {
			writeEncodedInt(os, LuaValue.TNIL);
		} else {
			writeEncodedInt(os, value.type());
		}
		switch (value.type()) {
		case LuaValue.TBOOLEAN:
			os.writeBoolean(value.checkboolean());
			break;
		case LuaValue.TINT:
			writeEncodedInt(os, value.checkint());
			break;
		case LuaValue.TTABLE:
			writeTable(os, value);
			break;
		case LuaValue.TNUMBER:
			os.writeDouble(value.checkdouble());
			break;

		case LuaValue.TSTRING:
			byte[] bytes = value.checkjstring().getBytes(Helper.utf8);
			writeEncodedInt(os, bytes.length);
			os.write(bytes);
			break;

		case LuaValue.TFUNCTION:
			if (value.isclosure()) {
				LuaClosure closure = value.checkclosure();
				DumpState.dump(closure.p, os, true);
			}
			break;

		default:
		case LuaValue.TNIL:
		case LuaValue.TNONE:
			break;
		}
	}

	public LuaValue readTable(DataInputStream is) throws IOException {
		LuaTable table = new LuaTable();
		int size = readEncodedInt(is);
		for (int i = 0; i < size; i++) {
			LuaValue key = readValue(is);
			LuaValue value = readValue(is);
			if (key.type() != LuaValue.TNIL && key.type() != LuaValue.TNONE)
				table.set(key, value);
		}
		return table;
	}

	public LuaValue readValue(DataInputStream is) throws IOException {
		int type = readEncodedInt(is);
		switch (type) {
		case LuaValue.TBOOLEAN:
			return LuaValue.valueOf(is.readBoolean());
		case LuaValue.TINT:
			return LuaValue.valueOf(readEncodedInt(is));
		case LuaValue.TTABLE:
			return readTable(is);
		case LuaValue.TNUMBER:
			return LuaValue.valueOf(is.readDouble());

		case LuaValue.TSTRING:
			int length = readEncodedInt(is);
			byte[] bytes = new byte[length];
			is.read(bytes);
			return LuaValue.valueOf(new String(bytes, Helper.utf8));

		case LuaValue.TFUNCTION:
			return LoadState.load(is, "script", env);

		default:
		case LuaValue.TNIL:
			return LuaNil.NIL;
		case LuaValue.TNONE:
			return LuaNil.NONE;
		}
	}

	public static void writeEncodedInt(OutputStream os, int value)
			throws IOException {
		long num;
		for (num = value & 0xFFFFFFFFL; num >= 0x80; num >>= 7)
			os.write((int) ((num & 0x7F) | 0x80));
		os.write((int) num);
	}

	public static int readEncodedInt(InputStream is) throws IOException {
		int num = 0;
		int shift = 0;
		while (shift < 35) {
			int b = is.read();
			num |= (b & 0x7F) << shift;
			shift += 7;
			if ((b & 0x80) == 0)
				return num;
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null)
			return "";
		String output = null;

		LuaTable subTable = new LuaTable();
		subTable.setmetatable(envMeta);

		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args, 1, " ");
			if (argsImp == null)
				argsImp = "";
			subTable.set("argc", (args.length - 1));
			subTable.set("args", argsImp);
			subTable.set("ioru", (args.length - 1 == 0 ? sender.getNick()
					: argsImp));
			LuaTable arg = new LuaTable();
			for (int i = 1; i < args.length; i++)
				arg.set(i, args[i]);
			subTable.set("arg", arg);
		}
		
		LuaState state = new LuaState(bot, channel, sender, cache);

		if (channel != null)
			subTable.set("channel", ChannelData.getChannelData(state, channel));
		subTable.set("bot", bot.getNick());
		subTable.set("sender", sender.getNick());
		subTable.set("host", sender.getHostmask());

		LuaClosure func = null;
		Object obj = state.get(luaHash, code);
		if (obj instanceof LuaClosure) {
			func = (LuaClosure) obj;
			func.setfenv(subTable);
		}

		final ExecutorService service = Executors.newSingleThreadExecutor(sandboxFactory);
		Print print = null;
		try {
			if (func == null) {
				func = LuaC.instance.load(new ByteArrayInputStream(code.getBytes(Helper.utf8)), "script", subTable).checkclosure();
				state.put(luaHash, code, func);
			}
			LuaRunner r = new LuaRunner(func,state);
			print = r.printer;
			Future<String> f = service.submit(r);
			output = f.get(30, TimeUnit.SECONDS);
		} catch (LuaError e) {
			output = e.getMessage();
		} catch (TimeoutException e) {
			output = "Script timed out";
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			service.shutdown();
			LuaState.clearState(state);
			if (print != null)
				print.dispose();
		}
		if (output == null || output.isEmpty())
			return null;

		return Utils.mungeAllNicks(channel, 2, output);
	}

	public class CmdLua extends ScriptCommand {
		public String command() {return "lua";}
		public String help(Parameters params) {
			return "lua\nlua {code} - runs Lua code";
		}
	}

	public class CmdLuaReset extends Command {
		public String command() {
			return "resetlua";
		}

		public String help(Parameters params) {
			return "resetlua\nresetlua - resets Lua environment";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			try {
				initLua();
				callback.append("Done.");
			} catch (Throwable e) {
				callback.append(e.getMessage());
			}
		}
	}

	public class LuaRunner implements Callable<String> {

		private final LuaClosure func;
		private final LuaState state;
		public final Print printer;

		public LuaRunner(LuaClosure f, LuaState s) {
			func = f;
			state = s;
			printer = new Print(f.getfenv());
			f.getfenv().rawset("print", printer);
		}

		@Override
		public String call() throws Exception {
			try {
				/*
				 * LuaValue[] stack = new LuaValue[func.p.maxstacksize];
				 * System.arraycopy(LuaValue.NILS, 0, stack, 0,
				 * func.p.maxstacksize); Varargs out = (Varargs)
				 * closureExecute.invoke(func, stack, LuaValue.NONE);
				 */
				LuaState.setState(state);
				Varargs out = func.invoke();
				if (printer.hasOutput())
					return printer.getOutput();
				if (out != LuaValue.NONE)
					return out.eval().tojstring();
				return null;
				/*
				 * StringBuilder sb = new StringBuilder(stack.length*20); for
				 * (int i = 0; i < stack.length;i++) { LuaValue v = stack[i]; if
				 * (v == LuaValue.NIL || v == LuaValue.NONE) continue;
				 * sb.append(v); sb.append('('); sb.append(v.typename());
				 * sb.append(')'); sb.append('\n'); } return sb.toString();
				 */
			} catch (LuaError ex) {
				ex.printStackTrace(System.out);
				return ex.getMessage();
			} finally {
				LuaState.clearState();
				printer.dispose();
			}
		}
	}

	public static class Print extends VarArgFunction {

		private final LuaValue env;
		private final ByteArrayOutputStream array;
		private final PrintStream stream;
		private boolean disposed = false;

		public Print(LuaValue luaValue) {
			this.env = luaValue;
			array = new ByteArrayOutputStream();
			stream = new PrintStream(array);
		}

		@Override
		public Varargs invoke(Varargs args) {
			if (disposed)
				throw new LuaError("Print used while disposed.");
			LuaValue tostring = env.get("tostring");
			for (int i = 1, n = args.narg(); i <= n; i++) {
				if (i > 1)
					stream.write('\t');
				LuaString s = tostring.call(args.arg(i)).strvalue();
				int z = s.indexOf((byte) 0, 0);
				stream.write(s.m_bytes, s.m_offset, z >= 0 ? z : s.m_length);
			}
			stream.println();
			return NONE;
		}

		public boolean hasOutput() {
			return array.size() > 0;
		}

		public String getOutput() throws UnsupportedEncodingException {
			return array.toString("UTF-8");
		}
		
		public void dispose() {
			disposed = true;
			stream.close();
		}
	}

	public static class CmdData extends LuaValue {
		@Override
		public int type() {
			return LuaValue.TUSERDATA;
		}

		@Override
		public String typename() {
			return "userdata";
		}

		@Override
		public LuaValue get(LuaValue key) {
			return CmdFunction.create(key.checkjstring());
		}
	}
	
	private static class CmdFunction extends OneArgFunction {
		final Command cmd;

		private CmdFunction(Command factoid) {
			this.cmd = factoid;
		}

		public synchronized static LuaValue create(String cmd) {
			if (cmd == null || cmd.isEmpty())
				return NIL;

			/*LuaValue v = parent.getfenv().get("state");
			if (!(v instanceof LuaState))
				return NIL;
			LuaState state = (LuaState) v;*/
			LuaState state = LuaState.getState();
			if (state == null)
				return NIL;

			Command cmdobj = Command.getCommand(state.bot, state.user, state.chan, EType.Channel, new CommandCallback(), cmd);
			if (cmdobj == null)
				return NIL;

			Object obj = state.get(cmdFuncHash, cmdobj.command());
			if (obj instanceof CmdFunction)
				return (CmdFunction) obj;

			CmdFunction function = new CmdFunction(cmdobj);
			state.put(cmdFuncHash, cmdobj.command(), function);
			return function;
		}

		@Override
		public LuaValue call(LuaValue arg) {
			/*LuaValue obj = env.get("state");
			if (!(obj instanceof LuaState))
				return NIL;
			LuaState state = (LuaState) obj;*/
			LuaState state = LuaState.getState();
			if (state == null)
				return NIL;
			String args = arg.optjstring("");
			Parameters params = new Parameters(state.bot, EType.Channel, state.chan, state.user, args);
			CommandCallback callback = new CommandCallback();
			try {
				cmd.doCommand(params, callback);
				if (callback.type != EType.Channel)
					return NIL;
				return valueOf(callback.output.toString());
			} catch (Exception e) {
				throw new LuaError(e);
			}
		}
	}

	private static class ChannelData extends LuaValue {
		public final Channel channel;
		public final LuaFunction isOp;
		public final LuaFunction isVoiced;

		public static LuaValue getChannelData(LuaState state, Channel channel) {
			if (state == null || channel == null)
				return NIL;
			if (state.containsKey(channelHash, channel))
				return (LuaValue) state.get(channelHash, channel);
			ChannelData data = new ChannelData(channel);
			state.put(channelHash,channel, data);
			return data;
		}

		private ChannelData(final Channel channel) {
			this.channel = channel;

			isOp = new OneArgFunction() {
				@Override
				public LuaValue call(LuaValue arg) {
					User user = channel.getBot().getUser(arg.checkjstring());
					if (user == null)
						return NIL;
					return valueOf(channel.isOp(user));
				}
			};

			isVoiced = new OneArgFunction() {
				@Override
				public LuaValue call(LuaValue arg) {
					User user = channel.getBot().getUser(arg.checkjstring());
					if (user == null)
						return NIL;
					return valueOf(channel.hasVoice(user));
				}
			};
		}

		@Override
		public int type() {
			return LuaValue.TUSERDATA;
		}

		@Override
		public String typename() {
			return "userdata";
		}

		@Override
		public LuaValue get(LuaValue key) {
			String name = key.checkjstring();
			if (name.equals("topic"))
				return valueOf(channel.getTopic());
			else if (name.equals("name"))
				return valueOf(channel.getName());
			else if (name.equals("isop"))
				return isOp;
			else if (name.equals("isvoiced"))
				return isVoiced;
			else if (name.equals("users"))
				return listOfUsers(channel.getUsers());
			else if (name.equals("ops"))
				return listOfUsers(channel.getOps());
			else if (name.equals("voiced"))
				return listOfUsers(channel.getVoices());
			return super.get(key);
		}

		private static LuaValue listOfUsers(Set<User> users) {
			LuaValue[] values = new LuaValue[users.size()];
			int i = 0;
			for (User user : users)
				values[i++] = valueOf(user.getNick());
			return listOf(values);
		}
	}

	public static class PCall extends VarArgFunction {

		private static final String[] LIBV_KEYS = { "pcall", // (f, arg1, ...)
																// -> status,
																// result1, ...
				"xpcall", // (f, err) -> result1, ...
		};

		public LuaValue init() {
			bind(env, PCall.class, LIBV_KEYS, 1);
			return env;
		}

		public Varargs invoke(Varargs args) {
			switch (opcode) {
			case 0: {
				init();
				break;
			}
			case 1: // "pcall", // (f, arg1, ...) -> status, result1, ...
			{
				LuaValue func = args.checkvalue(1);
				LuaThread.onCall(this);
				try {
					return pcall(func, args.subargs(2), null);
				} finally {
					LuaThread.onReturn();
				}
			}
			case 2: // "xpcall", // (f, err) -> result1, ...
			{
				LuaThread.onCall(this);
				try {
					return pcall(args.arg1(), NONE, args.checkvalue(2));
				} finally {
					LuaThread.onReturn();
				}
			}
			}
			return NONE;
		}

		public static Varargs pcall(LuaValue func, Varargs args, LuaValue errfunc) {
			try {
				for (int i = LuaThread.getCallstackDepth(); i > 0; i--) {
					LuaFunction func2 = LuaThread.getCallstackFunction(i);
					if (func == func2)
						return NONE;
				}
				System.out.println(func);
				LuaThread thread = LuaThread.getRunning();
				LuaValue olderr = thread.err;
				try {
					thread.err = errfunc;
					return varargsOf(LuaValue.TRUE, func.invoke(args));
				} finally {
					thread.err = olderr;
				}
			} catch (LuaError le) {
				String m = le.getMessage();
				return varargsOf(FALSE, m != null ? valueOf(m) : NIL);
			} catch (Exception e) {
				String m = e.getMessage();
				return varargsOf(FALSE, valueOf(m != null ? m : e.toString()));
			}
		}
	}
}
