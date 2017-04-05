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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
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

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("")
public class EmoRecService extends ResourceConfig {
	logger logger_instance = new logger((new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"))
			.format(Calendar.getInstance().getTime()).replace("/", "_").replace(":", "_").replace(" ", "_"));


	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail)// ,
	{
		String uploadedFileLocation = paths.FileDirectory + fileDetail.getFileName();
		// save it
		writeToFile(uploadedInputStream, uploadedFileLocation);
		String output = "File uploaded to : " + uploadedFileLocation;
		return Response.status(200).entity(output).build();
	}

	@Path("/")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response nothing() {
		return (Response.status(200).entity(genderateHelp()).build());
	}

	boolean DOCKER = true;
	@Path("/getdims")
	@GET
	@Produces("application/json")
	public Response getdims(@QueryParam("dims") String dim, @QueryParam("url") String url,
			@DefaultValue("") @QueryParam("json") String jsonfile,
			@DefaultValue("-1,-1") @QueryParam("timing") String time,
			@DefaultValue("false") @QueryParam("mobile") String mobilemode,
			@DefaultValue("") @QueryParam("texts") String texts) throws JSONException {
	
		logger_instance.write(1, "Inside getdims");
		if (dim == null) {
			return (Response.status(200).entity(genderateHelp()).build());
		}
		dim = dim.toLowerCase();
		File file = wget(url);// wget command to retrieve the video content
		logger_instance.write(1, "URL downloaded to " + file.getAbsolutePath());
		String extension = "";
		int i = file.getName().lastIndexOf('.');
		String[] splitedTexts = null;
		if (texts.length() > 0) {
			splitedTexts = texts.split(";");
		}
		if (i >= 0) {
			extension = file.getName().substring(i + 1);
		}
		File wavFile;
		wavFile = convertToWave(file);

		logger_instance.write(1, "File converted to wav: " + wavFile.getAbsolutePath());
		String[] times = time.split(";");
		String asrname = "____________________";
		String text = "";
		boolean doingASR = false;
		boolean asr_error = false;
		String temp_text = "";
		if (times[0].toLowerCase().compareTo("asr") == 0 || times[0].toLowerCase().compareTo("asr_word") == 0) {
			if (DOCKER) {
				return Response.status(200).entity("ASR and Sentiment are not available for the Docker version")
						.build();
			}
			logger_instance.write(1, " Perform ASR");
			doingASR = true;
			try {
				double audioDuration = getAudioDuration(wavFile.getAbsolutePath());

				String tim = "";
				double asraudioleng = 120;// audioDuration;
				for (double startTime = 0; startTime < audioDuration; startTime += asraudioleng) {
					double ending = Math.min(startTime + asraudioleng, audioDuration);
					double duration = ending - startTime;
					logger_instance.write(1, " Perform ASR, time:" + startTime + " - " + ending);
					if (duration < .5)
						break;
					File cuttedaudioasr = cutAudio(wavFile, String.valueOf(startTime), String.valueOf(duration),
							100000 + startTime);
					JSONArray jasr = null;
					try {
						jasr = performASR2(cuttedaudioasr.getAbsolutePath());
					} catch (JSONException e) {

					}
					if (jasr == null) {
						temp_text += "Problem with ASR (probably license is expired);";
						tim += ((Double) startTime).toString() + "," + ((Double) ending).toString() + ";";
						continue;
					}
					if (jasr.length() == 0) {
						temp_text += "NO TEXT IS DETECTED!;";
						tim += ((Double) startTime).toString() + "," + ((Double) ending).toString() + ";";						
					} else {
						ArrayList<String> temp_texta = new ArrayList<String>();
						ArrayList<Integer> start_time = new ArrayList<Integer>();
						ArrayList<Integer> end_time = new ArrayList<Integer>();
						for (int iw = 0; iw < jasr.length(); iw++) {
							if (jasr.getJSONObject(iw).getString("word").compareTo("<s>") != 0
									&& jasr.getJSONObject(iw).getString("word").compareTo("</s>") != 0) {
								temp_texta.add(jasr.getJSONObject(iw).getString("word"));
								start_time.add(jasr.getJSONObject(iw).getInt("start"));
								end_time.add(jasr.getJSONObject(iw).getInt("end"));
							}
						}

						if (times[0].toLowerCase().compareTo("asr_word") == 0) {
							for (int iw = 0; iw < temp_texta.size(); iw++) {
								if (temp_texta.get(iw).compareTo(".") == 0)
									continue;
								double vst = start_time.get(iw).doubleValue() / 10000000 + startTime;
								double ven = end_time.get(iw).doubleValue() / 10000000 + startTime;
								tim += ((Double) vst).toString() + "," + ((Double) ven).toString() + ";";
								temp_text += temp_texta.get(iw) + ";";
							}
						} else {
							boolean start = true;
							boolean prevDot = false;
							for (int iw = 0; iw < temp_texta.size(); iw++) {
								if (temp_texta.get(iw).compareTo(".") == 0) {
									if (prevDot)
										continue;// double dots
									double v = start_time.get(iw).doubleValue() / 10000000 + startTime;
									tim += ((Double) v).toString() + ";";
																			
									temp_text += ";";
									start = true;
									prevDot = true;
									continue;
								}
								prevDot = false;
								if (start == true) {
									double v = start_time.get(iw).doubleValue() / 10000000 + startTime;
									tim += ((Double) v).toString() + ",";
									start = false;
								}
								temp_text += temp_texta.get(iw) + " ";
								if (iw == temp_texta.size() - 1 && !temp_text.endsWith(".")) {
									temp_text += ";";
									double v = end_time.get(iw).doubleValue() / 10000000 + startTime;
									tim += ((Double) v).toString() + ";";
								}

							}
						}
					}
				}
				if (tim.endsWith(";"))
					tim = tim.substring(0, tim.length() - 1);
				if (temp_text.endsWith(";"))
					temp_text = temp_text.substring(0, temp_text.length() - 1);
				splitedTexts = temp_text.split(";");
				times = tim.split(";");
			} catch (IOException e) {
				splitedTexts = "IOException during ASR".split(",");
				logger_instance.write(1, "IOException during ASR" + e.getMessage());
				asr_error = true;
				dim = dim.replace("sentiment,", "").replace(",sentiment", "").replace("sentiment", "");
				times = "-1,-1".split(";");
			} catch (InterruptedException e) {
				e.printStackTrace();
				return Response.status(200).entity("InterruptedException during ASR").build();
			}
		}
		if (mobilemode.compareTo("true") == 0) {
			times = new String[] { "-1,-1" };
			if (doingASR)
				splitedTexts = new String[] { temp_text.replace("%20", " ").replace(";", ". ") };
		}
		String finalJSONString = "[";
		String[] dims = dim.split(",");
		ArrayList<Classifier> clf = new ArrayList<Classifier>();
		logger_instance.write(1, "Loading config.json from :" + paths.getVar("REST_OPENSMILE") + "configs.json");
		JSONObject jo = loadJsonObject(paths.getVar("REST_OPENSMILE") + "configs.json");
		logger_instance.write(1, "JSON is loaded: " + jo.toString());
		JSONArray ja = jo.getJSONArray("configs");
		logger_instance.write(1, "dims:" + dims.length);
		for (int d = 0; d < dims.length; d++) {
			if ((dims[d].toLowerCase().compareTo("sentiment") == 0 || dims[d].toLowerCase().compareTo("asr") == 0)
					&& DOCKER) {
				return Response.status(200).entity("ASR and Sentiment are not available for the Docker version")
						.build();
			}

			boolean added = false;
			for (int j = 0; j < ja.length(); j++) {
				if (((JSONObject) ja.get(j)).getString("id").compareTo(dims[d].toLowerCase()) == 0) {
					Classifier c = ClassifierFactory.createClassifier((JSONObject) ja.get(j), logger_instance);
					c.configure((JSONObject) ja.get(j));
					clf.add(c);
					added = true;
					break;
				}
			}
			if (!added)
				return Response.status(200).entity(dims[d] + " is not supported!").build();/**/
		}
		logger_instance.write(1, "" + clf.size() + " classifiers are added");
		ExecutorService exec = Executors.newFixedThreadPool(times.length);
		logger_instance.write(1, "times:" + times.length);
		JSONObject finalJson = new JSONObject();

		finalJson.put("@context", "http://senpy.cluster.gsi.dit.upm.es/api/contexts/Results.jsonld");
		finalJson.put("@type", "results");

		for (i = 0; i < times.length; i++) {
			String[] timesplit = times[i].split(",");
			String duration = "-1";
			if (timesplit[1].trim().compareTo("-1") == 0)
				duration = "-1";
			else {
				Double d = Double.valueOf(timesplit[1].trim()) - Double.valueOf(timesplit[0].trim());
				duration = d.toString();
			}
			final File cuttedAudio = cutAudio(wavFile, timesplit[0], duration, i);
			final JSONArray jaresult = new JSONArray();
			final CountDownLatch latch = new CountDownLatch(clf.size());
			final String[] textttt = splitedTexts;
			final String[] jsonresultstr = new String[clf.size()];
			final boolean da = doingASR;
			final int iiii = i;
			for (int iclf = 0; iclf < clf.size(); iclf++) {
				final Classifier cl = clf.get(iclf);
				final int iiclf = iclf;
				exec.submit(new Runnable() {
					@Override
					public void run() {
						logger_instance.write(1, " run ");
						try {
							if (cl.getInType().compareTo("audio") == 0) {
								JSONObject res = cl.classify(cuttedAudio.getAbsolutePath());
								jaresult.put(res);
							} else if (cl.getInType().compareTo("text") == 0 && da == true && textttt != null) {
								JSONObject res = cl.classify(textttt[iiii].replace(" ", "_"));
								jaresult.put(res);
							}
						} catch (Exception e) {
							logger_instance.write(1, " classification exception: " + e.getMessage());
							e.printStackTrace();
						} finally {
						}
						logger_instance.write(1, " finished ");
						latch.countDown();
					}
				});
			}
			logger_instance.write(1, " latch wait ");
			try {
				latch.await();
			} catch (InterruptedException e) {
				logger_instance.write(1, " latch InterruptedException: " + e.getMessage());
				e.printStackTrace();
			}
			// create json

			JSONObject job = jaresult.getJSONObject(0);
			for (int ij = 1; ij < jaresult.length(); ij++)
				mergeJSONsFull(job, jaresult.getJSONObject(ij));

			if (splitedTexts != null && splitedTexts.length > 0 && i < splitedTexts.length) {
				job.put("Text", splitedTexts[i]);
			}
			job.put("@id", url + "#time=" + times[i]);
			finalJson.append("entries", job);

			System.out.println("" + i + "/" + times.length + " processed.");
		}

		if (doingASR) {
			JSONObject j = new JSONObject();
			j.put("ASR", "Powered by Phonexia");
			finalJson.append("analysis", j);
		}

		// deleting
		String[] ss = asrname.split("/");
		final String ffnn = ss[ss.length - 1];
		ss = file.toString().split("/");
		final String wwfn = ss[ss.length - 1];
		final File folder = new File(paths.FileDirectory);
		final File[] files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				boolean b = false;
				b = b | name.startsWith(ffnn);
				b = b | name.startsWith(wwfn);
				return b;
			}
		});
		for (final File ffile : files) {
			if (!ffile.delete()) {
				System.err.println("Can't remove " + ffile.getAbsolutePath());
			}
		}
		return Response.status(200).entity(finalJson.toString(4)).build();
	}


	@Path("/getconfigfile")
	@GET
	@Produces("application/plain")
	public Response getdims(@QueryParam("filename") String configfilename) throws JSONException {
		String resource_folder = paths.getVar("REST_OPENSMILE") + "resources/";
		File f = new File(resource_folder + configfilename);
		if (f.exists()) {
			try {
				InputStream is = new FileInputStream(resource_folder + configfilename);
				BufferedReader buf = new BufferedReader(new InputStreamReader(is));

				String line = buf.readLine();
				StringBuilder sb = new StringBuilder();

				while (line != null) {
					sb.append(line).append("\n");
					line = buf.readLine();
				}
				String fileAsString = sb.toString();
				// Read more:
				// http://javarevisited.blogspot.com/2015/09/how-to-read-file-into-string-in-java-7.html#ixzz4VNVnnCZR
				return Response.status(200).entity(fileAsString).build();/**/
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return Response.status(200).entity("file read error").build();/**/
			}
		} else
			return Response.status(202).entity("File_not_Exists").build();/**/

	}

	@Path("/getByConfig")
	@GET
	@Produces("application/json")
	public Response getByConfig(@QueryParam("url") String url, @DefaultValue("") @QueryParam("json") String jsonfile,
			@DefaultValue("") @QueryParam("conf") String conffile,
			@DefaultValue("-1,-1") @QueryParam("timing") String time,
			@DefaultValue("") @QueryParam("texts") String texts) throws JSONException {
		File file = wget(url);// wget command to retrieve the video content
		JSONObject jo = loadJsonObject(conffile);
		JSONArray ja = jo.getJSONArray("configs");
		ArrayList<Classifier> clf = new ArrayList<Classifier>();
		for (int i = 0; i < ja.length(); i++) {
			jo = ja.getJSONObject(i);
			Classifier c = ClassifierFactory.createClassifier(jo.getString("classifier"), logger_instance);
			c.configure(jo);
			clf.add(c);
		}
		String[] splitedTexts = null;
		if (texts.length() > 0) {
			splitedTexts = texts.split(";");
		}
		String extension = "";
		int i = file.getName().lastIndexOf('.');
		if (i >= 0) {
			extension = file.getName().substring(i + 1);
		}
		File wavFile;
		if (extension.toLowerCase().compareTo("wav") == 0) {
			wavFile = file;
		} else {
			wavFile = convertToWave(file);
		}
		String[] times = time.split(";");
		String finalJSONString = "";
		ExecutorService exec = Executors.newFixedThreadPool(times.length);
		for (i = 0; i < times.length; i++) {
			String[] timesplit = times[i].split(",");
			final File cuttedAudio = cutAudio(wavFile, timesplit[0], timesplit[1], i);
			final JSONArray jaresult = new JSONArray();
			final CountDownLatch latch = new CountDownLatch(clf.size());
			try {
				for (final Classifier cl : clf) {
					exec.submit(new Runnable() {
						@Override
						public void run() {
							jaresult.put(cl.classify(cuttedAudio.getAbsolutePath()));
							latch.countDown();
						}
					});
				}
				latch.await();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
			JSONObject jsonobj = write2JSON(url, i, timesplit[0], timesplit[1], jaresult);
			if (i < splitedTexts.length) {
				jsonobj.put("Text", splitedTexts[i]);
			}
			if (jsonfile.compareTo("") != 0) {// add to already existed json
				JSONObject jsonobjexist = loadJsonObject(jsonfile);
				jsonobj = mergeJSONs(jsonobjexist, jsonobj);
			}
			finalJSONString += jsonobj.toString() + "\n";
		}
		exec.shutdown();
		return Response.status(200).entity(finalJSONString).build();
	}

	@Path("/deleteFile")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response deleteFile(@QueryParam("url") String url) throws JSONException {
		String[] urls = url.split("/");
		final String output = urls[urls.length - 1];
		File direct = new File(paths.FileDirectory);
		String[] files = direct.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith(output))
					return true;
				return false;
			}
		});
		for (String s : files) {
			File f = new File(paths.FileDirectory + s);
			f.delete();
		}
		return Response.status(200).entity(files.length + " file(s) deleted!").build();
	}

	public static Object mergeJSONsFull(Object json1Obj, Object json2Obj) {
		if (json1Obj instanceof JSONObject && json2Obj instanceof JSONObject) {
			JSONObject jo1 = (JSONObject) json1Obj;
			JSONObject jo2 = (JSONObject) json2Obj;
			Set<String> entrySet2 = jo2.keySet();
			for (String key : entrySet2) {
				if (!jo1.has(key))
					jo1.put(key, jo2.get(key));
				else {
					if (jo2.get(key) instanceof JSONObject || jo2.get(key) instanceof JSONArray) {
						mergeJSONsFull(jo1.get(key), jo2.get(key));
						// jo1.put(key, jj);
					}
				}
			}
			return jo1;
		} else if (json1Obj instanceof JSONArray && json2Obj instanceof JSONArray) {
			JSONArray jo1 = (JSONArray) json1Obj;
			JSONArray jo2 = (JSONArray) json2Obj;
			for (int i2 = 0; i2 < jo2.length(); i2++){
				if(!jo2.getJSONObject(i2).has("@id")){
					jo1.put(jo2.getJSONObject(i2));
					continue;
				}
				for (int i1 = 0; i1 < jo1.length(); i1++) {
					if(!jo1.getJSONObject(i1).has("@id") || !jo2.getJSONObject(i2).has("@id")){						
						continue;
					}
					if (jo2.getJSONObject(i2).getString("@id").compareTo(jo1.getJSONObject(i1).getString("@id")) == 0) {
						mergeJSONsFull(jo1.getJSONObject(i2), jo2.getJSONObject(i1));
					}
				}				
			}

			return jo1;
		} else
			return null;
	}

	private JSONObject mergeJSONs(JSONObject jsonobjexist, JSONObject jsonobj) {
		JSONObject jc = jsonobj.getJSONArray("@context").getJSONObject(0);
		jsonobjexist.accumulate("@context", jc);
		JSONObject janalysis = jsonobj.getJSONArray("analysis").getJSONObject(0);
		JSONArray ja = jsonobjexist.getJSONArray("analysis");
		ja.put(janalysis);

		String emotionid = jsonobjexist.getJSONArray("entries").getJSONObject(0).getString("@id");
		JSONObject jemotions = jsonobj.getJSONArray("entries").getJSONObject(0).getJSONArray("emotions")
				.getJSONObject(0);
		String idnew = jemotions.getString("@id");
		String[] subid = idnew.split("#");
		jemotions.put("@id", emotionid + "#" + subid[1]);
		JSONArray jem2 = jsonobjexist.getJSONArray("entries").getJSONObject(0).getJSONArray("emotions");
		jem2.put(jemotions);
		return jsonobjexist;
	}

	private JSONArray performASR2(String wavefilename) throws IOException, InterruptedException {
		String r = String.valueOf(Math.random()).replace(".", "_");
		// Process p = Runtime.getRuntime().exec("curl -H \"x:x\" -X POST
		// --data-binary @"+wavefilename+" --user testuser:testuser
		// http://x.x.x.x:x/audiofile?path="+r+"");
		Process p = Runtime.getRuntime().exec(paths.SMILExtractDir + "asr/asr_upload.sh " + wavefilename);
		p.waitFor();
		String line = "";
		String fullLine1 = "";
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((line = in.readLine()) != null)
			fullLine1 = fullLine1 + line;

		JSONObject j = null;
		try {
			j = new JSONObject(fullLine1);
		} catch (JSONException e) {
			return null;
		}
		if (!(j.has("result") && j.getJSONObject("result").has("name")))
			return null;// "ASR unsucessfull";
		// running asr
		// p = Runtime.getRuntime().exec("curl --user testuser:testuser -H
		// \"x:x\" -X GET
		// http://x.x.x.x:x/technologies/stt?path="+r+".wav&model=ENGLISH_L&result_type=one_best");
		p = Runtime.getRuntime().exec(paths.SMILExtractDir + "asr/asr_run.sh " + wavefilename);
		p.waitFor();
		String fullLine3 = "";
		in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((line = in.readLine()) != null)
			fullLine3 = fullLine3 + line;
	
		j = new JSONObject(fullLine3);
		String id = j.getJSONObject("result").getJSONObject("info").getString("id");

		while (true) {
			String fullLine4 = "";
			Thread.sleep(2000);
			// p = Runtime.getRuntime().exec("curl --user testuser:testuser -H
			// \"x:x\" -X GET http://x.x.x.x:x/pending/"+id);
			p = Runtime.getRuntime().exec(paths.SMILExtractDir + "asr/checkpending.sh " + id);
			p.waitFor();
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = in.readLine()) != null)
				fullLine4 = fullLine4 + line;
			j = new JSONObject(fullLine4);
			if (!j.getJSONObject("result").has("info")
					|| j.getJSONObject("result").getJSONObject("info").getString("state").compareTo("finished") == 0)
				break;
		}
		// p = Runtime.getRuntime().exec("curl --user testuser:testuser -H
		// \"x:x\" -X GET http://x.x.x.x:x/done/"+id);
		// p = Runtime.getRuntime().exec("curl --user testuser:testuser -H
		// \"x:x\" -X GET
		// http://x.x.x.x:x/technologies/stt?path="+r+".wav&model=ENGLISH_L&result_type=one_best");
		p = Runtime.getRuntime().exec(paths.SMILExtractDir + "asr/asr_run.sh " + wavefilename);
		p.waitFor();
		in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String fullLine5 = "";
		while ((line = in.readLine()) != null)
			fullLine5 = fullLine5 + line;
		j = new JSONObject(fullLine5);
		JSONArray jo = j.getJSONObject("result").getJSONObject("one_best_result").getJSONArray("segmentation");
	
		return jo;

	}

	private String performASR(String wavefilename) throws IOException, InterruptedException {
		String r = String.valueOf(Math.random());
		String fname = paths.FileDirectory + "asrout" + r + ".tmp";
		Process p = Runtime.getRuntime().exec("sh " + paths.SMILExtractDir + "asr_prep.sh " + wavefilename);
		p.waitFor();

		FileReader f = new FileReader(wavefilename + ".asrupload.txt");
		BufferedReader br = new BufferedReader(f);
		String aline;
		boolean passed = false;
		while ((aline = br.readLine()) != null) {
			if (aline.contains("info format")) {
				passed = true;
				break;
			}
		}
		br.close();
		if (passed == false) {
			return "Posting file to ASR server failed";
		}

		f = new FileReader(wavefilename + ".asrloc.txt");
		String locationOnServer = "";

		br = new BufferedReader(f);
		while ((aline = br.readLine()) != null) {
			if (aline.contains("Location")) {
				locationOnServer = aline.substring(10);
				break;
			}
		}
		br.close();

		boolean asr = false;
		do {
			Thread.sleep(2000);
			p = Runtime.getRuntime().exec(
					"sh " + paths.SMILExtractDir + "asr_get_res.sh " + locationOnServer + " " + fname + ".trans.txt");
			p.waitFor();
			f = new FileReader(fname + ".trans.txt");
			br = new BufferedReader(f);
			while ((aline = br.readLine()) != null) {
				if (aline.contains("result name=\"lvcsr\"")) {
					asr = true;
					br.close();
					break;
				}
			}
		} while (asr == false);

		p = Runtime.getRuntime().exec("bash " + paths.SMILExtractDir + "asr_processor.sh " + fname + ".trans.txt "
				+ wavefilename + " " + fname + ".json");
		p.waitFor();

		return fname;

	}

	public static String readFile(String filename) throws IOException {
		FileReader f = new FileReader(filename);
		BufferedReader br = new BufferedReader(f);
		String s = "";
		String aline;
		while ((aline = br.readLine()) != null) {
			s = s + aline;
		}
		br.close();
		return s;
	}

	static public JSONObject loadJsonObject(String jsonfilename) {
		String line = "";
		String jsonTxt = "";
		BufferedReader is = null;
		try {
			is = new BufferedReader(new InputStreamReader(new FileInputStream(jsonfilename)));
			while ((line = is.readLine()) != null) {
				jsonTxt += line;
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		JSONObject jsonobjexist = new JSONObject(jsonTxt);
		return jsonobjexist;
	}

	private JSONObject write2JSON(String url, Integer index, String time, String duration, JSONArray jaresult) {
		JSONObject json = new JSONObject();
		JSONArray jarray = new JSONArray();
		JSONObject emovoc = new JSONObject();
		emovoc.put("emovoc", "http://www.gsi.dit.upm.es/ontologies/onyx/vocabularies/emotionml/ns");
		jarray.put(emovoc);
		json.put("@context", jarray);

		JSONArray janalysisarray = new JSONArray();
		JSONObject janalysis = new JSONObject();
		janalysis.put("@id", "me:SpeechAnalysis");
		janalysis.put("@type", "onyx:EmotionAnalysis");
		janalysis.put("onyx:usesEmotionModel", "emovoc:pad-dimensions");
		janalysisarray.put(janalysis);
		json.put("analysis", janalysisarray);

		JSONArray jemotionsarray = new JSONArray();
		JSONObject jemotion = new JSONObject();
		jemotion.put("@id", "Entry" + index.toString() + "#time=" + time + "," + duration);
		jemotion.put("prov:wasGeneratedBy", "me:SpeechAnalysis");
		jemotion.put("onyx:hasEmotion", jaresult);
		jemotionsarray.put(jemotion);

		JSONObject jentry = new JSONObject();
		jentry.put("emotions", jemotionsarray);

		JSONArray jentriesarray = new JSONArray();
		jentriesarray.put(jentry);

		json.put("entries", jentriesarray);
		return json;
	}

	private File cutAudio(File file, String startTime, String duration, double d) {
		File F = new File(file + "_p" + ((Double) d).toString() + ".wav");
		try {
			if (F.exists())
				F.delete();
			String durationStr = "";
			if (startTime.startsWith(".")) {
				startTime = startTime + " ";
				startTime = "0" + String.copyValueOf(startTime.toCharArray(), 0, startTime.length() - 1);
			}
			if (startTime.compareTo("-1") == 0)
				startTime = "0";
			if (duration.compareTo("-1") != 0)
				durationStr = " -t " + duration;
			if (durationStr.startsWith(".")) {
				durationStr = durationStr + " ";
				durationStr = "0" + String.copyValueOf(durationStr.toCharArray(), 0, durationStr.length() - 1);
			}
			Process p = Runtime.getRuntime()
					.exec("avconv" + " -i " + file + " -acodec copy " + " -ac 1 -ss " + startTime + // start
							durationStr + // duration
							" " + F.toString());
			p.waitFor();
			p = Runtime.getRuntime().exec("qwavheaderdump -q -F " + F.toString());// Fixing header
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return F;
	}

	// convert av to .wav
	private File convertToWave(File file) {
		File F = new File(file.toString() + ".wav");
		try {
			if (F.exists())
				F.delete();
			// if (!F.exists())

			Process p = Runtime.getRuntime()
					.exec("avconv -loglevel quiet -i " + file + " -ac 1 -vn -f wav " + F.toString());
			p.waitFor();

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return F;
	}

	// download the resource
	private File wget(String url) {
		String[] urls = url.split("/");
		String output = paths.FileDirectory + urls[urls.length - 1];
		File F = new File(output);
		try {
			if (!F.exists()) {
				Process p = Runtime.getRuntime().exec("wget -q -O " + output + " " + url);
				p.waitFor();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			F = null;
			logger_instance.write(1, "wget exception: wget -q -O " + output + " " + url + "\n" + e.getMessage());
			logger_instance.close();
		}
		return F;
	}

	private double getAudioDuration(String filename) {
		File file = new File(filename);
		AudioInputStream audioInputStream;
		float durationInSeconds = 0;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(file);
			AudioFormat format = audioInputStream.getFormat();
			long audioFileLength = file.length();
			int frameSize = format.getFrameSize();
			float frameRate = format.getFrameRate();
			durationInSeconds = (audioFileLength / (frameSize * frameRate));
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}

		return durationInSeconds;
	}

	private String genderateHelp() {
		List<String> lines;
		String st="";
		try {
			lines = Files.readAllLines(Paths.get(paths.getVar("REST_OPENSMILE") + "/general/help.txt"), Charset.defaultCharset());
			for (String line : lines) {
	            st+=line+"\n";
	        }
		} catch (IOException e) {
			st = "";
		}
		return st; 
	}

	// save uploaded file to new location
	private void writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) {

		try {
			OutputStream out = new FileOutputStream(new File(uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
