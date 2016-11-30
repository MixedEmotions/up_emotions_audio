package com.opensmile.maven;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class logger {
	//private static volatile logger logger_instance;
	static PrintWriter F;
	int debug_priority = 1;
	static String filename; 
	logger() {
		try {
			F = new PrintWriter(paths.logFolder + filename + ".txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();			
		}
	}
	logger(String filename){
		try {
			logger.filename=filename;
			F = new PrintWriter(paths.logFolder + filename + ".txt");			
		} catch (FileNotFoundException e) {
			e.printStackTrace();			
		}
	}
	
	void write(int priority, String s) {
		if (priority > debug_priority)
			return;
		synchronized (logger.class) {
			if (F == null)
				try {
					F = new PrintWriter(paths.logFolder + filename + ".txt");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

			// Get the date today using Calendar object.
			Date today = Calendar.getInstance().getTime();
			// Using DateFormat format method we can create a string
			// representation of a date with the defined format.
			String reportDate = df.format(today);
			// Print what date is today!
			F.append(reportDate + " ---- " + s + "\n");
			F.flush();
		}
	}

	void close() {
		F.close();
	}

	public String file_name() {
		return paths.logFolder + filename + ".txt";
	}
}
