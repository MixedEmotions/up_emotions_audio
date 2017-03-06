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
