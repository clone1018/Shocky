import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;

import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.MultiChannel;
import pl.shockah.shocky.Shocky;

public class ModuleInvite extends Module {
	private static final Pattern chanExists = Pattern.compile("Information on \\x02(#\\w+)\\x02:");
	private static final Pattern chanNotExists = Pattern.compile("Channel \\x02(#\\w+)\\x02 is not registered.");
	public Map<String,String> queue = new ConcurrentHashMap<String,String>();
	
	public String name() {return "invite";}
	public boolean isListener() {return true;}
	
	public void onInvite(InviteEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getBot().getUser(event.getUser()))) return;
		queue.put(event.getChannel(), event.getUser());
		event.getBot().sendMessage("ChanServ", String.format("INFO %s",event.getChannel()));
	}
	@Override
	public void onNotice(NoticeEvent<PircBotX> event) throws Exception {
		if (!event.getUser().getNick().equals("ChanServ"))
			return;
		
		if (queue.isEmpty())
			return;
		
		Matcher m = chanNotExists.matcher(event.getMessage());
		if (m.find()) {
			String channel = m.group(1);
			String username = queue.remove(channel);
			if (username != null) {
				User user = event.getBot().getUser(username);
				if (user != null)
					Shocky.sendNotice(event.getBot(),user,"I will only join registered channels.");
			}
			return;
		}
	
		m = chanExists.matcher(event.getMessage());
		if (m.find()) {
			String channel = m.group(1);
			String username = queue.remove(channel);
			if (username != null)
				joinChannel(event.getBot(), channel, username);
		}
	}
	@Override
	public void onServerResponse(ServerResponseEvent<PircBotX> event) throws Exception {
		if (event.getCode() == 401 && !queue.isEmpty()) {
			String[] tokens = event.getResponse().split("\\s+");
			if (!tokens[1].equals("ChanServ"))
				return;
			Iterator<Entry<String, String>> iter = queue.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<String, String> entry = iter.next();
				joinChannel(event.getBot(), entry.getKey(), entry.getValue());
				iter.remove();
			}
		}
	}
	
	private void joinChannel(PircBotX bot, String channel, String username) {
		try {
			MultiChannel.join(channel);
		} catch (Exception e) {
			if (username != null) {
				User user = bot.getUser(username);
				if (user != null)
					Shocky.sendNotice(bot,user,"I'm already in channel "+channel);
			}
		}
	}
}