/*
 * Author: 598Johnn897
 * 
 * Date: Jan 15, 2015
 * Package: org.teammoose
 */
package org.teammoose;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * 
 * @author 598Johnn897
 * @since
 */
public class TMJavaPlugin extends JavaPlugin
{
	public TMJavaPlugin()
	{
		TeamMooseFramework.registerTMPlugin(this);
	}
}
