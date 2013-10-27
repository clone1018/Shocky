import java.io.File;
import java.util.HashMap;
import java.util.Random;

import org.pircbotx.Channel;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;

public class ModuleSamantha extends Module  {
	
	private HashMap<Channel,Long> nextAction = new HashMap<Channel,Long>();
	private Random rnd = new Random();

	@Override
	public String name() {return "samantha";}
	public boolean isListener() {return true;}

	@Override
	public void onEnable(File dir) {
		Data.config.setNotExists("samantha-chance",5);
		Data.config.setNotExists("samantha-next",1000*60*2);
	}
	
	private boolean canPerformAction(Channel chan) {
		if (!nextAction.containsKey(chan))
			return true;
		long time = nextAction.get(chan);
		if (System.currentTimeMillis() >= time) {
			nextAction.remove(chan);
			return true;
		}
		return false;
	}
	
	private void performedAction(Channel chan) {
		nextAction.put(chan, System.currentTimeMillis()+Data.forChannel(chan).getInt("samantha-next"));
	}

	public void onMessage(MessageEvent<ShockyBot> event) {
		if (!canPerformAction(event.getChannel()) || rnd.nextInt(Data.forChannel(event.getChannel()).getInt("samantha-chance")) > 0)
			return;
		
		String[] args = event.getMessage().split(" ");
		boolean result = doAss(event, args);
		if (!result) result = doNipples(event, args);
		
		if (result)
			performedAction(event.getChannel());
	}
	
	public boolean doAss(MessageEvent<ShockyBot> event, String... args) {
		if (args.length < 3)
			return false;
		for (int i = 1; i < args.length - 1; i++) {
			if (args[i].equalsIgnoreCase("ass")) {
				Shocky.sendChannel(event.getBot(),event.getChannel(),String.format("%s %s-%s", args[i-1],args[i],args[i+1]));
				return true;
			}
		}
		return false;
	}
	
	public boolean doNipples(MessageEvent<ShockyBot> event, String... args) {
		if (args.length != 2)
			return false;
		if (args[0].equalsIgnoreCase("i'm")||args[0].equalsIgnoreCase("im")) {
			Shocky.sendChannel(event.getBot(),event.getChannel(),String.format("YOU'RE %s? feel these nipples!", args[1]));
			return true;
		}
		return false;
	}
}
