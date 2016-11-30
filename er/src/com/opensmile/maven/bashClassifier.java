package com.opensmile.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

public class bashClassifier extends Classifier {
	String run_command = "";
	@Override
	public boolean configure(JSONObject json) {
		run_command = json.getString("run_command");
		onyxentity = json.getString("onyxentity");
		intype = json.getString("intype");
		return true;
	}

	@Override
	public JSONObject classify(String audioFile) {
		String line="";
		String fullLine="";
		int idx = run_command.lastIndexOf("/");
		String full_path = paths.SMILExtractDir+run_command.substring(0,idx);
		String str = "bash "+paths.SMILExtractDir+run_command+" "+audioFile;
		try {
			logger_instance.write(1,"full_path="+full_path);
			logger_instance.write(1,"bash command: " + str);
			for(int trial=0;trial<3;trial++){
				fullLine="";
				Process p = Runtime.getRuntime().exec(str,null,new File(full_path));			
				p.waitFor();
				logger_instance.write(1,"bash command performed");
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));			
				while((line=in.readLine())!=null)
					fullLine=fullLine+line;
				in = new BufferedReader(new InputStreamReader(p.getErrorStream()));			
				while((line=in.readLine())!=null)
					fullLine=fullLine+line;
				if(fullLine.length()>0)
					break;
			}
			logger_instance.write(1,"bash output:"+ fullLine);
		} catch (IOException | InterruptedException e) {			
			e.printStackTrace();
		}
		JSONObject jo = null;
		try{
			jo = new JSONObject(fullLine.substring(fullLine.indexOf("{")));
		}catch (JSONException e){
			System.out.println(fullLine);
			e.printStackTrace();
		}
		catch(StringIndexOutOfBoundsException e){
			System.out.println(fullLine);
			e.printStackTrace();
		}

		JSONObject jop = new JSONObject();
		switch((String)jo.get("TYPE")){
		case "regression":
			double out = jo.getDouble("VALUE");
			double conf = (double)(((JSONObject)jo.getJSONArray("PROB").get(0)).getDouble("CONFIDENCE"));
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
