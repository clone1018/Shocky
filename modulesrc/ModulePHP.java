import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.IFactoidData;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.sql.Factoid;

public class ModulePHP extends ScriptModule implements IFactoidData {
	public static final File savedData = new File("data", "php").getAbsoluteFile();
	
	protected Command cmd;
	
	public String name() {return "php";}
	public String identifier() {return "php";}
	public char stringCharacter() {return '\'';}
	public void onEnable() {
		if (!savedData.exists())
			savedData.mkdirs();
		
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Command.addCommands(this, cmd = new CmdPHP());
		Data.protectedKeys.add("php-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Map<Integer,Object> cache, PircBotX bot, EType type, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		StringBuilder sb = new StringBuilder("$channel=");
		appendEscape(sb,channel.getName());
		sb.append(";$bot=");
		appendEscape(sb,bot.getNick());
		sb.append(";$sender=");
		appendEscape(sb,sender.getNick());
		sb.append(';');
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," ");
			sb.append("$argc=").append((args.length-1)).append(";$args=");
			appendEscape(sb,argsImp);
			sb.append(";$ioru=empty($args)?$sender:$args");
			//appendEscape(sb,(args.length == 1 ? sender.getNick() : argsImp));
			sb.append(";$arg=explode(' ',$args);");
		}
		
		User[] users = channel.getUsers().toArray(new User[0]);
		sb.append("$randnick=");
		appendEscape(sb,users[new Random().nextInt(users.length)].getNick());
		sb.append(';');
		
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
		
		String ret = q.readWhole();
		q.close();
		
		try {
			JSONObject json = new JSONObject(ret);
			JSONObject error = json.optJSONObject("error");
			if (error != null)
				return error.getString("message");
			String safe_errors = json.optString("safe_errors", null);
			if (safe_errors != null)
				return safe_errors;
			String newdata = json.optString("data", null);
			if (factoid != null && newdata != null && (data == null || !newdata.contentEquals(data)))
				setData(factoid,newdata);
			return json.getString("output");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public String readFile(File file) {
		try {
			StringBuilder sb = new StringBuilder();
			FileInputStream fs = new FileInputStream(file);
			InputStreamReader sr = new InputStreamReader(fs, "UTF-8");
			char[] buffer = new char[64];
			int i = -1;
			while ((i = sr.read(buffer)) > 0)
				sb.append(buffer, 0, i);
			sr.close();
			fs.close();
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean writeFile(File file, CharSequence str) {
		try {
			FileOutputStream fs = new FileOutputStream(file);
			OutputStreamWriter sr = new OutputStreamWriter(fs, "UTF-8");
			sr.append(str);
			sr.flush();
			sr.close();
			fs.close();
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
	
	public class CmdPHP extends Command {
		public String command() {return "php";}
		public String help(Parameters params) {
			return "\nphp {code} - runs PHP code";
			}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount < 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String output = parse(null,params.bot,params.type,params.channel,params.sender,null,params.input,null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
		}
	}
}