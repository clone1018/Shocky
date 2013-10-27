package org.pircbotx;

import javax.net.SocketFactory;

public class ShockyMultiBotManager extends MultiBotManager {
	
	public ShockyMultiBotManager(String name) {
		super(name);
	}

	public ShockyMultiBotManager(ShockyBot dummyBot) {
		super(dummyBot);
	}
	
    @SuppressWarnings("unchecked")
	public PircBotX createBot(String hostname, int port, String password, SocketFactory socketFactory)
    {
    	ShockyBot bot = new ShockyBot();
        bot.setListenerManager(listenerManager);
        bot.setName(name);
        bot.setVerbose(verbose);
        bot.setSocketTimeout(socketTimeout);
        bot.setMessageDelay(messageDelay);
        bot.setLogin(login);
        bot.setAutoNickChange(autoNickChange);
        bot.setEncoding(encoding);
        bot.setDccInetAddress(dcciNetAddress);
        bot.setDccPorts(dccports);
        bots.add(new BotEntry(bot, hostname, port, password, socketFactory));
        return bot;
    }

}
