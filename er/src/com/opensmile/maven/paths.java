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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class paths {
	static String FileDirectory = getVar("REST_FILES");
	static String SMILExtractDir = getVar("REST_OPENSMILE");
	static String logFile = getVar("REST_OPENSMILE") + "/log.txt";
	static String logFolder = getVar("REST_OPENSMILE") + "/logs/";
	static String SMILEconfig = "IS09_emotion_Mine_AR_res_4corpus_0_FE.conf";
	static String SMILEconfigAugmented = "IS09_emotion_Mine_AR_res_4corpus_0_FE_Augmented.conf";

	static String getVar(String varname) {
		BufferedReader bufferedReader = null;
		File f = null;
		var_file = "/path/to/rest_vars";
		try {
			f = new File(var_file);
			System.out.println(f.getAbsolutePath());
			if (!f.exists()) {
				System.out.println(f.getAbsolutePath() + " does not exist!");
				FileNotFoundException e = new FileNotFoundException();
				throw e;
			}
			bufferedReader = new BufferedReader(new FileReader(var_file));
			String line;
			String[] ls;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.length() < 2) {
					bufferedReader.close();
					return "";
				}
				ls = line.split("=");
				if (ls[0].compareTo(varname) == 0) {
					bufferedReader.close();
					return ls[1];
				}
			}
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return f.getAbsolutePath() + " " + e.getMessage();
		}
		return "";
	}
}
