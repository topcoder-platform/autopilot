package com.topcoder.kafka.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
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
	private PropertyResourceBundle prb = null;

	public String targetURL = "";
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
			clientId = prb.getString("kafka.security.clientId");
			clientSecret = prb.getString("kafka.security.clientsecret");
			authDomain = prb.getString("kafka.security.authdomain");
			authAudience = prb.getString("kafka.security.authAudience");
			topicName = prb.getString("kafka.topic");
			tokenExpirationTime = Integer.parseInt(prb.getString("kafka.security.tokenExpirationtime"));
		} catch(NumberFormatException e) {
			tokenExpirationTime = 60 * 24;
		} catch (Exception e) {
			setLocalKafkaVariables();
			getLog().log(Level.ERROR, "Exception in loading Kafka Properties : " + e.getMessage());
			// e.printStackTrace();
		}
	}

	private void setLocalKafkaVariables() {
		targetURL = "https://api.topcoder-dev.com/v5/bus/events";
		topicName = "notifications.kafka.queue.java.test";
		clientId= "5fctfjaLJHdvM04kSrCcC8yn0I4t1JTd";
		clientSecret = "GhvDENIrYXo-d8xQ10fxm9k7XSVg491vlpvolXyWNBmeBdhsA5BAq2mH4cAAYS0x";
		authDomain = "topcoder-newauth.auth0.com";
		authAudience = "https://www.topcoder.com";
	}

	public KafkaMessageProducer() {
		this.log = LogManager.getLog(DEFAULT_LOG_NAME);
		loadProperties();
	}

	public void postRequestUsingGson(Object payload) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Central location to set topicname
		MessageTemplate messageTemplate = new MessageTemplate();
		messageTemplate.setTopic(topicName);
		messageTemplate.setOriginator("AUTO_PILOT");
		messageTemplate.setTimestamp(dateFormat.format(new Date()));
		messageTemplate.setMime_type("application/json");
		messageTemplate.setPayload(payload);

		Gson gson = new Gson();
		String strMessage = gson.toJson(messageTemplate);

		getLog().log(Level.DEBUG, "KAFKA_MESSAGE :::" + strMessage);
		try {
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(targetURL);
			Entity entity = Entity.entity(strMessage, MediaType.APPLICATION_JSON_TYPE);
			Response response = target.request(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + getM2MToken())
			  .post(entity, Response.class);

			getLog().log(Level.INFO, response);
		} catch(Exception e) {
			getLog().log(Level.ERROR, "Exception in When sending message to Kakfa Bus : " + e.getMessage());
		}
	}

	protected Log getLog() {
		return log;
	}

	public String getM2MToken() throws Exception {
		JWTTokenGenerator generator = JWTTokenGenerator.getInstance(this.clientId,
			this.clientSecret, this.authAudience,this.authDomain,this.tokenExpirationTime);
		return generator.getMachineToken();
	}

}
