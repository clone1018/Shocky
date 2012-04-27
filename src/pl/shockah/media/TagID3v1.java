package pl.shockah.media;

import java.io.File;

import pl.shockah.BinBuffer;
import pl.shockah.BinFile;

public class TagID3v1 extends Tag {
	protected static final String[] genres = new String[]{
		"Blues","Classic Rock","Country","Dance","Disco","Funk","Grunge","Hip-Hop","Jazz","Metal","New Age","Oldies","Other","Pop",
		"R&B","Rap","Reggae","Rock","Techno","Industrial","Alternative","Ska","Death Metal","Pranks","Soundtrack","Euro-Techno",
		"Ambient","Trip-Hop","Vocal","Jazz+Funk","Fusion","Trance","Classical","Instrumental","Acid","House","Game","Sound Clip",
		"Gospel","Noise","Alternative Rock","Bass","Soul","Punk","Space","Meditative","Instrumental Pop","Instrumental Rock",
		"Ethnic","Gothic","Darkwave","Techno-Industrial","Electronic","Pop-Folk","Eurodance","Dream","Southern Rock","Comedy",
		"Cult","Gangsta","Top 40","Christian Rap","Pop/Funk","Jungle","Native American","Cabaret","New Wave","Psychadelic","Rave",
		"Showtunes","Trailer","Lo-Fi","Tribal","Acid Punk","Acid Jazz","Polka","Retro","Musical","Rock & Roll","Hard Rock"};
	
	public void read(File file) {
		BinBuffer buffer = new BinBuffer();
		new BinFile(file,false).read(buffer);
		
		buffer.setPos(buffer.getSize()-128);
		String str = buffer.readChars(128);
		str = new StringBuilder(str).reverse().toString();
		
		if (!str.startsWith("TAG")) return;
		
		title = deleteNull(str.substring(3,30+3-1));
		artist = deleteNull(str.substring(33,30+33-1));
		album = deleteNull(str.substring(63,30+63-1));
		year = Integer.parseInt(deleteNull(str.substring(93,4+93-1)));
		comment = deleteNull(str.substring(97,30+97-1));
		genre = getGenre(str.charAt(127));
	}
	
	protected String deleteNull(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == 0) str = new StringBuilder(str).deleteCharAt(i--).toString();
		}
		return str;
	}
	protected String getGenre(int genreID) {
		if (genreID >= 0 && genreID <= genres.length-1) return genres[genreID];
		return "Unknown";
	}
	
	public boolean allowExtension(String ext) {
		return ext.equals("mp3");
	}
}