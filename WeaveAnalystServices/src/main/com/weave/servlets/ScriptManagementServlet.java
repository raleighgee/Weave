package com.weave.servlets;

import static weave.config.WeaveConfig.initWeaveConfig;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import weave.config.WeaveContextParams;
import weave.servlets.WeaveServlet;

import com.weave.config.AwsContextParams;
import com.weave.models.ScriptManagerService;

public class ScriptManagementServlet extends WeaveServlet
{
	private static final long serialVersionUID = 1L;
	
	public ScriptManagementServlet(){
		
	}
	
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		initWeaveConfig(WeaveContextParams.getInstance(config.getServletContext()));
		rDirectory = new File(AwsContextParams.getInstance(config.getServletContext()).getRScriptsPath());
		stataDirectory = new File(AwsContextParams.getInstance(config.getServletContext()).getStataScriptsPath());
	}
	
	private File rDirectory;
	private File stataDirectory;
	
	// TODO replicate the methods in Script Manager Service and use the appropriate directories
	
//	public Object delegateToScriptManagementMethods(String action, Map<String, Object> params){
//		Object returnStatus = null;
//		
//		if(action.matches("REPORT_SCRIPTS_LIST")){
//				returnStatus = ScriptManagerService.getListOfScripts();
//		}
//		else if(action.matches("REPORT_SCRIPT_CONTENTS")){
//			try {
//				returnStatus = ScriptManagerService.getScript(params);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} 
//		}
//		else if(action.matches("SAVE_METADATA")){
//			try{
//				returnStatus = ScriptManagerService.saveMetadata(params);
//			}
//			catch(Exception e){
//				e.printStackTrace();
//			}
//		}
//		else if(action.matches("REPORT_SCRIPT_METADATA")){
//			try{
//				returnStatus = ScriptManagerService.getScriptMetadata(params);
//			}
//			catch(Exception e){
//				e.printStackTrace();
//			}
//		}
//		else if(action.matches("UPLOAD_NEW_SCRIPT")){
//				returnStatus = ScriptManagerService.uploadNewScript(params);
//		}
//	
//		
//		
//		
//		return returnStatus;
//	}
}
