/**
 * License
 * 
 * Licensed under the GNU GPL v3
 * http://www.gnu.org/licenses/gpl.html
 * 
 */
package com.googlecode.lineblog.websocket.v2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;

import com.googlecode.lineblog.websocket.v2.HelpUtil;

public class SimpleWebSocketClient {
	public static void main(String[] args) throws Exception {
		Socket sk = new Socket("127.0.0.1",8091);
		BufferedInputStream in = new BufferedInputStream(sk.getInputStream());
		BufferedOutputStream writer = new BufferedOutputStream(sk.getOutputStream());
		writer.write("GET / HTTP/1.1".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("Upgrade: WebSocket".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("Connection: Upgrade".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("Origin: null".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("Sec-WebSocket-Key1: 4 @1  46546xW%0l 1 5".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("Sec-WebSocket-Key2: 12998 5 Y3 1  .P00".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("Host: 127.0.0.1".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("\r\n".getBytes());
		writer.write("12345678".getBytes("iso-8859-1"));
		System.out.println(HelpUtil.formatBytes("12345678".getBytes("iso-8859-1")));
		writer.flush();

		Thread.sleep(3000);//waiting for server return
		if(in.available() > 0){
			byte[] buf = new byte[in.available()];
			in.read(buf);
			System.out.print(new String(buf, "gbk"));
		}
		sk.close();
	}
}