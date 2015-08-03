package com.animated.rng.whiteboard.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.animated.rng.whiteboard.util.Log.Logger;

/**
 * Creates nice log files for both the client and the server
 * 
 * @author Srinivas Kaza
 */
public class CommonLogger extends Logger {

	public static final String LOG_DIRECTORY = "assets/logs";
	public static final String LOG_PREFIX = "LOG_";
	
	public static BufferedWriter LOG;
	public static boolean isServer;
	
	@Override
	public void log(int level, String category, String message, Throwable ex) {
		
		if (LOG == null)
		{
			String prefix = LOG_PREFIX;
			if (isServer)
				prefix += "_SERVER_";
			else
				prefix += "_CLIENT_";
			String date = new SimpleDateFormat("HH:mm:ss-yyyy_MM_dd").format(Calendar.getInstance().getTime()).toString();
			File logFile = new File(LOG_DIRECTORY + '/' + prefix + date);
			
			try {
				if (!logFile.exists()) {
					logFile.createNewFile();
				}
				LOG = new BufferedWriter(new FileWriter(logFile));
			} catch (IOException e) {
				System.err.println("\nError upon creating log file\n");
				e.printStackTrace();
			}
		}
		
		String style = null;
		
		switch (level) {
		case Log.LEVEL_ERROR: style = "error"; break;
		case Log.LEVEL_WARN: style = "warning"; break;
		case Log.LEVEL_INFO: style = "info"; break;
		case Log.LEVEL_DEBUG: style = "debug"; break;
		case Log.LEVEL_TRACE: style = "trace"; break;
		}
		
		String timeStamp = new SimpleDateFormat("HH:mm:ss-yyyy/MM/dd").format(Calendar.getInstance().getTime());
		StringBuilder builder = new StringBuilder();
		builder.append(timeStamp);
		builder.append(": ");
		builder.append("-- ");
		builder.append(style.toUpperCase());
		builder.append(" -- [");
		if (category != null)
			builder.append(category);
		else
			builder.append("ZebraViews");
		builder.append("] ");
		builder.append(message);
		
		if (ex != null)
		{
			StringWriter errors = new StringWriter();
			ex.printStackTrace(new PrintWriter(errors));
			builder.append("\n" + errors.toString());
		}
		
		builder.append("\n");
		String text = builder.toString();
		
		System.out.println(text);
		
		try {
			CommonLogger.LOG.write(text);
			CommonLogger.LOG.flush();
		} catch (IOException e) {
			System.err.println("\nIO error upon printing log information\n");
			e.printStackTrace();
		}
	}

}
