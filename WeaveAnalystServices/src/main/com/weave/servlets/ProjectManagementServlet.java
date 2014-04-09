package com.weave.servlets;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import weave.beans.JsonRpcRequestModel;
import weave.servlets.WeaveServlet;

import com.weave.interfaces.IScriptEngine;
import com.weave.models.AwsProjectService;

import flex.messaging.io.amf.ASObject;


public class ProjectManagementServlet extends WeaveServlet implements IScriptEngine 
{	
	private static final long serialVersionUID = 1L;
	protected final String ACTION = "action";
	protected final String PARAMS = "params";
	public ProjectManagementServlet(){
		
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		handleServletRequest(request, response);
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	handleServletRequest(request, response);
    }
	
    private void handleServletRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
    	try{
	    		if (request.getMethod().equals("GET"))
	    		{
	        		
	    		}
	    		else if (request.getMethod().equals("POST"))
	    		{
	    			List<String> urlParamNames = Collections.list(request.getParameterNames());
	        		
	    			HashMap<String, Object> params = new HashMap<String,Object>();
	    			
	    			for (String paramName : urlParamNames)
	    				params.put(paramName, request.getParameterValues(paramName));
	    			
	    			String action = params.remove(ACTION).toString();
	    			
	    			delegateToMethods(action, params);
	    		    	
	        	}
    		}
    	
    		finally{
    			
    		}
    	}
    	
    
	public Object delegateToMethods(String action, Map<String, Object> params){
		Object returnStatus = null;
		
		if(action.matches("REPORT_PROJECTS")){
			try {
				returnStatus = AwsProjectService.getProjectFromDatabase();
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if(action.matches("REPORT_QUERY_OBJECTS"))
		{
			try {
				returnStatus = AwsProjectService.getQueryObjectsFromDatabase(params);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		else if(action.matches("DELETE_PROJECT"))
		{
			try {
				returnStatus = AwsProjectService.deleteProjectFromDatabase(params);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if(action.matches("DELETE_QUERY_OBJECT")){
			try {
				returnStatus = AwsProjectService.deleteQueryObjectFromProjectFromDatabase(params);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if(action.matches("INSERT_QUERY_OBJECTS")){
			try {
				returnStatus = AwsProjectService.insertMultipleQueryObjectInProjectFromDatabase(params);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
		return returnStatus;
		
	}
}
