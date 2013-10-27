import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.Timer;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.ShockyBot;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;

public class ModuleAutoKick extends Module {
	private Map<Channel,Map<User,CheckerStructure>> data = Collections.synchronizedMap(new HashMap<Channel,Map<User,CheckerStructure>>());
	
	public String name() {return "autokick";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
		Data.config.setNotExists("autokick-messages",5);
		Data.config.setNotExists("autokick-delay",1000);
	}
	public void onDisable() {
		for (Channel key1 : data.keySet()) {
			Map<User,CheckerStructure> map = data.get(key1);
			for (User key2 : map.keySet())
				map.get(key2).resetTimers();
		}
		data.clear();
	}
	
	private void onEvent(PircBotX bot, Channel channel, User user) {
		if (!channel.isOp(bot.getUserBot())) return;
		if (!data.containsKey(channel))
			data.put(channel,Collections.synchronizedMap(new TreeMap<User,ModuleAutoKick.CheckerStructure>()));
		Map<User,CheckerStructure> map = data.get(channel);
		if (!map.containsKey(user))
			map.put(user,new CheckerStructure(bot,channel,user));
		map.get(user).runTimer();
	}
	
	public void onMessage(MessageEvent<ShockyBot> event) {
		onEvent(event.getBot(), event.getChannel(), event.getUser());
	}
	public void onActionMessage(ActionEvent<PircBotX> event) {
		onEvent(event.getBot(), event.getChannel(), event.getUser());
	}
	
	public class CheckerStructure implements ActionListener {
		private final PircBotX bot;
		private final Channel channel;
		private final User nick;
		private List<Timer> timers = Collections.synchronizedList(new ArrayList<Timer>());
		private int counter;
		
		public CheckerStructure(PircBotX bot, Channel channel, User nick) {
			this.bot = bot;
			this.channel = channel;
			this.nick = nick;
			
			counter = Data.forChannel(channel).getInt("autokick-messages");
		}
		
		public synchronized void runTimer() {
			if (--counter == 0) {
				bot.kick(channel,nick,"Excessive spam");
				resetTimers();
			} else {
				Timer t = new Timer(Data.forChannel(channel).getInt("autokick-delay"),this);
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