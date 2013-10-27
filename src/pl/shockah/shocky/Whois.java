package pl.shockah.shocky;

import org.pircbotx.ShockyUser;
import org.pircbotx.User;

public class Whois {
	
	public static String getWhoisLogin(User user) {
		if (user instanceof ShockyUser)
			return ((ShockyUser)user).getAccount();
		return null;
	}
	public static void clearWhois(User user) {
		if (user instanceof ShockyUser)
			((ShockyUser)user).account = null;
	}
	
	public static void setWhois(User user, String account) {
		if (user instanceof ShockyUser)
			((ShockyUser)user).account = account;
	}
}