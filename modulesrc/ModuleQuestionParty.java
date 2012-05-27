import java.util.ArrayList;
import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleQuestionParty extends Module {
	protected Command cmd;
	
	public String getQ() {
		HTTPQuery q = new HTTPQuery("http://questionparty.com/questions/rand/");
		
		q.connect(true,false);
		ArrayList<String> list = q.read();
		q.close();
		
		String question = ""; ArrayList<String> answers = new ArrayList<String>();
		for (String s : list) {
			s = StringTools.unescapeHTML(s.trim());
			if (s.startsWith("<h1>")) question = s.substring(4,s.length()-5);
			if (s.startsWith("<li>")) answers.add(s.substring(4));
		}
		
		StringBuilder sb = new StringBuilder();
		int i = Math.min(answers.size(),5);
		Random rnd = new Random();
		while (i-- > 0) {
			if (sb.length() != 0) sb.append(" | ");
			int n = rnd.nextInt(answers.size());
			sb.append(answers.get(n)); answers.remove(n);
		}
		return question+"\n"+sb;
	}
	
	public String name() {return "questionparty";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdQuestionParty());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdQuestionParty extends Command {
		public String command() {return "questionparty";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("questionparty/qparty/qp");
			sb.append("\nquestionparty - random question with up to 5 random answers");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("qparty") || cmd.equals("qp");}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			callback.append(getQ());
		}
	}
}