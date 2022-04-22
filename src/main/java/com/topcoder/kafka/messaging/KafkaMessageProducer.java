package com.topcoder.kafka.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

	/**
	 * <p>
	 * Represents the event bus api target url. Referenced in ctor().
	 * </p>
	 */
	public String targetURL = "";

	/**
	 * <p>
	 * Represents the Kafka topic name. Referenced in ctor().
	 * </p>
	 */
	public String topicName = "";

	/**
	 * <p>
	 * Represents the Kafka originator name. Referenced in ctor().
	 * </p>
	 */
	public String originator = "";

	/**
	 * <p>
	 * Represents the M2M auth0 client id. Referenced in ctor().
	 * </p>
	 */
	public String clientId = "";

	/**
	 * <p>
	 * Represents the M2M auth0 client secret. Referenced in ctor().
	 * </p>
	 */
	public String clientSecret = "";

	/**
	 * <p>
	 * Represents the M2M auth0 audience. Referenced in ctor().
	 * </p>
	 */
	public String authAudience = "";

	/**
	 * <p>
	 * Represents the M2M auth0 domain. Referenced in ctor().
	 * </p>
	 */
	public String authDomain = "";

	/**
	 * <p>
	 * Represents the M2M token expiration time. Referenced in ctor().
	 * </p>
	 */
	public int tokenExpirationTime;

	/**
	 * <p>
	 * Represents the M2M token expiration time. Referenced in ctor().
	 * </p>
	 */
	public String authProxyServerUrl = "";

	/**
	 * <p>
	 * Represents the default log name to be used for auditing. Referenced in
	 * ctor().
	 * </p>
	 */
	public static final String DEFAULT_LOG_NAME = "AutoPilot";

	/**
	 * <p>
	 * Represents the kafka message mime type.
	 * </p>
	 */
	private static final String MIME_TYPE = "application/json";

	/**
	 * <p>
	 * Represents the kafka config properties file location ctor().
	 * </p>
	 */
	private static final String CONFIG_FILE_PATH = "/config/kafka-config.properties";

	/**
	 * <p>
	 * Represents the log used to do auditing whenever a phase is started/ended. The
	 * audit log should include timestamp, project, phase, operation, and the
	 * operator This variable is initially null, initialized in constructor and
	 * immutable afterwards. It can be retrieved with the getter.
	 * </p>
	 */
	private final Log log;

	private void setLocalKafkaVariables() {
		targetURL = "https://api.topcoder-dev.com/v5/bus/events";
		topicName = "notifications.kafka.queue.java.test";
		originator = "AUTO_PILOT";
		clientId = "5fctfjaLJHdvM04kSrCcC8yn0I4t1JTd";
		clientSecret = "GhvDENIrYXo-d8xQ10fxm9k7XSVg491vlpvolXyWNBmeBdhsA5BAq2mH4cAAYS0x";
		authDomain = "topcoder-newauth.auth0.com";
		authAudience = "https://www.topcoder.com";
	}

	private PropertyResourceBundle getPropertyBundle() throws Exception {
		PropertyResourceBundle prb = null;
		String fileLocation = System.getProperty("user.dir") + CONFIG_FILE_PATH;
		File file = new File(fileLocation);
		if (file.exists()) {
			getLog().log(Level.INFO, "Using file in " + fileLocation);
			prb = new PropertyResourceBundle(new FileInputStream(file));
		} else {
			getLog().log(Level.WARN, "Will use file in classpath");
			prb = new PropertyResourceBundle(getClass().getResourceAsStream(CONFIG_FILE_PATH));
		}
		return prb;
	}

	public KafkaMessageProducer() {
		this.log = LogManager.getLog(DEFAULT_LOG_NAME);

		try {
			PropertyResourceBundle prb = getPropertyBundle();
			targetURL = prb.getString("kafka.target.url");
			clientId = prb.getString("kafka.security.clientId");
			clientSecret = prb.getString("kafka.security.clientsecret");
			authDomain = prb.getString("kafka.security.authdomain");
			authAudience = prb.getString("kafka.security.authAudience");
			topicName = prb.getString("kafka.topic");
			originator = prb.getString("kafka.originator");
			tokenExpirationTime = Integer.parseInt(prb.getString("kafka.security.tokenExpirationtime"));
			authProxyServerUrl = prb.getString("kafka.security.authProxyServerUrl");
		} catch (NumberFormatException e) {
			tokenExpirationTime = 60 * 24;
		} catch (Exception e) {
			setLocalKafkaVariables();
			getLog().log(Level.ERROR, "Exception in loading Kafka Properties : " + e.getMessage());
		}

		getLog().log(Level.INFO, String.format("Topic %s, originator %s", topicName, originator));
	}

	public void postRequestUsingGson(Object payload) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Central location to set topicname
		MessageTemplate messageTemplate = new MessageTemplate();
		messageTemplate.setTopic(topicName);
		messageTemplate.setOriginator(originator);
		messageTemplate.setTimestamp(dateFormat.format(new Date()));
		messageTemplate.setMime_type(MIME_TYPE);
		messageTemplate.setPayload(payload);

		Gson gson = new Gson();
		String strMessage = gson.toJson(messageTemplate);

		getLog().log(Level.DEBUG, "KAFKA_MESSAGE ::: " + strMessage);
		try {
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(targetURL);
			Entity entity = Entity.entity(strMessage, MediaType.APPLICATION_JSON_TYPE);
			Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
					.header("Authorization", "Bearer " + getM2MToken()).post(entity, Response.class);

			getLog().log(Level.INFO, response);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			getLog().log(Level.ERROR, "Exception when sending message to Kakfa Bus : " + e.getMessage());
			getLog().log(Level.ERROR, "details: " + sw.toString());
		} catch (Throwable e) {
			getLog().log(Level.ERROR, "Throwable when sending message to Kakfa Bus : " + e.getMessage());
		}
	}

	protected Log getLog() {
		return log;
	}

	public String getM2MToken() throws Exception {
		JWTTokenGenerator generator = JWTTokenGenerator.getInstance(this.clientId, this.clientSecret, this.authAudience,
				this.authDomain, this.tokenExpirationTime, this.authProxyServerUrl);
		return generator.getMachineToken();
	}

}
