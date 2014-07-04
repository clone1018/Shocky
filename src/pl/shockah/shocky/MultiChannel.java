package pl.shockah.shocky;

import java.io.IOException;
import java.util.*;

import org.pircbotx.*;
import org.pircbotx.exception.*;

import pl.shockah.Reflection;

public class MultiChannel {
	private static List<PircBotX> botList = new LinkedList<PircBotX>();
	protected static String channelPrefixes = null;
	
	public static Channel get(String name) {
		name = name.toLowerCase();
		synchronized (botList) {
			for (PircBotX entry : botList) {
				for (Channel channel : entry.getChannels()) {
					if (channel.getName().equalsIgnoreCase(name))
						return channel;
				}
			}
		}
		return null;
	}
	
	private static PircBotX createBot() {
		PircBotX bot = Shocky.getBotManager().createBot(Data.config.getString("main-server"));
		bot.setVersion(Data.config.getString("main-version"));
		if (!connect(bot))
			return null;
		if (channelPrefixes == null)
			channelPrefixes = Reflection.getPrivateValue(PircBotX.class,"channelPrefixes",bot);
		botList.add(bot);
		return bot;
	}
	
	public static boolean connect(PircBotX bot) {
		String server = Data.config.getString("main-server");
		try {
			bot.connect(server.contentEquals("localhost") ? null : server);
			if (!bot.isConnected())
				return false;
			
			startWebServer(bot);
			
			if (!Data.config.getString("main-nickservpass").isEmpty())
				bot.identify(Data.config.getString("main-nickservpass"));
			return true;
		} catch (NickAlreadyInUseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IrcException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void startWebServer(PircBotX bot) {
		try {
			if (!WebServer.exists() && WebServer.start(bot.getInetAddress().getHostAddress(),8000))
				System.out.println("--- Shocky web server is running! ---");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<String> getBotChannels(PircBotX bot) throws Exception {
		Set<Channel> set = bot.getChannels();
		List<String> list = new ArrayList<String>(set.size());
		for (Channel channel : set)
			list.add(channel.getName());
		return Collections.unmodifiableList(list);
	}
	
	public static void lostChannel(String channel) throws Exception {
		Data.channels.remove(channel);
	}
	
	public static void join(String... channels) throws Exception {
		join(botList, channels);
	}
	
	public synchronized static void join(List<? extends PircBotX> bots, String... channels) throws Exception {
		if (channels == null || channels.length == 0)
			return;
		
		List<Thread> joinThreads = new LinkedList<Thread>();
		synchronized (botList) {
			List<String> currentChannels = new LinkedList<String>();
			for (PircBotX bot : botList)
				currentChannels.addAll(getBotChannels(bot));
		
			List<String> joinList = new LinkedList<String>(Arrays.asList(channels));
			joinList.removeAll(currentChannels);
			if (joinList.size() == 0)
				return;
			
			Iterator<String> joinIter = joinList.iterator();
			for (PircBotX bot : bots) {
				if (!joinIter.hasNext())
					break;
				List<String> channelList = new ArrayList<String>(getBotChannels(bot));
				channelList.removeAll(joinList);
				if (channelList.size() >= Data.config.getInt("main-maxchannels"))
					continue;
				joinThreads.add(joinChannels(bot, joinIter, channelList.size()));
			}
			
			if (bots == botList) {
				while (joinIter.hasNext()) {
					Thread t = joinChannels(createBot(), joinIter, 0);
					if (t == null)
						break;
					joinThreads.add(t);
					Thread.sleep(3000);
				}
			}
		}
		for (Thread t : joinThreads)
			t.join();
		System.out.format("Finished joining %d channel%s",channels.length,channels.length==1?"":"s").println();
	}
	
	private static Thread joinChannels(PircBotX bot, Iterator<String> iter, int start) {
		if (bot == null || iter == null || !iter.hasNext())
			return null;
		List<String> botChannel = new ArrayList<String>();
		for (int i = start; iter.hasNext() && i < Data.config.getInt("main-maxchannels"); ++i) {
			String channel = iter.next().toLowerCase();
			synchronized (Data.channels) {
				if (!Data.channels.contains(channel))
					Data.channels.add(channel);
			}
			botChannel.add(channel);
		}
		Thread t = new JoinChannelsThread(bot,botChannel.toArray(new String[0]));
		t.start();
		return t;
	}
	
	private static class JoinChannelsThread extends Thread {
		private final PircBotX bot;
		private final String[] channels;
		public JoinChannelsThread(PircBotX bot, String[] channels) {
			super();
			this.setDaemon(true);
			this.bot = bot;
			this.channels = channels;
		}

		@Override
		public void run() {
		for (int i = 0; i < channels.length; ++i) {
				String channel = channels[i];
				if (bot.channelExists(channel))
					continue;
				if (i > 0 && (i % 10 == 0)) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				bot.joinChannel(channel);
			}
		}
	}
	
	public static void part(String... channels) throws Exception {
		List<String> argsList = null;
		if (channels == null || channels.length == 0)
			argsList = new LinkedList<String>(Data.channels);
		else
			argsList = Arrays.asList(channels);
		if (!Data.channels.removeAll(argsList))
			return;
		
		synchronized (botList) {
			for (PircBotX bot : botList) {
				List<String> partList = new LinkedList<String>(getBotChannels(bot));
				partList.retainAll(argsList);
				for (String channel : partList)
					bot.partChannel(bot.getChannel(channel));
			}
		}
	}
}