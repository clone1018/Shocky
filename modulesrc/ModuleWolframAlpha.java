import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.LogManager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.ILua;

public class ModuleWolframAlpha extends Module implements ILua {
	protected Command cmd;
	
	public Map<String,String> getResultMap(String apiKey, String query, String xquery) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		XPathFactory xpathFactory = XPathFactory.newInstance();
		
		try {
			query = URLEncoder.encode(query,"UTF8");
			DocumentBuilder builder = factory.newDocumentBuilder();
			HTTPQuery q = HTTPQuery.create("http://api.wolframalpha.com/v2/query?appid="+apiKey+"&format=Plaintext&input="+query);
			Document doc;
			try {
				q.connect(true,false);
				doc = builder.parse(q.getConnection().getInputStream());
			} finally {
				q.close();
			}
			
			Element docElement = doc.getDocumentElement();
			boolean error = Boolean.parseBoolean(docElement.getAttribute("error"));
			boolean success = Boolean.parseBoolean(docElement.getAttribute("success"));
			if (error || !success)
				return null;
			
			Map<String,String> parts = new LinkedHashMap<String,String>();
			XPath xpath = xpathFactory.newXPath();
			XPathExpression xptitle = xpath.compile("./@title");
			NodeList xe = (NodeList) xpath.evaluate(xquery,doc,XPathConstants.NODESET);
			for (int i = 0; i < xe.getLength();++i) {
				String text = StringTools.deleteWhitespace(xe.item(i).getTextContent()).trim();
				if (text.length() > 0)
					parts.put(xptitle.evaluate(xe.item(i)),text);
			}
			return parts;
		} catch (Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	public String getResult(Channel channel, String query) {
		String apiKey = Data.forChannel(channel).getString("wolfram-apikey");
		if (apiKey.isEmpty()) return ">>> WolframAlpha module can't be used without setting up an API key. Get one at http://products.wolframalpha.com/developers/ <<<";
		
		Map<String,String> resultMap = getResultMap(apiKey,query,"//queryresult/pod[(@primary='true') or (@title='Alternate form')]");
			
		ArrayList<String> parts = new ArrayList<String>();
		if (resultMap == null)
			parts.add("No result");
		try {query = URLEncoder.encode(query,"UTF8");} catch (UnsupportedEncodingException e) {}
		parts.add(Utils.shortenUrl("http://www.wolframalpha.com/input/?i="+query));
		if (resultMap != null) {
			for (Entry<String, String> part : resultMap.entrySet()) {
				parts.add(Colors.BOLD+part.getKey()+Colors.NORMAL+": "+part.getValue());
			}
		}
			
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (sb.length() != 0) sb.append(" | ");
			sb.append(part);
		}
			
		return StringTools.limitLength(sb);
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
			result = Utils.mungeAllNicks(params.channel,0,result,params.sender);
			callback.append(result);
		}
	}
	
	public class Function extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			String apiKey = Data.config.getString("wolfram-apikey");
			if (apiKey.isEmpty())
				return NIL;
			Map<String,String> result = getResultMap(apiKey,arg.checkjstring(),"//queryresult/pod");
			LuaValue[] values = new LuaValue[result.size()<<1];
			int i = 0;
			for (Entry<String,String> item : result.entrySet()) {
				values[i++] = valueOf(item.getKey());
				values[i++] = valueOf(item.getValue());
			}
			return tableOf(values);
		}
	}

	@Override
	public void setupLua(LuaTable env) {
		try {
			Class.forName("pl.shockah.HTTPQuery");
			Class.forName("pl.shockah.HTTPQuery$Method");
			LogManager.getLoggingMXBean();
		} catch (Exception e) {
			e.printStackTrace();
		}
		env.rawset("wa", new Function());
	}
}