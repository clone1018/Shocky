import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.XMLObject;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleWolframAlpha extends Module {
	protected Command cmd;
	
	public String getResult(Channel channel, String query) {
		String apiKey = Data.forChannel(channel).getString("wolfram-apikey");
		if (apiKey.isEmpty()) return ">>> WolframAlpha module can't be used without setting up an API key. Get one at http://products.wolframalpha.com/developers/ <<<";
		
		try {
			HTTPQuery q = HTTPQuery.create("http://api.wolframalpha.com/v2/query?appid="+apiKey+"&format=Plaintext&input="+URLEncoder.encode(query,"UTF8"));
			q.connect(true,false);
			XMLObject xBase = XMLObject.deserialize(q.readWhole());
			q.close();
			
			if (xBase.getBaseElement().getAttribute("error").equals("true")) return null;
			if (xBase.getBaseElement().getAttribute("success").equals("false")) return "No result | "+Utils.shortenUrl("http://www.wolframalpha.com/input/?i="+URLEncoder.encode(query,"UTF8"));
			
			ArrayList<String> parts = new ArrayList<String>();
			parts.add(Utils.shortenUrl("http://www.wolframalpha.com/input/?i="+URLEncoder.encode(query,"UTF8")));
			
			ArrayList<XMLObject> xPods = xBase.getElements("queryresult").get(0).getElements("pod");
			for (XMLObject xPod : xPods) {
				if (!"true".equals(xPod.getAttribute("primary")) && !xPod.getAttribute("title").equals("Alternate form")) continue;
				
				StringBuilder sb = new StringBuilder();
				ArrayList<XMLObject> xSubpods = xPod.getElements("subpod");
				for (XMLObject xSubpod : xSubpods) {
					if (xSubpod.getElements("plaintext").isEmpty()) continue;
					if (sb.length() != 0) sb.append("  ");
					sb.append(xSubpod.getElements("plaintext").get(0).getValue());
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
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdWolframAlpha());
		Command.addCommand(this, "wa", cmd);
		Data.config.setNotExists("wolfram-apikey","");
		Data.protectedKeys.add("wolfram-apikey");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdWolframAlpha extends Command {
		public String command() {return "wolframalpha";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("wolframalpha/wa");
			sb.append("\nwolframalpha {query} - shows WolframAlpha's result");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			String result = getResult(params.channel, params.input);
			if (result == null) return;
			result = Utils.mungeAllNicks(params.channel,0,result,params.sender.getNick());
			callback.append(result);
		}
	}
}