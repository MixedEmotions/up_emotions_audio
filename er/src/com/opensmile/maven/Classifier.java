package com.opensmile.maven;

import java.io.File;

import org.json.JSONObject;

public abstract class Classifier {	
	public String arg="";
	public logger logger_instance;
	String configFile = "";
	String onyxentity = "";
	String intype="";
	public abstract boolean configure(JSONObject json);
	public abstract JSONObject classify(String inputString);
	public String getInType(){
		return intype;
	}
}
