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

public class ModulePHP extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "php";}
	public String identifier() {return "php";}
	public void onEnable() {
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Command.addCommands(this, cmd = new CmdPHP());
		Data.protectedKeys.add("php-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Map<Integer,Object> cache, PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
		if (code == null) return "";
		
		StringBuilder sb = new StringBuilder("$channel = '"+channel.getName().replace("'","\\'")+"';$bot = '"+bot.getNick().replace("'","\\'")+"';$sender = '"+sender.getNick().replace("'","\\'")+"';");
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			sb.append("$argc = "+(args.length-1)+";$args = '"+argsImp.replace("'","\\'")+"';$ioru = '"+(args.length-1 == 0 ? sender.getNick() : argsImp).replace("'","\\'")+"';");
			sb.append("$arg = explode(' ',$args);");
		}
		
		User[] users = channel.getUsers().toArray(new User[channel.getUsers().size()]);
		sb.append("$randnick = '"+users[new Random().nextInt(users.length)].getNick().replace("'","\\'")+"';");
		
		code = sb.toString()+code;
		
		HTTPQuery q = HTTPQuery.create(Data.forChannel(channel).getString("php-url"),HTTPQuery.Method.POST);
		q.connect(true,true);
		q.write(HTTPQuery.parseArgs("code",code));
		
		String ret = q.readWhole();
		q.close();
		
		return ret;
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
			
			String output = parse(null,params.bot,params.type,params.channel,params.sender,params.input,null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
		}
	}
}