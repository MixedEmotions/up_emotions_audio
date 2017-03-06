/***
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
***/

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
