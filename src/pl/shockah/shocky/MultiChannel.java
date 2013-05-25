package pl.shockah.shocky;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.pircbotx.*;
import org.pircbotx.exception.*;

import pl.shockah.Reflection;

public class MultiChannel {
	private static Map<PircBotX,List<String>> channelMap = new TreeMap<PircBotX,List<String>>(NickComparator.singleton);
	protected static String channelPrefixes = null;
	
	public static Channel get(String name) {
		name = name.toLowerCase();
		for (Entry<PircBotX, List<String>> entry : channelMap.entrySet()) {
			for (String channel : entry.getValue()) {
				if (channel.equalsIgnoreCase(name))
					return entry.getKey().getChannel(channel);
			}
		}
		return null;
	}
	
	private static PircBotX createBot() {
		PircBotX bot = Shocky.getBotManager().createBot(Data.config.getString("main-server"));
		try {
			bot.setVersion(Data.config.getString("main-version"));
			String server = Data.config.getString("main-server");
			bot.connect(server.contentEquals("localhost") ? null : server);
			if (!Data.config.getString("main-nickservpass").isEmpty())
				bot.identify(Data.config.getString("main-nickservpass"));
			if (channelPrefixes == null)
				channelPrefixes = Reflection.getPrivateValue(PircBotX.class,"channelPrefixes",bot);
			channelMap.put(bot, new ArrayList<String>(Data.config.getInt("main-maxchannels")));
			return bot;
		} catch (NickAlreadyInUseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IrcException e) {
			e.printStackTrace();
		}
		if (bot != null && bot.isConnected())
			bot.disconnect();
		return null;
	}
	
	public static List<String> getBotChannels(PircBotX bot) throws Exception {
		return Collections.unmodifiableList(channelMap.get(bot));
	}
	
	public static void lostChannel(PircBotX bot, String channel) throws Exception {
		channelMap.get(bot).remove(channel);
		Data.channels.remove(channel);
	}
	
	public static void join(String... channels) throws Exception {
		if (channels == null || channels.length == 0)
			return;
		
		LinkedList<String> currentChannels = new LinkedList<String>();
		for (List<String> channelList : channelMap.values())
			currentChannels.addAll(channelList);
		
		List<String> joinList = new LinkedList<String>(Arrays.asList(channels));
		joinList.removeAll(currentChannels);
		if (joinList.size() == 0)
			return;
		
		int i = 0;
		for (String channel : joinList) {
			channel = channel.toLowerCase();
			if (!Data.channels.contains(channel))
				Data.channels.add(channel);
			
			PircBotX bot = null;
			for (Entry<PircBotX, List<String>> entry : channelMap.entrySet()) {
				List<String> channelList = entry.getValue();
				if (!channelList.contains(channel) && channelList.size() < Data.config.getInt("main-maxchannels")) {
					bot = entry.getKey();
					break;
				}
			}
			if (bot == null)
				bot = createBot();
		
			if (bot == null) break;
			else {
				if (bot.getChannels().contains(channel))
					throw new Exception("Already in channel "+channel);
				
				if (bot.getChannelsNames().size() >= Data.config.getInt("main-maxchannels"))
					throw new Exception("In an unexpected number of channels.");
				
				bot.joinChannel(channel);
				channelMap.get(bot).add(channel);
				i++;
				if (i >= 10) {
					Thread.sleep(3000);
					i=0;
				}
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
		
		for (Entry<PircBotX, List<String>> entry : channelMap.entrySet()) {
			PircBotX bot = entry.getKey();
			List<String> channelList = entry.getValue();
			List<String> partList = new LinkedList<String>(channelList);
			partList.retainAll(argsList);
			if (channelList.removeAll(partList)) {
				for (String channel : partList) {
					Channel channelObj = bot.getChannel(channel);
					if (channelObj != null)
						bot.partChannel(channelObj);
				}
			}
		}
	}
}