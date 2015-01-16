/*
 * Author: 598Johnn897
 * 
 * Date: Jan 8, 2015
 * Package: org.teammoose
 */
package org.teammoose;

import org.apache.commons.lang.Validate;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 
 * @author 598Johnn897
 * @since
 */
public class TeamMooseMain extends JavaPlugin
{
	public static TeamMooseMain instance;
	public static TeamMooseMain get()
	{
		Validate.notNull(instance);
		return instance;
	}
	
	@Override public void onLoad()
	{

	}
	
	@Override public void onEnable()
	{		
		instance = this;
		try
		{

		} catch(Exception e)
		{
			e.printStackTrace();
		} finally {
			
		}
	}
	
	@Override public void onDisable()
	{
		
	}
}
