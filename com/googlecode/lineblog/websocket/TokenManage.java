/**
 * License
 * 
 * Licensed under the GNU GPL v3
 * http://www.gnu.org/licenses/gpl.html
 * 
 */
package com.googlecode.lineblog.websocket;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author lichangshu E-mail:lchshu001@gmail.com
 * @version 2011-9-30 下午03:30:10
 */
public class TokenManage {

	private static Log log = LogFactory.getLog(TokenManage.class);

	private Map<String, TokenThread> tts = new Hashtable<String, TokenThread>();
	public static TokenManage tokenManage = new TokenManage();

	private TokenManage() {
	}

	public static TokenManage getTokenManage(){
		return tokenManage;
	}

	/**
	 * 如果 socket 被关闭，则返回 null
	 * @param token
	 * @return
	 */
	public TokenThread getTokenThread(String token) {
		TokenThread tt = tts.get(token);
		if(tt != null && tt.socketIsClosed()) {
			removeTokenThread(token);
			return null;
		}
		return tt;
	}

	public void broadcastMessage(String message){
		for(String key : tts.keySet()){
			TokenThread tt = this.getTokenThread(key);
			if(tt!=null)
				try {
					tt.sendMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public void putTokenThread(String token, TokenThread tokenThread) {
		tts.put(token, tokenThread);
	}

	public void removeTokenThread(String token) {
		tts.remove(token);
	}

	/**
	 *  all:you_messages
	 *  length_32_user_token:your_message
	 *  out:length_32_user_token
	 *  
	 * @param fromToken
	 * @param cmd
	 * @throws Exception
	 */
	public void executCommond(String fromToken, String cmd) throws Exception{
		if (cmd.toLowerCase().startsWith("all:")) {
			this.broadcastMessage(cmd.substring("all:".length()));
		} else if (cmd.length() > 32 && cmd.charAt(32) == ':') {
			String token = cmd.substring(0, 32);
			String message = cmd.substring(33);
			TokenThread tt = this.getTokenThread(token);
			if(tt == null) {
				if(fromToken != null){
					tt = this.getTokenThread(fromToken);
				}
				if(tt == null)
					log.error("[" + token + "] not find!");
				else
					tt.sendMessage("[" + token + "] not find!");
				return;
			}
			tt.sendMessage(message);
			log.debug("send : token [" + cmd.substring(0, 32)
					+ "] message : [" + cmd.substring(33) + "]");
		} else if (cmd.toLowerCase().startsWith("out:")) {
			String token = cmd.substring("out:".length());
			TokenThread tt = this.getTokenThread(token);
			if(tt != null)
				tt.logOut();
		} else {
			//throw new Exception("Commond Error : ["+ cmd +"]");
			TokenThread tt = this.getTokenThread(fromToken);
			tt.sendMessage("Commond Error : ["+ cmd +"]");
		}
	}
}
