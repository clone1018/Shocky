package pl.shockah.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import pl.shockah.Pair;

public class TaskFileCopyMulti extends Task {
	protected final ArrayList<Pair<File,File>> pairs;
	
	public TaskFileCopyMulti(ITaskCallback callback, ArrayList<Pair<File,File>> pairs) {
		super(callback);
		this.pairs = pairs;
	}
	
	public void run() {
		long current = 0, size = totalSize();
		
		for (Pair<File,File> pair : pairs) {
			File src = pair.get1();
			File dst = pair.get2();
			if (src.exists() && src.isFile()) {
				if (!dst.exists() || dst.isFile()) {
					try {
						if (dst.exists()) dst.delete();
						dst.createNewFile();
						progressStr = src.getAbsolutePath()+"\n"+dst.getAbsolutePath();
						
						FileInputStream fis = new FileInputStream(src);
						BufferedInputStream bis = new BufferedInputStream(fis);
						
						FileOutputStream fos = new FileOutputStream(dst);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						
						int value;
						while ((value = bis.read()) != -1) {
							progress = ((float)current)/((float)size);
							bos.write(value);
							current++;
						}
						
						bos.flush();
						bos.close();
						fos.close();
						
						bis.close();
						fis.close();
					} catch (FileNotFoundException e) {e.printStackTrace();
					} catch (IOException e) {e.printStackTrace();}
				}
			}
		}
		progress = 1f;
		super.run();
	}
	
	protected long totalSize() {
		long size = 0;
		for (Pair<File,File> pair : pairs) {
			File file = pair.get1();
			if (file.exists() && file.isFile()) size += file.length();
		}
		return size;
	}
}