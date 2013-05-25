import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.sql.Factoid;

public class ModulePython extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "python";}
	public String identifier() {return "py";}
	public void onEnable() {
		Data.config.setNotExists("python-url","http://eval.appspot.com/eval");
		Command.addCommands(this, cmd = new CmdPython());
		Data.protectedKeys.add("python-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Map<Integer,Object> cache, PircBotX bot, EType type, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		StringBuilder sb = new StringBuilder("channel=");
		appendEscape(sb,channel.getName());
		sb.append(";bot=");
		appendEscape(sb,bot.getNick());
		sb.append(";sender=");
		appendEscape(sb,sender.getNick());
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
		
		User[] users = channel.getUsers().toArray(new User[channel.getUsers().size()]);
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
	
	public class CmdPython extends Command {
		public String command() {return "python";}
		public String help(Parameters params) {
			return "python\npython {code} - runs Python code";
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