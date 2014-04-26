package weave.servlets;

import java.util.Map;

import weave.servlets.WeaveServlet;

import weave.models.ScriptManagerService;

public class ScriptManagementServlet extends WeaveServlet
{
	private static final long serialVersionUID = 1L;
	
	public ScriptManagementServlet(){
		
	}
	
	public Object delegateToScriptManagementMethods(String action, Map<String, Object> params){
		Object returnStatus = null;
		
		if(action.matches("REPORT_SCRIPTS_LIST")){
				returnStatus = ScriptManagerService.getListOfScripts();
		}
		else if(action.matches("REPORT_SCRIPT_CONTENTS")){
			try {
				returnStatus = ScriptManagerService.getScript(params);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		else if(action.matches("SAVE_METADATA")){
			try{
				returnStatus = ScriptManagerService.saveMetadata(params);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		else if(action.matches("REPORT_SCRIPT_METADATA")){
			try{
				returnStatus = ScriptManagerService.getScriptMetadata(params);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		else if(action.matches("UPLOAD_NEW_SCRIPT")){
				returnStatus = ScriptManagerService.uploadNewScript(params);
		}
	
		
		
		
		return returnStatus;
	}
}
