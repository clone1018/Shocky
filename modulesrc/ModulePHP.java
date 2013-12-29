import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.Helper;
import pl.shockah.StringTools;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.IFactoidData;
import pl.shockah.shocky.sql.Factoid;

public class ModulePHP extends ScriptModule implements IFactoidData {
	public static final File savedData = new File("data", "php").getAbsoluteFile();
	
	protected Command cmd;
	
	public String name() {return "php";}
	public String identifier() {return "php";}
	public char stringCharacter() {return '\'';}
	public void onEnable(File dir) {
		if (!savedData.exists())
			savedData.mkdirs();
		
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Command.addCommands(this, cmd = new CmdPHP());
		Data.protectedKeys.add("php-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public File[] getReadableFiles() {
		return new File[]{savedData};
	}
	
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		User[] users;
		HashSet<Pair<String,String>> set = new HashSet<Pair<String,String>>();
		if (channel == null)
			users = new User[]{bot.getUserBot(),sender};
		else {
			users = channel.getUsers().toArray(new User[0]);
			set.add(Pair.of("channel", channel.getName()));
		}
		set.add(Pair.of("bot", bot.getNick()));
		set.add(Pair.of("sender", sender.getNick()));
		set.add(Pair.of("host", sender.getHostmask()));
		set.add(Pair.of("randnick", users[new Random().nextInt(users.length)].getNick()));
		
		StringBuilder sb = new StringBuilder();
		buildInit(sb,set);
		if (message != null) {
			String[] args = message.replace("\\", "\\\\").split(" ");
			String argsImp = StringTools.implode(args,1," ");
			sb.append("$argc=").append((args.length-1)).append(";$args=");
			appendEscape(sb,argsImp);
			sb.append(";$ioru=empty($args)?$sender:$args;$arg=empty($args)?array():explode(' ',$args);");
		}
		
		String data = getData(factoid);
		if (data != null) {
			sb.append("$_STATE=json_decode(");
			appendEscape(sb,data);
			sb.append(");");
		}
		
		sb.append(code);
		
		HTTPQuery q = HTTPQuery.create(Data.forChannel(channel).getString("php-url"),HTTPQuery.Method.POST);
		q.connect(true,true);
		q.write(HTTPQuery.parseArgs("code",sb.toString()));
		
		String ret = null;
		try {
			ret = q.readWhole();
			q.close();
			JSONObject json = new JSONObject(ret);
			//json = json.getJSONObject("output");
			JSONObject error = json.optJSONObject("error");
			if (error != null)
				return error.getString("message");
			String safe_errors = json.optString("safe_errors", null);
			if (safe_errors != null)
				return safe_errors;
			String newdata = json.optString("data", null);
			if (factoid != null && newdata != null && (data == null || !newdata.contentEquals(data)))
				setData(factoid,newdata);
			return json.optString("output");
		} catch (JSONException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (ret != null)?ret.trim():null;
	}
	
	private void buildInit(StringBuilder sb, Iterable<Pair<String,String>> set) {
		for (Pair<String,String> pair : set) {
			sb.append('$').append(pair.getLeft()).append('=');
			appendEscape(sb,pair.getRight());
			sb.append(';');
		}
	}
	
	public String readFile(File file) {
		try {
			FileInputStream fs = new FileInputStream(file);
			InputStreamReader sr = new InputStreamReader(fs, Helper.utf8);
			char[] buffer = new char[(int)file.length()];
			sr.read(buffer);
			sr.close();
			return new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean writeFile(File file, CharSequence str) {
		try {
			FileOutputStream fs = new FileOutputStream(file);
			OutputStreamWriter sr = new OutputStreamWriter(fs, Helper.utf8);
			sr.append(str);
			//sr.flush();
			sr.close();
			//fs.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public File getFactoidSave(Factoid f) {
		if (f != null)
			return new File(savedData,String.format("%s_%d", f.channel== null? "global" : f.channel, f.name.hashCode()));
		return null;
	}
	
	@Override
	public String getData(Factoid f) {
		File saveFile = getFactoidSave(f);
		if (saveFile != null && saveFile.exists() && saveFile.canRead())
			return readFile(saveFile);
		return null;
	}
	@Override
	public boolean setData(Factoid f, CharSequence data) {
		if (f == null)
			return false;
		if (data != null && data.length() < (1024 * 10)) {
			File saveFile = getFactoidSave(f);
			try {
				if (saveFile.exists() || saveFile.createNewFile())
					return writeFile(saveFile,data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public class CmdPHP extends ScriptCommand {
		public String command() {return "php";}
		public String help(Parameters params) {
			return "\nphp {code} - runs PHP code";
		}
	}
}