package pl.shockah.shocky;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.pircbotx.PircBotX;
import pl.shockah.shocky.events.ActionOutEvent;
import pl.shockah.shocky.events.MessageOutEvent;
import pl.shockah.shocky.events.NoticeOutEvent;
import pl.shockah.shocky.events.PrivateMessageOutEvent;

@SuppressWarnings({"rawtypes","unchecked"}) public class ListenerAdapter extends org.pircbotx.hooks.ListenerAdapter<PircBotX> {
	static {
		for (Method curMethod : ListenerAdapter.class.getDeclaredMethods()) {
			if (curMethod.getName().equals("onEvent")) continue;
			Class curClass = curMethod.getParameterTypes()[0];
			if (!curClass.isInterface()) {
				Set methods = new HashSet();
				methods.add(curMethod);
				eventToMethod.put(curClass, methods);
			}
		}
		for (Method curMethod : ListenerAdapter.class.getDeclaredMethods()) {
			Class curClass = curMethod.getParameterTypes()[0];
			if (!curClass.isInterface()) continue;
			for (Class curEvent : eventToMethod.keySet()) if (curClass.isAssignableFrom(curEvent)) (eventToMethod.get(curEvent)).add(curMethod);
		}
	}
	
	public void onMessageOut(MessageOutEvent<PircBotX> event) throws Exception {}
	public void onActionOut(ActionOutEvent<PircBotX> event) throws Exception {}
	public void onPrivateMessageOut(PrivateMessageOutEvent<PircBotX> event) throws Exception {}
	public void onNoticeOut(NoticeOutEvent<PircBotX> event) throws Exception {}
}