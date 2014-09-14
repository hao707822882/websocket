/**
 * License
 * 
 * Licensed under the GNU GPL v3
 * http://www.gnu.org/licenses/gpl.html
 * 
 */
package com.googlecode.lineblog.websocket.v2;

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

	public static final String COMMEND_OPTION_LOGIN = "login";
	public static final String COMMEND_OPTION_LOGOUT = "logout";
	
	private static Log log = LogFactory.getLog(TokenManage.class);

	private Map<String, TokenThread> tts = new Hashtable<String, TokenThread>();
	private Hashtable<String, String> us = new Hashtable<String, String>(); //用户名和token
	public static TokenManage tokenManage = new TokenManage();

	private TokenManage() {
	}

	public static TokenManage getTokenManage(){
		return tokenManage;
	}

	public int tokenThreadCount(){
		return tts.size();
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

	/**
	 * 如果 socket 被关闭，则返回 null
	 * @param token
	 * @return
	 */
	public TokenThread getTokenUser(String name) {
		String token = us.get(name);
		if(token != null){
			return getTokenThread(token);
		}
		return null;
	}

	public String[] getTokenUsers() {
		int len = us.size();//有同步的问题
		String[] list = new String[len];
		int i=0;
		for(String s : us.keySet()){
			list[i] = s.toString();
			i++;
			if(i >= len) break;
		}
		return list;
	}

	public void broadcastMessage(MessageJSON json){
		for(String key : tts.keySet()){
			TokenThread tt = this.getTokenThread(key);
			if(tt!=null)
				try {
					tt.sendMessage(json);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public void putTokenThread(String token, TokenThread tokenThread) {
		tts.put(token, tokenThread);
	}

	public int pushMessage(String from, String to, String message) throws IOException {
		String token = us.get(to);
		if(token == null ) 
			return -1;
		TokenThread tt = tts.get(token);
		if(tt==null) 
			return -2;
		tt.sendMessage(new MessageJSON(MessageJSON.MessageType_p2p_mes, message, from));
		log.debug(from + " SAY TO : " + to + message);
		return 0;
	}

	/**
	 * 
	 * @param token
	 * @param name
	 * @return
	 * 	-2 用户名重复
	 * 	-1 未找到与token对应的线程
	 * 	0  登录成功
	 */
	public int putToken2Name(String token, String name) {
		TokenThread  tt = tts.get(token);
		String nm = us.get(name);
		if(nm != null){
			return -2;
		}
		if(tt != null){
			tt.setUser(name);
			us.put(name, token);
			return 0;
		}
		return -1;
	}

	public TokenThread removeTokenThread(String token) {
		TokenThread tt = tts.remove(token);
		String name = tt.getUser();
		if(name != null){
			us.remove(name);
		}
		return tt;
	}

	/**
	 *  默认是广播信息
	 *  
	 *  length_32_user_token:your_message
	 *  out:length_32_user_token
	 *  
	 * @param fromToken
	 * @param cmd
	 * @throws IOException 
	 */
	public void executCommond(String fromToken, String cmd, String... option) throws IOException{
		String type = "";
		if(option != null && option.length > 0){
			type = option[0];
		}
		//广播登录 
		if(COMMEND_OPTION_LOGIN.equals(type)){
			this.broadcastMessage(new MessageJSON(MessageJSON.MessageType_login, cmd));
			return;
		}else //广播退出
			if(COMMEND_OPTION_LOGOUT.equals(type)){
				this.broadcastMessage(new MessageJSON(MessageJSON.MessageType_logout, cmd));
				return;
		}
		//用户互发
		int mh = cmd.indexOf(':');
		if(mh > 0){
			String user = cmd.substring(0, mh);
			String message = cmd.substring(mh + 1);
			if(pushMessage(getTokenThread(fromToken).getUser(), user, message) == 0){
				return ;
			}
		}
		//广播
		String user = null;
		if(fromToken != null){
			TokenThread tt = getTokenThread(fromToken);
			if(tt!=null){
				user = tt.getUser();
			}
		}
		this.broadcastMessage(new MessageJSON(MessageJSON.MessageType_bc_mes, cmd, user));
	}
}
