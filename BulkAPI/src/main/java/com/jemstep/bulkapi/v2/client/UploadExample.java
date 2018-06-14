package com.jemstep.bulkapi.v2.client;

import com.jemstep.bulkapi.v2.RestBulkClient;

public class UploadExample {

	public static void main(String[] args) {
		
		if(args.length != 2)
		{
			System.out.println("Usage: UploadExample object_name file_path");
		}
		else
		{	
		//Account_PE__e
        //Account_PE.csv	
		String object = args[0];
		String file = args[1];
		RestBulkClient client = new RestBulkClient().createFromRefreshToken();

		String jobId = client.createJob(object);
		int status = 0;
		if (jobId != null) {
			status = client.uploadData(file, jobId);
			client.closeOrAbortJob(jobId);

		}

		if (status == 201) {
			client.getSucessfulResults(jobId);
			client.getFailedResults(jobId);
			client.getUnprocessedResults(jobId);
		}
		}

	}

}
