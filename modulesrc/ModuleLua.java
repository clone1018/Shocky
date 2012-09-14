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

import pl.shockah.Helper;
import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.prototypes.IFactoid;
import pl.shockah.shocky.threads.*;

public class ModuleLua extends ScriptModule implements ResourceFinder {
	
	public static final File binary = new File("data","luastate.bin").getAbsoluteFile();
	public static final File scripts = new File("data","lua").getAbsoluteFile();
	
	protected Command cmd;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("lua");
	private final SandboxSecurityManager secure = new SandboxSecurityManager(sandboxGroup,scripts);
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);
	LuaTable env = null;
	LuaTable envMeta = null;

	@Override
	public String name() {return "lua";}
	@Override
	public String identifier() {return "lua";}
	@Override
	public void onEnable() {
		initLua();
		Command.addCommands(this, cmd = new CmdLua());
	}
	@Override
	public void onDisable() {
		Command.removeCommands(cmd);
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
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initLua() {
		BaseLib.FINDER = this;
		env = new LuaTable();
		env.load(new JseBaseLib());
		env.load(new PackageLib());
		env.load(new TableLib());
		env.load(new StringLib());
		env.load(new JseMathLib());
		
		env.load(new BotLib());
		env.load(new JSONLib());
		
		env.set("factoid", new FactoidData());
		
		try {
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
		} catch(Exception e) {
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
				if (filename.indexOf('.')==-1)
					name = name.split("\\.")[0];
				if (name.equalsIgnoreCase(filename)) {
					return new FileInputStream(files[i]);
				}
			} catch (FileNotFoundException e) {
			}
		}
		return null;
	}
	
	public static void writeTable(DataOutputStream os, LuaValue value) throws IOException {
		LuaTable table = value.checktable();
		LuaValue[] keys = table.keys();
		writeEncodedInt(os,keys.length);
		for (LuaValue value2 : keys) {
			writeValue(os, value2);
			writeValue(os, table.get(value2));
		}
	}
	
	public static void writeValue(DataOutputStream os, LuaValue value) throws IOException {
		if (value.type() == LuaValue.TFUNCTION && !value.isclosure()) {
			writeEncodedInt(os,LuaValue.TNIL);
		} else {
			writeEncodedInt(os,value.type());
		}
		switch (value.type()) {
		case LuaValue.TBOOLEAN: os.writeBoolean(value.checkboolean()); break;
		case LuaValue.TINT: writeEncodedInt(os,value.checkint()); break;
		case LuaValue.TTABLE: writeTable(os,value); break;
		case LuaValue.TNUMBER: os.writeDouble(value.checkdouble()); break;
		
		case LuaValue.TSTRING:
			byte[] bytes = value.checkjstring().getBytes(Helper.utf8);
			writeEncodedInt(os,bytes.length);
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
		case LuaValue.TNONE: break;
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
		case LuaValue.TBOOLEAN: return LuaValue.valueOf(is.readBoolean());
		case LuaValue.TINT: return LuaValue.valueOf(readEncodedInt(is));
		case LuaValue.TTABLE: return readTable(is);
		case LuaValue.TNUMBER: return LuaValue.valueOf(is.readDouble());
		
		case LuaValue.TSTRING:
			int length = readEncodedInt(is);
			byte[] bytes = new byte[length];
			is.read(bytes);
			return LuaValue.valueOf(new String(bytes,Helper.utf8));
		
		case LuaValue.TFUNCTION: return LoadState.load(is, "script", env);
			
		default:
		case LuaValue.TNIL: return LuaNil.NIL;
		case LuaValue.TNONE: return LuaNil.NONE;
		}
	}
	
	public static void writeEncodedInt(OutputStream os, int value) throws IOException
	{
		long num;
		for (num = value & 0xFFFFFFFFL; num >= 0x80; num >>= 7)
			os.write((int)((num & 0x7F) | 0x80));
		os.write((int)num);
	}
	
	public static int readEncodedInt(InputStream is) throws IOException
	{
		int num = 0;
		int shift = 0;
		while (shift < 35)
		{
			int b = is.read();
			num |= (b & 0x7F) << shift;
			shift += 7;
			if ((b & 0x80) == 0)
				return num;
		}
		return Integer.MAX_VALUE;
	}


	@Override
	public synchronized String parse(PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
		if (code == null) return "";
		String output = null;
		
		LuaTable subTable = new LuaTable();
		subTable.setmetatable(envMeta);
		
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			subTable.set("argc",(args.length-1));
			subTable.set("args",argsImp);
			subTable.set("ioru",(args.length-1 == 0 ? sender.getNick() : argsImp));
			LuaTable arg = new LuaTable();
			for (int i = 1; i < args.length; i++)
				arg.set(i, args[i]);
			subTable.set("arg",arg);
		}
		
		subTable.set("channel", ChannelData.getChannelData(channel));
		subTable.set("bot", bot.getNick());
		subTable.set("sender", sender.getNick());
		
		IFactoid module = (IFactoid)Module.getModule("factoid");
		FactoidFunction.initFields(module, bot, channel, sender);

		LuaRunner r = new LuaRunner(subTable,code);
		final ExecutorService service = Executors.newSingleThreadExecutor(sandboxFactory);
		try {
			Future<String> f = service.submit(r);
		   	output = f.get(30, TimeUnit.SECONDS);
		}
		catch(TimeoutException e) {
		    output = "Script timed out";
		}
		catch(Exception e) {
		    throw new RuntimeException(e);
		}
		finally {
		    service.shutdown();
		    FactoidFunction.initFields(null, null, null, null);
		}
		if (output == null || output.isEmpty())
			return null;

		return output;
	}
	
	public class CmdLua extends Command {
		public String command() {return "lua";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "lua\nlua {code} - runs Lua code";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}

			System.out.println(message);
			String output = parse(bot,type,channel,sender,StringTools.implode(args,1," "),null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(Utils.mungeAllNicks(channel, output))));
		}
	}
	
	public class LuaRunner implements Callable<String> {
		
		private final LuaTable sandbox;
		private final String code;
		
		public LuaRunner(LuaTable s, String c) {
			sandbox = s;
			code = c;
		}

		@Override
		public String call() throws Exception {
			synchronized (secure) {
				ByteArrayOutputStream sw = new ByteArrayOutputStream();
				PrintStream pw = new PrintStream(sw);
				BaseLib.instance.STDERR = pw;
				BaseLib.instance.STDOUT = pw;
			
				SecurityManager sysSecure = System.getSecurityManager();
			
				try {
					LuaFunction func = LuaC.instance.load(new ByteArrayInputStream(code.getBytes(Helper.utf8)), "script", sandbox);
				
					System.setSecurityManager(secure);
					secure.enabled = true;
					Varargs out = func.invoke();
					if (sw.size() > 0)
						return sw.toString("UTF-8");
					return out.tojstring();
				}
				catch(LuaError ex) {
					return ex.getMessage();
				} finally {
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
			return FactoidFunction.create(key.checkjstring());
		}
	}
	
	private static class FactoidFunction extends OneArgFunction {
		private static final Map<String,FactoidFunction> internMap = new HashMap<String,FactoidFunction>();
		
		final String factoid;
		
		private static IFactoid module;
		private static PircBotX bot;
		private static Channel chan;
		private static User user;
		
		private FactoidFunction(String factoid) {
			this.factoid = factoid;
		}
		
		public synchronized static LuaValue create(String factoid) {
			if (factoid == null || factoid.isEmpty())
				return NIL;
			
			if (internMap.containsKey(factoid))
				return internMap.get(factoid);
			
			FactoidFunction function = new FactoidFunction(factoid);
			internMap.put(factoid, function);
			return function;
		}

		public synchronized static void initFields(IFactoid module, PircBotX bot, Channel chan, User user) {
			FactoidFunction.module = module;
			FactoidFunction.bot = bot;
			FactoidFunction.chan = chan;
			FactoidFunction.user = user;
		}
		
		@Override
		public LuaValue call(LuaValue arg) {
			String args = arg.optjstring(null);
			StringBuilder message = new StringBuilder(factoid);
			if (args != null) {
				message.append(' ');
				message.append(args);
			}
			try {
				return valueOf(module.runFactoid(bot, chan, user, message.toString()));
			} catch (Exception e) {
				throw new LuaError(e);
			}
		}
	}
	
	private static class ChannelData extends LuaValue {
		private static final HashMap<Channel,ChannelData> intern = new HashMap<Channel,ChannelData>();
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
			if (name.equalsIgnoreCase("topic"))
				return valueOf(channel.getTopic());
			else if (name.equalsIgnoreCase("name"))
				return valueOf(channel.getName());
			else if (name.equalsIgnoreCase("isop"))
				return isOp;
			else if (name.equalsIgnoreCase("isvoiced"))
				return isVoiced;
			else if (name.equalsIgnoreCase("users"))
				return listOfUsers(channel.getUsers());
			else if (name.equalsIgnoreCase("ops"))
				return listOfUsers(channel.getOps());
			else if (name.equalsIgnoreCase("voiced"))
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
}
