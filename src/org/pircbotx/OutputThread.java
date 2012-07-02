package org.pircbotx;

import java.io.BufferedWriter;
import java.util.concurrent.LinkedBlockingQueue;
import pl.shockah.shocky.events.MessageOutEvent;
import pl.shockah.shocky.events.NoticeOutEvent;
import pl.shockah.shocky.events.PrivateMessageOutEvent;

public class OutputThread extends Thread {
	protected PircBotX bot = null;
	protected LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	protected final BufferedWriter bwriter;

	protected OutputThread(PircBotX bot, BufferedWriter bwriter) {
		this.bot = bot;
		this.bwriter = bwriter;
	}

	public void sendRawLineNow(String line) {
		if (line.length() > this.bot.getMaxLineLength() - 2) line = line.substring(0,this.bot.getMaxLineLength() - 2);
		synchronized (this.bwriter) {
			failIfNotConnected();
			try {
				this.bwriter.write(line + "\r\n");
				this.bwriter.flush();
				this.bot.log(">>>" + line);
				handleLine(line);
			} catch (Exception e) {
				throw new RuntimeException("Exception encountered when writng to socket",e);
			}
		}
	}

	public void send(String message) {
		failIfNotConnected();
		try {
			this.queue.put(message);
		} catch (InterruptedException ex) {
			throw new RuntimeException("Can't add message to queue",ex);
		}
	}

	public int getQueueSize() {
		return this.queue.size();
	}

	protected void failIfNotConnected() throws RuntimeException {
		if (!this.bot.isConnected()) throw new RuntimeException("Trying to send message when no longer connected");
	}

	public void run() {
		try {
			while (true) {
				String line = this.queue.take();
				failIfNotConnected();
				if ((line != null) && (this.bot.isConnected())) {
					sendRawLineNow(line);
				}

				Thread.sleep(this.bot.getMessageDelay());
			}
		} catch (InterruptedException e) {}
	}
	
	@SuppressWarnings({"rawtypes","unchecked"}) protected void handleLine(String line) {
		String[] spl = line.split(" ");
		if (line.toUpperCase().startsWith("PRIVMSG")) {
			if (line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
				return;
			} else {
				if (bot.channelPrefixes.indexOf(spl[1].charAt(0)) != -1) {
					bot.getListenerManager().dispatchEvent(new MessageOutEvent(bot,bot.getChannel(spl[1]),line.substring(line.indexOf(" :")+2)));
				} else {
					bot.getListenerManager().dispatchEvent(new PrivateMessageOutEvent(bot,bot.getUser(spl[1]),line.substring(line.indexOf(" :")+2)));
				}
			}
		} else if (line.toUpperCase().startsWith("NOTICE")) {
			bot.getListenerManager().dispatchEvent(new NoticeOutEvent(bot,bot.getUser(spl[1]),line.substring(line.indexOf(" :")+2)));
		}
	}
}