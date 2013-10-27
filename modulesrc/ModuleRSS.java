import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.shockah.FileLine;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleRSS extends Module {
	private static final String[] rssFormats = new String[] {
		"EEE, dd MMM yyyy HH:mm:ss z",
		"EEE, dd MMM yyyy HH:mm:ss Z"};
	private static final String[] atomFormats = new String[] {
		"yyyy-MM-dd'T'HH:mm:ss'Z'",
		"yyyy-MM-dd'T'HH:mm:ss"};
	
	protected Command cmd;
	protected List<Feed> feeds = Collections.synchronizedList(new LinkedList<Feed>());
	private Thread updater;
	
	public String name() {return "rss";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdRSS());
		
		ArrayList<String> lines = FileLine.read(new File(dir,"rss.cfg"));
		int n = 4;
		for (int i = 0; i < lines.size(); i += n) {
			try {
				URL url = new URL(lines.get(i));
				long time = Long.parseLong(lines.get(i+1));
				Date date = time <= 0 ? null : new Date(time);
				long interval;
				String[] channels;
				try {
					interval = Long.parseLong(lines.get(i+2));
					channels = lines.get(i+3).split(" ");
				} catch (NumberFormatException e) {
					n = 3;
					interval = (5*60*1000);
					channels = lines.get(i+2).split(" ");
				}
				feeds.add(new Feed(url,interval,date,channels));
			} catch (MalformedURLException e) {
				continue;
			}
		}
		startUpdater();
	}
	public void onDisable() {
		Command.removeCommands(cmd);
		stopUpdater();
	}
	public void onDataSave(File dir) {
		ArrayList<String> lines = new ArrayList<String>();
		for (Feed feed : feeds) {
			lines.add(feed.getURL().toString());
			lines.add(Long.toString((feed.getDate()!=null)?feed.getDate().getTime():0L));
			lines.add(Long.toString(feed.getInterval()));
			lines.add(StringTools.implode(feed.channels.toArray(new String[feed.channels.size()])," "));
		}
		FileLine.write(new File(dir,"rss.cfg"),lines);
	}
	
	public void stopUpdater() {
		if (updater == null)
			return;
		while (updater.isAlive()) {
			updater.interrupt();
			try {
				updater.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void startUpdater() {
		if (updater != null && updater.isAlive())
			stopUpdater();
		updater = new Thread(new FeedUpdater(),"rss");
		updater.setDaemon(true);
		updater.start();
	}
	
	private class FeedUpdater implements Runnable {
		@Override
		public void run() {
			Feed f = null;
			try {
				while(true) {
					if (feeds.isEmpty())
						return;
					if (feeds.size() > 1)
						Collections.sort(feeds);
					f = feeds.iterator().next();
					long sleep = f.getNext()-System.currentTimeMillis();
					if (sleep > 0)
						Thread.sleep(sleep);
					f.update();
				}
			} catch (InterruptedException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected class Feed implements Runnable, Comparable<Feed> {
		private final URL url;
		private final long interval;
		private Date lastDate;
		private long nextRun = System.currentTimeMillis();
		public ArrayList<String> channels = new ArrayList<String>();
		
		public Feed(URL url, long interval, Date date, String... channels) {
			this.url = url;
			this.interval = interval;
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
		public URL getURL() {return url;}
		public long getInterval() {return interval;}
		public Date getDate() {return lastDate;}
		public long getNext() {return nextRun;}
		
		public void update() {
			System.out.print("Updating feed: ");
			System.out.println(url);
			nextRun = System.currentTimeMillis()+interval;
			new Thread(this,url.getHost()).start();
		}
		
		public Date parseAtomDate(String s) {
			Date d = parseDate(atomFormats,s.substring(0,19));
			if (d == null)
				return null;
			
			try {
				Calendar c = Calendar.getInstance();
				c.setTimeZone(TimeZone.getTimeZone("GMT"));
				c.setTime(d);
				
				s = s.substring(19);
				long t = c.getTime().getTime();
				int sign = s.charAt(0) == '+' ? 1 : -1;
				t += sign*Integer.parseInt(s.substring(1,3))*(60*60*1000);
				t += sign*Integer.parseInt(s.substring(4,6))*(60*1000);
				return new Date(t);
			} catch (Exception e) {e.printStackTrace();}
			return null;
		}
		
		public Date parseDate(String[] formats, String s) {
			for (int i=0; i < formats.length; ++i) {
				DateFormat sdf = new SimpleDateFormat(formats[i]);
				try {
					return sdf.parse(s);
				} catch (ParseException e) {}
			}
			return null;
		}

		@Override
		public void run() {
			HTTPQuery q = new HTTPQuery(url);
			q.connect(true,false);
			q.setUserAgentFirefox();
			
			Document xBase;
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				xBase = builder.parse(q.getConnection().getInputStream());
			} catch (Exception e1) {
				e1.printStackTrace();
				return;
			} finally {
				q.close();
			}
			
			List<FeedEntry> ret = new LinkedList<FeedEntry>();
			Date newest = null;
			try {
				XPathFactory xpathFactory = XPathFactory.newInstance();
				XPath xpath = xpathFactory.newXPath();
				XPathExpression xTitle = xpath.compile("./title");
				NodeList feeds = (NodeList) xpath.evaluate("//feed/entry", xBase, XPathConstants.NODESET);
				if (feeds.getLength() > 0) {
					XPathExpression xHref = xpath.compile("./link/@href");
					XPathExpression xDateAtom = xpath.compile("./updated|./published");
					String channel = xpath.evaluate("//feed/title", xBase);
					for (int i = 0; i < feeds.getLength(); ++i) {
						Node xEntry = feeds.item(i);
						Date entryDate;
					
						String entryTitle = xTitle.evaluate(xEntry);
						String entryLink = xHref.evaluate(xEntry);
						String date = xDateAtom.evaluate(xEntry);
						if (date != null) {
							entryDate = parseDate(atomFormats, date);
							if (entryDate == null)
								entryDate = parseAtomDate(date);
						} else continue;
					
						if (newest == null || entryDate.after(newest))
							newest = entryDate;
						if (lastDate == null) continue;
						if (entryDate.after(lastDate)) ret.add(new FeedEntry(channel,entryTitle,entryLink,entryDate));
					}
				} else {
					feeds = (NodeList) xpath.evaluate("//rss/channel/item", xBase, XPathConstants.NODESET);
					if (feeds.getLength() > 0) {
						XPathExpression xLink = xpath.compile("./link");
						XPathExpression xDateRSS = xpath.compile("./pubDate|./dc:date");
						String channel = xpath.evaluate("//rss/channel/title", xBase);
						for (int i = 0; i < feeds.getLength(); ++i) {
							Node xEntry = feeds.item(i);
							String entryTitle = xTitle.evaluate(xEntry);
							String entryLink = xLink.evaluate(xEntry);
							String date = xDateRSS.evaluate(xEntry);
							if (date == null)
								continue;
							
							Date entryDate = parseDate(rssFormats, date);
							if (entryDate == null)
								continue;
							
							if (newest == null || entryDate.after(newest))
								newest = entryDate;
							if (lastDate == null) continue;
							if (entryDate.after(lastDate))
								ret.add(new FeedEntry(channel,entryTitle,entryLink,entryDate));
						}
					}
				}
			
				if (newest != null) lastDate = newest;
				if (!ret.isEmpty()) {
					Collections.sort(ret);
					newEntries(this,ret);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		@Override
		public int compareTo(Feed o) {
			if (nextRun > o.nextRun)
				return 1;
			if (nextRun < o.nextRun)
				return -1;
			return 0;
		}
	}
	protected class FeedEntry implements Comparable<FeedEntry> {
		private String channel, title, url;
		private Date date;
		private boolean needsShorten = true;
		
		public FeedEntry(String channel, String title, String url, Date date) {
			this.channel = channel;
			this.title = title;
			this.url = url;
			this.date = date;
		}
		
		public String toString() {
			if (needsShorten) {
				needsShorten = false;
				title = Utils.shortenAllUrls(StringTools.unescapeHTML(title));
				url = Utils.shortenUrl(url);
			}
			return channel+' '+Utils.timeAgo(date)+" | "+title+" | "+url;
		}

		public int compareTo(FeedEntry entry) {
			return date.compareTo(entry.date);
		}
	}
	
	public class CmdRSS extends Command {
		public String command() {return "rss";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			if (params.type == EType.Channel) {
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
		
		public void doCommand(Parameters params, CommandCallback callback) {
			String action = null;
			URL url = null;
			long interval = (5*60*1000);
			Channel c = null;
			callback.type = EType.Notice;
			
			try {
				if (params.type == EType.Channel) {
					if (params.tokenCount == 0) {
						c = params.channel;
					} else if (params.tokenCount == 1) {
						c = Shocky.getChannel(params.nextParam());
					} else if (params.tokenCount == 2) {
						action = params.nextParam();
						c = params.channel;
						url = new URL(params.nextParam());
					} else if (params.tokenCount == 3) {
						action = params.nextParam();
						String s = params.nextParam();
						if (s.startsWith("#"))
							c = Shocky.getChannel(s);
						else {
							interval = Utils.parseInterval(s);
							c = params.channel;
						}
						url = new URL(params.nextParam());
					} else if (params.tokenCount == 4) {
						action = params.nextParam();
						interval = Utils.parseInterval(params.nextParam());
						c = Shocky.getChannel(params.nextParam());
						url = new URL(params.nextParam());
					}
					if (c == null) {
						callback.append("No such channel");
						return;
					}
				} else {
					if (params.tokenCount == 1) {
						c = Shocky.getChannel(params.nextParam());
					} else if (params.tokenCount == 3) {
						action = params.nextParam();
						c = Shocky.getChannel(params.nextParam());
						url = new URL(params.nextParam());
					}
					if (c == null) {
						callback.append("No such channel");
						return;
					}
				}
			} catch (MalformedURLException e) {
				callback.append("Malformed URL");
				return;
			}
			if (action != null) params.checkOp();
			
			if (action == null) {
				StringBuffer sb = new StringBuffer();
				for (Feed feed : feeds) {
					if (!feed.channels.contains(c.getName())) continue;
					if (sb.length() != 0) sb.append("\n");
					sb.append(feed.getURL()).append(' ').append(Utils.timeAgo(feed.getInterval()/1000));
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
				if (interval < 300)
					interval = 300;
				interval*=1000;
				feeds.add(new Feed(url,interval,null,c.getName()));
				callback.append("Added");
				startUpdater();
			} else if (action.equals("remove")) {
				Iterator<Feed> iter = feeds.iterator();
				boolean modified = false;
				try {
					while (iter.hasNext()) {
						Feed feed = iter.next();
						if (feed.getURL().equals(url)) {
							if (feed.channels.contains(c.getName())) {
								feed.channels.remove(feed.channels.indexOf(c.getName()));
								if (feed.channels.isEmpty()) {
									iter.remove();
									modified = true;
								}
								callback.append("Removed");
							} else callback.append("Feed isn't on channel's list");
							return;
						}
					}
					callback.append("Feed isn't on channel's list");
				} finally {
					if (modified)
						startUpdater();
				}
			}
		}
	}
	
	public void newEntries(Feed feed, Iterable<FeedEntry> entries) throws InterruptedException {
		for (String s : feed.channels) {
			Channel channel = Shocky.getChannel(s);
			if (channel == null) return;
			PircBotX bot = channel.getBot();
			for (FeedEntry entry : entries) {
				Shocky.sendChannel(bot,channel,Utils.mungeAllNicks(channel,0,"RSS: "+entry));
				Thread.sleep(2000);
			}
		}
	}
}