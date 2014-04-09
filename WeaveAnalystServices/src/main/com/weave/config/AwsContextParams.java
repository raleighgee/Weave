package com.weave.config;

import javax.servlet.ServletException;

import weave.config.WeaveContextParams;

public class AwsContextParams extends WeaveContextParams
{
	private static String awsConfigPath = "";
	
	public AwsContextParams(String configPath, String docrootPath ) throws ServletException
	{
		
		super(configPath, docrootPath);
		awsConfigPath= configPath + "/../aws-config/";
	}
	
	
	public static String getAwsConfigPath(){
		
		return awsConfigPath;
	}
	
}
