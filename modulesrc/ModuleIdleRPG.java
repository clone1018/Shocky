import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.ShockyBot;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.interfaces.ILua;

public class ModuleIdleRPG extends Module implements ILua {
	public static DecimalFormat formatXP = new DecimalFormat("###,###", new DecimalFormatSymbols(Locale.ENGLISH));
	public static DecimalFormat formatXPPercent = new DecimalFormat("###,###.#", new DecimalFormatSymbols(Locale.ENGLISH));

	//private Player.LinkedList players = new Player.LinkedList();
	private Map<String,Player> players = Collections.synchronizedMap(new TreeMap<String,Player>(new ComparatorIgnoreCase()));

	public String name() {
		return "idlerpg";
	}

	public boolean isListener() {
		return true;
	}

	public void onDisable() {
		players.clear();
	}

	public void onEnable(File dir) {
		Data.config.setNotExists("idlerpg-channel", "#shockyidlerpg");
		Data.config.setNotExists("idlerpg-announce", true);
		Data.config.setNotExists("idlerpg-leaderboards-print", 5);
		if (!Data.protectedKeys.contains("idlerpg-channel"))
			Data.protectedKeys.addAll(Arrays.asList(new String[] {
					"idlerpg-channel", "idlerpg-announce",
					"idlerpg-leaderboards-print" }));
		try {
			File f = new File(dir, "idlerpg.json");
			if (f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null) {
					sb.append(line);
					sb.append('\n');
					line = br.readLine();
				}
				readJSON(new JSONObject(sb.toString()));
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onDataSave(File dir) {
		File file = new File(dir, "idlerpg.json");
		File temp = null;
		try {
			try {
				temp = File.createTempFile("shocky", ".tmp");
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			System.out.printf("File: %s Temp: %s", file.getAbsolutePath(), temp.getAbsolutePath()).println();
			
			PrintWriter pw = new PrintWriter(temp);
			pw.write(writeJSON().toString());
			pw.close();
			
			if (file.exists())
				file.delete();
			temp.renameTo(file);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (temp != null && temp.exists())
				temp.delete();
		}
	}

	public void readJSON(JSONObject json) {
		try {
			if (json.has("players")) {
				JSONArray jPlayers = json.getJSONArray("players");
				for (int i = 0; i < jPlayers.length(); i++) {
					JSONObject jPlayer = jPlayers.getJSONObject(i);
					Player player = new Player(jPlayer.getString("name"));
					player.level = jPlayer.getInt("level");
					player.xp = jPlayer.getInt("xp");
					player.lastUpdate = jPlayer.getInt("lastUpdate");

					//Player player2 = this.players.findByName(player.name);
					Player player2 = this.players.get(player.name);
					if (player2 != null && player2.level >= player.level)
						continue;
					//this.players.add(player);
					this.players.put(player.name,player);
				}
			}
		} catch (Exception localException) {
		}
	}

	public JSONObject writeJSON() {
		JSONObject json = new JSONObject();
		try {
			JSONArray jPlayers = new JSONArray();
			for (Player player : this.players.values()) {
				JSONObject jPlayer = new JSONObject();
				jPlayer.put("name", player.name);
				jPlayer.put("level", player.level);
				jPlayer.put("xp", player.xp);
				jPlayer.put("lastUpdate", player.lastUpdate);
				jPlayers.put(jPlayer);
			}
			json.put("players", jPlayers);
		} catch (Exception localException) {
		}
		return json;
	}

	public static void send(MessageEvent<ShockyBot> ev, String message) {
		Command.EType type = Command.EType.Channel;
		String allowedChannel = Data.config.getString("idlerpg-channel");
		if ((!allowedChannel.isEmpty())
				&& (!allowedChannel.equalsIgnoreCase(ev.getChannel().getName())))
			type = Command.EType.Notice;

		Shocky.send(ev.getBot(), type, ev.getChannel(), ev.getUser(), message);
	}

	public static void send(Session session, String message) {
		Command.EType type = Command.EType.Channel;
		String allowedChannel = Data.config.getString("idlerpg-channel");
		if ((!allowedChannel.isEmpty())
				&& (!allowedChannel.equalsIgnoreCase(session.channel.getName())))
			type = Command.EType.Notice;

		Shocky.send(session.bot, type, session.channel, session.user, message);
	}

	public static void announce(MessageEvent<ShockyBot> ev, String message) {
		Command.EType type = Command.EType.Channel;
		String allowedChannel = Data.config.getString("idlerpg-channel");
		if ((!allowedChannel.isEmpty())
				&& (!allowedChannel.equalsIgnoreCase(ev.getChannel().getName()))
				&& (!Data.config.getBoolean("idlerpg-announce")))
			type = Command.EType.Notice;

		Shocky.send(ev.getBot(), type, ev.getChannel(), ev.getUser(), message);
	}

	public static void announce(Session session, String message) {
		Command.EType type = Command.EType.Channel;
		String allowedChannel = Data.config.getString("idlerpg-channel");
		if ((!allowedChannel.isEmpty())
				&& (!allowedChannel.equalsIgnoreCase(session.channel.getName()))
				&& (!Data.config.getBoolean("idlerpg-announce")))
			type = Command.EType.Notice;

		Shocky.send(session.bot, type, session.channel, session.user, message);
	}

	public void onMessage(MessageEvent<ShockyBot> ev) {
		if (Data.isBlacklisted(ev.getUser()))
			return;

		String msg = ev.getMessage();
		if (!msg.startsWith(">"))
			return;

		String identify = Shocky.getLogin(ev.getUser());
		if (identify == null) {
			send(ev, ev.getUser().getNick()+": You need to be identified to NickServ to play IdleRPG.");
			return;
		}
		Session session = new Session(ev.getBot(), ev.getChannel(), ev.getUser(), identify);
		session.player = this.players.get(identify);

		msg = msg.substring(1).trim();
		String[] spl = msg.isEmpty() ? new String[0] : msg.split(" ");
		if ((spl.length == 0)
				|| ((spl.length <= 2) && ("status".startsWith(spl[0]
						.toLowerCase())))) {
			Player check = session.player;
			if (spl.length == 2)
				check = this.players.get(spl[1]);

			if (check == null) {
				if (spl.length == 2) {
					send(ev, ev.getUser().getNick() + ": Player '" + spl[1]
							+ "' doesn't exist.");
					return;
				}
				this.players.put(identify,new Player(identify));
				send(ev, ev.getUser().getNick() + ": Welcome to the IdleRPG, "
						+ identify + '!');
				return;
			}

			check.update(session);
			send(ev, check.printStatus(session, true, true));
		} else if ((spl.length == 1)
				&& ("leaderboards".startsWith(spl[0].toLowerCase()))) {
			for (Player p : this.players.values())
				p.update(session);

			ArrayList<Player> list = new ArrayList<Player>(this.players.values());
			Collections.sort(list, new ComparatorLevel(false));

			StringBuilder print = new StringBuilder();
			StringBuilder paste = new StringBuilder();

			int maxPrint = Data.config.getInt("idlerpg-leaderboards-print");
			for (int i = 0; i < list.size(); i++) {
				if (i != 0) {
					if (i < maxPrint)
						print.append(" | ");
					paste.append('\n');
				}

				Player p = (Player) list.get(i);
				if (i < maxPrint)
					print.append(i + 1).append(". ")
					.append(p.printStatus(session, list.size() <= maxPrint, false));
				paste.append(i + 1).append(". ").append(p.printStatus(session, true, false));
			}
			if (list.size() > maxPrint) {
				String url = Utils.paste(paste);
				if ((url != null) && (!url.isEmpty()))
					print.append(" | Full leaderboards: ").append(url);
			}

			send(ev, Utils.mungeAllNicks(ev.getChannel(), 0, print, ev.getUser()));
		}
	}

	public static class Player {
		public String name;
		public int level;
		public int xp;
		public long lastUpdate;
		
		private static final Map<Integer,Integer> xpTable = new HashMap<Integer,Integer>();
		public static int getXPForLevel(int level) {
			if (level <= 1)
				return 0;
			if (xpTable.containsKey(level))
				return xpTable.get(level);
			long a = 0L;
			for (int x = 1; x < level; ++x)
				a += (int) (x + 300.0D * Math.pow(2.0D, x / 7.0D));
			int value = (int) (a / 4.0D);
			xpTable.put(level, value);
			return value;
		}
		
		public int getXPForNextLevel() {
			return getXPForLevel(this.level + 1);
		}

		public static String printBar(double value, int length) {
			double f = 1.0D / length;
			char[] c = new char[length + 2];
			int i = 0;
			c[i++]='[';
			for (; i <= length; ++i)
				c[i]=(value >= i * f) ? '=' : ' ';
			c[i]=']';
			return new String(c);
		}

		public Player(String name) {
			this.name = name;
			this.level = 1;
			this.xp = 0;
			this.lastUpdate = (System.currentTimeMillis() / 1000L);
		}

		public void update(Session session) {
			long time = System.currentTimeMillis() / 1000L;
			long diff = time - this.lastUpdate;
			this.lastUpdate = time;

			int xp2l = getXPForNextLevel();
			this.xp = (int) (this.xp + diff);
			if ((session != null) && (session.player == this)
					&& (this.xp >= xp2l)) {
				this.xp = 0;
				this.level += 1;
			}

			if (this.xp > xp2l)
				this.xp = xp2l;
		}

		public String printStatus(Session session, boolean printXP, boolean printTime) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.name);
			if ((session.identify.equalsIgnoreCase(this.name)) && (!session.user.getNick().equalsIgnoreCase(this.name)))
				sb.append(" / ").append(session.user.getNick());
			sb.append(", level ").append(this.level);

			int xp2l = getXPForNextLevel();
			if (printXP) {
				if ((this.level != 1) && (this.xp == 0)) {
					sb.append(", LEVEL UP!");

					if (this.level % 5 == 0) {
						StringBuilder sb2 = new StringBuilder();
						sb2.append(">>> CONGRATULATIONS! ").append(this.name);
						if ((session.identify.equalsIgnoreCase(this.name))
								&& (!session.user.getNick()
										.equalsIgnoreCase(this.name)))
							sb2.append(" / ").append(session.user.getNick());
						sb2.append(" achieved level ").append(this.level).append("! <<<");
						ModuleIdleRPG.announce(session, sb2.toString());
					}
				} else if (this.xp == xp2l)
					sb.append(", level up available");
				else 
					sb.append(", XP: ").append(formatXP.format(this.xp)).append(" / ").append(formatXP.format(xp2l));

				if ((this.xp != 0) && (this.xp != xp2l))
					sb.append(' ')
					.append(printBar(1.0D * this.xp / xp2l, 20))
					.append(" (").append(formatXPPercent.format(100.0D * this.xp / xp2l)).append("%)");
			}
			if ((printTime) && (this.xp != xp2l))
				sb.append(" | ").append(Utils.timeAgo(xp2l - this.xp)).append(" until level up");
			return sb.toString();
		}
	}
	
	public static class ComparatorLevel implements Comparator<Player> {
		public final int dir;
		public ComparatorLevel(boolean asc) {
			dir = asc ? 1 : -1;
		}
		public int compare(Player p1, Player p2) {
			if (p1.level != p2.level)
				return p1.level < p2.level ? -dir : dir;
			if (p1.xp != p2.xp)
				return p1.xp < p2.xp ? -dir : dir;
			return 0;
		}
	}
	
	public static class ComparatorIgnoreCase implements Comparator<String> {
		public int compare(String s1, String s2) {
			return s1.compareToIgnoreCase(s2);
		}
	}

	public static class Session {
		public final PircBotX bot;
		public final Channel channel;
		public final User user;
		public final String identify;
		public Player player;

		public Session(PircBotX bot, Channel channel, User user, String identify) {
			this.bot = bot;
			this.channel = channel;
			this.user = user;
			this.identify = identify;
		}
	}
	
	public LuaValue getPlayerTable(String name) {
		if (!players.containsKey(name))
			return LuaValue.NIL;
		Player player = players.get(name);
		player.update(null);
		return getPlayerTable(player);
	}
	
	public LuaValue getPlayerTable(Player player) {
		LuaTable t = new LuaTable();
		t.rawset("name", player.name);
		t.rawset("xp", player.xp);
		t.rawset("need", player.getXPForNextLevel());
		t.rawset("level", player.level);
		t.rawset("lastUpdate", player.lastUpdate);
		return t;
	}
	
	public class StatusFunction extends OneArgFunction {

		@Override
		public LuaValue call(LuaValue arg) {
			return getPlayerTable(arg.checkjstring());
		}
	}
	
	public class LeaderboardFunction extends ZeroArgFunction {

		@Override
		public LuaValue call() {
			int maxPrint = Data.config.getInt("idlerpg-leaderboards-print");
			List<Player> list = new ArrayList<Player>(players.values());
			
			for (Player p : list)
				p.update(null);
			Collections.sort(list, new ComparatorLevel(false));
			
			LuaTable t = new LuaTable();
			for (int i = 0; i < maxPrint && i < list.size(); ++i)
				t.rawset(i+1, getPlayerTable(list.get(i)));
			return t;
		}
	}

	@Override
	public void setupLua(LuaTable env) {
		try {
			Class.forName("ModuleIdleRPG$Player");
			Class.forName("ModuleIdleRPG$ComparatorLevel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		LuaTable t = new LuaTable();
		t.rawset("status", new StatusFunction());
		t.rawset("leaders", new LeaderboardFunction());
		env.set("idlerpg", t);
	}
}