package pl.shockah.shocky;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;

public class ShockyListenerManager<E extends PircBotX> extends ThreadedListenerManager<E> {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void dispatchEvent(final Event<E> event) {
		synchronized (this.listeners) {
			String channel = getEventChannel(event);
			for (final Listener curListener : this.listeners) {
				if (channel == null || allowEvent(channel,curListener))
				this.pool.submit(new Runnable() {
					public void run() {
						try {
							curListener.onEvent(event);
						} catch (Throwable t) {
							event.getBot().logException(t);
						}
					}
				});
			}
		}
	}
	
	public String getEventChannel(Event<E> event) {
		if (event instanceof MessageEvent)
			return ((MessageEvent<E>)event).getChannel().getName();
		else if (event instanceof ActionEvent)
			return ((ActionEvent<E>)event).getChannel().getName();
		else if (event instanceof InviteEvent)
			return ((InviteEvent<E>)event).getChannel();
		else if (event instanceof ModeEvent)
			return ((ModeEvent<E>)event).getChannel().getName();
		else if (event instanceof JoinEvent)
			return ((JoinEvent<E>)event).getChannel().getName();
		else if (event instanceof KickEvent)
			return ((KickEvent<E>)event).getChannel().getName();
		else if (event instanceof PartEvent)
			return ((PartEvent<E>)event).getChannel().getName();
		else if (event instanceof NoticeEvent) {
			Channel channel = ((NoticeEvent<E>)event).getChannel();
			return channel != null ? channel.getName() : null;
		}
		return null;
	}
	
	public boolean allowEvent(String channel, Listener<E> curListener) {
		if (curListener instanceof Module) {
			Module module = (Module)curListener;
			return module.isEnabled(channel);
		}
		return true;
	}
}
