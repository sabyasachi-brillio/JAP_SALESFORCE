package com.jemstep.bulkapi.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SfdcRestAPIExample {
	
	private static final String TOKEN_URL =  "https://login.salesforce.com/services/oauth2/token";
	private static final String CLIENT_ID = "3MVG9zlTNB8o8BA109nYxvVODfPoS4t1BejIPsn7lEkNvVfFX2._DHwWLxkJi60AHeb8fUEohGte56FfXTPh9";
	private static final String CLIENT_SECRET = "6545678178920959810";
	private static final String GRANT_TYPE = "refresh_token";
	private static final String REFRESH_TOKEN = "5Aep861UTWIWNgl0kdGvykHnCR3hoRyKPUER35xbgjYUZ92F2GjYTxdSOHPNBtvf9KQwQOcyISv8x.8ltaAqIZI";
	private static final String BEARER = "Bearer";
	private static final String CONTENT_TYPE = "application/json";
	private static final String ACCEPT = "application/json";
	private static final String REST_URI = "/services/data/v42.0/jobs";
	private static final String CREATE_JOB_URI = "/ingest/";
	
	public static void main(String args[]) {
		RefreshTokenResponseWrapper wrapper = getAccessTokenFromRefreshToken();
		//System.out.println("wrapper -> "+wrapper.getAccessToken());
		
		createJob(wrapper);
	}
	
	private static RefreshTokenResponseWrapper getAccessTokenFromRefreshToken() {
		RefreshTokenResponseWrapper wrapper = null;
		CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.createDefault();

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
            final String accessToken = loginResult.get("access_token").asText();
            final String instanceUrl = loginResult.get("instance_url").asText();

            wrapper = new RefreshTokenResponseWrapper(accessToken, instanceUrl);
            System.out.println("accessToken - "+accessToken);
            System.out.println("instanceUrl - "+instanceUrl);
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
        	try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        return wrapper;
	}
	
	private static void createJob(RefreshTokenResponseWrapper wrapper) {
        try {
        	final String createJobURI = "https://japqa.my.salesforce.com" + REST_URI + CREATE_JOB_URI;
        	System.out.println("createJobURI -> "+createJobURI);
        	final String authorization = BEARER + " " + "00Df4000002Zlle!ARwAQGINn778tkjvmVCZIRrEKDH52qLWDMLsRXKHlDP_JvGmRRGK6dnWIkom7e3L1O9RRIgce5uoo2yCHB1ayUp2NlADDenI";
        	System.out.println("authorization -> "+authorization);

            //Set Headers
            final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
            apiParams.add(new BasicNameValuePair("Authorization", authorization));
            apiParams.add(new BasicNameValuePair("Content-Type", CONTENT_TYPE));
            apiParams.add(new BasicNameValuePair("Accept", ACCEPT));
            
            //Set JSON body
            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            final CreateJobRequest request = new CreateJobRequest("", "CSV", "insert", "CRLF");
            /*request.object = "Contact";
            request.contentType = "CSV";
            request.operation = "insert";
            request.lineEnding = "CRLF";*/
            final String requestJson = mapper.writeValueAsString(request);
            final StringEntity jsonBody = new StringEntity(requestJson);

            //post the request
            final HttpPost post = new HttpPost(createJobURI);
            //post.setEntity(new UrlEncodedFormEntity(apiParams));
            //post.setEntity(jsonBody);
            System.out.println("jsonBody -> "+mapper.readValue(jsonBody.getContent(), JsonNode.class));
            
            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final HttpResponse response = httpclient.execute(post);

            final JsonNode responseJson = mapper.readValue(response.getEntity().getContent(), JsonNode.class);
            System.out.println("responseJson -> "+responseJson);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        	
        }
	}
}


