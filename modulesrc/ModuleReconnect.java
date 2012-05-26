import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Timer;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.DisconnectEvent;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;

public class ModuleReconnect extends Module implements ActionListener {
	private List<Timer> timers = Collections.synchronizedList(new ArrayList<Timer>());
	private List<PircBotX> bots = Collections.synchronizedList(new ArrayList<PircBotX>());
	
	public String name() {return "reconnect";}
	public boolean isListener() {return true;}
	public void onEnable() {
		timers.clear();
		bots.clear();
	}
	public void onDisable() {
		for (Timer timer : timers) timer.stop();
	}
	
	public void onDisconnect(DisconnectEvent<PircBotX> event) {
		if (Shocky.isClosing()) return;
		Timer t;
		timers.add(t = new Timer(10000,this));
		bots.add(event.getBot());
		t.start();
	}
	public void actionPerformed(ActionEvent event) {
		timers.remove(0);
		PircBotX bot = bots.remove(0);
		Channel[] channels = bot.getChannels().toArray(new Channel[0]);
		try {
			bot.connect(bot.getServer());
			for (Channel channel : channels) bot.joinChannel(channel.getName());
		} catch (Exception e) {e.printStackTrace();}
	}
}