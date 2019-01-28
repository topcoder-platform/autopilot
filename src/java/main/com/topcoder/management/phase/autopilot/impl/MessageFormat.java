package com.topcoder.management.phase.autopilot.impl;

public class MessageFormat {

	private String date;
	private long projectId;
	private long phaseId;
	private String phaseTypeName;
	private String state;
	private String operator;
	private String projectStatus;
	
	public MessageFormat(String date, long projectId, long phaseId, String phaseTypeName, String state, String operator,
						 String projectStatus) {
		super();
		this.date = date;
		this.projectId = projectId;
		this.phaseId = phaseId;
		this.phaseTypeName = phaseTypeName;
		this.state = state;
		this.operator = operator;
		this.projectStatus = projectStatus;
	}
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public long getProjectId() {
		return projectId;
	}
	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}
	public long getPhaseId() {
		return phaseId;
	}
	public void setPhaseId(long phaseId) {
		this.phaseId = phaseId;
	}
	public String getPhaseTypeName() {
		return phaseTypeName;
	}
	public void setPhaseTypeName(String phaseTypeName) {
		this.phaseTypeName = phaseTypeName;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getProjectStatus() {
		return projectStatus;
	}

	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}
}
