package com.opensmile.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.catalina.ha.backend.CollectedInfo;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("")
public class FusionService extends ResourceConfig {
	logger logger_instance = new logger((new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"))
			.format(Calendar.getInstance().getTime()).replace("/", "_").replace(":", "_").replace(" ", "_"));
	
	@Path("/")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response help() {
		String st = "Run this command to fuse the results of audio and video outpus: \n wget \"localhost:8080/er/general/fuse?video=`cat json_video_plain.txt`&audio=`cat json_audio_plain.txt`\""
				+ "\n In which the files should have the following entities. \n"
				+ "Note: keep ':time=start,end' in the \"@id\" section.\n\n"
				+ "---------------------------- json_audio_plain.txt -------------------------------\n";
		JSONObject jo = EmoRecService.loadJsonObject(paths.getVar("REST_OPENSMILE") + "/fusion/json_sample_audio.txt");
		st += jo.toString(4);
		st += "\n---------------------------- json_video_plain.txt -------------------------------\n";
		jo = EmoRecService.loadJsonObject(paths.getVar("REST_OPENSMILE") + "/fusion/json_sample_video.txt");
		st += jo.toString(4);
		st += "\n---------------------------- output -------------------------------\n";
		jo = EmoRecService.loadJsonObject(paths.getVar("REST_OPENSMILE") + "/fusion/json_sample_output.txt");
		st += jo.toString(4);

		return Response.status(202).entity(st).build();/**/
	}

	@Path("/fuse")
	@GET
	@Produces("application/json")
	// @Produces(MediaType.TEXT_PLAIN)
	public Response fuse(@QueryParam("audio") String in_audio, @QueryParam("video") String in_video,
			@QueryParam("text") String in_text) throws JSONException {

		JSONObject audio = null;
		if (in_audio != null)
			audio = new JSONObject(in_audio);
		JSONObject video = null;
		if (in_video != null)
			video = new JSONObject(in_video);
		JSONArray ja_audio = audio.getJSONArray("entries");
		JSONArray ja_video = video.getJSONArray("entries");

		ArrayList<Double> cutTime = getAllTimes(ja_audio);
		cutTime.addAll(getAllTimes(ja_video));

		// removing duplicates
		Set<Double> hs = new HashSet<>();
		hs.addAll(cutTime);
		cutTime.clear();
		cutTime.addAll(hs);
		Collections.sort(cutTime);

		JSONObject finalJson = new JSONObject();
		if (video.has("analysis")) {
			JSONArray ja = video.getJSONArray("analysis");
			for (int i = 0; i < ja.length(); i++)
				finalJson.append("analysis", ja.get(i));
		}

		if (audio.has("analysis")) {
			JSONArray ja = audio.getJSONArray("analysis");
			for (int i = 0; i < ja.length(); i++)
				finalJson.append("analysis", ja.get(i));
		}
		finalJson.append("analysis", "emotionVAfusion");

		if (audio.has("@context"))
			finalJson.put("@context", audio.get("@context"));
		else if (video.has("@context"))
			finalJson.put("@context", video.get("@context"));
		if (audio.has("@type"))
			finalJson.put("@type", audio.get("@type"));
		else if (video.has("@type"))
			finalJson.put("@type", video.get("@type"));

		for (int i = 0; i < cutTime.size() - 1; i++) {
			JSONObject ent_aud = findEntry(ja_audio, cutTime.get(i), cutTime.get(i + 1));
			JSONObject ent_vid = findEntry(ja_video, cutTime.get(i), cutTime.get(i + 1));
			JSONObject ent_fus = null;
			if (ent_aud != null || ent_vid != null)
				ent_fus = fuse(ent_aud, ent_vid);
			else
				continue;
			String id = ent_fus.getString("@id").split(":time=")[0];
			ent_fus.put("@id", id + ":time=" + cutTime.get(i) + "," + cutTime.get(i + 1));
			finalJson.append("entries", ent_fus);
		}

		return Response.status(200).entity(finalJson.toString(4)).build();

	}

	JSONObject fuse(JSONObject a, JSONObject b) {
		JSONObject fused = null;
		if (a == null)
			fused = new JSONObject(b.toString());
		else if (b == null)
			fused = new JSONObject(a.toString());
		else
			fused = new JSONObject(((JSONObject) EmoRecService.mergeJSONsFull(a, b)).toString());
		JSONArray emotions = fused.getJSONArray("emotions");
		double arousal = 0;
		int iArousal = 0;
		double valence = 0;
		int iValence = 0;
		for (int i = 0; i < emotions.length(); i++) {
			if (emotions.getJSONObject(i).has("onyx:hasEmotion"))
				if (emotions.getJSONObject(i).get("onyx:hasEmotion") instanceof JSONObject) {
					JSONObject jo = emotions.getJSONObject(i).getJSONObject("onyx:hasEmotion");
					if (jo.has("pad:arousal")) {
						iArousal++;
						arousal += jo.getDouble("pad:arousal");
					}
					if (jo.has("pad:valence")) {
						iValence++;
						valence += jo.getDouble("pad:valence");
					}
					if (jo.has("pad:pleasure")) {
						iValence++;
						valence += jo.getDouble("pad:pleasure");
					}
				}
		}
		arousal /= iArousal;
		valence /= iValence;

		JSONObject f = new JSONObject();
		f.put("pad:arousal", arousal);
		f.put("pad:valence", valence);
		JSONObject finalFusion = new JSONObject();
		finalFusion.put("@type", "emotion");
		finalFusion.put("@id", "emotionVAfusion");
		finalFusion.put("onyx:hasEmotion", f);
		fused.append("emotions", finalFusion);
		return fused;

	}

	JSONObject findEntry(JSONArray ja, Double st, Double en) {
		for (int i = 0; i < ja.length(); i++) {
			double[] sten = getStartEnd(ja.getJSONObject(i));
			if (sten[0] >= st && sten[0] < en || sten[1] > st && sten[1] <= en || sten[0] <= st && sten[1] >= en)
				return ja.getJSONObject(i);
		}
		return null;
	}

	ArrayList<Double> getAllTimes(JSONArray ja) {
		ArrayList<Double> allTimes = new ArrayList<>();
		for (int i = 0; i < ja.length(); i++) {
			double[] sten = getStartEnd(ja.getJSONObject(i));
			if (sten != null) {
				allTimes.add(sten[0]);
				allTimes.add(sten[1]);
			}
		}
		return allTimes;
	}

	double[] getStartEnd(JSONObject ent) {
		double[] sten = null;
		String id = ent.getString("@id");
		String[] sp = id.split(":time=");
		if (sp.length > 1) {
			String[] times = sp[1].split(",");
			sten = new double[2];
			sten[0] = Double.valueOf(times[0]);
			sten[1] = Double.valueOf(times[1]);
		}
		return sten;
	}
	
	@Path("/emotionVAfusion")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response emotionVAfusion() {
		JSONObject jo = EmoRecService.loadJsonObject(paths.getVar("REST_OPENSMILE") + "/general/emotionVAfusion.json");		
		return Response.status(202).entity(jo.toString(4)).build();
	}
}
