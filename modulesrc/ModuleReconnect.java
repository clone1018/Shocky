import java.util.List;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.DisconnectEvent;

import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.MultiChannel;
import pl.shockah.shocky.Shocky;

public class ModuleReconnect extends Module /*implements ActionListener*/ {
	//private List<Timer> timers = Collections.synchronizedList(new ArrayList<Timer>());
	//private List<PircBotX> bots = Collections.synchronizedList(new ArrayList<PircBotX>());
	
	public String name() {return "reconnect";}
	public boolean isListener() {return true;}
	public void onEnable() {
		//timers.clear();
		//bots.clear();
	}
	public void onDisable() {
		//for (Timer timer : timers) timer.stop();
	}
	
	public void onDisconnect(DisconnectEvent<PircBotX> event) {
		if (Shocky.isClosing()) return;
		/*Timer t;
		timers.add(t = new Timer(10000,this));
		bots.add(event.getBot());
		t.start();*/
		try {
			PircBotX bot = event.getBot();
			List<String> channels = MultiChannel.getBotChannels(bot);
			System.out.print("Reconnecting ");
			System.out.println(bot.getName());
			if (channels == null) {
				System.out.println("Could not rejoin channels.");
				return;
			} else {
				System.out.print("Will rejoin these channels: ");
				System.out.println(StringTools.implode(channels.toArray(), ","));
			}
			
			Thread.sleep(10000);
			bot.connect(bot.getServer());
			
			for (String channel : channels)
				bot.joinChannel(channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*public void actionPerformed(ActionEvent event) {
		timers.remove(0);
		PircBotX bot = bots.remove(0);
		Channel[] channels = bot.getChannels().toArray(new Channel[0]);
		try {
			bot.connect(bot.getServer());
			for (Channel channel : channels) bot.joinChannel(channel.getName());
		} catch (Exception e) {e.printStackTrace();}
	}*/
}