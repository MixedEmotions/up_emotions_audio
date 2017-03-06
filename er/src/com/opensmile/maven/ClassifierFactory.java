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

import org.json.JSONObject;

public class ClassifierFactory {
	public ClassifierFactory() {		
		
	}	
	static public Classifier createClassifier(String name,logger logger_instance){
		if(name.compareTo("opensmilesvm")==0){
			Classifier c = new openSMILEclassifier();
			c.logger_instance = logger_instance;
		}
		return null;
	}
	public static Classifier createClassifier(JSONObject jo, logger logger_instance) {
		Classifier c;
		switch(jo.getString("classifier")){
		case "opensmilesvm":
			c = new openSMILEclassifier();
			c.logger_instance = logger_instance;
			return c;
		case "bash":
			c = new bashClassifier();
			c.logger_instance = logger_instance;
			return c;
	}
	return null;
	}
}
