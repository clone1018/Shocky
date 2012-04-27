package pl.shockah.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TaskFileCopy extends Task {
	protected final File src, dst;
	
	public TaskFileCopy(ITaskCallback callback, File source, File destination) {
		super(callback);
		src = source;
		dst = destination;
	}
	
	public void run() {
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
					long current = 0, size = src.length();
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
		progress = 1f;
		super.run();
	}
}