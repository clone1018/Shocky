import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import pl.shockah.HTTPQuery;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleQuestionParty extends Module {
	protected Command cmd;
	public Pattern headerPattern = Pattern.compile("<h1>(.+?)</h1>");
	public Pattern itemPattern = Pattern.compile("<li>(.+)");
	
	public String getQ() {
		HTTPQuery q;
		try {
			q = HTTPQuery.create("http://questionparty.com/questions/rand/");
		} catch (MalformedURLException e1) {return null;}
		
		q.connect(true,false);
		String html = null;
		try {
			html = q.readWhole();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			q.close();
		}
		
		if (html == null)
			return null;
		
		Matcher matcher = headerPattern.matcher(html);
		if(!matcher.find())
			return "";
		String question = matcher.group(1);
		
		matcher = itemPattern.matcher(html);
		ArrayList<String> answers = new ArrayList<String>();
		while (matcher.find()) {
			answers.add(matcher.group(1));
		}
		
		StringBuilder sb = new StringBuilder();
		int i = Math.min(answers.size(),5);
		Random rnd = new Random();
		while (i-- > 0) {
			if (sb.length() != 0) sb.append(" | ");
			int n = rnd.nextInt(answers.size());
			sb.append(answers.get(n).trim()); answers.remove(n);
		}
		sb.insert(0, '\n');
		sb.insert(0, question);
		return StringEscapeUtils.unescapeHtml4(sb.toString());
	}
	
	public String name() {return "questionparty";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdQuestionParty());
		Command.addCommand(this, "qparty", cmd);
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdQuestionParty extends Command {
		public String command() {return "questionparty";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("questionparty/qparty");
			sb.append("\nquestionparty - random question with up to 5 random answers");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			callback.append(getQ());
		}
	}
}