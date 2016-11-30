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
