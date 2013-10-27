import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pircbotx.PircBotX;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserModeEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import pl.shockah.FileLine;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.events.ActionOutEvent;
import pl.shockah.shocky.events.MessageOutEvent;
import pl.shockah.shocky.lines.Line;
import pl.shockah.shocky.lines.LineAction;
import pl.shockah.shocky.lines.LineEnterLeave;
import pl.shockah.shocky.lines.LineKick;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.lines.LineOther;

public class ModuleControllerAlert extends Module {
	protected Command cmd;
	protected List<ImmutablePair<String,Alert>> alerts = Collections.synchronizedList(new ArrayList<ImmutablePair<String,Alert>>());
	
	public String name() {return "alert";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdAlert());
		
		ArrayList<String> lines = FileLine.read(new File(dir,"alerts.cfg"));
		for (int i = 0; i < lines.size(); i += 2) alerts.add(new ImmutablePair<String,Alert>(lines.get(i),Alert.newAlert(lines.get(i+1))));
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	public void onDataSave(File dir) {
		ArrayList<String> lines = new ArrayList<String>();
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			lines.add(pair.left);
			lines.add(pair.right.toString());
		}
		FileLine.write(new File(dir,"alerts.cfg"),lines);
	}
	
	public void onMessage(MessageEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] <"+event.getUser().getNick()+"> "+event.getMessage());
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onAction(ActionEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] * "+event.getUser().getNick()+" "+event.getMessage());
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onTopic(TopicEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] "+event.getUser().getNick()+" has changed the topic to: "+event.getTopic());
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onKick(KickEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] "+event.getSource().getNick()+" kicked "+event.getRecipient().getNick()+" ("+event.getReason()+")");
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onJoin(JoinEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] "+event.getUser().getNick()+" has joined");
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onPart(PartEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] "+event.getUser().getNick()+" has left");
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onQuit(QuitEvent<ShockyBot> event) {
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: "+event.getUser().getNick()+" has quit ("+event.getReason()+")");
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onMode(ModeEvent<ShockyBot> event) {
		String mode = event.getMode();
		if (mode.charAt(0) == ' ') mode = "+"+mode.substring(1);
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: ["+event.getChannel().getName()+"] * "+event.getUser().getNick()+" sets mode "+mode);
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	public void onUserMode(UserModeEvent<ShockyBot> event) {
		String mode = event.getMode();
		if (mode.charAt(0) == ' ') mode = "+"+mode.substring(1);
		for (int i = 0; i < alerts.size(); i++) {
			ImmutablePair<String,Alert> pair = alerts.get(i);
			if (pair.right.matches(event)) {
				Shocky.sendNotice(event.getBot(),Shocky.getUser(pair.left),"Alert: "+pair.right+"\n"
						+"Line: * "+event.getSource().getNick()+" sets mode "+mode+" "+event.getTarget().getNick());
				if (!pair.right.cont) alerts.remove(i--);
			}
		}
	}
	
	public class CmdAlert extends Command {
		public String command() {return "alert";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("[r:controller] alert - lists set alerts");
			sb.append("[r:controller] alert add {parameters} - alerts you of specific events");
			sb.append("[r:controller] alert remove {n} - removes an alert");
			sb.append("\nparameters:");
			sb.append("\n-channel {text} | specify the channel to monitor");
			sb.append("\n-unick {text} | -unickserv {text} | -ulogin {text} | -uhost {text} | specify the user to monitor");
			sb.append("\n-type {text} | specify the event type ([m]essage/[a]ction/[e]nterleave/[k]ick/[o]ther)");
			sb.append("\n-regex {text} | the regex for [m]essage and [a]ction types");
			sb.append("\n-cont {boolean} | if the alert should continue after first occurence");
			return sb.toString();
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			
			if (params.tokenCount == 0) {
				int i2 = 1;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < alerts.size(); i++) {
					ImmutablePair<String,Alert> pair = alerts.get(i);
					if (pair.left.equals(params.sender.getNick())) {
						if (sb.length() != 0) sb.append("\n");
						sb.append(""+(i2++)+": "+pair.right);
					}
				}
				callback.append(sb.length() == 0 ? "No alerts set" : sb.toString());
				return;
			} else if (params.tokenCount >= 2) {
				String method = params.nextParam();
				if (method.equalsIgnoreCase("remove")) {
					String idString = params.nextParam();
					int i2 = 1, toRemove = Integer.parseInt(idString);
					for (int i = 0; i < alerts.size(); i++) {
						ImmutablePair<String,Alert> pair = alerts.get(i);
						if (pair.left.equals(params.sender.getNick()) && i2++ == toRemove) {
							alerts.remove(i);
							callback.append("Removed");
							return;
						}
					}
					callback.append("No such index");
					return;
				} else if (method.equalsIgnoreCase("add")) {
					Alert alert = Alert.newAlert(params.getParams(0));
					if (alert.isProper()) {
						alerts.add(new ImmutablePair<String,Alert>(params.sender.getNick(),alert));
						callback.append("Added");
						return;
					}
				}
			}
			
			callback.append(help(params));
			return;
		}
	}
	
	protected static class Alert {
		protected final String channel, uNick, uNickServ, uLogin, uHost, regex;
		protected final Class<? extends Line> type;
		protected final boolean cont;
		
		protected static Alert newAlert(String args) {
			String[] a = args.split(" ");
			ArrayList<String> l = new ArrayList<String>();
			for (int i = 0; i < a.length; i++) {
				if (!a[i].startsWith("\"")) l.add(a[i]); else {
					StringBuilder sb = new StringBuilder(a[i]);
					while (++i < a.length) {
						sb.append(" "+a[i]);
						if (a[i].charAt(a[i].length()-1) == '"' && a[i].charAt(a[i].length()-2) != '\\') {
							sb.deleteCharAt(0);
							sb.deleteCharAt(sb.length()-1);
							break;
						}
					}
					l.add(sb.toString());
				}
			}
			return new Alert(l.toArray(new String[l.size()]));
		}
		
		protected Alert(String[] args) {
			String _channel = null, _uNick = null, _uNickServ = null, _uLogin = null, _uHost = null, _type = null, _regex = null, _cont = null;
			for (int i = 0; i < args.length; i++) {
				String s = args[i].toLowerCase();
				if (s.equals("-channel") && i+1 < args.length) {_channel = args[++i].toLowerCase(); continue;}
				if (s.equals("-unick") && i+1 < args.length) {_uNick = args[++i].toLowerCase(); continue;}
				if (s.equals("-unickserv") && i+1 < args.length) {_uNickServ = args[++i].toLowerCase(); continue;}
				if (s.equals("-ulogin") && i+1 < args.length) {_uLogin = args[++i].toLowerCase(); continue;}
				if (s.equals("-uhost") && i+1 < args.length) {_uHost = args[++i]; continue;}
				if (s.equals("-type") && i+1 < args.length) {_type = args[++i]; continue;}
				if (s.equals("-regex") && i+1 < args.length) {_regex = args[++i]; continue;}
				if (s.equals("-cont") && i+1 < args.length) {_cont = args[++i]; continue;}
			}
			channel = _channel;
			uNick = _uNick;
			uNickServ = _uNickServ;
			uLogin = _uLogin;
			uHost = _uHost;
			regex = _regex;
			cont = _cont == null ? false : (_cont.equals("1") || _cont.equals("t") || _cont.equals("true"));
			if (_type == null) type = Line.class;
			else if (_type.charAt(0) == 'm') type = LineMessage.class;
			else if (_type.charAt(0) == 'a') type = LineAction.class;
			else if (_type.charAt(0) == 'e') type = LineEnterLeave.class;
			else if (_type.charAt(0) == 'k') type = LineKick.class;
			else if (_type.charAt(0) == 'o') type = LineOther.class;
			else type = Line.class;
		}
		
		protected boolean isProper() {
			return !(channel == null && uNick == null && uNickServ == null && uLogin == null && uHost == null && regex == null);
		}
		@SuppressWarnings("unchecked") protected boolean matches(Event<ShockyBot> event) {
			boolean matches = true;
			if (matches && type != Line.class) {
				if ((event instanceof MessageEvent || event instanceof MessageOutEvent) && type != LineMessage.class) matches = false;
				if ((event instanceof ActionEvent || event instanceof ActionOutEvent) && type != LineAction.class) matches = false;
				if ((event instanceof JoinEvent || event instanceof PartEvent || event instanceof QuitEvent) && type != LineEnterLeave.class) matches = false;
				if (event instanceof KickEvent && type != LineKick.class) matches = false;
				if (matches && type != LineOther.class) matches = false;
			}
			if (matches && channel != null) {
				try {
					Method m = event.getClass().getMethod("getChannel");
					if (m == null) matches = false; else {
						m.setAccessible(true);
						String eChannel = (String)m.invoke(event);
						if (!eChannel.toLowerCase().equals(eChannel)) matches = false;
					}
				} catch (Exception e) {e.printStackTrace();}
			}
			if (matches && uNick != null) {
				String eNick = null;
				if (event instanceof MessageEvent || event instanceof ActionEvent) eNick = ((GenericMessageEvent<PircBotX>)event).getUser().getNick().toLowerCase();
				if (event instanceof MessageOutEvent || event instanceof ActionOutEvent) eNick = ((GenericMessageEvent<PircBotX>)event).getBot().getNick().toLowerCase();
				if (event instanceof KickEvent) matches = ((KickEvent<ShockyBot>)event).getSource().getNick().toLowerCase().equals(uNick) || ((KickEvent<ShockyBot>)event).getRecipient().getNick().toLowerCase().equals(uNick);
				if (matches && (eNick == null || !eNick.equals(uNick))) matches = false;
			}
			if (matches && uNickServ != null) {
				String eNickServ = null;
				if (event instanceof MessageEvent || event instanceof ActionEvent) eNickServ = Shocky.getLogin(((GenericMessageEvent<PircBotX>)event).getUser()).toLowerCase();
				if (event instanceof MessageOutEvent || event instanceof ActionOutEvent) eNickServ = Shocky.getLogin(((GenericMessageEvent<PircBotX>)event).getBot().getUserBot()).toLowerCase();
				if (event instanceof KickEvent) matches = Shocky.getLogin(((KickEvent<ShockyBot>)event).getSource()).toLowerCase().equals(uNickServ) || Shocky.getLogin(((KickEvent<ShockyBot>)event).getRecipient()).toLowerCase().equals(uNickServ);
				if (matches && (eNickServ == null || !eNickServ.equals(uNickServ))) matches = false;
			}
			if (matches && uLogin != null) {
				String eLogin = null;
				if (event instanceof MessageEvent || event instanceof ActionEvent) eLogin = ((GenericMessageEvent<PircBotX>)event).getUser().getLogin().toLowerCase();
				if (event instanceof MessageOutEvent || event instanceof ActionOutEvent) eLogin = ((GenericMessageEvent<PircBotX>)event).getBot().getLogin().toLowerCase();
				if (event instanceof KickEvent) matches = ((KickEvent<ShockyBot>)event).getSource().getLogin().toLowerCase().equals(uLogin) || ((KickEvent<ShockyBot>)event).getRecipient().getLogin().toLowerCase().equals(uLogin);
				if (matches && (eLogin == null || !eLogin.equals(uLogin))) matches = false;
			}
			if (matches && uHost != null) {
				String eHost = null;
				if (event instanceof MessageEvent || event instanceof ActionEvent) eHost = ((GenericMessageEvent<PircBotX>)event).getUser().getHostmask();
				if (event instanceof MessageOutEvent || event instanceof ActionOutEvent) eHost = ((GenericMessageEvent<PircBotX>)event).getBot().getUserBot().getHostmask();
				if (event instanceof KickEvent) matches = ((KickEvent<ShockyBot>)event).getSource().getHostmask().equals(uHost) || ((KickEvent<ShockyBot>)event).getRecipient().getHostmask().equals(uHost);
				if (matches && (eHost == null || !eHost.equals(uHost))) matches = false;
			}
			if (matches && regex != null && (type == Line.class || type == LineMessage.class || type == LineAction.class)) {
				if (event instanceof GenericMessageEvent) if (!((GenericMessageEvent<PircBotX>)event).getMessage().matches(regex)) matches = false;
			}
			return matches;
		}
		
		public String toString() {
			ArrayList<String> args = new ArrayList<String>();
			if (channel != null) args.add("-channel "+channel);
			if (uNick != null) args.add("-unick "+uNick);
			if (uNickServ != null) args.add("-unickserv "+uNickServ);
			if (uLogin != null) args.add("-ulogin "+uLogin);
			if (uHost != null) args.add("-uhost "+uHost);
			if (regex != null) args.add("-regex "+(regex.contains(" ") ? "\""+regex.replace("\"","\\\"")+"\"" : regex));
			if (cont) args.add("-cont 1");
			
			if (type == LineMessage.class) args.add("-type m");
			else if (type == LineAction.class) args.add("-type a");
			else if (type == LineEnterLeave.class) args.add("-type e");
			else if (type == LineKick.class) args.add("-type k");
			else if (type == LineOther.class) args.add("-type o");
			
			StringBuilder sb = new StringBuilder();
			for (String s : args) {
				if (sb.length() != 0) sb.append(" ");
				sb.append(s);
			}
			return sb.toString();
		}
	}
}