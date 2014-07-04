package org.pircbotx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import javax.swing.Timer;

import org.pircbotx.hooks.TemporaryListener;
import org.pircbotx.hooks.events.ServerResponseEvent;

import pl.shockah.Helper;
import pl.shockah.shocky.ListenerAdapter;

public class ShockyUser extends User {
	
	public String account;
	private long recheckDelay;

	protected ShockyUser(PircBotX bot, String nick) {
		super(bot, nick);
	}

	public String getAccount() {
		synchronized(this) {
			if (account != null)
				return account;
			if (recheckDelay > System.currentTimeMillis())
				return null;
		
			PircBotX bot = getBot();
			if (bot != null && bot.getServerInfo() != null && bot.getServerInfo().isWhoX())
				account = getAccountFromWHOX();
			else {
				Whois whois = new Whois(this);
				while (!whois.finished())
					Helper.sleep(10);
				account = whois.account;
			}
		
			if (account == null)
				recheckDelay = System.currentTimeMillis()+60000;
		}
		return account;
	}
	
	private String getAccountFromWHOX() {
		PircBotX bot = getBot();
		WhoXListener listener = new WhoXListener(bot);
		synchronized (listener) {
			bot.getListenerManager().addListener(listener);
			bot.sendRawLine("WHO "+getNick()+" %na");
			try {
				listener.wait(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				listener.done();
			}
			return listener.account;
		}
	}
	
	public static class Whois extends ListenerAdapter implements ActionListener {
		private final ShockyUser user;
		private final Timer timer = new Timer(250,this);
		private boolean finished = false;
		private String account;
		
		private Whois(ShockyUser user) {
			this.user = user;
			PircBotX bot = user.getBot();
			bot.getListenerManager().addListener(this);
			bot.sendRawLineNow("WHOIS "+user.getNick());
		}
		
		public void onServerResponse(ServerResponseEvent<ShockyBot> event) {
			if (event.getCode() == 330) {
				timer.stop();
				account = event.getResponse().split(" ")[2];
				remove();
			} else if (event.getCode() == 318) {
				if (!timer.isRunning()) timer.start();
			}
		}
		public void actionPerformed(ActionEvent arg0) {
			if (finished) return;
			remove();
			account = null;
		}
		private void remove() {
			user.getBot().getListenerManager().removeListener(this);
			finished = true;
		}
		public boolean finished() {
			return finished;
		}
	}
	
	public class WhoXListener extends TemporaryListener {
		public String account;
		private WhoXListener(PircBotX bot) {
			super(bot);
		}
		@SuppressWarnings("rawtypes")
		public void onServerResponse(ServerResponseEvent event) throws Exception {
			if (event.getCode()!=354)
				return;
			StringTokenizer tok = new StringTokenizer(event.getResponse());
			tok.nextToken();//Bot name
			if (!getNick().contentEquals(tok.nextToken()))
				return;
			String acc = tok.nextToken();
			if (!acc.contentEquals("0"))
				account = acc;
			synchronized (this) {
				this.notify();
			}
		}
	}
}
