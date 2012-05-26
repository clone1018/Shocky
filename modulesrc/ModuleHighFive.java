import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.swing.Timer;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.Config;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;

public class ModuleHighFive extends Module implements ActionListener {
	private Config config = new Config();
	private HashMap<String,String> started = new HashMap<String,String>();
	private HashMap<String,Boolean> way = new HashMap<String,Boolean>();
	private HashMap<String,TimerClear> timers = new HashMap<String,TimerClear>();
	
	public int changeStat(String nick1, String nick2, int change) {
		nick1 = nick1.toLowerCase();
		nick2 = nick2.toLowerCase();
		
		ArrayList<String> nicks = new ArrayList<String>(Arrays.asList(new String[]{nick1,nick2}));
		Collections.sort(nicks);
		
		nick1 = nicks.get(0); nick2 = nicks.get(1);
		if (nick1.equals(nick2)) change = 0;
		
		config.setNotExists(nick1+" "+nick2,0);
		int i = config.getInt(nick1+" "+nick2)+change;
		config.set(nick1+" "+nick2,i);
		return change == 0 ? 0 : i;
	}
	
	public String name() {return "highfive";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("hf-announce",true);
		Data.config.setNotExists("hf-maxtime",1000*60*5);
		config.load(new File("data","highfive.cfg"));
	}
	public void onDisable() {
		for (String key : timers.keySet()) timers.get(key).stop();
		timers.clear();
	}
	public void onDataSave() {
		config.save(new File("data","highfive.cfg"));
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		List<String> list = Arrays.asList(event.getMessage().split(" "));
		
		String s = started.get(event.getChannel().getName());
		if (s == null && (list.contains("o/") ^ list.contains("\\o"))) {
			started.put(event.getChannel().getName(),event.getUser().getNick());
			way.put(event.getChannel().getName(),list.contains("o/"));
			TimerClear tc = new TimerClear(Data.config.getInt("hf-maxtime"),this,event.getChannel().getName());
			timers.put(event.getChannel().getName(),tc); tc.start();
		}
		if (s != null && (list.contains("\\o") ^ list.contains("o/"))) {
			if (list.contains("o/") == way.get(event.getChannel().getName())) return;
			
			if (event.getUser().equals(s) && event.getBot().getUserBot().getChannelsOpIn().contains(event.getChannel())) {
				event.getBot().kick(event.getChannel(),event.getUser());
				started.remove(event.getChannel().getName());
				way.remove(event.getChannel().getName());
				timers.get(event.getChannel().getName()).stop(); timers.remove(event.getChannel().getName());
				return;
			}
			
			int stat = changeStat(s,event.getUser().getNick(),1);
			if (stat != 0) {
				String msg = s+" o/ * \\o "+event.getUser().getNick()+" - "+getOrderNumber(stat)+" time";
				if (Data.config.getBoolean("hf-announce")) Shocky.sendChannel(event.getBot(),event.getChannel(),msg);
				else {
					Shocky.sendNotice(event.getBot(),event.getUser(),msg);
					Shocky.sendNotice(event.getBot(),event.getBot().getUser(s),msg);
				}
			}
			
			started.remove(event.getChannel().getName());
			way.remove(event.getChannel().getName());
			timers.get(event.getChannel().getName()).stop(); timers.remove(event.getChannel().getName());
		}
	}
	
	public String getOrderNumber(int n) {
		int n100 = n % 100, n10 = n % 10;
		if (n100 == 1) return ""+n+"st";
		if (n100 == 2) return ""+n+"nd";
		if (n100 == 3) return ""+n+"rd";
		if (n100 < 20) return ""+n+"th";
		if (n10 == 1) return ""+n+"st";
		if (n10 == 2) return ""+n+"nd";
		if (n10 == 3) return ""+n+"rd";
		return ""+n+"th";
	}

	public void actionPerformed(ActionEvent e) {
		TimerClear tc = (TimerClear)e.getSource();
		tc.stop();
		started.remove(tc.channel);
		way.remove(tc.channel);
		timers.remove(tc.channel);
	}
	
	public class TimerClear extends Timer {
		private static final long serialVersionUID = 7774482809649593019L;
		private final String channel;

		public TimerClear(int ms, ActionListener al, String channel) {
			super(ms,al);
			this.channel = channel;
		}
	}
}