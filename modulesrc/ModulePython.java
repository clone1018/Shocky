import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;

public class ModulePython extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "python";}
	public String identifier() {return "py";}
	public void onEnable(File dir) {
		Data.config.setNotExists("python-url","http://eval.appspot.com/eval");
		Command.addCommands(this, cmd = new CmdPython());
		Data.protectedKeys.add("python-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		User[] users;
		StringBuilder sb = new StringBuilder();
		if (channel == null)
			users = new User[]{bot.getUserBot(),sender};
		else {
			users = channel.getUsers().toArray(new User[0]);
			sb.append("channel=");
			appendEscape(sb,channel.getName());
			sb.append(';');
		}
		sb.append("bot=");
		appendEscape(sb,bot.getNick());
		sb.append(";sender=");
		appendEscape(sb,sender.getNick());
		sb.append(";host=");
		appendEscape(sb,sender.getHostmask());
		sb.append(';');
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," ");
			sb.append("argc=").append(args.length-1).append(";args=");
			appendEscape(sb,argsImp);
			sb.append(";ioru=");
			appendEscape(sb,(args.length == 1 ? sender.getNick() : argsImp));
			sb.append(';');
			sb.append("arg=[");
			for (int i = 1; i < args.length; i++) {
				if (i != 1) sb.append(',');
				appendEscape(sb,args[i]);
			}
			sb.append("];");
		}
		
		sb.append("randnick=");
		appendEscape(sb,users[new Random().nextInt(users.length)].getNick());
		sb.append(';');
		
		sb.append(code);
		
		HTTPQuery q = HTTPQuery.create(Data.forChannel(channel).getString("python-url")+'?'+HTTPQuery.parseArgs("statement",sb.toString()));
		q.connect(true,false);
		
		sb = new StringBuilder();
		ArrayList<String> result = q.readLines();
		q.close();
		if (result.size()>0 && result.get(0).contentEquals("Traceback (most recent call last):"))
			return result.get(result.size()-1);
		
		for (String line : result) {
			if (sb.length() != 0) sb.append('\n');
			sb.append(line);
		}
		
		return sb.toString();
	}
	
	public class CmdPython extends ScriptCommand {
		public String command() {return "python";}
		public String help(Parameters params) {
			return "python\npython {code} - runs Python code";
		}
	}
}