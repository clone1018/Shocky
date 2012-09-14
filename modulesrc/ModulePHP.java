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
import pl.shockah.shocky.cmds.Command.EType;

public class ModulePHP extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "php";}
	public String identifier() {return "php";}
	public void onEnable() {
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Command.addCommands(this, cmd = new CmdPHP());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
		if (code == null) return "";
		
		StringBuilder sb = new StringBuilder("$channel = \""+channel.getName()+"\";$bot = \""+bot.getNick().replace("\"","\\\"")+"\";$sender = \""+sender.getNick().replace("\"","\\\"")+"\";");
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			sb.append("$argc = "+(args.length-1)+";$args = \""+argsImp.replace("\"","\\\"")+"\";$ioru = \""+(args.length-1 == 0 ? sender.getNick() : argsImp).replace("\"","\\\"")+"\";");
			sb.append("$arg = array(");
			for (int i = 1; i < args.length; i++) {
				if (i != 1) sb.append(",");
				sb.append("\""+args[i].replace("\"","\\\"")+"\"");
			}
			sb.append(");");
		}
		
		User[] users = channel.getUsers().toArray(new User[channel.getUsers().size()]);
		sb.append("$randnick = \""+users[new Random().nextInt(users.length)].getNick().replace("\"","\\\"")+"\";");
		
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
		public String help(PircBotX bot, EType type, Channel channel, User sender) {return "\nphp {code} - runs PHP code";}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			System.out.println(message);
			String output = parse(bot,type,channel,sender,StringTools.implode(args,1," "),null);
			if (output != null && !output.isEmpty()) {
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
			}
		}
	}
}