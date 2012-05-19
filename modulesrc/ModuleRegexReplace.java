import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.lines.LineMessage;


public class ModuleRegexReplace extends Module {

	@Override
	public String name() {return "regexreplace";}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if (Data.isBlacklisted(event.getUser())) return;
		String s = event.getMessage();
		if (!s.startsWith("s/")) return;
		String[] args = s.split("/");
		if (args.length < 3 || args.length > 4) return;
		if (args[1].isEmpty()) return;
		int flags = 0;
		boolean single = true;
		if (args.length == 4 && !args[3].isEmpty()) {
			for (char c : args[3].toCharArray()) {
				switch (c) {
				case 'd': flags |= Pattern.UNIX_LINES; break;
				case 'g': single = false; break;
				case 'i': flags |= Pattern.CASE_INSENSITIVE; break;
				case 'm': flags |= Pattern.MULTILINE; break;
				case 's': flags |= Pattern.DOTALL; break;
				case 'u': flags |= Pattern.UNICODE_CASE; break;
				case 'x': flags |= Pattern.COMMENTS; break;
				}
			}
		}
		Module module = Module.getModule("rollback");
		if (module == null) return;
		Pattern pattern = Pattern.compile(args[1],flags);
		Matcher matcher = pattern.matcher("");
		Method method = module.getClass().getDeclaredMethod("getRollbackLines", Class.class, String.class, String.class, String.class, boolean.class, int.class, int.class);
		@SuppressWarnings("unchecked")
		ArrayList<LineMessage> lines = (ArrayList<LineMessage>) method.invoke(module, LineMessage.class, event.getChannel().getName(), null, null, true, 10, 0);
		boolean found = false;
		StringBuffer sb = new StringBuffer();
		for (int i = lines.size()-2; i>=0 && !found; i--) {
			LineMessage line = lines.get(i);
			matcher.reset(line.text);
			while (matcher.find() && !(single && found)) {
				found = true;
				matcher.appendReplacement(sb, args[2]);
			}
			if (found) {
				matcher.appendTail(sb);
				Shocky.sendChannel(event.getBot(), event.getChannel(), StringTools.limitLength(sb));
				return;
			}
		}
	}
}
