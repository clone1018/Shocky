package pl.shockah;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
				InputStream is = new BufferedInputStream(new FileInputStream(file));
				if (gzip)
					is = new GZIPInputStream(is);
				
				while ((value = is.read()) != -1) {
					buffer.writeByte(value);
					if (++read == bytes) break;
				}
				
				is.close();
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
			
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file,true));
			if (gzip)
				os = new GZIPOutputStream(os);
			
			while (buffer.bytesLeft() > 0)
				os.write(buffer.readByte());
			
			os.flush();
			os.close();
		} catch (Exception e) {e.printStackTrace();}
	}
}