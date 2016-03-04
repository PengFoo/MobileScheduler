package com.onlinesacrifice;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onlinesacrifice.RenderMachineClient;

public class Scheduler {
	List<RenderMachineClient> renderMachineList = new ArrayList<RenderMachineClient>();

    
    public int emptyRenderMachineList()
    {	
    	this.renderMachineList = new ArrayList<RenderMachineClient>();
    	return 1;
    }
    
    public int addRenderMachine(String ip, int port)
    {
    	RenderMachineClient rm = new RenderMachineClient(ip, port);
    	return this.addRenderMachine(rm);
    }
    
    public int addRenderMachine(RenderMachineClient rm)
    {
    	this.renderMachineList.add(rm);
    	return 1;
    }
    
    public int removeRenderMachine(String ip, int port)
    {
    	RenderMachineClient rm = new RenderMachineClient(ip, port);
    	return this.removeRenderMachine(rm);
    }
    
    public int removeRenderMachine(RenderMachineClient rm)
    {
    	if (this.renderMachineList.contains(rm))
    	{
    		rm.disconnect();
    		this.renderMachineList.remove(rm);
    		return 1;
    	}
    	else
    	{
    		return -1;
    	}
    }
    
	public Scheduler()
	{
		//TODO
	}

	
	public int renderRomm(int roomId)
	{
		//TODO
		RenderMachineClient rm = findRenderMachine();
		if(rm != null)
		{
			rm.renderRoom(roomId);
		}
		
		return 1;
	}
	
	/**
	 * find one render machine which is available
	 * @return RenderMachineClient
	 */
	public RenderMachineClient findRenderMachine()
	{
		//TODO
		// magic code , help balance the machine load
		// shuffle the machine order so that the iterator may visit different 
		// machine every time
		Collections.shuffle(renderMachineList); 
		
		for(RenderMachineClient rmc : renderMachineList)
		{
			//long now = System.currentTimeMillis();
			if (rmc.rmStatus == RenderMachineStatus.OK)
			{
				RenderMachineStatus rms = rmc.checkStatus();
				if (rms == RenderMachineStatus.OK)
				{
					return rmc;
				}
			}
			
		}
		return null;
	}
	
}
