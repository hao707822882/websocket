/**
 * License
 * 
 * Licensed under the GNU GPL v3
 * http://www.gnu.org/licenses/gpl.html
 * 
 */
package com.googlecode.lineblog.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author lichangshu E-mail:lchshu001@gmail.com
 * @version 2011-9-30 下午03:30:17
 */
public class TokenThread extends Thread {

	public static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	public static final String HEADER_CODE = "iso-8859-1";
	public static final String PROTOCOL = "chat";
	public static final int SPLITVERSION = 6;

	private int webSocketVersion = 0;
	private String token;
	private OutputStream out;
	private InputStream in;
	private Socket socket;
	private static Log log = LogFactory.getLog(TokenThread.class);

	public TokenThread(Socket socket) throws IOException {
		if (socket == null)
			throw new RuntimeException("socket is not null!");
		this.socket = socket;
		this.in = new BufferedInputStream(this.socket.getInputStream());
		this.out = new BufferedOutputStream(this.socket.getOutputStream());
		this.accept();//握手
	}

	public synchronized void sendMessage(String message) throws IOException {
		if(webSocketVersion < 1)
			HelpUtil.writeFrame(out, message);
		else
			WebSocketFram.writeWebSocketV6Fram(out, message);
		out.flush();
	}

	public boolean socketIsClosed(){
		return this.socket.isClosed();
	}

	public void logOut(){
		if(socketIsClosed()) return;
		try {
			if(webSocketVersion <= 0)
				out.write(WebSocketFram.getCloseV1To6());
			if(webSocketVersion < 6)
				out.write(WebSocketFram.getCloseV1To6());
			else
				out.write(WebSocketFram.getCloseV7UP());
			out.flush();
			this.in.close();
			this.socket.close();
			TokenManage.getTokenManage().removeTokenThread(this.token);
			log.info("[" + this.token + "] has logout!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while(true){
				byte[] bts;
				if(webSocketVersion < SPLITVERSION)
					bts = HelpUtil.readFrame(in);
				else{
					WebSocketFram ws = WebSocketFram.parseWebSocketV6Fram(in);
					bts = WebSocketFram.readWebSocketV6(in, ws);
				}
				if(bts == null || bts.length <= 0) return;
				String message = new String(bts, HelpUtil.WEB_SOCKET_CHARSET);
				log
						.debug(token + ":"
								+ new String(bts, HelpUtil.WEB_SOCKET_CHARSET));
				//发送规则
				try {
					TokenManage.getTokenManage().executCommond(this.getToken(), message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			this.logOut();
		}
	}

	public String getToken() {
		return token;
	}

	/**
	 * 握手
	 */
	private void accept() {
		log.info("Start : " + socket.getRemoteSocketAddress().toString());
		try {
			String reqestLine = new String(HelpUtil.readLine(in));
			// headers : HTTP request header!
			Map<String, String> requestHeaders = new HashMap<String, String>();
			while (true) {
				byte[] bts = HelpUtil.readLine(in);
				if (bts[0] == '\r' && bts[1] == '\n') {
					break;
				}
				String line = new String(bts);
				int mh = line.indexOf(':');
				requestHeaders.put(line.substring(0, mh), line.substring(mh + 1)
						.trim());
			}
			//HTTP request headers!
			if (log.isDebugEnabled()) {
				log.debug("-- request : headers --");
				log.debug(reqestLine);
				for (String key : requestHeaders.keySet()) {
					log.debug(key + ": " + requestHeaders.get(key));
				}
			}

			// HTTP response header!
			Map<String,String> resMap = new HashMap<String, String>();
			String responsLine = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n";
			resMap.put("Upgrade", "WebSocket");
			resMap.put("Connection", "Upgrade");
			if(requestHeaders.get("Origin") != null)
				resMap.put("Sec-WebSocket-Origin", requestHeaders.get("Origin"));
			resMap.put("Sec-WebSocket-Location", "ws://" + requestHeaders.get("Host").trim() + "/");

			String token = null;
			byte[] content = null;
			//Sec-WebSocket-Version: 6  及 以上
			String version = requestHeaders.get("Sec-WebSocket-Version");
			if(version != null){
				webSocketVersion = new Integer(version);
			}
			if(webSocketVersion >= 4){
				String code = requestHeaders.get("Sec-WebSocket-Key") + GUID;
				byte[] bts = MessageDigest.getInstance("SHA1").digest(code.getBytes(HEADER_CODE));
				code = HelpUtil.getBASE64(bts);
				resMap.put("Sec-WebSocket-Accept", code);
				//Sec-WebSocket-Protocol: chat
				resMap.put("Sec-WebSocket-Protocol", PROTOCOL);
				if(requestHeaders.get("Sec-WebSocket-Version") != null)
					resMap.put("Sec-WebSocket-Version", requestHeaders.get("Sec-WebSocket-Version"));
				if(requestHeaders.get("Sec-WebSocket-Origin") != null)
					resMap.put("Sec-WebSocket-Origin", requestHeaders.get("Sec-WebSocket-Origin"));
				token = HelpUtil.md5(code);
			}else{// 0 -- 3
				// the end 8 Byte!
				int len = 8; // in.available();
				byte[] key3 = new byte[len];
				if (in.read(key3) != len)
					throw new RuntimeException();
				log.debug(HelpUtil.formatBytes(key3));
				String key1 = requestHeaders.get("Sec-WebSocket-Key1");
				String key2 = requestHeaders.get("Sec-WebSocket-Key2");
				int k1 = HelpUtil.parseWebsokcetKey(key1);
				int k2 = HelpUtil.parseWebsokcetKey(key2);

				byte[] sixteenByte = new byte[16];
				System.arraycopy(HelpUtil.intTo4Byte(k1), 0, sixteenByte, 0, 4);
				System.arraycopy(HelpUtil.intTo4Byte(k2), 0, sixteenByte, 4, 4);
				System.arraycopy(key3, 0, sixteenByte, 8, 8);
				byte[] md5 = MessageDigest.getInstance("MD5").digest(sixteenByte);
				content = md5;
				log.debug("response content : " + HelpUtil.formatBytes(md5));
				token = HelpUtil.md5(md5);
			}
			log.info("START , token [" + token + "]");

			//return user message
			out.write(responsLine.getBytes(HEADER_CODE));
			log.debug(responsLine);
			for(String key : resMap.keySet()){
				out.write((key + ": " + resMap.get(key) + "\r\n").getBytes(HEADER_CODE));
				if(log.isDebugEnabled())
					log.debug(key + ": " + resMap.get(key));
			}
			out.write("\r\n".getBytes());

			if(content != null)
				out.write(content);

			// return user the token!
			this.sendMessage(token);//HelpUtil.writeFrame(out, token);
			out.flush();

			this.token = token;
			TokenManage.getTokenManage().putTokenThread(token, this);

		} catch (Exception e) {
			e.printStackTrace();
			if (socket != null)
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	public int getWebSocketVersion() {
		return webSocketVersion;
	}
}