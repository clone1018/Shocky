import java.io.File;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.Config;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.interfaces.ILua;

public class ModuleNickPlusPlus extends Module implements ILua {
	private Config config = new Config();
	
	public int changeStat(String nick, int change) {
		nick = nick.toLowerCase();
		config.setNotExists(nick,0);
		int i = config.getInt(nick)+change;
		config.set(nick,i);
		return i;
	}
	
	public String name() {return "nickplusplus";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
		Data.config.setNotExists("npp-announce",true);
		config.load(new File(dir,"nickplusplus.cfg"));
	}
	public void onDataSave(File dir) {
		config.save(new File(dir,"nickplusplus.cfg"));
	}
	
	public void onMessage(MessageEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		if (event.getMessage().matches("^("+Utils.patternNick.pattern()+")((\\+\\+)|(\\-\\-)|(\\=\\=))$")) {
			String nick = event.getMessage().substring(0,event.getMessage().length()-2);
			if (nick.length() < 3) return;
			
			if (event.getMessage().endsWith("++") && event.getUser().getNick().equalsIgnoreCase(nick)) {
				if (event.getBot().getUserBot().getChannelsOpIn().contains(event.getChannel())) event.getBot().kick(event.getChannel(),event.getUser(),event.getMessage());
				return;
			}
			
			int stat;
			if (event.getMessage().endsWith("++")) stat = changeStat(nick,1);
			else if (event.getMessage().endsWith("--")) stat = changeStat(nick,-1);
			else stat = changeStat(nick,0);
			if (event.getMessage().endsWith("==") || Data.forChannel(event.getChannel()).getBoolean("npp-announce")) Shocky.sendChannel(event.getBot(),event.getChannel(),nick+" == "+stat);
			else Shocky.sendNotice(event.getBot(),event.getUser(),nick+" == "+stat);
		}
	}

	public class Function extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			String nick = arg.checkjstring().toLowerCase();
			return valueOf(config.exists(nick)?config.getInt(nick):0);
		}
	}

	@Override
	public void setupLua(LuaTable env) {
		env.set("npp", new Function());
	}
}