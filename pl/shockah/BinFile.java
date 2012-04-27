package pl.shockah;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BinFile {
	public final File file;
	public final boolean gzip;
	
	public BinFile(File file) {this(file,false);}
	public BinFile(File file, boolean gzip) {
		this.file = file;
		this.gzip = gzip;
	}
	
	public BinBuffer read() {return read(file.length());}
	public BinBuffer read(long bytes) {
		BinBuffer binb = new BinBuffer((int)file.length());
		read(binb,bytes);
		return binb;
	}
	public void read(BinBuffer buffer) {read(buffer,file.length());}
	public void read(BinBuffer buffer, long bytes) {
		if (file.exists()) {
			try {
				int value; long read = 0;
				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);
				
				if (gzip) {
					GZIPInputStream gis = new GZIPInputStream(bis);
					while ((value = gis.read()) != -1) {
						buffer.writeByte(value);
						if (++read == bytes) break;
					}
					gis.close();
				} else {
					while ((value = bis.read()) != -1) {
						buffer.writeByte(value);
						if (++read == bytes) break;
					}
				}
				
				bis.close();
				fis.close();
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	
	public void write(BinBuffer buffer) {
		if (file.exists()) file.delete();
		append(buffer);
	}
	public void append(BinBuffer buffer) {
		try {
			if (!file.exists()) file.createNewFile();
			
			FileOutputStream fos = new FileOutputStream(file,true);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			if (gzip) {
				GZIPOutputStream gos = new GZIPOutputStream(bos);
				while (buffer.bytesLeft() > 0) gos.write(buffer.readByte());
				gos.flush(); gos.close();
			} else {
				while (buffer.bytesLeft() > 0) bos.write(buffer.readByte());
				bos.flush();
			}
			
			bos.close();
			fos.close();
		} catch (Exception e) {e.printStackTrace();}
	}
}