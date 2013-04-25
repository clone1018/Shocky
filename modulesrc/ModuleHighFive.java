import java.io.File;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.Config;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;

public class ModuleHighFive extends Module/* implements ActionListener*/ {
	private Config config = new Config();
	private HashMap<String,User> started = new HashMap<String,User>();
	private HashMap<String,Long> timers = new HashMap<String,Long>();
	private static final Pattern pattern = Pattern.compile("(\\s|^)(o/|\\\\o)(\\s|$)", Pattern.CASE_INSENSITIVE);
	
	public int changeStat(String nick1, String nick2, int change) {
		nick1 = nick1.toLowerCase();
		nick2 = nick2.toLowerCase();
		if (nick1.equals(nick2))
			return 0;
		
		//ArrayList<String> nicks = new ArrayList<String>(Arrays.asList(new String[]{nick1,nick2}));
		//Collections.sort(nicks);
		//nick1 = nicks.get(0); nick2 = nicks.get(1);
		if (nick1.compareTo(nick2)>0) {
			String temp = nick1;
			nick1 = nick2;
			nick2 = temp;
		}
		
		String key = nick1+' '+nick2;
		config.setNotExists(key,0);
		int i = config.getInt(key)+change;
		config.set(key,i);
		return i;
	}
	
	public String name() {return "highfive";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("hf-announce",true);
		Data.config.setNotExists("hf-maxtime",1000*60*5);
		Data.config.setNotExists("hf-kickat",5);
		config.load(new File("data","highfive.cfg"));
	}
	public void onDisable() {
	}
	public void onDataSave() {
		config.save(new File("data","highfive.cfg"));
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		String msg = event.getMessage();
		if (!pattern.matcher(msg).find())
			return;
		
		String chan = event.getChannel().getName();
		Long time = timers.get(chan);
		User s = started.get(chan);
		
		if (s != null && time != null && time < System.currentTimeMillis()) {
			started.remove(chan);
			timers.remove(chan);
			s = null;
		}
		
		if (s == null) {
			time = System.currentTimeMillis()+Data.forChannel(event.getChannel()).getInt("hf-maxtime");
			//event.respond(String.format("Will expired at %s", DateFormat.getDateTimeInstance().format(new Date(time))));
			started.put(chan,event.getUser());
			timers.put(chan,time);
		} else {
			/*if (event.getUser().equals(s) && event.getBot().getUserBot().getChannelsOpIn().contains(event.getChannel())) {
				event.getBot().kick(event.getChannel(),event.getUser());
				started.remove(chan);
				timers.remove(chan);
				return;
			}*/
			
			int stat = changeStat(s.getNick(),event.getUser().getNick(),1);
			if (stat != 0) {
				if (event.getChannel().isOp(event.getBot().getUserBot()) && stat >= Data.forChannel(event.getChannel()).getInt("hf-kickat")) {
					stat = changeStat(s.getNick(),event.getUser().getNick(),-stat);
					msg = s.getNick()+" o/ * \\o "+event.getUser().getNick()+" - have been kicked!";
					event.getBot().kick(event.getChannel(), s);
					event.getBot().kick(event.getChannel(), event.getUser());
					if (Data.forChannel(event.getChannel()).getBoolean("hf-announce"))
						Shocky.sendChannel(event.getBot(),event.getChannel(),msg);
					else {
						Shocky.sendNotice(event.getBot(),event.getUser(),msg);
						Shocky.sendNotice(event.getBot(),s,msg);
					}
				} else {
					msg = s.getNick()+" o/ * \\o "+event.getUser().getNick()+" - "+getOrderNumber(stat)+" time";
					if (Data.forChannel(event.getChannel()).getBoolean("hf-announce"))
						Shocky.sendChannel(event.getBot(),event.getChannel(),msg);
					else {
						Shocky.sendNotice(event.getBot(),event.getUser(),msg);
						Shocky.sendNotice(event.getBot(),s,msg);
					}
				}
				
				started.remove(chan);
				timers.remove(chan);
			}
		}
	}
	
	public String getOrderNumber(int n) {
		StringBuilder sb = new StringBuilder(3);
		sb.append(n);
		int n100 = n % 100;
		if (n100 >= 10 && n100 < 20) {
			sb.append("th");
		} else {
			switch (n % 10) {
			case 1: sb.append("st"); break;
			case 2: sb.append("nd"); break;
			case 3: sb.append("rd"); break;
			default: sb.append("th"); break;
			}
		}
		return sb.toString();
	}

	/*public void actionPerformed(ActionEvent e) {
		TimerClear tc = (TimerClear)e.getSource();
		tc.stop();
		started.remove(tc.channel);
		timers.remove(tc.channel);
	}
	
	public class TimerClear extends Timer {
		private static final long serialVersionUID = 7774482809649593019L;
		private final String channel;

		public TimerClear(int ms, ActionListener al, String channel) {
			super(ms,al);
			this.channel = channel;
		}
	}*/
}