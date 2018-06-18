package com.jemstep.bulkapi.v2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sforce.async.JobStateEnum;

public class RestBulkClient {
	private static String TOKEN_URL;
	private static String CLIENT_ID;
	private static String CLIENT_SECRET;
	private static String REFRESH_TOKEN;
	private static String accessToken;
	private static String instanceUrl;

	private static final String GRANT_TYPE = "refresh_token";
	private static final String BEARER = "Bearer";
	private static final String CONTENT_TYPE = "application/json";
	private static final String ACCEPT = "application/json";
	private static final String REST_URI = "/services/data/v42.0/jobs/ingest/";
	private static final String CREATE_JOB_URI = "/ingest/";

	static {
		Properties configProps = new Properties();
		try {
			configProps.load(new FileInputStream("config.properties"));
			TOKEN_URL = (String) configProps.get("TOKEN_URL");
			CLIENT_ID = (String) configProps.get("CLIENT_ID");
			CLIENT_SECRET = (String) configProps.get("CLIENT_SECRET");
			REFRESH_TOKEN = (String) configProps.get("REFRESH_TOKEN");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public RestBulkClient createFromRefreshToken() {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			final List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
			loginParams.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
			loginParams.add(new BasicNameValuePair("client_id", CLIENT_ID));
			loginParams.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
			loginParams.add(new BasicNameValuePair("refresh_token", REFRESH_TOKEN));

			final HttpPost post = new HttpPost(TOKEN_URL);
			post.setEntity(new UrlEncodedFormEntity(loginParams));
			final HttpResponse loginResponse = httpclient.execute(post);

			// parse
			final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

			final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);
			accessToken = loginResult.get("access_token").asText();
			instanceUrl = loginResult.get("instance_url").asText();
			System.out.println("accessToken - " + accessToken);
			System.out.println("instanceUrl - " + instanceUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return this;
	}

	public String createJob(String object) {
		JsonNode responseJson = null;
		HttpResponse response = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			final String uri = instanceUrl + REST_URI;
			System.out.println("createjob URI -> " + uri);
			final String authorization = BEARER + " " + accessToken;

			// Set JSON body
			final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
			final CreateJobRequest request = new CreateJobRequest(object, "CSV", "insert", "LF");

			final String requestJson = mapper.writeValueAsString(request);
			final StringEntity jsonBody = new StringEntity(requestJson);

			// post the request
			final HttpPost post = new HttpPost(uri);
			post.setHeader("Authorization", authorization);
			post.setHeader("Content-Type", CONTENT_TYPE);
			post.setEntity(jsonBody);
			System.out
					.println("create job input jsonBody -> " + mapper.readValue(jsonBody.getContent(), JsonNode.class));

			response = httpclient.execute(post);
			responseJson = mapper.readValue(response.getEntity().getContent(), JsonNode.class);

			System.out.println("create job responseJson -> " + responseJson);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return (responseJson.get("id") == null?null:(responseJson.get("id")).asText());

	}

	public int uploadData(String file, String jobId) {
		final String uploadUri = instanceUrl + REST_URI + jobId + "/batches";
		System.out.println("upload data uri -> " + uploadUri);
		final String authorization = BEARER + " " + accessToken;

		StringEntity data = null;
		HttpResponse response = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			String fileContents = new String(Files.readAllBytes(Paths.get(file)));
			data = new StringEntity(fileContents);

			data.setContentType("text/csv");

			final HttpPut put = new HttpPut(uploadUri);
			put.setHeader("Authorization", authorization);
			put.setHeader("Content-Type", "text/csv");
			put.setHeader("Accept", "application/json");
			put.setEntity(data);
			response = httpclient.execute(put);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("upload data response status code -> " + response.getStatusLine().getStatusCode());
		return response.getStatusLine().getStatusCode();
	}

	public void getSucessfulResults(String jobId) {
		// Set JSON body
		final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String url = instanceUrl + REST_URI + jobId + "/successfulResults/";
		final String authorization = BEARER + " " + accessToken;

		final HttpGet get = new HttpGet(url);
		get.setHeader("Authorization", authorization);
		get.setHeader("Content-Type", CONTENT_TYPE);

		HttpResponse response = null;
		String responseEntity = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			response = httpclient.execute(get);
			if(response.getEntity() != null)
				responseEntity = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("get successful responseJson -> " + responseEntity);

	}

	public void getFailedResults(String jobId) {
		// Set JSON body
		final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String url = instanceUrl + REST_URI + jobId + "/failedResults/";
		final String authorization = BEARER + " " + accessToken;

		final HttpGet get = new HttpGet(url);
		get.setHeader("Authorization", authorization);
		get.setHeader("Content-Type", CONTENT_TYPE);
		HttpResponse response = null;
		String responseEntity = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			response = httpclient.execute(get);
			responseEntity = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("get failed responseJson -> " + responseEntity);
	}

	public void getUnprocessedResults(String jobId) {
		// Set JSON body
		final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String url = instanceUrl + REST_URI + jobId + "/unprocessedrecords/";
		final String authorization = BEARER + " " + accessToken;
		// Set Headers
		final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
		apiParams.add(new BasicNameValuePair("Authorization", authorization));
		apiParams.add(new BasicNameValuePair("Content-Type", CONTENT_TYPE));

		apiParams.add(new BasicNameValuePair("Accept", ACCEPT));

		final HttpGet get = new HttpGet(url);
		get.setHeader("Authorization", authorization);
		get.setHeader("Content-Type", CONTENT_TYPE);

		HttpResponse response = null;
		String responseEntity = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			response = httpclient.execute(get);
			responseEntity = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("get unprocessed responseJson -> " + responseEntity);
	}

	public void closeOrAbortJob(String jobId) {
		// Set JSON body
		final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String url = instanceUrl + REST_URI + jobId;
		final String authorization = BEARER + " " + accessToken;

		final HttpPatch patch = new HttpPatch(url);
		patch.setHeader("Authorization", authorization);
		patch.setHeader("Content-Type", CONTENT_TYPE);
		StringEntity jsonBody = null;
		HttpResponse response = null;
		JsonNode responseJson = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			jsonBody = new StringEntity("{\"state\":" + "\"" + JobStateEnum.UploadComplete.toString() + "\"}");
			patch.setEntity(jsonBody);
			response = httpclient.execute(patch);
			responseJson = mapper.readValue(response.getEntity().getContent(), JsonNode.class);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("close job responseJson -> " + responseJson);
	}

	public void getAllJobs() {
		// Set JSON body
		final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String url = instanceUrl + "/services/data/v42.0/jobs/ingest";
		final String authorization = BEARER + " " + accessToken;

		final HttpGet get = new HttpGet(url);
		get.setHeader("Authorization", authorization);
		get.setHeader("Content-Type", CONTENT_TYPE);

		final CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpResponse response = null;
		try {
			response = httpclient.execute(get);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JsonNode responseJson = null;
		try {
			responseJson = mapper.readValue(response.getEntity().getContent(), JsonNode.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("responseJson -> " + responseJson);

	}

}