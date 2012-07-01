package org.pircbotx;

import java.util.Comparator;

public class NickComparator implements Comparator<PircBotX>{
	public static final NickComparator singleton = new NickComparator();
	
	private NickComparator() {
	}

	@Override
	public int compare(PircBotX o1, PircBotX o2) {
		return o1.getNick().compareTo(o2.getNick());
	}
}
