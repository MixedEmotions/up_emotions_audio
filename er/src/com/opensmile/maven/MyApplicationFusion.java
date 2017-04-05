package com.opensmile.maven;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
public class MyApplicationFusion extends ResourceConfig {
	public MyApplicationFusion(){
		super(FusionService.class,MultiPartFeature.class);
	}
}
