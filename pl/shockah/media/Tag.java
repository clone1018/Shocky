package pl.shockah.media;

public class Tag {
	public String artist = "", title = "", album = "", comment = "", genre = "";
	public int year = 0;
	
	public boolean allowExtension(String ext) {
		return false;
	}
}