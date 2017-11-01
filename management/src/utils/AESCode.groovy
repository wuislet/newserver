import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
public class AESCode {
/**
	 * �ṩ��Կ���������м���
	 * 
	 * @param sSrc
	 * @param key
	 * @param iv
	 * @return
	 * @throws Exception
	 */
	public static String Encrypt(String sSrc, byte[] key, byte[] iv) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "�㷨/ģʽ/���뷽ʽ"
		IvParameterSpec _iv = new IvParameterSpec(iv);// ʹ��CBCģʽ����Ҫһ������iv�������Ӽ����㷨��ǿ��
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, _iv);
		byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
		return Base64.encodeBase64String(encrypted);
	}
	/**
	 * �ṩ��Կ���������н���
	 * 
	 * @param sSrc
	 * @param key
	 * @param iv
	 * @return
	 * @throws Exception
	 */
	public static String Decrypt(String sSrc, byte[] key, byte[] iv) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		IvParameterSpec _iv = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, _iv);
		byte[] encrypted = Base64.decodeBase64(sSrc);
		byte[] original = cipher.doFinal(encrypted);
		return new String(original, "utf-8");
	}
	/**
	 * ʹ����Կ���м���
	 * 
	 * @param sSrc
	 * @param keyStr
	 * @return
	 * @throws Exception
	 */
	public static String Encrypt(String sSrc, String keyStr) throws Exception {
		byte[] key = GeneralKey(keyStr);
		byte[] iv = GeneralIv(keyStr);
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "�㷨/ģʽ/���뷽ʽ"
		IvParameterSpec _iv = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, _iv);
		byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
		return Base64.encodeBase64String(encrypted);
	}
	/**
	 * ʹ����Կ���н���
	 * 
	 * @param sSrc
	 * @param keyStr
	 * @return
	 * @throws Exception
	 */
	public static String Decrypt(String sSrc, String keyStr) throws Exception {
		byte[] key = GeneralKey(keyStr);
		byte[] iv = GeneralIv(keyStr);
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		IvParameterSpec _iv = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, _iv);
		byte[] encrypted = Base64.decodeBase64(sSrc);// ����base64����
		byte[] original = cipher.doFinal(encrypted);
		return new String(original, "utf-8");
	}
	/**
	 * ������Կ�ֽ���
	 * 
	 * @param keyStr
	 * @return
	 * @throws Exception
	 */
	private static byte[] GeneralKey(String keyStr) throws Exception {
		byte[] bytes = keyStr.getBytes("utf-8");
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(bytes);
		return md.digest();
	}
	/**
	 * �����ӽ��������ֽ���
	 * 
	 * @param keyStr
	 * @return
	 * @throws Exception
	 */
	private static byte[] GeneralIv(String keyStr) throws Exception {
		byte[] bytes = keyStr.getBytes("utf-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(bytes);
		return md.digest();
	}
}