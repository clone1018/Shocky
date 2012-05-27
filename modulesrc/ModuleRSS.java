import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import javax.swing.Timer;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.FileLine;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.XMLObject;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleRSS extends Module {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
	private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z",Locale.US); 
	private static final SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final SimpleDateFormat sdf4 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	static {
		 sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		 sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
		 sdf3.setTimeZone(TimeZone.getTimeZone("GMT"));
		 sdf4.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	protected Command cmd;
	protected ArrayList<Feed> feeds = new ArrayList<Feed>();
	
	public String name() {return "rss";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdRSS());
		
		ArrayList<String> lines = FileLine.read(new File("data","rss.cfg"));
		for (int i = 0; i < lines.size(); i += 3) {
			String url = lines.get(i);
			long time = Long.parseLong(lines.get(i+1)); Date date = time <= 0 ? null : new Date(time);
			String[] channels = lines.get(i+2).split(" ");
			feeds.add(new Feed(url,date,channels));
		}
	}
	public void onDisable() {
		Command.removeCommands(cmd);
		for (Feed feed : feeds) feed.stop();
	}
	public void onDataSave() {
		ArrayList<String> lines = new ArrayList<String>();
		for (Feed feed : feeds) {
			lines.add(feed.getURL());
			lines.add(""+feed.getDate().getTime());
			lines.add(StringTools.implode(feed.channels.toArray(new String[feed.channels.size()])," "));
		}
		FileLine.write(new File("data","rss.cfg"),lines);
	}
	
	protected class Feed implements ActionListener {
		private final String url;
		private Date lastDate;
		private Timer timer = new Timer(5*60*1000,this);
		public ArrayList<String> channels = new ArrayList<String>();
		
		public Feed(String url, Date date, String... channels) {
			this.url = url;
			timer.start();
			this.channels.addAll(Arrays.asList(channels));
			lastDate = date;
			if (lastDate == null) update();
		}
		public boolean equals(Object o) {
			if (o == null) return false;
			if (o instanceof Feed) return ((Feed)o).getURL().equals(getURL());
			return false;
		}
		public int hashCode() {return url.hashCode();}
		public String getURL() {return url;}
		public Date getDate() {return lastDate;}
		
		public void actionPerformed(ActionEvent event) {
			timer.restart();
			update();
		}
		public void stop() {
			timer.stop();
		}
		public void update() {
			new Thread(){
				private Feed feed;
				
				public void start(Feed feed) {
					this.feed = feed;
					super.start();
				}
				
				public void run() {
					ArrayList<FeedEntry> ret = new ArrayList<FeedEntry>();
					Date newest = null;
					
					HTTPQuery q = new HTTPQuery(url,"GET");
					q.connect(true,false);
					XMLObject xBase = XMLObject.deserialize(q.readWhole());
					if (xBase.getAllElements().get(0).getName().equals("feed")) {
						ArrayList<XMLObject> xEntries = xBase.getElement("feed").get(0).getElement("entry");
						for (XMLObject xEntry : xEntries) {
							Date entryDate;
							
							String entryTitle = xEntry.getElement("title").get(0).getValue();
							String entryLink = xEntry.getElement("link").get(0).getAttribute("href");
							if (xEntry.getElement("updated").size() > 0) {
								String s = xEntry.getElement("updated").get(0).getValue();
								try {
									entryDate = sdf4.parse(s);
								} catch (ParseException ex) {
									entryDate = parseAtomDate(s);
								}
							} else if (xEntry.getElement("published").size() > 0) {
								String s = xEntry.getElement("published").get(0).getValue();
								try {
									entryDate = sdf4.parse(s);
								} catch (ParseException ex) {
									entryDate = parseAtomDate(s);
								}
							} else continue;
							
							if (newest == null || entryDate.after(newest)) newest = new Date(entryDate.getTime());
							if (lastDate == null) continue;
							if (entryDate.after(lastDate)) ret.add(new FeedEntry(entryTitle,entryLink,entryDate));
						}
					} else if (xBase.getAllElements().get(0).getName().equals("rss")) {
						ArrayList<XMLObject> xEntries = xBase.getElement("rss").get(0).getElement("channel").get(0).getElement("item");
						for (XMLObject xEntry : xEntries) {
							Date entryDate = null;
							
							String entryTitle = xEntry.getElement("title").get(0).getValue();
							String entryLink = xEntry.getElement("link").get(0).getValue();
							try {
								if (xEntry.getElement("pubDate").size() > 0) {
									entryDate = sdf.parse(xEntry.getElement("pubDate").get(0).getValue());
									if (entryDate == null) entryDate = sdf2.parse(xEntry.getElement("pubDate").get(0).getValue());
								} else if (xEntry.getElement("dc:date").size() > 0) {
									entryDate = sdf3.parse(xEntry.getElement("dc:date").get(0).getValue());
								} else continue;
							} catch (Exception e) {e.printStackTrace();}
							
							if (newest == null || entryDate.after(newest)) newest = new Date(entryDate.getTime());
							if (lastDate == null) continue;
							if (entryDate.after(lastDate)) ret.add(new FeedEntry(entryTitle,entryLink,entryDate));
						}
					}
					
					if (newest != null) lastDate = newest;
					if (!ret.isEmpty()) {
						Collections.sort(ret);
						newEntries(feed,ret);
					}
				}
			}.start(this);
		}
		
		public Date parseAtomDate(String s) {
			try {
				
				Calendar c = Calendar.getInstance();
				c.setTimeZone(TimeZone.getTimeZone("GMT"));
				c.setTime(sdf3.parse(s.substring(0,19)));
				
				s = s.substring(19);
				long t = c.getTime().getTime();
				int sign = s.charAt(0) == '+' ? 1 : -1;
				t += sign*Integer.parseInt(s.substring(1,3))*60*60*1000;
				t += sign*Integer.parseInt(s.substring(4,6))*60*1000;
				return new Date(t);
			} catch (Exception e) {e.printStackTrace();}
			return null;
		}
	}
	protected class FeedEntry implements Comparable<FeedEntry> {
		private final String title, url;
		private final Date date;
		
		public FeedEntry(String title, String url, Date date) {
			this.title = Utils.shortenAllUrls(StringTools.unescapeHTML(title));
			this.url = Utils.shortenUrl(url);
			this.date = date;
		}
		
		public String toString() {
			return Utils.timeAgo(date)+" | "+title+" | "+url;
		}

		public int compareTo(FeedEntry entry) {
			return date.compareTo(entry.date);
		}
	}
	
	public class CmdRSS extends Command {
		public String command() {return "rss";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			if (type == EType.Channel) {
				sb.append("rss [channel] - list feeds\n");
				sb.append("[r:op] rss add [channel] {url} - add a new feed\n");
				sb.append("[r:op] rss remove [channel] {url} - remove a feed");
			} else {
				sb.append("rss {channel} - list feeds\n");
				sb.append("[r:op] rss add {channel} {url} - add a new feed\n");
				sb.append("[r:op] rss remove {channel} {url} - remove a feed");
			}
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			String action = null, url = null;
			Channel c = null;
			callback.type = EType.Notice;
			
			if (type == EType.Channel) {
				if (args.length == 1) {
					c = channel;
				} else if (args.length == 2) {
					c = Shocky.getChannel(args[1]);
				} else if (args.length == 3) {
					action = args[1];
					c = channel;
					url = args[2];
				} else if (args.length == 4) {
					action = args[1];
					c = Shocky.getChannel(args[2]);
					url = args[3];
				}
				if (c == null) {
					callback.append("No such channel");
					return;
				}
			} else {
				if (args.length == 2) {
					c = Shocky.getChannel(args[1]);
				} else if (args.length == 4) {
					action = args[1];
					c = Shocky.getChannel(args[2]);
					url = args[3];
				}
				if (c == null) {
					callback.append("No such channel");
					return;
				}
			}
			if (action != null && !canUseOp(bot,type,c,sender)) return;
			
			if (action == null) {
				StringBuffer sb = new StringBuffer();
				for (Feed feed : feeds) {
					if (!feed.channels.contains(c.getName())) continue;
					if (sb.length() != 0) sb.append("\n");
					sb.append(feed.getURL());
				}
				callback.append(sb);
			} else if (action.equals("add")) {
				for (Feed feed : feeds) {
					if (feed.getURL().equals(url)) {
						if (feed.channels.contains(c.getName())) {
							callback.append("Feed already on channel's list");
						} else {
							feed.channels.add(c.getName());
							callback.append("Added");
						}
						return;
					}
				}
				feeds.add(new Feed(url,null,c.getName()));
				callback.append("Added");
			} else if (action.equals("remove")) {
				for (int i = 0; i < feeds.size(); i++) {
					Feed feed = feeds.get(i);
					if (feed.getURL().equals(url)) {
						if (feed.channels.contains(c.getName())) {
							feed.channels.remove(feed.channels.indexOf(c.getName()));
							if (feed.channels.isEmpty()) {
								feed.stop();
								feeds.remove(feeds.indexOf(feed));
							}
							callback.append("Removed");
						} else callback.append("Feed isn't on channel's list");
						return;
					}
				}
				callback.append("Feed isn't on channel's list");
			}
		}
		
		public Random getRandom(User user) {
			boolean loggedIn = user.getLogin() != null && !user.getLogin().isEmpty();
			return new Random((new Date().getTime()/(1000*60*60))+(loggedIn ? user.getLogin() : user.getHostmask()).hashCode());
		}
	}
	
	public void newEntries(Feed feed, ArrayList<FeedEntry> entries) {
		for (String s : feed.channels) {
			Channel channel = Shocky.getChannel(s);
			if (channel == null) return;
			PircBotX bot = Shocky.getBotForChannel(s);
			for (FeedEntry entry : entries) Shocky.sendChannel(bot,channel,Utils.mungeAllNicks(channel,"RSS: "+entry));
		}
	}
}