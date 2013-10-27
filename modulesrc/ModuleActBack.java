import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.ActionEvent;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;

public class ModuleActBack extends Module {
	public String name() {return "actback";}
	public boolean isListener() {return true;}
	
	public void onAction(ActionEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		String[] spl = event.getAction().split(" ");
		List<String> list = Arrays.asList(spl);
		if (spl.length >= 2 && list.contains(event.getBot().getNick())) {
			ArrayList<String> alist = new ArrayList<String>(Arrays.asList(spl));
			if (spl.length == 2) alist.add("back"); else alist.add(2,"back");
			event.getBot().sendAction(event.getChannel(),(StringTools.implode(alist.toArray(new String[alist.size()])," ").replace(event.getBot().getNick(),event.getUser().getNick())));
		}
	}
}