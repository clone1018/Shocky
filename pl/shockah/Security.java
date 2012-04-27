package pl.shockah;

import java.security.MessageDigest;

public class Security {
	public static String md5AsString(BinBuffer binb) {
		try {
			MessageDigest alg = MessageDigest.getInstance("MD5");
			alg.reset();
			alg.update(binb.getByteBuffer());
			
			byte[] digest = alg.digest();
			StringBuffer sb = new StringBuffer();
			
			for (byte b : digest) sb.append(Integer.toHexString(0xFF & b));
			return sb.toString();
		} catch (Exception e) {e.printStackTrace();}
		return "";
	}
}