package com.onlinesacrifice;

import com.onlinesacrifice.*;

public interface ProtocolHandler {
	
	/**
	 * render a room
	 * @param roomId
	 * @return 1 for success; -1 for failed
	 */
	public int renderRoom(int roomId);
	
	
	/**
	 * check whether the render machine is available or full;
	 * @return none
	 */
	public RenderMachineStatus checkStatus();
	
	public int disconnect();
	
	public int connect();
	
	/**
	 * send a specific string to keep alive the socket
	 * @return 1 for keep alive; -1 for keep alive failed
	 */
	public int keepAlive();
	
	/**
	 * handle the String format message and decide which action to take,
	 * actions are the functions above including 
	 * connect
	 * disconnect
	 * render room request
	 * check status
	 * @param message
	 */
	public void handleMessage(String message);
	
	
}
