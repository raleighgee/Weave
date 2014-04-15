package com.weave.servlets;

import static weave.config.WeaveConfig.initWeaveConfig;

import java.rmi.RemoteException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import weave.config.WeaveContextParams;
import weave.servlets.DataService;
import weave.servlets.WeaveServlet;
import weave.utils.SQLUtils.WhereClause.NestedColumnFilters;

import com.weave.beans.ScriptResult;
import com.weave.config.AwsContextParams;
import com.weave.interfaces.IScriptEngine;
import com.weave.models.AwsRService;
import com.weave.models.AwsStataService;
import com.weave.utils.AWSUtils;
import com.weave.utils.AWSUtils.SCRIPT_TYPE;

public class ComputationServlet extends WeaveServlet implements IScriptEngine
{	
	ComputationServlet()
	{
	}
	
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		initWeaveConfig(WeaveContextParams.getInstance(config.getServletContext()));
		programPath = WeaveContextParams.getInstance(config.getServletContext()).getRServePath();
		tempDirPath = AwsContextParams.getInstance(config.getServletContext()).getAwsConfigPath() + "temp";
	}
	
	private static final long serialVersionUID = 1L;
	
	private String programPath = "";
	private String tempDirPath = "";
	public ScriptResult runScript(String scriptName, int[] ids, NestedColumnFilters filters) throws Exception
	{
		Object resultData = null;
		
		long startTime = 0; 
		long endTime = 0;
		long time1 = 0;
		long time2 = 0;
		
		// Start the timer for the data request
		startTime = System.currentTimeMillis();
		Object[][] recordData = DataService.getFilteredRows(ids, filters, null).recordData;
		if(recordData.length == 0){
			throw new RemoteException("Query produced no rows...");
		}
		
		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			// R requires the data as column data
			Object[][] columnData = AWSUtils.transpose(recordData);
			endTime = System.currentTimeMillis(); // end timer for data request
			recordData = null;
			time1 = startTime - endTime;
			
			// Run and time the script
			startTime = System.currentTimeMillis();
			resultData = AwsRService.runScript(scriptName, columnData);
			endTime = System.currentTimeMillis();
			time2 = startTime - endTime;
			
		}
		else if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			endTime = System.currentTimeMillis(); // end timer for data request
			
			// Run and time the script
			startTime = System.currentTimeMillis();
			resultData = AwsStataService.runScript(scriptName, recordData, programPath, tempDirPath);
			endTime = System.currentTimeMillis();
			time2 = endTime - startTime;
		}
		
		ScriptResult result = new ScriptResult();
		
		result.data = resultData;
		result.times[0] = time1;
		result.times[1] = time2;
		
		return result;
	}
}
