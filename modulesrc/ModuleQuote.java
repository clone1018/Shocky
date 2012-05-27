import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.BinBuffer;
import pl.shockah.BinFile;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleQuote extends Module {
	protected Command cmd, cmdAdd, cmdRemove;
	private HashMap<String,ArrayList<Quote>> quotes = new HashMap<String,ArrayList<Quote>>();
	
	public String name() {return "quote";}
	public void onEnable() {
		File dir = new File("data","quotes"); dir.mkdir();
		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) return;
			
			String channel = f.getName();
			BinBuffer binb = new BinFile(f).read(); binb.setPos(0);
			quotes.put(channel,new ArrayList<Quote>());
			while (binb.bytesLeft() > 0) {
				String[] nicks = binb.readUString().split(" ");
				String quote = binb.readUString();
				quotes.get(channel).add(new Quote(nicks,quote));
			}
		}
		
		Command.addCommands(cmd = new CmdQuote(),cmdAdd = new CmdQuoteAdd(),cmdRemove = new CmdQuoteRemove());
	}
	public void onDisable() {
		Command.removeCommands(cmd,cmdAdd,cmdRemove);
	}
	public void onDataSave() {
		File dir = new File("data","quotes"); dir.mkdir();
		BinBuffer binb = new BinBuffer();
		
		Iterator<Entry<String,ArrayList<Quote>>> it = quotes.entrySet().iterator();
		while (it.hasNext()) {
			binb.clear();
			Entry<String,ArrayList<Quote>> pair = it.next();
			
			ArrayList<Quote> quotes = pair.getValue();
			for (Quote quote : quotes) {
				binb.writeUString(StringTools.implode(quote.nicks.toArray(new String[quote.nicks.size()])," "));
				binb.writeUString(quote.quote);
			}
			
			binb.setPos(0);
			new BinFile(new File(dir,pair.getKey())).write(binb);
		}
	}
	
	public class CmdQuote extends Command {
		public String command() {return "quote";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("quote/q");
			sb.append("\nquote [channel] [nick] [id] - shows a quote");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			if (type != EType.Channel) return false;
			return cmd.equals(command()) || cmd.equals("q");
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length == 1 && type != EType.Channel) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String aChannel = type == EType.Channel ? channel.getName() : null, aNick = null;
			int aId = 0;
			
			if (args.length == 2) {
				if (args[1].charAt(0) == '#') aChannel = args[1];
				else if (StringTools.isNumber(args[1])) aId = Integer.parseInt(args[1]);
				else aNick = args[1];
			} else if (args.length == 3) {
				if (args[1].charAt(0) == '#') {
					aChannel = args[1];
					if (StringTools.isNumber(args[2])) aId = Integer.parseInt(args[2]);
					else aNick = args[2];
				} else if (StringTools.isNumber(args[2])) {
					aId = Integer.parseInt(args[2]);
					aNick = args[1];
				}
			} else if (args.length == 4) {
				aChannel = args[1];
				aNick = args[2];
				aId = Integer.parseInt(args[3]);
			}
			if (aChannel == null) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			if (aNick != null) aNick = aNick.toLowerCase();
			ArrayList<Quote> list = new ArrayList<Quote>();
			for (Quote quote : quotes.get(aChannel)) if (aNick == null || quote.nicks.contains(aNick)) list.add(quote);
			if (list.isEmpty()) {
				callback.append("No quotes found");
				return;
			}
			
			if (aId == 0) aId = new Random().nextInt(list.size()+1);
			if (aId < 0) aId = list.size()-aId-1;
			aId = Math.min(Math.max(aId,1),list.size()+1);
			
			String quote = Utils.mungeAllNicks(channel, list.get(aId-1).quote);
			callback.append("["+aChannel+": "+(aId)+"/"+(list.size())+"] "+quote);
		}
	}
	public class CmdQuoteAdd extends Command {
		public String command() {return "quoteadd";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("quoteadd/qadd");
			sb.append("\nquoteadd {nick1};{nick2};(...) {quote} - adds a quote");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			if (type != EType.Channel) return false;
			return cmd.equals(command()) || cmd.equals("qadd");
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			callback.type = EType.Notice;
			if (args.length < 3) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String[] nicks = args[1].toLowerCase().split(Pattern.quote(";"));
			String quote = StringTools.implode(args,2," ");
			if (!quotes.containsKey(channel.getName())) quotes.put(channel.getName(),new ArrayList<Quote>());
			quotes.get(channel.getName()).add(new Quote(nicks,quote));
			callback.append("Done.");
		}
	}
	
	public class CmdQuoteRemove extends Command {
		public String command() {return "quoteremove";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("quoteremove/qdel");
			sb.append("\nquoteremove [channel] [nick] id - removes a quote");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			if (type != EType.Channel) return false;
			return cmd.equals(command()) || cmd.equals("qdel");
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (!canUseController(bot,type,sender)) return;
			String[] args = message.split(" ");
			callback.type = EType.Notice;
			if (args.length == 1 && type != EType.Channel) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String aChannel = type == EType.Channel ? channel.getName() : null, aNick = null;
			int aId = Integer.MIN_VALUE;
			
			if (args.length == 2) {
				if (args[1].charAt(0) == '#') aChannel = args[1];
				else if (StringTools.isNumber(args[1])) aId = Integer.parseInt(args[1]);
				else aNick = args[1];
			} else if (args.length == 3) {
				if (args[1].charAt(0) == '#') {
					aChannel = args[1];
					if (StringTools.isNumber(args[2])) aId = Integer.parseInt(args[2]);
					else aNick = args[2];
				} else if (StringTools.isNumber(args[2])) {
					aId = Integer.parseInt(args[2]);
					aNick = args[1];
				}
			} else if (args.length == 4) {
				aChannel = args[1];
				aNick = args[2];
				aId = Integer.parseInt(args[3]);
			}
			if (aChannel == null) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			if (aId == Integer.MIN_VALUE) {
				callback.append("Please specify a number.");
				return;
			}
			
			if (aNick != null) aNick = aNick.toLowerCase();
			ArrayList<Quote> list = new ArrayList<Quote>();
			for (Quote quote : quotes.get(aChannel)) if (aNick == null || quote.nicks.contains(aNick)) list.add(quote);
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
			callback.append("Removed quote: "+quote.quote);
		}
	}
	
	public class Quote {
		public final ArrayList<String> nicks = new ArrayList<String>();
		public final String quote;
		
		public Quote(String[] nicks, String quote) {
			this.nicks.addAll(Arrays.asList(nicks));
			this.quote = quote;
		}
	}
}