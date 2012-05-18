import java.io.File;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.Config;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;

public class ModuleNickPlusPlus extends Module {
	private Config config = new Config();
	
	public int changeStat(String nick, int change) {
		nick = nick.toLowerCase();
		config.setNotExists(nick,0);
		int i = config.getInt(nick)+change;
		config.set(nick,i);
		return i;
	}
	
	public String name() {return "nickplusplus";}
	public void onEnable() {
		Data.config.setNotExists("npp-announce",true);
		config.load(new File("data","nickplusplus.cfg"));
	}
	public void onDataSave() {
		config.save(new File("data","nickplusplus.cfg"));
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		if (event.getMessage().matches("^("+Utils.patternNick.pattern()+")((\\+\\+)|(\\-\\-)|(\\=\\=))$")) {
			String nick = event.getMessage().substring(0,event.getMessage().length()-2);
			
			if (event.getMessage().endsWith("++") && event.getUser().getNick().equalsIgnoreCase(nick)) {
				if (event.getBot().getUserBot().getChannelsOpIn().contains(event.getChannel())) event.getBot().kick(event.getChannel(),event.getUser(),event.getMessage());
				return;
			}
			
			int stat;
			if (event.getMessage().endsWith("++")) stat = changeStat(nick,1);
			else if (event.getMessage().endsWith("--")) stat = changeStat(nick,-1);
			else stat = changeStat(nick,0);
			if (event.getMessage().endsWith("==") || Data.config.getBoolean("npp-announce")) Shocky.sendChannel(event.getBot(),event.getChannel(),nick+" == "+stat);
			else Shocky.sendNotice(event.getBot(),event.getUser(),nick+" == "+stat);
		}
	}
}