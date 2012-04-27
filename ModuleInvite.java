import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.InviteEvent;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.MultiChannel;
import pl.shockah.shocky.Shocky;

public class ModuleInvite extends Module {
	public String name() {return "invite";}
	public void load() {}
	public void unload() {}
	
	public void onInvite(InviteEvent<PircBotX> event) {
		if (Data.getBlacklistNicks().contains(event.getUser().toLowerCase())) return;
		try {
			MultiChannel.join(event.getChannel());
		} catch (Exception e) {Shocky.sendNotice(event.getBot(),event.getBot().getUser(event.getUser()),"I'm already in channel "+event.getChannel());}
	}
}