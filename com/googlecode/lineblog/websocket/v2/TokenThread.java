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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lineblog.websocket.v2.HelpUtil;
import com.googlecode.lineblog.websocket.v2.TokenManage;
import com.googlecode.lineblog.websocket.v2.TokenThread;
import com.googlecode.lineblog.websocket.v2.WebSocketServer;

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
	private String user = null;//用户名
	private OutputStream out;
	private InputStream in;
	private Socket socket;
	private int status = 0; //握手状态
	private static Log log = LogFactory.getLog(TokenThread.class);

	public TokenThread(Socket socket) throws IOException {
		if (socket == null)
			throw new RuntimeException("socket is not null!");
		this.socket = socket;
		this.in = new BufferedInputStream(this.socket.getInputStream());
		this.out = new BufferedOutputStream(this.socket.getOutputStream());
		this.status = this.accept();//握手
	}

	public synchronized void sendMessage(MessageJSON json) throws IOException {
		String message = json.toJSON();
		if(webSocketVersion < 6)
			HelpUtil.writeFrame(out, message);
		else
			WebSocketV6Fram.writeWebSocketV6Fram(out, message);
		out.flush();
	}

	public boolean socketIsClosed(){
		return this.socket.isClosed();
	}

	public void logOut(){
		try {
			if(webSocketVersion < 6)
				out.write(WebSocketV6Fram.getCloseV5Fram());
			else
				out.write(WebSocketV6Fram.getCloseV6Fram());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			TokenManage.getTokenManage().removeTokenThread(this.token);
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if(this.user != null)//广播退出
					TokenManage.getTokenManage().executCommond(this.token, this.user,
							TokenManage.COMMEND_OPTION_LOGOUT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			log.info("[" + this.token + "] has logout!");
		}
	}

	public int login(String name){
		return TokenManage.getTokenManage().putToken2Name(this.token, name);
	}

	@Override
	public void run() {
		if(status == 0) return;
		try {
			while(true){
				byte[] bts;
				if(webSocketVersion < SPLITVERSION)
					bts = HelpUtil.readFrame(in);
				else{
					WebSocketV6Fram ws = WebSocketV6Fram.parseWebSocketV6Fram(in);
					bts = WebSocketV6Fram.readWebSocketV6(in, ws);
				}
				if(bts == null) return;
				String message = new String(bts, HelpUtil.WEB_SOCKET_CHARSET);
				log
						.debug(token + ":"
								+ new String(bts, HelpUtil.WEB_SOCKET_CHARSET));
				//发送规则
				try {
					if(this.user == null){
						if(message.matches("^[0-9A-Za-z\\u00FF-\\u9FA5]*$")){
							int st = login(message);
							if(st == 0 ){
								this.sendMessage(new MessageJSON(MessageJSON.MessageType_self, message));
								TokenManage.getTokenManage().executCommond(this.getToken(),
										message, TokenManage.COMMEND_OPTION_LOGIN);
								continue;
							}else if(st == -2){
								message = "用户名重复重新输入！";
							}
						}else{
							message = "只能为英文字符或汉字，不能为标点";
						}
						sendMessage(new MessageJSON(MessageJSON.MessageType_error,message,
								System.currentTimeMillis(),null));
						continue;
					}else{
						TokenManage.getTokenManage().executCommond(this.getToken(), message);
					}
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
	private int accept() {
		log.info("Start : " + socket.getRemoteSocketAddress().toString() + "[" + TokenManage.getTokenManage().tokenThreadCount() + "]");
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

			//Upgrade:WebSocket is null , so it's a general http request!
			if(requestHeaders.get("Upgrade") == null){
				String url = "http://127.0.0.1" + reqestLine.split(" ")[1];
				//socket.setSoTimeout(5000);
				url = new URL(url).getPath();
				HelpUtil.writeFile(WebSocketServer.ROOT + url, out);
				socket.close();
				return 0;
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
			if(webSocketVersion >= 6){
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
			}else{
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
			// HelpUtil.writeFrame(out, token);
			//getTokenUsers
			this.sendMessage(new MessageJSON(MessageJSON.MessageType_user, 
					TokenManage.getTokenManage().getTokenUsers(),System.currentTimeMillis(),null));
			this.sendMessage(new MessageJSON(MessageJSON.MessageType_system, "请输入用户名！"));
			out.flush();

			this.token = token;
			TokenManage.getTokenManage().putTokenThread(token, this);
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			if (socket != null)
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			return 0;
		}
	}

	public int getWebSocketVersion() {
		return webSocketVersion;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}
}