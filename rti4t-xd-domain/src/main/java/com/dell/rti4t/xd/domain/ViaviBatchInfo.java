package com.dell.rti4t.xd.domain;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class ViaviBatchInfo implements Serializable {
	public String name;
	public Date downloaded;
	public Integer status;
	public Date created;
	public int failedCount;
	public Date lastFailed;
}
