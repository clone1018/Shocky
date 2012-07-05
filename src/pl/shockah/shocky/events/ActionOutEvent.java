package pl.shockah.shocky.events;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class ActionOutEvent<T extends PircBotX> extends Event<T> implements GenericMessageEvent<T> {
	protected final Channel channel;
	protected final String message;

	public ActionOutEvent(T bot, Channel channel, String message) {
		super(bot);
		this.channel = channel;
		this.message = message;
	}

	public void respond(String response) {}
	public Channel getChannel() {
		return this.channel;
	}
	public String getMessage() {
		return this.message;
	}
	public User getUser() {
		return bot.getUserBot();
	}

	public String toString() {
		return "ActionOutEvent(channel=" + getChannel() + ", message=" + getMessage() + ")";
	}

	@SuppressWarnings("unchecked") public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ActionOutEvent<?>)) return false;
		ActionOutEvent<? extends PircBotX> other = (ActionOutEvent<? extends PircBotX>)o;
		if (!other.canEqual(this)) return false;
		if (!super.equals(o)) return false;
		if (getChannel() == null ? other.getChannel() != null : !getChannel().equals(other.getChannel())) return false;
		return getMessage() == null ? other.getMessage() == null : getMessage().equals(other.getMessage());
	}

	public boolean canEqual(Object other) {
		return other instanceof ActionOutEvent;
	}

	public int hashCode() {
		int result = 1;
		result = result * 31 + super.hashCode();
		result = result * 31 + (getChannel() == null ? 0 : getChannel().hashCode());
		result = result * 31 + (getMessage() == null ? 0 : getMessage().hashCode());
		return result;
	}
}