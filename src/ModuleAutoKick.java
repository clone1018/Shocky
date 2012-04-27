import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.Timer;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;

public class ModuleAutoKick extends Module {
	private Map<String,Map<String,CheckerStructure>> data = Collections.synchronizedMap(new TreeMap<String,Map<String,CheckerStructure>>());
	
	public String name() {return "autokick";}
	public void load() {
		Data.config.setNotExists("autokick-messages",5);
		Data.config.setNotExists("autokick-delay",1000);
	}
	public void unload() {
		for (String key1 : data.keySet()) {
			Map<String,CheckerStructure> map = data.get(key1);
			for (String key2 : map.keySet()) map.get(key2).resetTimers();
		}
		data.clear();
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (!data.containsKey(event.getChannel().getName())) data.put(event.getChannel().getName(),Collections.synchronizedMap(new TreeMap<String,ModuleAutoKick.CheckerStructure>()));
		Map<String,CheckerStructure> map = data.get(event.getChannel().getName());
		if (!map.containsKey(event.getUser().getNick().toLowerCase())) map.put(event.getUser().getNick().toLowerCase(),new CheckerStructure(event.getBot(),event.getChannel().getName(),event.getUser().getNick().toLowerCase()));
		map.get(event.getUser().getNick().toLowerCase()).runTimer();
	}
	public void onActionMessage(ActionEvent<PircBotX> event) {
		if (!data.containsKey(event.getChannel().getName())) data.put(event.getChannel().getName(),Collections.synchronizedMap(new TreeMap<String,ModuleAutoKick.CheckerStructure>()));
		Map<String,CheckerStructure> map = data.get(event.getChannel().getName());
		if (!map.containsKey(event.getUser().getNick().toLowerCase())) map.put(event.getUser().getNick().toLowerCase(),new CheckerStructure(event.getBot(),event.getChannel().getName(),event.getUser().getNick().toLowerCase()));
		map.get(event.getUser().getNick().toLowerCase()).runTimer();
	}
	
	public class CheckerStructure implements ActionListener {
		private final PircBotX bot;
		private final String channel, nick;
		private List<Timer> timers = Collections.synchronizedList(new ArrayList<Timer>());
		private int counter = Data.config.getInt("autokick-messages");
		
		public CheckerStructure(PircBotX bot, String channel, String nick) {
			this.bot = bot;
			this.channel = channel;
			this.nick = nick;
		}
		
		public synchronized void runTimer() {
			if (--counter == 0) {
				Channel c = bot.getChannel(channel);
				for (User user : c.getUsers()) if (user.getNick().toLowerCase().equals(nick)) {
					bot.kick(c,user,"Excessive spam");
					resetTimers();
					break;
				}
			} else {
				Timer t = new Timer(Data.config.getInt("autokick-delay"),this);
				timers.add(t);
				t.start();
			}
		}
		public synchronized void resetTimers() {
			for (Timer atk : timers) atk.stop();
			timers.clear();
		}
		public synchronized void actionPerformed(java.awt.event.ActionEvent e) {
			((Timer)e.getSource()).stop();
			timers.remove(e.getSource());
			counter++;
		}
	}
}