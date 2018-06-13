package com.jemstep.bulkapi.v2.client;

import com.jemstep.bulkapi.v2.RestBulkClient;

public class UploadExample {

	public static void main(String[] args) {
	RestBulkClient client = new RestBulkClient().createFromRefreshToken();
	
    String jobId = client.createJob("Account_PE__e");
	int status = 0;
	if(jobId != null)
	{
		 status = client.uploadData("Account_PE.csv", jobId);
		 client.closeOrAbortJob(jobId);
		 
	}
	
	if(status == 201)
	{
		client.getSucessfulResults(jobId);
		client.getFailedResults(jobId);
		client.getUnprocessedResults(jobId);
	}
	
	}
}
