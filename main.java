import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.AbstractItemContainer;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.Category;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.Item;

import javax.imageio.ImageIO;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@ScriptManifest(
author = "beezdul",
name = "xS Chocolate Crusher",
version = 1.0,
description = "Grinds chocolate bars into dust. Simply stand in front of a bank with a knife/pestle in your inventory",
category = Category.MONEYMAKING)

public class main extends AbstractScript {

    Font font;
    Item knife;
    Item nextChocolate;
    
	private long timeBegan;
	private long timeRan;
	
	private int countGroundBars = 0;
	private int valueChocolateBar = 0;
	private int valueChocolateDust = 0;
	private int valueProfit = 0;
	
    private int lastSlot = -1;
    
    
	public void refreshPrices()
	{
		valueChocolateBar = PriceLookup.getAverageBuyOffer(1973);
		valueChocolateDust = PriceLookup.getAverageSellOffer(1975);
		
		valueProfit = valueChocolateDust - valueChocolateBar;
	}
	
	public void onStart()
	{
		//Winblows users
        font = new Font("Arial", Font.PLAIN, 12);
        
        if(System.getProperty("os.name").contains("nix"))
        	font = new Font("Sans", Font.PLAIN, 12);
        
		timeBegan = System.currentTimeMillis();
		
		log("~~~ Running xS Chocolate Crusher");
		log("~~~ Don't get high off your own supply!\r\n");
		
		if(getTool() != null == false)
		{
			log("~~~ Could not find a knife - exiting");
			stop();
		}
		
		log("~~~ Getting prices...");
		refreshPrices();
		log("~~~ Chocolate bar: " + valueChocolateBar);
		log("~~~ Chocolate dust: " + valueChocolateDust);
		log("~~~ Profit: " + valueProfit);
	}

	private enum State {
		GRIND, DEPOSIT, WITHDRAW, WAIT
	};

	private State getState() {
		if(getInventory().contains("Chocolate bar"))
		{
			if(getTool() != null)
			{
				return State.GRIND;
			}
			else
			{
				log("~~~ No knife! Trying to get one from the bank");
				return State.WITHDRAW;
			}
		}
		else //NO CHOCOLATE BARS
		{
			if(getInventory().contains("Chocolate dust"))
			{
				return State.DEPOSIT;
			}
			return State.WITHDRAW;
		}
		//return State.WAIT;
	}

	public void onExit() {
		log("~~~ Bye!");
	}
	
	public Item getTool()
	{
		if(getInventory().get("Knife") != null)
		{
			return getInventory().get("Knife");
		}
		else if(getInventory().get("Pestle and mortar") != null)
		{
			return getInventory().get("Pestle and mortar");
		}
		return null; //nope :/
	}
	
    @Override
    public void onPaint(Graphics2D g) {
        long profit = valueProfit * countGroundBars;
        int lineHeight = 14;
        int startHeight = 268;
        
    	timeRan = System.currentTimeMillis() - this.timeBegan;
        
        String[] paintLines = new String[] {
        		"-- xS Chocolate Crusher v1.0 --",
        		"Time Running: " + formatTime(timeRan),
        		"Chocolate Bars Ground: " + countGroundBars,
        		"Total Profit: " + profit,
        		"Hourly Profit: " + (int)((profit / (timeRan / 3600000d)) / 1000d) + "k"
        		};

        g.setFont(font);
        g.setColor(new Color(0f, 0f, 0f, 0.6f));
        g.fillRect(0, startHeight - lineHeight, 230, lineHeight * (paintLines.length + 1));
        
        int l = 1;
        for(String line : paintLines)
        {
            drawShadowString(g, line, 8, startHeight + (lineHeight * l), Color.WHITE, Color.BLACK);
            l++;
        }
        
        //Highlight items
        if(knife != null)
        {
	        g.setColor(Color.RED);
	        g.draw(getInventory().itemBounds(knife));
        }
        if(nextChocolate != null)
        {
        	g.setColor(Color.ORANGE);
        	g.draw(getInventory().itemBounds(nextChocolate));
        	
        }
        
    }
    
    public String formatTime(long millis)
    {
    	return String.format("%02d:%02d:%02d",
    			TimeUnit.MILLISECONDS.toHours(millis),
    			TimeUnit.MILLISECONDS.toMinutes(millis) -  
    			TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
    			TimeUnit.MILLISECONDS.toSeconds(millis) - 
    			TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));   
    }
    
    public void drawShadowString(Graphics2D g, String s, int x, int y, Color face, Color shadow)
    {
        g.setColor(shadow);
    	g.drawString(s, x+1,y+1);
    	g.setColor(face);
    	g.drawString(s,x,y);
    }
    
    @Override
    public void onMouse(java.awt.event.MouseEvent Event)
    {
    	
    };    
    
	@Override
	public int onLoop() {
		try
		{
			switch (getState())
			{
				case GRIND:
					knife = getTool();
					nextChocolate = getInventory().get("Chocolate bar");
					
					if(nextChocolate.getSlot() != lastSlot)
					{
						if(knife != null)
						{
							if(knife.useOn(nextChocolate))
							{
								lastSlot = nextChocolate.getSlot();
								countGroundBars++;
								sleep(Calculations.random(10,50));
								getMouse().move(getTool().getDestination());
								
								// Wait until the grinding is complete
								while(getInventory().getItemInSlot(lastSlot).getName() == "Chocolate Bar"){} 
								sleep(Calculations.random(10,200));
							}
						}
					}
					break;
				case DEPOSIT:
				    if(!getBank().isOpen()){
				    	getBank().open();
						sleep(Calculations.random(292, 1543));
				    }
				    else
				    {
				    	getBank().depositAll("Chocolate dust");
				    	//getBank().close();
				    	//We're about to withdraw so there's no need to close
				    }
					break;
				case WITHDRAW:
				    if(!getBank().isOpen()){
				    	getBank().open();
				    }
				    else
				    {
				    	if(getBank().withdrawAll("Chocolate bar") == false)
				    	{
				    		log("~~~ Looks like you're out of chocolate bars");
				    		stop();
				    	}
						sleep(Calculations.random(300, 500));
				    	getBank().close();
						sleep(Calculations.random(300, 500));
				    }
					break;
				case WAIT:
					sleep(Calculations.random(300, 500));
					break;
			}
		}catch(Exception ex)
		{
			log("~~~ Caught exception: " + ex.getMessage());
		}
		return Calculations.random(300, 750);
	}
}
