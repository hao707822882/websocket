/**
 * License
 * 
 * Licensed under the GNU GPL v3
 * http://www.gnu.org/licenses/gpl.html
 * 
 */
package com.googlecode.lineblog.websocket.v2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * 
 * @author lichangshu E-mail:lchshu001@gmail.com
 * @version 2011-9-30 下午03:30:01
 */
public class HelpUtil {
	/**
	 * \r\n
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] readLine(InputStream in) throws IOException {
		ByteArrayOutputStream bts = new ByteArrayOutputStream();
		int last = 0;
		while (true) {
			int b = in.read();
			if (b == -1)
				break;
			bts.write(b);
			if (last == '\r' && b == '\n') {
				break;
			}
			last = b;
		}
		return bts.toByteArray();
	}

	/**
	 * 高字节在前
	 * @param b
	 * @return
	 */
	public static long toLong(byte ... b) {
		long l = 0;
		int len = b.length;
		for(int i=0; i<len && i < 8; i++){
			l = (long) l << 8 | byte2UnsignInt(b[i]);
		}
		return l;
	}

	/**
	 * 字节转无符号整数
	 * @param b
	 * @return
	 */
	public static int byte2UnsignInt(byte b) {
		if( b >> 1 == b >>> 1)
			return b;
		else
			return 256 + b;
	}

	/**
	 * 高字节在前
	 * @param b
	 * @return
	 */
	public static int toInt(byte ... b) {
		return (int)toLong(b);
	}

	/**
	 * 高字节在前
	 * @param b
	 * @return
	 */
	public static short toShort(byte ... b) {
		return (short)toLong(b);
	}

	/**
	 * MD5
	 * 
	 * @param plainText
	 * @return
	 */

	public static String md5(String plainText) {
		return md5(plainText.getBytes());
	}

	public static String md5(byte[] plainText) {
		String result = "";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(plainText);
			byte b[] = md.digest();
			int i;
			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
			result = buf.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		System.out.println(toLong((byte)1,(byte)2,(byte)3,(byte)4));
		System.out.println((int)((byte)-128));
		byte a = 0;
		System.out.println(a);
	}

	/**
	 * 返回null 表示结束！
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] readFrame(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int t = in.read();
		if(t == -1) return null;
		while (true) {
			int r = in.read();
			if (r == 0x0)
				throw new RuntimeException("data format error!");
			if (r == 0Xff || r == -1) {
				return out.toByteArray();
			} else {
				out.write(r);
			}
		}
	}

	public static byte[] readFrame_V6(InputStream in) throws IOException {
		return null;
	}

	public static String readFrameString(InputStream in) throws IOException {
		return new String(readFrame(in), WEB_SOCKET_CHARSET);
	}

	public static final String WEB_SOCKET_CHARSET = "UTF-8";

	public static void writeFrame(OutputStream out, String content)
			throws IOException {
		out.write(0x00);
		out.write(content.getBytes(WEB_SOCKET_CHARSET));
		out.write(0xff);
	}

	public static byte[] intTo4Byte(int i) {
		return numberToByte(i, 4);
	}

	public static byte[] shortTo4Byte(int i) {
		return numberToByte(i, 2);
	}

	public static byte[] longTo4Byte(long i) {
		return numberToByte(i, 8);
	}

	/**
	 * 从高位向低位取值，高位在前，慎用
	 * @param l
	 * @param length
	 * @return
	 */
	private static byte[] numberToByte(long l, int length) {
		byte[] bts = new byte[length];
		for(int i=0; i<length; i++){
			bts[i] = (byte) (l >> ((length -i - 1) * 8));
		}
		return bts;
	}

	public static int parseWebsokcetKey(String str) {
		str = str.trim();
		return (int) (Long.valueOf(str.replaceAll("\\D", "")) / str.replaceAll(
				"\\S", "").length());
	}


	public static String formatBytes(byte ... bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (byte byt : bytes) {
			sb.append(String.format("%02X ", byt));
		}
		return sb.toString();
	}
	
	public static String getBASE64(byte[] b) {
		String s = null;
		if (b != null) {
			s = new sun.misc.BASE64Encoder().encode(b);
		}
		return s;
	}

	public static byte[] getFromBASE64(String s) {
		byte[] b = null;
		if (s != null) {
			sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
			try {
				b = decoder.decodeBuffer(s);
				return b;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return b;
	}
	
	public static void writeFile(String path, OutputStream out) throws IOException{
		File file = new File(path);
		if(file.exists() && !file.isDirectory()){
			out.write((
				"HTTP/1.1 200 OK" +
				"\r\n" +
				"Content-Type: text/html" +
				"\r\n" +
				"Content-Length: " + file.length() +
				"\r\n" +
				"\r\n").getBytes());
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			while(true){
				int data = in.read();
				if(data == -1) break;
				out.write(data);
			}
			in.close();
		}else{
			String ct = "400 - NOT FIND! \r\n PATH : " + path;
			out.write((
				"HTTP/1.1 400 NOT FIND" +
				"\r\n" +
				"Content-Type: text/html" +
				"\r\n" +
				"Content-Length: " + ct.getBytes("iso-8859-1") +
				"\r\n" +
				"\r\n").getBytes());
			out.write(ct.getBytes());
		}
		out.flush();
	}
}
