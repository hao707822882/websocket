package com.googlecode.lineblog.websocket.v2;

public class MessageJSON {

	public static final int MessageType_error = -1;
	public static final int MessageType_bc_mes = 0;
	public static final int MessageType_p2p_mes = 1;
	public static final int MessageType_user = 2;
	public static final int MessageType_login = 3;
	public static final int MessageType_logout = 4;
	public static final int MessageType_self = 10;
	public static final int MessageType_system = 99;

	private int type;
	private String message;
	private long time;
	private String user;

	public String getMessage() {
		return message;
	}
	
	public MessageJSON(int type, String message, long time, String user) {
		this.type = type;
		this.setMessage(message);
		this.time = time;
		this.user = user;
	}
	
	public MessageJSON(int type, String[] message, long time, String user) {
		this.type = type;
		this.setMessage(message);
		this.time = time;
		this.user = user;
	}
	
	public MessageJSON(int type, String message, String user) {
		this(type,message,System.currentTimeMillis(),user);
	}
	
	public MessageJSON(int type, String message) {
		this(type,message,System.currentTimeMillis(),null);
	}

	public MessageJSON(String message){
		this(MessageType_bc_mes, message);
	}

	public void setMessage(String... message) {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for(String m:message){
			buf.append('"');
			buf.append(m.replaceAll("\"", "\\\\\""));
			buf.append('"');
			buf.append(",");
		}
		if(message.length > 0) buf.setLength(buf.length()-1);
		buf.append("]");
		this.message = buf.toString();
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String toJSON(){
		return "{\"type\":" + type + ", \"time\":" + time + ", \"message\":" + message 
				+ (user==null?"" : ", \"user\": \"" + user + "\"") + "}";
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
