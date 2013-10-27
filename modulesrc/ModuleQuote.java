import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import pl.shockah.BinBuffer;
import pl.shockah.BinFile;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleQuote extends Module {
	protected Command cmd, cmdAdd, cmdRemove;
	private Map<String,List<Quote>> quotes = new HashMap<String,List<Quote>>();
	
	public String name() {return "quote";}
	public void onEnable(File dir) {
		dir = new File(dir,"quotes"); dir.mkdir();
		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) return;
			
			String channel = f.getName();
			BinBuffer binb = new BinFile(f).read(); binb.setPos(0);
			List<Quote> list = new ArrayList<Quote>();
			quotes.put(channel,list);
			while (binb.bytesLeft() > 0) {
				String[] nicks = binb.readUString().split(" ");
				String quote = binb.readUString();
				list.add(new Quote(nicks,quote));
			}
		}
		
		Command.addCommands(this, cmd = new CmdQuote(),cmdAdd = new CmdQuoteAdd(),cmdRemove = new CmdQuoteRemove());
		Command.addCommand(this, "q", cmd);
		Command.addCommand(this, "qadd", cmdAdd);
		Command.addCommand(this, "qdel", cmdRemove);
	}
	public void onDisable() {
		Command.removeCommands(cmd,cmdAdd,cmdRemove);
		quotes.clear();
	}
	public void onDataSave(File dir) {
		dir = new File(dir,"quotes"); dir.mkdir();
		BinBuffer binb = new BinBuffer();
		
		Iterator<Entry<String,List<Quote>>> it = quotes.entrySet().iterator();
		while (it.hasNext()) {
			binb.clear();
			Entry<String,List<Quote>> pair = it.next();
			
			List<Quote> quotes = pair.getValue();
			for (Quote quote : quotes) {
				binb.writeUString(StringTools.implode(quote.nicks," "));
				binb.writeUString(quote.quote);
			}
			
			binb.setPos(0);
			new BinFile(new File(dir,pair.getKey())).write(binb);
		}
	}
	
	public class CmdQuote extends Command {
		public String command() {return "quote";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("quote/q");
			sb.append("\nquote [channel] [nick] [id] - shows a quote");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount==0 && params.type != EType.Channel) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String aChannel = params.type == EType.Channel ? params.channel.getName() : null, aNick = null;
			int aId = 0;
			
			if (params.tokenCount>=1) {
				String par1 = params.nextParam();
				if (par1.charAt(0) == '#')
					aChannel = par1;
				else if (StringTools.isNumber(par1))
					aId = Integer.parseInt(par1);
				else
					aNick = par1;
			
				if (params.tokenCount>=2) {
					String par2 = params.nextParam();
					if (StringTools.isNumber(par2))
						aId = Integer.parseInt(par2);
					else
						aNick = par2;
					
					if (params.tokenCount==3) {
						String par3 = params.nextParam();
						if (aId == 0 && StringTools.isNumber(par3))
							aId = Integer.parseInt(par3);
					}
				}
			}
			
			if (aChannel == null) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			List<Quote> quoteList = quotes.get(aChannel);
			if (quoteList == null) {
				callback.append("No quotes found");
				return;
			}
			
			if (aNick != null)
				aNick = aNick.toLowerCase();
			ArrayList<Quote> list = new ArrayList<Quote>();
			for (Quote quote : quoteList)
				if (aNick == null || Arrays.binarySearch(quote.nicks,aNick) >= 0)
					list.add(quote);
			if (list.isEmpty()) {
				callback.append("No quotes found");
				return;
			}
			
			if (aId == 0) aId = new Random().nextInt(list.size()+1);
			if (aId < 0) aId = list.size()-aId-1;
			aId = Math.min(Math.max(aId,1),list.size()+1);
			
			String quote = Utils.mungeAllNicks(params.channel, 0, list.get(aId-1).quote);
			callback.append('[')
			.append(aChannel)
			.append(": ")
			.append(aId)
			.append('/')
			.append(list.size())
			.append("] ")
			.append(quote);
		}
	}
	public class CmdQuoteAdd extends Command {
		public String command() {return "quoteadd";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("quoteadd/qadd");
			sb.append("\nquoteadd {nick1};{nick2};(...) {quote} - adds a quote");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			if (params.tokenCount < 2) {
				callback.append(help(params));
				return;
			}
			
			String key = params.channel.getName();
			String[] nicks = params.nextParam().toLowerCase().split(";");
			String quote = params.getParams(0);
			
			List<Quote> list;
			if (quotes.containsKey(key))
				list = quotes.get(key);
			else {
				list = new ArrayList<Quote>();
				quotes.put(key,list);
			}
			list.add(new Quote(nicks,quote));
			callback.append("Done.");
		}
	}
	
	public class CmdQuoteRemove extends Command {
		public String command() {return "quoteremove";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("quoteremove/qdel");
			sb.append("\nquoteremove [channel] [nick] id - removes a quote");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			params.checkController();
			callback.type = EType.Notice;
			if (params.tokenCount==0 && params.type != EType.Channel) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String aChannel = params.type == EType.Channel ? params.channel.getName() : null, aNick = null;
			int aId = 0;
			
			if (params.tokenCount>=1) {
				String par1 = params.nextParam();
				if (par1.charAt(0) == '#')
					aChannel = par1;
				else if (StringTools.isNumber(par1))
					aId = Integer.parseInt(par1);
				else
					aNick = par1;
			
				if (params.tokenCount>=2) {
					String par2 = params.nextParam();
					if (StringTools.isNumber(par2))
						aId = Integer.parseInt(par2);
					else
						aNick = par2;
					
					if (params.tokenCount==3) {
						String par3 = params.nextParam();
						if (StringTools.isNumber(par3))
							aId = Integer.parseInt(par3);
					}
				}
			}
			if (aChannel == null) {
				callback.append(help(params));
				return;
			}
			
			if (aId == Integer.MIN_VALUE) {
				callback.append("Please specify a number.");
				return;
			}
			
			if (aNick != null) aNick = aNick.toLowerCase();
			ArrayList<Quote> list = new ArrayList<Quote>();
			for (Quote quote : quotes.get(aChannel)) if (aNick == null || Arrays.binarySearch(quote.nicks,aNick) >= 0) list.add(quote);
			if (list.isEmpty()) {
				callback.append("No quotes found");
				return;
			}
			
			if (aId < 0) aId = list.size()-aId-1;
			aId = Math.min(Math.max(aId,1),list.size()+1);
			
			Quote quote = list.remove(aId-1);
			for (Iterator<Quote> quoteIter = quotes.get(aChannel).iterator(); quoteIter.hasNext();) {
				Quote quote2 = quoteIter.next();
				if (quote2.equals(quote))
					quoteIter.remove();
			}
			callback.append("Removed quote: ").append(quote.quote);
		}
	}
	
	public class Quote {
		public final String[] nicks;
		public final String quote;
		
		public Quote(String[] nicks, String quote) {
			Arrays.sort(nicks);
			this.nicks = nicks;
			this.quote = quote;
		}
	}
}