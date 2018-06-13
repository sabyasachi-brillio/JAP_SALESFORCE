package com.jemstep.bulkapi.v2;

public class RefreshTokenResponseWrapper {
	
	private String accessToken;
	private String instanceUrl;
	
	public RefreshTokenResponseWrapper(String accessToken, String instanceUrl) {
		this.accessToken = accessToken;
		this.instanceUrl = instanceUrl;
	}
	
	public String getAccessToken() {
		return this.accessToken;
	}
	
	public String getInstanceUrl() {
		return this.instanceUrl;
	}
}
