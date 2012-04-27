package pl.shockah.media;

import java.io.File;
import java.util.ArrayList;
import pl.shockah.Pair;

public abstract class Playlist {
	public abstract ArrayList<Pair<File,Tag>> load(File file);
	public abstract void write(File file, ArrayList<Pair<File,Tag>> pairs);
}