package pl.shockah;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FileLine {
	public static ArrayList<String> read(File file) {
		ArrayList<String> ret = new ArrayList<String>();
		if (!file.exists()) return ret;
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
			String line; while ((line = br.readLine()) != null) ret.add(line);
			br.close();
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}
	public static void write(File file, ArrayList<String> lines) {
		if (file.exists()) file.delete();
		try {
			file.createNewFile();
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
			for (int i = 0; i < lines.size(); i++) {
				if (i != 0) bw.write('\n');
				bw.write(lines.get(i));
			}
			bw.close();
		} catch (Exception e) {e.printStackTrace();}
	}
}