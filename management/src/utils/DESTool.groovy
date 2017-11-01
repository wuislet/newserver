package utils;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;
 
import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
 
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
/**
* 使用DES加密和解密的方法
* 
* @修改者  小老虎2
* @author:azhong 
* @change:exmorning
* */
public class DESTool {

   private final byte[] DESkey = "kingfykj".getBytes("UTF-8");// 设置密钥，略去
   private final byte[] DESIV = [0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF] as byte[];// 设置向量，略去

   private AlgorithmParameterSpec iv = null;// 加密算法的参数接口，IvParameterSpec是它的一个实现
   private Key key = null;

   public DESTool() throws Exception {
       DESKeySpec keySpec = new DESKeySpec(DESkey);// 设置密钥参数
       iv = new IvParameterSpec(DESIV);// 设置向量
       SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");// 获得密钥工厂
       key = keyFactory.generateSecret(keySpec);// 得到密钥对象

   }

   public String encode(String data) throws Exception {
       Cipher enCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");// 得到加密对象Cipher
       enCipher.init(Cipher.ENCRYPT_MODE, key, iv);// 设置工作模式为加密模式，给出密钥和向量
       byte[] pasByte = enCipher.doFinal(data.getBytes("utf-8"));
       BASE64Encoder base64Encoder = new BASE64Encoder();
       return base64Encoder.encode(pasByte);
   }

   public String decode(String data) throws Exception {
       Cipher deCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
       deCipher.init(Cipher.DECRYPT_MODE, key, iv);
       BASE64Decoder base64Decoder = new BASE64Decoder();

       byte[] pasByte = deCipher.doFinal(base64Decoder.decodeBuffer(data));

       return new String(pasByte, "UTF-8");
   }
   public static void main(String[] args) {
       try {
           String test = "aaaaaa";
           DESTool des = new DESTool();//自定义密钥
           System.out.println("加密前的字符："+test);
           System.out.println("加密后的字符："+des.encode(test));
           System.out.println("解密后的字符："+des.decode(des.encode(test)));
       } catch (Exception e) {
           e.printStackTrace();
       }
   }
}