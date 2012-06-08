import java.net.URLEncoder;
import java.util.ArrayList;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.XMLObject;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleWolframAlpha extends Module {
	protected Command cmd;
	
	public String getResult(String query) {
		String apiKey = Data.config.getString("wolfram-apikey");
		if (apiKey.isEmpty()) return ">>> WolframAlpha module can't be used without setting up an API key. Get one at http://products.wolframalpha.com/developers/ <<<";
		
		try {
			HTTPQuery q = new HTTPQuery("http://api.wolframalpha.com/v2/query?appid="+apiKey+"&format=Plaintext&input="+URLEncoder.encode(query,"UTF8"));
			q.connect(true,false);
			XMLObject xBase = XMLObject.deserialize(q.readWhole());
			q.close();
			
			if (xBase.getBaseElement().getAttribute("error").equals("true")) return null;
			if (xBase.getBaseElement().getAttribute("success").equals("false")) return "No result | "+Utils.shortenUrl("http://www.wolframalpha.com/input/?i="+URLEncoder.encode(query,"UTF8"));
			
			ArrayList<String> parts = new ArrayList<String>();
			parts.add(Utils.shortenUrl("http://www.wolframalpha.com/input/?i="+URLEncoder.encode(query,"UTF8")));
			
			ArrayList<XMLObject> xPods = xBase.getElement("queryresult").get(0).getElement("pod");
			for (XMLObject xPod : xPods) {
				if (!"true".equals(xPod.getAttribute("primary")) && !xPod.getAttribute("title").equals("Alternate form")) continue;
				
				StringBuilder sb = new StringBuilder();
				ArrayList<XMLObject> xSubpods = xPod.getElement("subpod");
				for (XMLObject xSubpod : xSubpods) {
					if (xSubpod.getElement("plaintext").isEmpty()) continue;
					if (sb.length() != 0) sb.append("  ");
					sb.append(xSubpod.getElement("plaintext").get(0).getValue());
				}
				if (sb.length() != 0) parts.add(Colors.BOLD+xPod.getAttribute("title")+Colors.NORMAL+": "+sb.toString().replace("\n"," "));
			}
			
			StringBuilder sb = new StringBuilder();
			for (String part : parts) {
				if (sb.length() != 0) sb.append(" | ");
				sb.append(part);
			}
			
			return StringTools.limitLength(sb.toString());
		} catch (Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	public String name() {return "wolfram";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdWolframAlpha());
		Command.addCommand("wa", cmd);
		Data.config.setNotExists("wolfram-apikey","");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdWolframAlpha extends Command {
		public String command() {return "wolframalpha";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("wolframalpha/wa");
			sb.append("\nwolframalpha {query} - shows WolframAlpha's result");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				if (i != 1) sb.append(" ");
				sb.append(args[i]);
			}
			
			String result = getResult(sb.toString());
			if (result == null) return;
			result = Utils.mungeAllNicks(channel,result,sender.getNick());
			callback.append(result);
		}
	}
}