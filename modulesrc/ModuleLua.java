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
import org.pircbotx.hooks.events.KickEvent;

import pl.shockah.Helper;
import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.IFactoidData;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.prototypes.IFactoid;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.threads.*;

public class ModuleLua extends ScriptModule implements ResourceFinder {

	public static final File binary = new File("data", "luastate.bin").getAbsoluteFile();
	public static final File scripts = new File("data", "lua").getAbsoluteFile();

	protected Command cmd, reset;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("lua");
	private final SandboxSecurityManager secure = new SandboxSecurityManager(sandboxGroup, scripts, binary);
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);
	private static final int luaHash = "lua".hashCode();

	LuaTable env = null;
	LuaTable envMeta = null;

	/*
	 * private static final Method closureExecute;
	 * 
	 * static { Method temp = null; try { temp =
	 * LuaClosure.class.getDeclaredMethod("execute", LuaValue[].class,
	 * Varargs.class); temp.setAccessible(true); } catch (Throwable t) { }
	 * closureExecute = temp; }
	 */

	@Override
	public String name() {
		return "lua";
	}

	@Override
	public String identifier() {
		return "lua";
	}

	@Override
	public void onEnable() {
		initLua();
		Command.addCommands(this, cmd = new CmdLua(), reset = new CmdLuaReset());
	}

	@Override
	public void onDisable() {
		Command.removeCommands(cmd, reset);
	}
	
	public boolean isListener() {return true;}
	public void onKick(KickEvent<PircBotX> event) {
		if (event.getRecipient()==event.getBot().getUserBot() && ChannelData.intern.containsKey(event.getChannel()))
				ChannelData.intern.remove(event.getChannel());
	}

	@Override
	public void onDataSave() {
		try {
			LuaValue value = env.get("irc");
			if (value.istable()) {
				if (binary.exists() || binary.createNewFile()) {
					FileOutputStream fs = new FileOutputStream(binary);
					DataOutputStream os = new DataOutputStream(fs);
					writeValue(os, value);
					os.flush();
					os.close();
					fs.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initLua() {
		BaseLib.FINDER = this;
		env = new LuaTable();
		env.load(new JseBaseLib());
		env.load(new PCall());
		env.load(new PackageLib());
		env.load(new TableLib());
		env.load(new StringLib());
		env.load(new JseMathLib());
		env.load(new JseOsLib());

		env.load(new BotLib());
		env.load(new JSONLib());

		env.set("factoid", new FactoidData());
		env.set("cmd", new CmdData());

		try {
			Class.forName("ModuleLua$FactoidFunction");
			Class.forName("ModuleLua$CmdFunction");
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
	public String parse(Map<Integer, Object> cache, PircBotX bot, EType type, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null)
			return "";
		int key = luaHash + code.hashCode();
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

		subTable.set("channel", ChannelData.getChannelData(channel));
		subTable.set("bot", bot.getNick());
		subTable.set("sender", sender.getNick());

		IFactoid module = (IFactoid) Module.getModule("factoid");
		LuaState state = new LuaState(module, bot, channel, sender, cache);
		subTable.set("state", state);

		LuaClosure func = null;
		if (cache != null && cache.containsKey(key)) {
			Object obj = cache.get(key);
			if (obj instanceof LuaClosure) {
				func = (LuaClosure) obj;
				func.setfenv(subTable);
			}
		}

		final ExecutorService service = Executors.newSingleThreadExecutor(sandboxFactory);
		try {
			if (func == null) {
				func = LuaC.instance.load(new ByteArrayInputStream(code.getBytes(Helper.utf8)), "script", subTable).checkclosure();
				if (cache != null)
					cache.put(key, func);
			}
			LuaRunner r = new LuaRunner(func);
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
		}
		if (output == null || output.isEmpty())
			return null;

		return Utils.mungeAllNicks(channel, 2, output);
	}

	public class CmdLua extends Command {
		public String command() {
			return "lua";
		}

		public String help(Parameters params) {
			return "lua\nlua {code} - runs Lua code";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount < 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}

			HashMap<Integer, Object> cache = new HashMap<Integer, Object>();
			String output = parse(cache,params.bot,params.type,params.channel,params.sender,null,params.input,null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
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
		private final Print printer;

		public LuaRunner(LuaClosure f) {
			func = f;
			printer = new Print(f.getfenv());
			f.getfenv().rawset("print", printer);
		}

		@Override
		public String call() throws Exception {
			SecurityManager sysSecure = System.getSecurityManager();

			try {

				if (sysSecure != secure) {
					System.setSecurityManager(secure);
					secure.enabled = true;
				}
				/*
				 * LuaValue[] stack = new LuaValue[func.p.maxstacksize];
				 * System.arraycopy(LuaValue.NILS, 0, stack, 0,
				 * func.p.maxstacksize); Varargs out = (Varargs)
				 * closureExecute.invoke(func, stack, LuaValue.NONE);
				 */
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
				return ex.getMessage();
			} finally {
				if (sysSecure != secure) {
					secure.enabled = false;
					System.setSecurityManager(sysSecure);
				}
			}
		}
	}

	public static class FactoidData extends LuaValue {
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
			LuaFunction parent = LuaThread.getCallstackFunction(LuaThread.getCallstackDepth());
			LuaValue env = parent.getfenv();
			if (env == null)
				return NIL;
			LuaValue obj = env.get("state");
			if (!(obj instanceof LuaState))
				return NIL;
			LuaState state = (LuaState) obj;
			if (state.chan != null && state.factoidmod instanceof Module) {
				Module factmod = (Module) state.factoidmod;
				if (!factmod.isEnabled(state.chan.getName()))
					return NIL;
			}
			LuaValue func = FactoidFunction.create(state, key.checkjstring());
			func.setfenv(env);
			return func;
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
			LuaFunction parent = LuaThread.getCallstackFunction(LuaThread.getCallstackDepth());
			LuaValue func = CmdFunction.create(parent, key.checkjstring());
			func.setfenv(parent.getfenv());
			return func;
		}
	}

	public static class LuaState extends LuaValue {
		public final IFactoid factoidmod;
		public final PircBotX bot;
		public final Channel chan;
		public final User user;
		public final Map<Integer, Object> cache;

		public LuaState(IFactoid module, PircBotX bot, Channel chan, User user, Map<Integer, Object> cache) {
			super();
			this.factoidmod = module;
			this.bot = bot;
			this.chan = chan;
			this.user = user;
			this.cache = cache;
		}

		@Override
		public int type() {
			return LuaValue.TUSERDATA;
		}

		@Override
		public String typename() {
			return "userdata";
		}
		
		public boolean isController() {
			if (bot == null) return true;
			if (bot.getInetAddress().isLoopbackAddress()) return true;
			if (Shocky.getLogin(user) == null) return false;
			return Data.controllers.contains(Shocky.getLogin(user));
		}
	}

	public static class Print extends VarArgFunction {

		private final LuaValue env;
		private final ByteArrayOutputStream array;
		private final PrintStream stream;

		public Print(LuaValue luaValue) {
			this.env = luaValue;
			array = new ByteArrayOutputStream();
			stream = new PrintStream(array);
		}

		@Override
		public Varargs invoke(Varargs args) {
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
	}

	private static class FactoidFunction extends OneArgFunction {
		//private static final Map<String, FactoidFunction> internMap = new HashMap<String, FactoidFunction>();
		private static final int factoidHash = "factoid".hashCode();
		private static final int factoidFuncHash = "factoidfunc".hashCode();
		
		final String factoid;

		private FactoidFunction(String factoid) {
			this.factoid = factoid;
		}

		public synchronized static LuaValue create(LuaState state, String factoid) {
			if (factoid == null || factoid.isEmpty())
				return NIL;
			
			int hash = factoidFuncHash+factoid.hashCode();
			if (state.cache != null) {
				if (state.cache.containsKey(hash))
				{
					Object obj = state.cache.get(hash);
					if (obj instanceof FactoidFunction)
						return (FactoidFunction)obj;
				}
			}

			//if (internMap.containsKey(factoid))
			//	return internMap.get(factoid);

			FactoidFunction function = new FactoidFunction(factoid);
			//internMap.put(factoid, function);
			state.cache.put(hash, function);
			return function;
		}
		
		private LuaState getState() {
			if (env == null)
				return null;
			LuaValue obj = env.get("state");
			if (!(obj instanceof LuaState))
				return null;
			LuaState state = (LuaState) obj;
			if (state.chan != null && state.factoidmod instanceof Module) {
				Module factmod = (Module) state.factoidmod;
				if (!factmod.isEnabled(state.chan.getName()))
					return null;
			}
			return state;
		}
		
		private Factoid getFactoid(LuaState state) {
			if (!(state.factoidmod instanceof Module))
				return null;
			int hash = factoidHash+factoid.hashCode();
			Factoid f = null;
			if (state.cache != null) {
				if (state.cache.containsKey(hash))
				{
					Object obj = state.cache.get(hash);
					if (obj instanceof Factoid)
						f = (Factoid)obj;
				}
			}
			if (f == null && state.chan != null)
				f = state.factoidmod.getFactoid(state.chan.getName(), factoid);
			if (f == null)
				f = state.factoidmod.getFactoid(null, factoid);
			if (f != null && state.cache != null && !state.cache.containsKey(hash))
				state.cache.put(hash, f);
			return f;
		}
		
		public ScriptModule getScriptModule(Factoid f) {
			String raw = f.rawtext;
			String type = null;
			if (raw.startsWith("<")) {
				int closingIndex = raw.indexOf(">");
				if (closingIndex != -1)
					type = raw.substring(1, closingIndex);
			}
			if (type != null)
				return Module.getScriptingModule(type);
			return null;
		}

		@Override
		public LuaValue call(LuaValue arg) {
			LuaState state = getState();
			if (state == null)
				return NIL;
			String args = arg.optjstring(null);
			StringBuilder message = new StringBuilder(factoid);
			if (args != null)
				message.append(' ').append(args);
			try {
				return valueOf(state.factoidmod.runFactoid(state.cache, state.bot, state.chan, state.user, message.toString()));
			} catch (Exception e) {
				throw new LuaError(e);
			}
		}

		@Override
		public LuaValue get(LuaValue key) {
			String name = key.checkjstring();
			if (name == null)
				return NIL;
			boolean src = name.equals("src");
			boolean author = false;
			boolean time = false;
			boolean data = false;
			if (!src)
				author = name.equals("author");
			if (!src && !author)
				time = name.equals("time");
			if (!src && !author && !time)
				data = name.equals("data");
			if (src||author||time||data) {
				LuaState state = getState();
				if (state == null)
					return NIL;
				Factoid f = getFactoid(state);
				if (f == null)
					return NIL;
				if (src)
					return valueOf(f.rawtext);
				if (author)
					return valueOf(f.author);
				if (time)
					return valueOf(f.stamp);
				if (data) {
					ScriptModule sModule = getScriptModule(f);
					if (sModule != null && sModule instanceof IFactoidData) {
						IFactoidData dModule = (IFactoidData)sModule;
						String s = dModule.getData(f);
						if (s != null)
							return valueOf(s);
					}
					return NIL;
				}
			}
			return super.get(key);
		}
		
		@Override
		public void set(LuaValue key, LuaValue value) {
			String name = key.checkjstring();
			if (name == null)
				return;
			if (name.equals("data")) {
				String data = value.checkjstring();
				if (data == null)
					return;
				LuaState state = getState();
				if (state == null || !state.isController())
					return;
				Factoid f = getFactoid(state);
				if (f == null)
					return;
				ScriptModule sModule = getScriptModule(f);
				if (sModule != null && sModule instanceof IFactoidData)
					((IFactoidData)sModule).setData(f, data);
			}
		}
	}

	private static class CmdFunction extends OneArgFunction {
		private static final Map<Command, CmdFunction> internMap = new HashMap<Command, CmdFunction>();

		final Command cmd;

		private CmdFunction(Command factoid) {
			this.cmd = factoid;
		}

		public synchronized static LuaValue create(LuaValue parent, String cmd) {
			if (cmd == null || cmd.isEmpty())
				return NIL;

			LuaValue obj = parent.getfenv().get("state");
			if (!(obj instanceof LuaState))
				return NIL;
			LuaState state = (LuaState) obj;

			Command cmdobj = Command.getCommand(state.bot, state.user, state.chan.getName(), EType.Channel, new CommandCallback(), cmd);
			if (cmdobj == null)
				return NIL;

			if (internMap.containsKey(cmdobj))
				return internMap.get(cmdobj);

			CmdFunction function = new CmdFunction(cmdobj);
			internMap.put(cmdobj, function);
			return function;
		}

		@Override
		public LuaValue call(LuaValue arg) {
			LuaValue obj = env.get("state");
			if (!(obj instanceof LuaState))
				return NIL;
			LuaState state = (LuaState) obj;
			String args = arg.optjstring("");
			if (args.isEmpty())
				args = cmd.command();
			else
				args = cmd.command()+' '+args;
			Parameters params = new Parameters(state.bot,EType.Channel,state.chan,state.user,args);
			CommandCallback callback = new CommandCallback();
			try {
				cmd.doCommand(params,callback);
				if (callback.type != EType.Channel)
					return NIL;
				return valueOf(callback.output.toString());
			} catch (Exception e) {
				throw new LuaError(e);
			}
		}
	}

	private static class ChannelData extends LuaValue {
		private static final HashMap<Channel, ChannelData> intern = new HashMap<Channel, ChannelData>();
		public final Channel channel;
		public final LuaFunction isOp;
		public final LuaFunction isVoiced;

		public synchronized static LuaValue getChannelData(Channel channel) {
			if (channel == null)
				return NIL;
			if (intern.containsKey(channel))
				return intern.get(channel);
			ChannelData data = new ChannelData(channel);
			intern.put(channel, data);
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
		
		private static final String[] LIBV_KEYS = {
			"pcall", // (f, arg1, ...) -> status, result1, ...
			"xpcall", // (f, err) -> result1, ...
		};
		
		public LuaValue init() {
			bind( env, PCall.class, LIBV_KEYS, 1 );
			return env;
		}
		
		public Varargs invoke(Varargs args) {
			switch ( opcode ) {
			case 0:
			{
				init(); break;
			}
			case 1: // "pcall", // (f, arg1, ...) -> status, result1, ...
			{
				LuaValue func = args.checkvalue(1);
				LuaThread.onCall(this);
				try {
					return pcall(func,args.subargs(2),null);
				} finally {
					LuaThread.onReturn();
				}
			}
			case 2: // "xpcall", // (f, err) -> result1, ...				
			{
				LuaThread.onCall(this);
				try {
					return pcall(args.arg1(),NONE,args.checkvalue(2));
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
			} catch ( LuaError le ) {
				String m = le.getMessage();
				return varargsOf(FALSE, m!=null? valueOf(m): NIL);
			} catch ( Exception e ) {
				String m = e.getMessage();
				return varargsOf(FALSE, valueOf(m!=null? m: e.toString()));
			}
		}
	}
}
