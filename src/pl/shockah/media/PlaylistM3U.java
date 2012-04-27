package pl.shockah.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import pl.shockah.Pair;

public class PlaylistM3U extends Playlist {
	public ArrayList<Pair<File,Tag>> load(File file) {
		ArrayList<Pair<File,Tag>> pairs = new ArrayList<Pair<File,Tag>>();
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			String strLine;
			if (br.readLine().equals("#EXTM3U")) {
				while ((strLine = br.readLine()) != null) {
					if (strLine.isEmpty()) continue;
					
					Tag stag = new Tag();
					if (strLine.startsWith("#EXTINF")) {
						strLine = strLine.substring(8);
						stag.title = strLine = strLine.substring(strLine.indexOf(',')+1);
						
						strLine = br.readLine();
					}
					
					File sfile = new File(strLine);
					if (!sfile.isAbsolute()) sfile = new File(file.getParentFile(),strLine);
					if (stag.title.isEmpty()) {
						TagID3v1 id3 = new TagID3v1();
						id3.read(sfile);
						if (stag.artist.isEmpty()) stag.artist = id3.artist;
						if (stag.title.isEmpty()) stag.title = id3.title;
					}
					
					pairs.add(new Pair<File,Tag>(sfile,stag));
				}
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();}
		
		return pairs;
	}
	public void write(File file, ArrayList<Pair<File, Tag>> pairs) {
		
	}
}