import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.prototypes.IRollback;

public class ModuleRegexReplace extends Module {
	
	public static String[] groupColors = new String[] {Colors.BLUE+",02", Colors.RED+",05", Colors.GREEN+",03", Colors.MAGENTA+",06", Colors.CYAN+",10"};
	
	@Override
	public void onEnable() {
	}
	@Override
	public void onDisable() {
	}

	@Override
	public String name() {return "regexreplace";}
	public boolean isListener() {return true;}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if (Data.isBlacklisted(event.getUser())) return;
		String s = event.getMessage();
		if (!s.startsWith("s/")&&!s.startsWith("m/")) return;
		String[] args = s.split("/", -1);
		boolean replace = args[0].contentEquals("s");
		if (!replace && event.getChannel().getMode().contains("c")) return;
		
		int flagPos = 2;
		if (replace)
			flagPos = 3;
		
		if (args.length != flagPos+1) return;
		if (args[1].isEmpty()) return;
		int flags = 0;
		boolean single = true;
		if (!args[flagPos].isEmpty()) {
			for (char c : args[flagPos].toCharArray()) {
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
		IRollback module = (IRollback)Module.getModule("rollback");
		if (module == null) return;
		Pattern pattern = Pattern.compile(args[1],flags);
		Matcher matcher = pattern.matcher("");
		ArrayList<LineMessage> lines = module.getRollbackLines(LineMessage.class, event.getChannel().getName(), null, null, s, true, 10, 0);
		
		final ExecutorService service = Executors.newFixedThreadPool(1);
		try {
		Future<String> run = service.submit(new Run(lines, matcher, single, replace ? args[2] : null));
		String output = run.get(5, TimeUnit.SECONDS);
		if (output != null)
			Shocky.sendChannel(event.getBot(), event.getChannel(), output);
		} catch(TimeoutException e) {
		}
		finally {
			service.shutdown();
		}
	}
	
	private static class Run implements Callable<String> {
		private final List<LineMessage> lines;
		private final Matcher matcher;
		private final boolean single;
		private final String replacement;
		
		public Run(List<LineMessage> lines, Matcher matcher, boolean single, String replacement) {
			this.lines = lines;
			this.matcher = matcher;
			this.single = single;
			this.replacement = replacement;
		}

		@Override
		public String call() throws Exception {
			boolean found = false;
			StringBuffer sb = new StringBuffer();
			for (int i = lines.size()-1; i>=0 && !found; i--) {
				LineMessage line = lines.get(i);
				String text = line.text;
				if (replacement == null)
					text = Colors.removeFormattingAndColors(text);
				matcher.reset(text);
				while (matcher.find() && !(single && found)) {
					found = true;
					if (replacement != null)
						matcher.appendReplacement(sb, replacement);
					else {
						String capture = matcher.group();
						char[] chars = capture.toCharArray();
						StringBuilder sb2 = new StringBuilder();
						Stack<Integer> color = new Stack<Integer>();
						int last = -1;
						for (int o = 0; o <= chars.length; o++) {
							for (int p = 0; p <= matcher.groupCount(); p++) {
								int s = matcher.start(p)-matcher.start();
								int e = matcher.end(p)-matcher.start();
								if (o == s)
									color.push(p);
								else if (o == e)
									color.pop();
							}
							
							if (color.isEmpty()) {
								last = -1;
								sb2.append(Colors.NORMAL);
							}
							else if (last != color.peek()) {
								sb2.append(groupColors[color.peek()%groupColors.length]);
								last = color.peek();
							}
							
							if (o < chars.length)
								sb2.append(chars[o]);
						}
						matcher.appendReplacement(sb, sb2.toString());
					}
				}
				if (found) {
					matcher.appendTail(sb);
					return StringTools.limitLength(sb);
				}
			}
			return null;
		}
	}
}
