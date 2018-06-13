package com.jemstep.bulkapi.v2;

public class CreateJobRequest {
	
	private String object;
	private String contentType;
	private String operation;
	private String lineEnding;
	
	
	
	public CreateJobRequest(String object, String contentType, String operation, String lineEnding) {
		super();
		this.object = object;
		this.contentType = contentType;
		this.operation = operation;
		this.lineEnding = lineEnding;
	}
	public String getObject() {
		return object;
	}
	public void setObject(String object) {
		this.object = object;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String getLineEnding() {
		return lineEnding;
	}
	public void setLineEnding(String lineEnding) {
		this.lineEnding = lineEnding;
	}
	
}
