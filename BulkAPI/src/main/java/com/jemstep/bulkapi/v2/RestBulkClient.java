package com.jemstep.bulkapi.v2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
	private static final String TOKEN_URL =  "https://login.salesforce.com/services/oauth2/token";
	private static final String CLIENT_ID = "3MVG9zlTNB8o8BA109nYxvVODfPoS4t1BejIPsn7lEkNvVfFX2._DHwWLxkJi60AHeb8fUEohGte56FfXTPh9";
	private static final String CLIENT_SECRET = "6545678178920959810";
	private static final String GRANT_TYPE = "refresh_token";
	private static final String REFRESH_TOKEN = "5Aep861UTWIWNgl0kdGvykHnCR3hoRyKPUER35xbgjYUZ92F2GjYTxdSOHPNBtvf9KQwQOcyISv8x.8ltaAqIZI";
	private static final String BEARER = "Bearer";
	private static final String CONTENT_TYPE = "application/json";
	private static final String ACCEPT = "application/json";
	private static final String REST_URI = "/services/data/v42.0/jobs/ingest/";
	private static final String CREATE_JOB_URI = "/ingest/";
		
	private String accessToken;
	private String instanceUrl;
	

	 public RestBulkClient createFromRefreshToken() {
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

	            this.accessToken = accessToken;
	            this.instanceUrl = instanceUrl;
	            //wrapper = new RefreshTokenResponseWrapper(accessToken, instanceUrl);
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
	        return this;
}
	 
	 public String createJob(String object)
	 {
		 try {
	        	final String createJobURI = instanceUrl + REST_URI;
	        	System.out.println("createjob createJobURI -> "+createJobURI);
	        	final String authorization = BEARER + " " + accessToken;

	            //Set JSON body
	            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	            final CreateJobRequest request = new CreateJobRequest(object, "CSV", "insert", "CRLF");
	           
	            final String requestJson = mapper.writeValueAsString(request);
	            final StringEntity jsonBody = new StringEntity(requestJson);

	            //post the request
	            final HttpPost post = new HttpPost(createJobURI);
	            post.setHeader("Authorization", authorization);
	            post.setHeader("Content-Type", CONTENT_TYPE);
	            post.setEntity(jsonBody);
	            System.out.println("create job input jsonBody -> "+mapper.readValue(jsonBody.getContent(), JsonNode.class));
	            
	            final CloseableHttpClient httpclient = HttpClients.createDefault();
	            final HttpResponse response = httpclient.execute(post);

	            final JsonNode responseJson = mapper.readValue(response.getEntity().getContent(), JsonNode.class);
	            
	            System.out.println("create job responseJson -> "+responseJson);
	            
	            return (responseJson.get("id")).asText();
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
	        finally {
	        	
	        }
		 return null;
		}
	 
	 public int uploadData(String file, String jobId)
	 {
		 final String uploadUri = instanceUrl + REST_URI+ jobId + "/batches";
     	 System.out.println("upload data createJobURI -> "+uploadUri);
     	 final String authorization = BEARER + " " + accessToken;
     	
     	 
     	 MultipartEntityBuilder builder = MultipartEntityBuilder.create();
       	 String fileContents = "Account_Name__c,Account_Number__c,Account_Status__c,Account_Type__c,Account_Value__c,Contact__c,Date_Updated__c,Institution__c,Jemstep_Account_Id__c,Jemstep_Id__c\n"+
                "Kotak,12345,OK,Saving,1230000,,,Govt,Account_PE_123,Account_PE_123\n";
//     	 StringBody stringBody1 = new StringBody(fileContents, ContentType.DEFAULT_TEXT);
//     	 
//     	 builder.addTextBody(file, fileContents,ContentType.DEFAULT_BINARY);
//     	 builder.addPart("upfile",stringBody1);
//         HttpEntity multipart = builder.build();
         
     	StringEntity data = null;
     	try {
			fileContents = new String(Files.readAllBytes(Paths.get(file)));
			
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        
		
		 data = new StringEntity(fileContents,"UTF-8");
		 data.setContentType("text/csv");
		 
         final HttpPut put = new HttpPut(uploadUri);
         put.setHeader("Authorization", authorization);
         put.setHeader("Content-Type", "text/csv");
         put.setHeader("Accept", "application/json");
         put.setEntity(data);
         
         HttpResponse response = null;
         
         try (CloseableHttpClient httpclient = HttpClients.createDefault()){
        	 response = httpclient.execute(put);
        	 
         } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
         
			
		return response.getStatusLine().getStatusCode();
	 }
	 
	 public void getSucessfulResults(String jobId)
	 {
		//Set JSON body
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String url = instanceUrl + REST_URI+ jobId + "/successfulResults/";
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
           
		String responseEntity = null;
      
        try {
			responseEntity = EntityUtils.toString(response.getEntity());
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        System.out.println("get successful responseJson -> "+responseEntity);
		
	 }
	 public void getFailedResults(String jobId)
	 {
		//Set JSON body
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String url = instanceUrl + REST_URI+ jobId + "/failedResults/";
        final String authorization = BEARER + " " + accessToken;
        //Set Headers
        final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
        apiParams.add(new BasicNameValuePair("Authorization", authorization));
        apiParams.add(new BasicNameValuePair("Content-Type", CONTENT_TYPE));
      
        apiParams.add(new BasicNameValuePair("Accept", ACCEPT));

         
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

		String responseEntity = null;
	      
        try {
			responseEntity = EntityUtils.toString(response.getEntity());
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        System.out.println("get failed responseJson -> "+responseEntity);
	 }
	 
	 
	 public void getUnprocessedResults(String jobId)
	 {
		//Set JSON body
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String url = instanceUrl + REST_URI+ jobId + "/unprocessedrecords/";
        final String authorization = BEARER + " " + accessToken;
        //Set Headers
        final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
        apiParams.add(new BasicNameValuePair("Authorization", authorization));
        apiParams.add(new BasicNameValuePair("Content-Type", CONTENT_TYPE));
      
        apiParams.add(new BasicNameValuePair("Accept", ACCEPT));

         
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


		String responseEntity = null;
	      
        try {
			responseEntity = EntityUtils.toString(response.getEntity());
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        System.out.println("get unprocessed responseJson -> "+responseEntity);
	 }
	 
	 public void closeOrAbortJob(String jobId)
	 {
		//Set JSON body
	        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	        String url = instanceUrl + REST_URI+ jobId;
	        final String authorization = BEARER + " " + accessToken;
	        //Set Headers
	        final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
	        apiParams.add(new BasicNameValuePair("Authorization", authorization));
	        apiParams.add(new BasicNameValuePair("Content-Type", CONTENT_TYPE));
	      
	        apiParams.add(new BasicNameValuePair("Accept", ACCEPT));

	         
	        final HttpPatch patch = new HttpPatch(url);
	        patch.setHeader("Authorization", authorization);
	        patch.setHeader("Content-Type", CONTENT_TYPE);
	        StringEntity jsonBody= null;
	        try {
				 jsonBody = new StringEntity("{\"state\":"+"\""+JobStateEnum.UploadComplete.toString()+"\"}");
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        patch.setEntity(jsonBody);
	        final CloseableHttpClient httpclient = HttpClients.createDefault();
	        HttpResponse response = null;
			try {
				response = httpclient.execute(patch);
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
	        System.out.println("close job responseJson -> "+responseJson);
	 }
	 
	 public void getAllJobs() {
			 //Set JSON body
	         final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	        String url = instanceUrl + "/services/data/v42.0/jobs/ingest";
	        final String authorization = BEARER + " " + accessToken;
	        //Set Headers
            final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
            apiParams.add(new BasicNameValuePair("Authorization", authorization));
            apiParams.add(new BasicNameValuePair("Content-Type", CONTENT_TYPE));
          
            apiParams.add(new BasicNameValuePair("Accept", ACCEPT));

             
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
            System.out.println("responseJson -> "+responseJson);
	 
	 }
	 
}