package com.onlinesacrifice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

import com.onlinesacrifice.RenderMachineStatus;

public class RenderMachineClient implements ProtocolHandler 
{
	public String ip;
	public int port;
	public String SchedulerIp = "unknown";
	
	public long lastCheckTime = 0;
	public long lastCheckStatusTime = 0;
	public boolean isAlive = false;
	public boolean isConnected = false;
	
	public RenderMachineStatus rmStatus = RenderMachineStatus.DISCONNECTED;
	
	public Socket socket = null;
	DataOutputStream dos = null;
	DataInputStream dis = null;
	
	boolean running = false;
	
	public long timeout = 10000; // 10s
	
	public Hashtable<String,String> messageList = new Hashtable<String, String>();
	
	public RenderMachineClient(String ip, int port)
	{

		this.ip = ip;
		this.port = port;
		this.connect();
	}
	
	
	@Override
	public int renderRoom(int roomId) 
	{
		if (this.rmStatus != RenderMachineStatus.OK)
		{
			return -1;
		}
		String messageId = "rm" + System.currentTimeMillis();
		String strRoomId = "" + roomId;
		String messageContent = this.sendMesage(messageId, strRoomId);
		//TODO handle the messagecontent
		if (messageContent == "ok")
		{
			return 1;
		}
		else
		{
			return 0;
		}

	}

	/**
	 * returns the render machine reply message content
	 * it is a sync method which means the thread shall wait the socket to return messages
	 * @param messageId the messageID String of sth+timestamp
	 * @param messageContent
	 * @return
	 */
	public String sendMesage(String messageId, String messageContent)
	{
		String message = messageId + "|" + messageContent;
		try 
		{
			this.lastCheckTime = System.currentTimeMillis();
			dos.writeUTF(message);
		} 
		catch (IOException e) 
		{
	        System.out.println("error send message" + message + "to" + this.ip + ":" + this.port);
	        return "failed";
		}
		
		long sendTime = System.currentTimeMillis();
		
		while(!messageList.containsKey(messageId))
		{
			long now = System.currentTimeMillis();
			if(now - sendTime > timeout)
			{
				System.out.println("request timeout");
				return "request timeout";
			}
			try 
			{
				Thread.sleep(15);
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}

		String _messageContent = messageList.get(messageId);
		messageList.remove(messageId);
		return _messageContent;
		
		
	}
	
	@Override
	public RenderMachineStatus checkStatus() 
	{
		String messageId = "cs" + System.currentTimeMillis();
		String checkStatus = "check-status";
		String ret = this.sendMesage(messageId, checkStatus);
		this.rmStatus = RenderMachineStatus.values()[Integer.parseInt(ret)];
		return RenderMachineStatus.values()[Integer.parseInt(ret)];

	}

	@Override
	public int disconnect() 
	{
		try
		{
			this.dis.close();
			this.dos.close();
			this.socket.close();
			this.running = false;
			this.isAlive = false;
			this.isConnected = false;
			
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}	
		return 0;
	}
	
	public void reconnect()
	{
		this.disconnect();
		this.reconnect();
	}

	@Override
	public int connect() 
	{
		try 
		{
			socket = new Socket(this.ip, this.port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			System.out.println("connect " + ip + ':' + port + " successful");
			
			this.isConnected = true;
			this.lastCheckTime = System.currentTimeMillis();
			this.running = true;
			
			this.checkStatus();
			
			new Thread(new KeepAliveWatchDog()).start();  
	        new Thread(new ReceiveWatchDog()).start();  
		} 
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int keepAlive() 
	{
		String messageId = "ka" + System.currentTimeMillis();
		String keepAlive = "KeepAlive";
		String messageContent = this.sendMesage(messageId, keepAlive);
		// doesn't matter if never write the code
		// the keep alive message is hardly use
		
		return 0;
	}
	
	/**
	 * the class keepAliveWatchDog mainly do two things
	 * 1. send keep alive pack
	 * 2. send check status pack and update status of the render machine
	 * the keep alive pack interval is 3s
	 * the check status pack interval is 10s
	 * 
	 * running must be true
	 * 
	 * @author fupeng
	 *
	 */
	class KeepAliveWatchDog implements Runnable
	{  
	        long checkDelay = 20;  
	        long keepAliveDelay = 3000;
	        long checkStatusDelay = 10000;
	        public void run() 
	        {  
	            while(running)
	            {  
	            	if (System.currentTimeMillis()- lastCheckStatusTime > checkStatusDelay)
	            	{
	            		RenderMachineClient.this.checkStatus();
	            		lastCheckStatusTime = System.currentTimeMillis();
	            	}
	            	else if(System.currentTimeMillis()- lastCheckTime > keepAliveDelay)
	                {  
	                    RenderMachineClient.this.keepAlive();  
	                    lastCheckTime = System.currentTimeMillis();
	                }
	                else
	                {  
	                    try 
	                    {  
	                        Thread.sleep(checkDelay);  
	                    } catch (InterruptedException e) 
	                    {  
	                        e.printStackTrace();  
	                    }  
	                }  
	            }  
	        }  
	    }  
	
	

	@Override
	public void handleMessage(String message) {
		// currently do nothing
		
	}
	
	/**
	 * a receiver that is used to watch the incoming message
	 * while new message in,
	 * the receiver shall put the message into the hashtable messageList
	 * the message is a <messageId, messageContent> tuple
	 * the messageId differs
	 * @author fupeng
	 *
	 */
	class ReceiveWatchDog implements Runnable
	{  
        public void run() 
        {  
            while(running)
            {  
                try 
                {  
                    if(dis.available()>0)
                    {  
                        String _message = dis.readUTF();
                        String messageId = _message.split("|")[0];
                        String messageContent = _message.split("|")[1];
                        messageList.put(messageId, messageContent);
                    }
                    else
                    {  
                        Thread.sleep(10);  
                    }  
                } 
                catch (Exception e) 
                {  
                    e.printStackTrace();  
                }   
            }  
        }  
    }  
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getSchedulerIp() {
		return SchedulerIp;
	}

	public void setSchedulerIp(String schedulerIp) {
		SchedulerIp = schedulerIp;
	}

	public long getLastCheckTime() {
		return lastCheckTime;
	}

	public void setLastCheckTime(int lastCheckTime) {
		this.lastCheckTime = lastCheckTime;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}


	

}
