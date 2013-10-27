import java.io.File;
import java.util.Collections;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.DisconnectEvent;

import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.MultiChannel;
import pl.shockah.shocky.Shocky;

public class ModuleReconnect extends Module /*implements ActionListener*/ {
	//private List<Timer> timers = Collections.synchronizedList(new ArrayList<Timer>());
	//private List<PircBotX> bots = Collections.synchronizedList(new ArrayList<PircBotX>());
	
	public String name() {return "reconnect";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
	}
	
	public void onDisconnect(DisconnectEvent<ShockyBot> event) {
		if (Shocky.isClosing()) return;
		try {
			ShockyBot bot = event.getBot();
			System.out.print("Reconnecting ");
			System.out.println(bot.getName());
			Thread.sleep(10000);
			
			if (MultiChannel.connect(bot))
				MultiChannel.join(Collections.singletonList(bot),Data.channels.toArray(new String[0]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}