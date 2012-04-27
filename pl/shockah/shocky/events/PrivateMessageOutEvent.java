package pl.shockah.shocky.events;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class PrivateMessageOutEvent<T extends PircBotX> extends Event<T> implements GenericMessageEvent<T> {
	protected final User user;
	protected final String message;

	public PrivateMessageOutEvent(T bot, User user, String message) {
		super(bot);
		this.user = user;
		this.message = message;
	}

	public void respond(String response) {}
	public User getUser() {
		return user;
	}
	public String getMessage() {
		return this.message;
	}

	public String toString() {
		return "PrivateMessageOutEvent(user=" + getUser() + ", message=" + getMessage() + ")";
	}

	@SuppressWarnings("unchecked") public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof PrivateMessageOutEvent<?>)) return false;
		PrivateMessageOutEvent<? extends PircBotX> other = (PrivateMessageOutEvent<? extends PircBotX>)o;
		if (!other.canEqual(this)) return false;
		if (!super.equals(o)) return false;
		if (getUser() == null ? other.getUser() != null : !getUser().equals(other.getUser())) return false;
		return getMessage() == null ? other.getMessage() == null : getMessage().equals(other.getMessage());
	}

	public boolean canEqual(Object other) {
		return other instanceof PrivateMessageOutEvent;
	}

	public int hashCode() {
		int result = 1;
		result = result * 31 + super.hashCode();
		result = result * 31 + (getUser() == null ? 0 : getUser().hashCode());
		result = result * 31 + (getMessage() == null ? 0 : getMessage().hashCode());
		return result;
	}
}