package pl.shockah.shocky;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashMap;
import javax.swing.Timer;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import pl.shockah.Helper;

public class Whois extends ListenerAdapter implements ActionListener {
	private static HashMap<String,String> map = new HashMap<String,String>();
	private static HashMap<String,Long> mapRecheck = new HashMap<String,Long>();
	
	public static String getWhoisLogin(User user) {
		if (map.containsKey(user.getNick())) return map.get(user.getNick());
		if (mapRecheck.containsKey(user.getNick())) {
			if (mapRecheck.get(user.getNick()) > new Date().getTime()) {
				mapRecheck.put(user.getNick(),new Date().getTime()+1000);
				return null;
			}
		}
		
		Whois whois = new Whois(user);
		while (!whois.finished()) Helper.sleep(10);
		String login = map.get(user.getNick());
		if (login == null) {
			map.remove(user.getNick());
			mapRecheck.put(user.getNick(),new Date().getTime()+1000);
		}
		return login;
	}
	public static void renameWhois(NickChangeEvent<PircBotX> event) {
		if (map.containsKey(event.getOldNick())) {
			String login = map.remove(event.getOldNick());
			map.put(event.getNewNick(),login);
		}
	}
	public static void clearWhois(User user) {
		if (map.containsKey(user.getNick())) map.remove(user.getNick());
	}
	
	private final PircBotX bot;
	private final User user;
	private final Timer timer = new Timer(250,this);
	private boolean finished = false;
	
	private Whois(User user) {
		this.user = user;
		bot = Shocky.getBots().iterator().next();
		Shocky.getBotManager().getListenerManager().addListener(this);
		bot.sendRawLineNow("WHOIS "+user.getNick());
	}
	
	public void onServerResponse(ServerResponseEvent<PircBotX> event) {
		if (event.getCode() == 330) {
			timer.stop();
			map.put(user.getNick(),event.getResponse().split(" ")[2]);
			remove();
		} else if (event.getCode() == 318) {
			if (!timer.isRunning()) timer.start();
		}
	}
	public void actionPerformed(ActionEvent arg0) {
		if (finished) return;
		remove();
		map.put(user.getNick(),null);
	}
	private void remove() {
		Shocky.getBotManager().getListenerManager().removeListener(this);
		finished = true;
	}
	public boolean finished() {
		return finished;
	}
}