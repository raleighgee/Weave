package weave.servlets;

import java.io.File;
import java.rmi.RemoteException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.google.gson.JsonObject;

import weave.config.AwsContextParams;
import weave.models.ScriptManagerService;
import weave.utils.AWSUtils;
import weave.utils.AWSUtils.SCRIPT_TYPE;

public class ScriptManagementServlet extends WeaveServlet
{
	private static final long serialVersionUID = 1L;
	
	public ScriptManagementServlet(){
		
	}

	private File rDirectory;
	private File stataDirectory;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		rDirectory = new File(AwsContextParams.getInstance(config.getServletContext()).getRScriptsPath());
		stataDirectory = new File(AwsContextParams.getInstance(config.getServletContext()).getStataScriptsPath());
	}
	
	public String getScript(String scriptName) throws Exception {

		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return ScriptManagerService.getScript(rDirectory, scriptName);
		} else if( AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return ScriptManagerService.getScript(stataDirectory, scriptName);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}

	public String[] getListOfScripts() throws Exception{

		File[] directories = {rDirectory, stataDirectory};
		return ScriptManagerService.getListOfScripts(directories);
	}

	public boolean saveScriptMetadata (String scriptName, JsonObject metadata) throws Exception {

		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return ScriptManagerService.saveScriptMetadata(rDirectory, scriptName, metadata);
		} else if( AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return ScriptManagerService.saveScriptMetadata(stataDirectory, scriptName, metadata);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}

	public Object getScriptMetadata (String scriptName) throws Exception {

		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return ScriptManagerService.getScriptMetadata(rDirectory, scriptName);
		} else if( AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return ScriptManagerService.getScriptMetadata(stataDirectory, scriptName);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}

	public boolean uploadNewScript(String scriptName, String content) throws Exception {

		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return ScriptManagerService.uploadNewScript(rDirectory, scriptName, content);
		} else if( AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return ScriptManagerService.uploadNewScript(stataDirectory, scriptName, content);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}

	public boolean uploadNewScript(String scriptName, String content, JsonObject metadata) throws Exception {

		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return ScriptManagerService.uploadNewScript(rDirectory, scriptName, content, metadata);
		} else if( AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return ScriptManagerService.uploadNewScript(stataDirectory, scriptName, content, metadata);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}

	public boolean deleteScript(String scriptName) throws Exception {

		if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return ScriptManagerService.deleteScript(rDirectory, scriptName);
		} else if( AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return ScriptManagerService.deleteScript(stataDirectory, scriptName);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
//	public Object delegateToScriptManagementMethods(String action, Map<String, Object> params){
//		Object returnStatus = null;
//		
//		if(action.matches("REPORT_SCRIPTS_LIST")){
//				returnStatus = ScriptManagerService.getListOfScripts(awsConfigPath);
//		}
//		else if(action.matches("REPORT_SCRIPT_CONTENTS")){
//			try {
//				returnStatus = ScriptManagerService.getScript(awsConfigPath,params);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} 
//		}
//		else if(action.matches("SAVE_METADATA")){
//			try{
//				returnStatus = ScriptManagerService.saveMetadata(awsConfigPath, params);
//			}
//			catch(Exception e){
//				e.printStackTrace();
//			}
//		}
//		else if(action.matches("REPORT_SCRIPT_METADATA")){
//			try{
//				returnStatus = ScriptManagerService.getScriptMetadata(awsConfigPath, params);
//			}
//			catch(Exception e){
//				e.printStackTrace();
//			}
//		}
//		else if(action.matches("UPLOAD_NEW_SCRIPT")){
//				returnStatus = ScriptManagerService.uploadNewScript(awsConfigPath, params);
//		}
//	
//		
//		
//		
//		return ScriptManagerService.returnStatus;
//	}
}
