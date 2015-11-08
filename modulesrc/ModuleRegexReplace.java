import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.interfaces.IRollback;
import pl.shockah.shocky.interfaces.ILinePredicate;
import pl.shockah.shocky.lines.LineAction;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.lines.LineWithUsers;

public class ModuleRegexReplace extends Module {

	public static enum Type {SUB, MATCH, TRANSLATE};
	public static final Pattern sedPattern = Pattern.compile("^([sm]|tr)/(.*?(?<!\\\\))/(?:(.*?(?<!\\\\))/)?([a-z]*)");
	public static String[] groupColors = new String[] {"01,14","03,09","10,11","05,04","06,13","07,08","02,12","15,00"};

	@Override
	public String name() {return "regexreplace";}
	public boolean isListener() {return true;}

	@Override
	public void onMessage(MessageEvent<ShockyBot> event) throws Exception {
		if (Data.isBlacklisted(event.getUser()))
			return;
		String output = run(event.getChannel(), event.getMessage());
		if (output != null)
			Shocky.sendChannel(event.getBot(), event.getChannel(), output);
	}
	
	public String run(Channel channel, String s) throws InterruptedException, ExecutionException {
		IRollback module = (IRollback) Module.getModule("rollback");
		if (module == null)
			return null;
		int start = 0;
		List<StringProcess> list = new LinkedList<StringProcess>();
		Matcher m = sedPattern.matcher(s);
		while(start < s.length()) {
			while(start < s.length() && Character.isWhitespace(s.charAt(start)))
				++start;
			m.region(start, s.length());
			if (!m.find())
				break;
			
			String stype = m.group(1);
			String pattern = m.group(2);
			String replacement = m.group(3);
			String params = m.group(4);
			if (pattern.isEmpty())
				return null;
			Type type;
			if (stype.contentEquals("s"))
				type = Type.SUB;
			else if (stype.contentEquals("m"))
				type = Type.MATCH;
			else if (stype.contentEquals("tr"))
				type = Type.TRANSLATE;
			else continue;
			if (type != Type.MATCH && replacement==null)
				return null;
			if (type == Type.MATCH && channel.getMode().contains("c"))
				return null;

			if (type == Type.TRANSLATE) {
				try {
					list.add(new Translate(StringTools.build_translate(pattern), StringTools.build_translate(replacement)));
				} catch (RuntimeException e) {
					return StringTools.deleteWhitespace(e.getMessage());
				}
			}
			else {
				int flags = 0;
				boolean single = true;
			
				for (int i = 0;i < params.length();++i) {
					switch (params.charAt(i)) {
					case 'g':single = false;break;
					case 'd':flags |= Pattern.UNIX_LINES;break;
					case 'i':flags |= Pattern.CASE_INSENSITIVE;break;
					case 'm':flags |= Pattern.MULTILINE;break;
					case 's':flags |= Pattern.DOTALL;break;
					case 'u':flags |= Pattern.UNICODE_CASE;break;
					case 'x':flags |= Pattern.COMMENTS;break;
					}
				}
		
				try {
					list.add(new Regex(Pattern.compile(pattern, flags), single, type, type != Type.MATCH ? replacement : null));
				} catch (PatternSyntaxException e) {
					return StringTools.deleteWhitespace(e.getMessage());
				}
			}
			
			start = m.end()+1;
		}
		
		if (list.isEmpty())
			return null;
		
		String user = null;
		if (start < s.length())
			user = s.substring(start).trim();
		
		final ExecutorService service = Executors.newFixedThreadPool(1);
		try {
			Future<String> run = service.submit(new Run(module, channel.getName(), user, s, list));
			return run.get(10, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			return null;
		} finally {
			service.shutdown();
		}
	}
	
	private interface StringProcess {
		boolean matches(String text);
		void init(CharSequence input);
		boolean run(boolean useLine, StringBuffer output);
	}
	
	private static class Translate implements StringProcess {
		public final String search;
		public final String replacement;
		
		private CharSequence text;
		
		public Translate(String search, String replacement) {
			this.search = search;
			this.replacement = replacement;
		}
		
		@Override
		public boolean matches(String text) {
			for (int i = text.length() - 1; i >= 0; --i)
			{
				if (search.indexOf(text.charAt(i)) >= 0) {
					this.text = text;
					return true;
				}
			}
			this.text = null;
			return false;
		}
		
		@Override
		public void init(CharSequence input) {
			this.text = input;
		}
		
		@Override
		public boolean run(boolean useLine, StringBuffer output) {
			this.text = StringTools.translate(search, replacement, text);
			output.append(text);
			return false;
		}
	}
	
	private static class Regex implements StringProcess {
		public final Pattern pattern;
		public final boolean single;
		public final Type type;
		public final String replacement;
		
		private Matcher matcher;
		
		public Regex(Pattern pattern, boolean single, Type type, String replacement) {
			this.pattern = pattern;
			this.single = single;
			this.type = type;
			this.replacement = replacement;
		}
		
		@Override
		public boolean matches(String text) {
			if (type == Type.MATCH)
				text = Colors.removeFormattingAndColors(text);
			return matcher.reset(text).find();
		}
		
		@Override
		public void init(CharSequence input) {
			matcher = pattern.matcher(input);
		}
		
		@Override
		public boolean run(boolean useLine, StringBuffer output) {
			if (!useLine && !matcher.find())
				return true;
			do {
				matcher.appendReplacement(output, (type == Type.SUB) ? replacement : coloredGroups());
				if (single)
					break;
			} while (matcher.find());
			matcher.appendTail(output);
			return false;
		}
		
		private String coloredGroups() {
			String capture = matcher.group();
			if (capture.isEmpty())
				return capture;
			StringBuilder sb = new StringBuilder();
			Deque<Integer> color = new LinkedList<Integer>();
			int last = -1;
			for (int o = 0; o <= capture.length(); ++o) {
				for (int p = 0; p <= matcher.groupCount(); ++p) {
					int s = matcher.start(p) - matcher.start();
					int e = matcher.end(p) - matcher.start();
					if (o == s)
						color.push(p);
					else if (o == e)
						color.pop();
				}

				if (color.isEmpty() && last != -1) {
					last = -1;
					sb.append(Colors.NORMAL);
				} else if (!color.isEmpty() && last != color.peek()) {
					last = color.peek();
					sb.append('\3').append(groupColors[last%groupColors.length]);
				}

				if (o < capture.length())
					sb.append(capture.charAt(o));
			}
			return sb.toString();
		}
	}

	private static class Run implements Callable<String>, ILinePredicate<LineWithUsers> {
		private final IRollback module;
		private final String channel;
		private final String user;
		private final String message;
		private final Iterable<StringProcess> regex;
		
		private StringProcess current;

		public Run(IRollback module, String channel, String user, String message, Iterable<StringProcess> regex) {
			this.module = module;
			this.channel = channel;
			this.user = user;
			this.message = message;
			this.regex = regex;
		}

		@Override
		public boolean accepts(LineWithUsers line) {
			String text;
			if (line instanceof LineMessage)
				text = ((LineMessage) line).text;
			else if (line instanceof LineAction)
				text = ((LineAction) line).text;
			else
				return false;
			return current.matches(text);
		}

		@Override
		public String call() throws Exception {
			LineWithUsers line = null;
			boolean useLine = true;
			StringBuffer sb = new StringBuffer();
			Iterator<StringProcess> iter = regex.iterator();
			while(iter.hasNext()) {
				current = iter.next();
				current.init(sb);
				if (useLine) {
					line = module.getRollbackLine(this, LineWithUsers.class, channel, user, null, message, true, 10, 0);
					if (line == null)
						return null;
					if (current.run(true, sb))
						return null;
					useLine = false;
				} else {
					sb = new StringBuffer();
					if (current.run(false, sb))
						return null;
				}
			}
			if (line instanceof LineAction) {
				sb.insert(0, "\001ACTION ");
				sb.append('\001');
			}
			return StringTools.limitLength(sb);
		}
	}
}
