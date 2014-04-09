package com.weave.servlets;

import org.apache.commons.io.FilenameUtils;

import weave.servlets.WeaveServlet;
import weave.utils.SQLUtils.WhereClause.NestedColumnFilters;

import com.weave.beans.ScriptResult;
import com.weave.interfaces.IScriptEngine;
import com.weave.models.AwsRService;
import com.weave.models.AwsStataService;;


public class ComputationServlet extends WeaveServlet implements IScriptEngine
{	
	ComputationServlet(){
		
	}
	private static final long serialVersionUID = 1L;
	
	private String computationEngineUsed = "";//TODO move logic into separate level
	
	@SuppressWarnings("unused")
	private void decideComputationEngine(String scriptName, int [] ids, NestedColumnFilters filters){
		
		String extension = FilenameUtils.getExtension(scriptName);
		
		//Use R as the computation engine
		if(extension.equalsIgnoreCase("R")){
			computationEngineUsed = "R";
		}
		//Use STATA as the computation engine
		if(extension.equalsIgnoreCase("do")){
			computationEngineUsed = "STATA";
		}
		
		if(computationEngineUsed.matches("R"))
			runRScript(scriptName, ids, filters);
		
		if(computationEngineUsed.matches("STATA"))
			runStataScript(scriptName, ids, filters);
		
	}
	
	
	private ScriptResult runRScript(String scriptName, int [] ids, NestedColumnFilters filters){
		ScriptResult finalResult = null;
		try {
			finalResult = AwsRService.runScriptWithFilteredColumns(scriptName, ids,  filters);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return finalResult;
	}
	
	private int runStataScript(String scriptName, int[]ids, NestedColumnFilters filters){
		int finalResult = 0;//TODO revise and change this
		try {
			finalResult = AwsStataService.runStataScript();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return finalResult;
	}
}
