package com.topcoder.kafka.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.PropertyResourceBundle;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.topcoder.management.phase.autopilot.impl.MessageFormat;
import com.topcoder.util.log.Level;
import com.topcoder.util.log.Log;
import com.topcoder.util.log.LogManager;
import com.appirio.tech.core.api.v3.util.jwt.JWTTokenGenerator;

public class KafkaMessageProducer {

	// public static final String baseUri =
	// "http://tc-email-service-elb-508840840.us-east-1.elb.amazonaws.com/eventbus";
	// public static final String authToken =
	// "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoibWVzc2FnZS1zZXJ2aWNlIiwiaWF0IjoxNTE3MzA4NjAzLCJleHAiOjE1MjU5NDg2MDN9.SURZlJkhPcwI7cm2SUqBf2Uv7MkaAULuNWHMKTrtykY";
	private Client client = null;
	private WebTarget target = null;
	private PropertyResourceBundle prb = null;

	public String targetURL = "";
	public String endPoint = "";
	public String topicName = "";
	public String clientId = "";
	public String clientSecret = "";
	public String authAudience = "";
	public String authDomain = "";
	public int tokenExpirationTime = 60 * 24;

	/**
	 * <p>
	 * Represents the default log name to be used for auditing. Referenced in
	 * ctor().
	 * </p>
	 */
	public static final String DEFAULT_LOG_NAME = "AutoPilot";
	
	/**
	 * <p>
	 * Represents the kafka config properties file location
	 * ctor().
	 * </p>
	 */
	public static final String fileLocation  = "/config/kafka-config.properties";
	/**
	 * <p>
	 * Represents the log used to do auditing whenever a phase is started/ended.
	 * The audit log should include timestamp, project, phase, operation, and
	 * the operator This variable is initially null, initialized in constructor
	 * and immutable afterwards. It can be retrieved with the getter.
	 * </p>
	 */
	private final Log log;

	/**
	 * This method will read property file and initialize required variables for
	 * Kafka messaging communication
	 */
	private void loadProperties() {
		try {
			prb = new PropertyResourceBundle(this.getClass().getResourceAsStream(fileLocation));
			targetURL = prb.getString("kafka.target.url");
			endPoint = prb.getString("kafka.target.endpoint");
			clientId = prb.getString("kafka.security.clientId");
			clientSecret = prb.getString("kafka.security.clientsecret");
			authDomain = prb.getString("kafka.security.authdomain");
			authAudience = prb.getString("kafka.security.authAudience");
			tokenExpirationTime = Integer.parseInt(prb.getString("kafka.security.tokenExpirationtime"));
			topicName = prb.getString("kafka.topic");
		}catch(NumberFormatException e) {
			tokenExpirationTime = 60 * 24;
		}catch (Exception e) {
			setLocalKafkaVariables();
			getLog().log(Level.ERROR, "Exception in loading Kafka Properties : " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void setLocalKafkaVariables() {
		targetURL = "https://api.topcoder-dev.com/eventbus/events";
//		endPoint = "/events";
		topicName = "notifications.kafka.queue.java.test";
		clientId= "5fctfjaLJHdvM04kSrCcC8yn0I4t1JTd";
		clientSecret = "GhvDENIrYXo-d8xQ10fxm9k7XSVg491vlpvolXyWNBmeBdhsA5BAq2mH4cAAYS0x";
		authDomain = "topcoder-newauth.auth0.com";
		authAudience = "https://www.topcoder.com";
		
	}

	public KafkaMessageProducer() {
		this.log = LogManager.getLog(DEFAULT_LOG_NAME);
		loadProperties();
		client = ClientBuilder.newClient();
		target = client.target(targetURL);
	}

	public void postRequestUsingGson(Object payload) {
		//target = target.path(endPoint);
		Gson gson = new Gson();
		// MessageTemplate msg = new MessageTemplate();
		// msg.setType("notifications.kafka.queue.java.test");
		// msg.setMessage("{ \"data\": \"Test auth code 12151..\" }");

		// Central location to set topicname
		MessageTemplate messageTemplate = new MessageTemplate();
		messageTemplate.setTopic(topicName);
		messageTemplate.setOriginator("AUTO_PILOT");
		messageTemplate.setTimestamp(new Date().toString());
		messageTemplate.setMime_type("application/json");
		messageTemplate.setPayload(payload);

		// TODO: this is temporary msg which will be fixed in later version from
		// bus API
		// Removed below as part of new json structure
		// String finalMsg = "{ \"data\": "+ payload +" }";
		// messageTemplate.setMessage(strMessage);
		// messageTemplate.setMessage(finalMsg);

		String strMessage = gson.toJson(messageTemplate);

		getLog().log(Level.INFO, "KAFKA_MESSAGE :::" + strMessage);

		try{
		Response response = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + getM2MToken())
				.post(Entity.entity(strMessage, MediaType.APPLICATION_JSON), Response.class);

		getLog().log(Level.INFO, response);
		}catch(Exception e){
			getLog().log(Level.ERROR, "Exception in When sending message to Kakfa Bus : " + e.getMessage());
			e.printStackTrace();
		}
		//getLog().log(Level.INFO, "KAFKA MESSAGE RES_CODE :::" + response.getStatus());

	}

	protected Log getLog() {
		return log;
	}
	
	public String getM2MToken() throws Exception {
		 JWTTokenGenerator generator = JWTTokenGenerator.getInstance(this.clientId,
	                this.clientSecret, this.authAudience,this.authDomain,this.tokenExpirationTime);
	     return generator.getMachineToken();
	}

	/*public static void main(String[] args) {
		KafkaMessageProducer test = new KafkaMessageProducer();

		MessageFormat message = new MessageFormat("Thu Feb 22 16:14:22 EST 2018", 30050490, 741615, "Registration",
				"END", "22841596");
		Gson gson = new GsonBuilder().create();
		System.out.println(gson.toJson(message));

		test.postRequestUsingGson(gson.toJson(message));
	}*/

}