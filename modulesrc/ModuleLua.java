import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.DumpState;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.prototypes.IFactoid;
import pl.shockah.shocky.threads.SandboxSecurityManager;
import pl.shockah.shocky.threads.SandboxThreadFactory;
import pl.shockah.shocky.threads.SandboxThreadGroup;

public class ModuleLua extends ScriptModule {
	protected Command cmd;
	protected SecurityManager secure = new SandboxSecurityManager();
	ThreadGroup sandboxGroup = new SandboxThreadGroup("lua");
	ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);
	LuaTable env = null;
	
	public static final File binary = new File("data","luastate.bin");

	@Override
	public String name() {return "lua";}
	@Override
	public String identifier() {return "lua";}
	@Override
	public void onEnable() {
		Command.addCommands(this, cmd = new CmdLua());
		env = new LuaTable();
		env.load(new JseBaseLib());
		env.load(new PackageLib());
		env.load(new TableLib());
		env.load(new StringLib());
		env.load(new JseMathLib());
		
		env.load(new BotLib());
		env.load(new JSONLib());
		
		LuaThread.setGlobals(env);
		
		env.set("factoid", new FactoidTable());
		
		try {
			if (binary.exists()) {
				FileInputStream fs = new FileInputStream(binary);
				ObjectInputStream is = new ObjectInputStream(fs);
				env.set("irc", readValue(is));
				is.close();
				fs.close();
			} else {
				env.set("irc", new LuaTable());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
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
					ObjectOutputStream os = new ObjectOutputStream(fs);
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
	
	public static void writeTable(ObjectOutputStream os, LuaValue value) throws IOException {
		LuaTable table = value.checktable();
		LuaValue[] keys = table.keys();
		writeEncodedInt(os,keys.length);
		for (LuaValue value2 : keys) {
			writeValue(os, value2);
			writeValue(os, table.get(value2));
		}
	}
	
	public static void writeValue(ObjectOutputStream os, LuaValue value) throws IOException {
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
			byte[] bytes = value.checkjstring().getBytes(Charset.forName("UTF-8"));
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
	
	public LuaValue readTable(ObjectInputStream is) throws IOException {
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
	
	public LuaValue readValue(ObjectInputStream is) throws IOException {
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
			return LuaValue.valueOf(new String(bytes,Charset.forName("UTF-8")));
		
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
	public String parse(PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
		if (code == null) return "";
		
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			env.set("argc",(args.length-1));
			env.set("args",argsImp);
			env.set("ioru",(args.length-1 == 0 ? sender.getNick() : argsImp));
			LuaTable arg = new LuaTable();
			for (int i = 1; i < args.length; i++)
				arg.set(i, args[i]);
			env.set("arg",arg);
		}
		
		env.set("channel", channel.getName());
		env.set("bot", bot.getNick());
		env.set("sender", sender.getNick());
		
		IFactoid module = (IFactoid)Module.getModule("factoid");
		FactoidFunction.initFields(module, bot, channel, sender);
		
		Sandbox sandbox = new Sandbox(bot,channel);
		env.set("bot", CoerceJavaToLua.coerce(sandbox));

		LuaRunner r = new LuaRunner(code);

		SecurityManager sysSecure = System.getSecurityManager();
		System.setSecurityManager(secure);
		String output = null;
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
		    System.setSecurityManager(sysSecure);
		    FactoidFunction.initFields(null, null, null, null);
		}
		if (output == null || output.isEmpty())
			return null;
		
		StringBuilder sb = new StringBuilder();
		for(String line : output.split("[\r\n]+")) {
			if (sb.length() != 0) sb.append(" | ");
			sb.append(line);
		}

		return StringTools.limitLength(sb);
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
				callback.append(output);
		}
	}
	
	public class Sandbox {
		private Random rnd = new Random();
		private final PircBotX bot;
		private final Channel chan;
		private User[] users;
		
		public Sandbox(PircBotX bot, Channel chan) {
			this.bot = bot;
			this.chan = chan;
		}
		
		public String randnick() {
			if (chan == null)
				return null;
			if (users == null)
				users = chan.getUsers().toArray(new User[0]);
			return users[rnd.nextInt(users.length)].getNick();
		}
		
		public boolean isOp(String username) {
			if (chan == null)
				return false;
			User user = bot.getUser(username);
			return user != null && chan.isOp(user);
		}
		
		public boolean isVoiced(String username) {
			if (chan == null)
				return false;
			User user = bot.getUser(username);
			return user != null && chan.hasVoice(user);
		}
		
		public String topic() {
			if (chan == null)
				return null;
			return chan.getTopic();
		}
		
		public String chanModes() {
			if (chan == null)
				return null;
			return chan.getMode();
		}
		
		public String toString() {
			return "Yes it is a bot";
		}
	}
	
	public class LuaRunner implements Callable<String> {
		
		private final String code;
		
		public LuaRunner(String c) {
			code = c;
		}

		@Override
		public String call() throws Exception {
			ByteOutputStream sw = new ByteOutputStream();
			PrintStream pw = new PrintStream(sw);
			BaseLib.instance.STDERR = pw;
			BaseLib.instance.STDOUT = pw;
			BaseLib.instance.STDIN = new ZeroInputStream();
			
			try {
				LuaFunction func = LuaC.instance.load(new ByteArrayInputStream(code.getBytes()), "script", env);
				Object out = func.invoke();
				if (sw.size() > 0)
					return new String(sw.getBytes(),0,sw.size(),Charset.forName("UTF-8"));
				if (out != null)
					return out.toString();
			}
			catch(LuaError ex) {
				return ex.getMessage();
			}
			return null;
		}
	}
	
	public static class FactoidTable extends LuaValue {

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
		
		public static LuaValue create(String factoid) {
			if (factoid == null || factoid.isEmpty())
				return NIL;
			
			if (internMap.containsKey(factoid))
				return internMap.get(factoid);
			
			FactoidFunction function = new FactoidFunction(factoid);
			internMap.put(factoid, function);
			return function;
		}

		public static void initFields(IFactoid module, PircBotX bot, Channel chan, User user) {
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
}
