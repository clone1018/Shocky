package pl.shockah;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Security {
	public static byte[] md5AsBytes(ByteBuffer buffer) {
		MessageDigest alg;
		try {
			alg = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		alg.update(buffer);
		return alg.digest();
	}
	
	public static String md5AsString(ByteBuffer buffer) {
		byte[] digest = md5AsBytes(buffer);
		StringBuffer sb = new StringBuffer(digest.length<<1);
		for (int i = 0; i < digest.length; ++i)
			sb.append(String.format("%02x", digest[i]));
		return sb.toString();
	}
}