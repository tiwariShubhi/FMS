package com.oopd.fms;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

public class post {

	private String msg;
	private String creator;
	private String id;
	private String creatorId;
	private String createdTimeStamp;
	private Date createdDate;
	private Time createdTime;
	private ArrayList<post> comments;
	
	post()
	{
		comments = new ArrayList<post>();
	}
	
	
	


	public ArrayList<post> getComments() {
		return comments;
	}





	public void setComments(ArrayList<post> comments) {
		this.comments = comments;
	}





	public String getMsg() {
		return msg;
	}




	public void setMsg(String msg) {
		this.msg = msg;
	}




	public String getCreator() {
		return creator;
	}




	public void setCreator(String creator) {
		this.creator = creator;
	}




	public String getId() {
		return id;
	}




	public void setId(String id) {
		this.id = id;
	}




	public String getCreatorId() {
		return creatorId;
	}




	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
		
	}




	public String getCreatedTimeStamp() {
		return createdTimeStamp;
	}




	public void setCreatedTimeStamp(String createdTimeStamp) {
		this.createdTimeStamp = createdTimeStamp;
	
		
	}




	public Date getCreatedDate() {
		return createdDate;
	}




	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}




	public Time getCreatedTime() {
		return createdTime;
	}




	public void setCreatedTime(Time createdTime) {
		this.createdTime = createdTime;
	}


	
}
