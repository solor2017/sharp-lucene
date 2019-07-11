/**
 * 
 */
package com.hcq.sharplucene.core.sample;

import com.hcq.sharplucene.core.annotation.FieldIndex;
import com.hcq.sharplucene.core.annotation.FieldStore;
import com.hcq.sharplucene.core.annotation.PKey;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: solor
 * @Description:
 */
public class SampleJavaBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7153417317917298956L;
	
	@PKey
	private int commentId;

	@FieldStore( "YES")
	@FieldIndex("NOT_ANALYZED")
	private String userName;

	@FieldStore( "YES")
	private boolean checkFlag;
	
	@FieldIndex("NOT_ANALYZED")
	private String url;
	
	@FieldStore( "YES")
	@FieldIndex("NOT_ANALYZED")
	private Date registTime;

	public int getCommentId() {
		return commentId;
	}

	public void setCommentId(int commentId) {
		this.commentId = commentId;
	}

	public String getUserName() {
		return userName;
		
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public boolean isCheckFlag() {
		return checkFlag;
	}

	public void setCheckFlag(boolean checkFlag) {
		this.checkFlag = checkFlag;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Date getRegistTime() {
		return registTime;
	}

	public void setRegistTime(Date registTime) {
		this.registTime = registTime;
	}
	
}
