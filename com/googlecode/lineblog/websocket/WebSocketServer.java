/**
 * License
 * 
 * Licensed under the GNU GPL v3
 * http://www.gnu.org/licenses/gpl.html
 * 
 */
package com.googlecode.lineblog.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * webSocket协议 http://www.whatwg.org/specs/web-socket-protocol/
 * http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-10
 * 
 * @author lichangshu E-mail:lchshu001@gmail.com
 * @version 2011-9-26 09:36:16
 */
public class WebSocketServer {

	private static Log log = LogFactory.getLog(WebSocketServer.class);
	public static final int SERVER_PORT = 8091;

	public static void main(String[] args) {
		try {
			ServerSocket ss = new ServerSocket(SERVER_PORT);
			log.info("Listen----" + SERVER_PORT + "----port!");
			new Console().start();
			while (true) {
				Socket sk = ss.accept();
				new TokenThread(sk).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class Console extends Thread {
		@Override
		public void run() {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					System.in));
			TokenManage mn = TokenManage.getTokenManage();
			try {
				while (true) {
					String line = reader.readLine().trim();
					try {
						mn.executCommond(null, line);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
