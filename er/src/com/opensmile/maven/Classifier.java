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

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class Classifier {	
	public String arg="";
	public logger logger_instance;
	String configFile = "";
	public String entry = "";
	String intype="";
	public abstract boolean configure(JSONObject json);
	public abstract JSONObject classify(String inputString);
	public String getInType(){
		return intype;
	}
	
	JSONObject createJsonEntry(String jsonpath,Object value){		
		String[] paths = jsonpath.split("/");
		JSONObject jo = new JSONObject();
		jo.put(paths[paths.length-1], value);
		
		for(int i=paths.length-2;i>=0;i--){
			if(paths[i].endsWith("]")){
				String id = paths[i].substring(paths[i].indexOf("[")+1,paths[i].length()-1);
				JSONArray ja = new JSONArray();
				jo.put("@id", id);
				ja.put(0,jo);				
				JSONObject tempObj = new JSONObject();
				jo = tempObj.put(paths[i].substring(0, paths[i].indexOf("[")-1), ja);
			}
			else{
				JSONObject tempObj = new JSONObject();
				jo = tempObj.put(paths[i],jo);
			}
		}
		return jo;
	}
}
/* SAMPLE INPUT JSON CONF 
{
"configs":[
{
	"id":"arousal_dummy",
	"classifier":"opensmilesvm",	
	"config":"IS09_full.conf",
	"entry":"emotions[emotionVA]/onyx:hasEmotion/pad:arousal",
	"intype":"audio",
},{
	"id":"valence_dummy",
	"classifier":"opensmilesvm",	
	"config":"IS09_c_full.conf",
	"entry":"emotions[emotionVA]/onyx:hasEmotion/pad:valence",
	"intype":"audio",
}]

SAMPLE OUTPUT JSON RESULTS
{
	"emovoc:pleasure":" 1.00",
	"confidence":0.2
}
 */