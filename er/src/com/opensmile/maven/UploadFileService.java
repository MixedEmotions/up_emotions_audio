package com.opensmile.maven;
/*package com.opensmile.first;
 
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
//import org.glassfish.jersey multipart.FormDataContentDisposition;
//import com.sun.jersey.multipart.FormDataParam;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/fileupload")
public class UploadFileService {
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {

		String uploadedFileLocation = "/media/sag/DATA/eclipse_workspace/UploadedFiles/" + fileDetail.getFileName();

		// save it
		saveToFile(uploadedInputStream, uploadedFileLocation);

		String output = "File uploaded via Jersey based RESTFul Webservice to: " + uploadedFileLocation;

		return Response.status(200).entity(output).build();
	}

	// save uploaded file to new location
	private void saveToFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {

		try {
			OutputStream out = null;
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
}*/