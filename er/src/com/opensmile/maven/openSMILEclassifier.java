package com.opensmile.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;

public class openSMILEclassifier extends Classifier {

	@Override
	public boolean configure(JSONObject jo) {
		configFile = jo.getString("config");
		onyxentity = jo.getString("onyxentity");
		intype = jo.getString("intype");
		return true;
	}

	@Override
	public JSONObject classify(String audioFile) {
		String line="";
		String fullLine="";
		try {
			String str = paths.SMILExtractDir+"SMILExtract "+		
					" -C "+paths.SMILExtractDir+configFile+
					" -I "+audioFile+ " -l 1";
			Process p = Runtime.getRuntime().exec(str,null,new File(paths.SMILExtractDir));
			logger_instance.write(1,"opensmile command: " + str);
			p.waitFor();
			logger_instance.write(1,"opensmile command performed");
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));			
			while((line=in.readLine())!=null)
				fullLine=fullLine+line;
			in = new BufferedReader(new InputStreamReader(p.getErrorStream()));			
			while((line=in.readLine())!=null)
				fullLine=fullLine+line;
			logger_instance.write(1,"opensmile output:"+ fullLine);
		} catch (IOException | InterruptedException e) {			
			e.printStackTrace();
		}
		JSONObject jo = new JSONObject(fullLine);
		
		JSONObject jop = new JSONObject();
		switch((String)jo.get("TYPE")){
		case "regression":
			double out = jo.getDouble("VALUE");
			double conf = (double)((JSONObject)jo.getJSONArray("PROB").get(0)).get("CONFIDENCE");
			jop.put(onyxentity, out);
			jop.put("confidence", conf);
			break;
		case "classification":
			jop.put(onyxentity, jo.getJSONArray("PROB"));
			break;			
		}
		return jop;
	}
}
