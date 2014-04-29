/*
     Weave (Web-based Analysis and Visualization Environment)
    Copyright (C) 2008-2011 University of Massachusetts Lowell

    This file is a part of Weave.

    Weave is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License, Version 3,
    as published by the Free Software Foundation.

    Weave is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Weave.  If not, see <http://www.gnu.org/licenses/>.
 */

package weave.servlets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.script.ScriptException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RFactor;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import weave.beans.RResult;
import weave.config.WeaveConfig;
import weave.config.WeaveContextParams;
import weave.utils.CSVParser;
import weave.utils.CommandUtils;
import weave.utils.SQLResult;
import weave.utils.SQLUtils;
import weave.utils.SQLUtils.WhereClause.NestedColumnFilters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AWSRService extends RService
{
	private static final long serialVersionUID = 1L;

	public AWSRService()
	{

	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		awsConfigPath = WeaveContextParams.getInstance(
				config.getServletContext()).getConfigPath();
		awsConfigPath = awsConfigPath + "/../aws-config/";
		programPath = WeaveContextParams.getInstance(config.getServletContext()).getStataPath();
		rScriptsPath= awsConfigPath + "RScripts/";
		stataScriptsPath = awsConfigPath + "StataScripts/";
		tempDirPath = awsConfigPath + "temp/";

	}

	private String awsConfigPath = "";
	private String programPath = "";
	private String rScriptsPath = "";
	private String stataScriptsPath = "";
	private String tempDirPath = "";
	
	public static class AWSConnectionObject {
		String connectionType;
        String user;
        String password;
        String schema;
        String host;
        String dsn;
	}
	
	
	public class ScriptResult {
		public Object data;
		
		/* {dataLoatTime, dataComputationTime} */
		public long[] times = new long[2];
	}

	
	/**
	    * @param userName author of a given Project
	    * @param projectName project which contains queryObjects
	    * @return  collection of queryObjects in the project 
	    * @throws Exception
	    */
	//retrieves all the projects belonging to a particular user
	public String[] getProjectFromDatabase() throws SQLException, RemoteException{
		SQLResult projectObjects= null;//all the projects belonging to the userName
		
		Connection con = WeaveConfig.getConnectionConfig().getAdminConnection();
		String schema = WeaveConfig.getConnectionConfig().getDatabaseConfigInfo().schema;
		
		List<String> selectColumns = new ArrayList<String>();
		selectColumns.add("projectName");//we're retrieving the list of projects in the projectName column in database
		
//		Map<String,String> whereParams = new HashMap<String, String>();
//		whereParams.put("userName", userName);
//		Set<String> caseSensitiveFields  = new HashSet<String>(); 
//		queryObjects= SQLUtils.getResultFromQuery(con, query, params, false); OR
//		projectObjects = SQLUtils.getResultFromQuery(con,selectColumns, "data", "stored_query_objects", whereParams, caseSensitiveFields);
		
		
		String query = String.format("SELECT distinct(%s) FROM %s", "projectName", (SQLUtils.quoteSchemaTable(con,schema, "stored_query_objects")));
		projectObjects = SQLUtils.getResultFromQuery(con,query, null, true );
		
		String[] projectNames = new String[projectObjects.rows.length];
		for(int i = 0; i < projectObjects.rows.length; i++){
			Object project = projectObjects.rows[i][0];//TODO find better way to do this
			projectNames[i] = project.toString();

		}
		
		con.close();
		
		return projectNames;
	}
	
		/*
 	    * @param userName author of a given Project
	    * @param projectName project which contains the requested query
	    * @param queryObjectName the filename that contains the requested queryObject
	    * @return the requested single queryObject 
	    * @throws Exception
	    */
	public SQLResult getSingleQueryObjectInProjectFromDatabase(String username, String projectName, String queryObjectName){
		SQLResult singleQueryObject = null;//the queryObject requested
		
		return singleQueryObject;
	};

	
   /** 
   * @param projectName project from which queryObjects have to be listed
   * @return finalQueryObjectCollection array of [jsonObjects, title of queryObjects]   
   * @throws Exception
   */
	public Object[] getQueryObjectsFromDatabase(String projectName) throws RemoteException, SQLException
	{
		Object[] finalQueryObjectCollection = new Object[3];
		
		Connection con = WeaveConfig.getConnectionConfig().getAdminConnection();
		String schema = WeaveConfig.getConnectionConfig().getDatabaseConfigInfo().schema;
		
		//we're retrieving the list of queryObjects in the selected project
		List<String> selectColumns = new ArrayList<String>();
		selectColumns.add("queryObjectTitle");
		selectColumns.add("queryObjectContent");
		selectColumns.add("projectDescription");
		
		
		Map<String,String> whereParams = new HashMap<String, String>();
		whereParams.put("projectName", projectName);
		Set<String> caseSensitiveFields  = new HashSet<String>();//empty 
		SQLResult queryObjectsSQLresult = SQLUtils.getResultFromQuery(con,selectColumns, schema, "stored_query_objects", whereParams, caseSensitiveFields);
		
		
		
		//getting names from queryObjectTitle
		String[] queryNames =  new String[queryObjectsSQLresult.rows.length];
		String projectDescription = null;
		for(int i = 0; i < queryObjectsSQLresult.rows.length; i++){
			Object singleSQLQueryObject = queryObjectsSQLresult.rows[i][0];//TODO find better way to do this
			queryNames[i] = singleSQLQueryObject.toString();
			
		}
		projectDescription = (queryObjectsSQLresult.rows[0][2]).toString();//TODO find better way to do this
		
		//getting json objects from queryObjectContent
		JSONObject[] finalQueryObjects = null;
		if(queryObjectsSQLresult.rows.length != 0)
		{
			ArrayList<JSONObject> jsonlist = new ArrayList<JSONObject>();
			JSONParser parser = new JSONParser();
			finalQueryObjects = new JSONObject[queryObjectsSQLresult.rows.length];
			
			
			for(int i = 0; i < queryObjectsSQLresult.rows.length; i++)
			{
				Object singleObject = queryObjectsSQLresult.rows[i][1];//TODO find better way to do this
				String singleObjectString = singleObject.toString();
				try{
					
					 Object parsedObject = parser.parse(singleObjectString);
					 JSONObject currentJSONObject = (JSONObject) parsedObject;
					
					 jsonlist.add(currentJSONObject);
				}
				catch (ParseException pe){
					
				}
				
			}//end of for loop
			
			finalQueryObjects = jsonlist.toArray(finalQueryObjects);
			
		}
		else{
			finalQueryObjects = null;
		}
		
		finalQueryObjectCollection[0] = finalQueryObjects;
		finalQueryObjectCollection[1] = queryNames;
		finalQueryObjectCollection[2] = projectDescription;
		con.close();
		return finalQueryObjectCollection;
		
	}
	
	public int deleteProjectFromDatabase(String projectName)throws RemoteException, SQLException
	{
//		Connection con = WeaveConfig.getConnectionConfig().getAdminConnection();
//		String schema = WeaveConfig.getConnectionConfig().getDatabaseConfigInfo().schema;
//		
//		
//		//Set<String> caseSensitiveFields  = new HashSet<String>(); 
//		Map<String,Object> whereParams = new HashMap<String, Object>();
//		whereParams.put("projectName", projectName);
//		WhereClause<Object> clause = new WhereClause<Object>(con, whereParams, null, true);
//		
//		int count = SQLUtils.deleteRows(con, schema, "stored_query_objects",clause);
//		con.close();
		return 0;//number of rows deleted
	}
	
	
	public int deleteQueryObjectFromProjectFromDatabase(String projectName, String queryObjectTitle)throws RemoteException, SQLException{
		
//		Connection con = WeaveConfig.getConnectionConfig().getAdminConnection();
//		String schema = WeaveConfig.getConnectionConfig().getDatabaseConfigInfo().schema;
//		Map<String,Object> whereParams = new HashMap<String, Object>();
//		whereParams.put("projectName", projectName);
//		whereParams.put("queryObjectTitle", queryObjectTitle);
//		WhereClause<Object> clause = new WhereClause<Object>(con, whereParams, null, true);
//		
//		int count = SQLUtils.deleteRows(con, schema, "stored_query_objects",clause);
//		con.close();
		return 0;//number of rows deleted
	}
	//adds a queryObject to the database
	public int insertQueryObjectInProjectFromDatabase(String userName, String projectName, String queryObjectTitle, String queryObjectContent) throws RemoteException, SQLException
	{
		Connection con = WeaveConfig.getConnectionConfig().getAdminConnection();
		String schema = WeaveConfig.getConnectionConfig().getDatabaseConfigInfo().schema;
		Map<String,Object> record = new HashMap<String, Object>();
		record.put("userName", userName);
		record.put("projectName", projectName);
		record.put("queryObjectTitle", queryObjectTitle);
		record.put("queryObjectContent", queryObjectContent);
		
		int count = SQLUtils.insertRow(con, schema, "stored_query_objects", record );
		con.close();
		return count;//single row added
	}
	
	public int insertMultipleQueryObjectInProjectFromDatabase(String userName, String projectName,String projectDescription, String[] queryObjectTitle, String[] queryObjectContent) throws RemoteException, SQLException
	{
		Connection con = WeaveConfig.getConnectionConfig().getAdminConnection();
		String schema = WeaveConfig.getConnectionConfig().getDatabaseConfigInfo().schema;
		List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
		
		for(int i = 0; i < queryObjectTitle.length; i++){
			Map<String,Object> record = new HashMap<String, Object>();
			record.put("userName", userName);
			record.put("projectName", projectName);
			record.put("projectDescription", projectDescription);
			record.put("queryObjectTitle", queryObjectTitle[i]);
			record.put("queryObjectContent", queryObjectContent[i]);
			records.add(record);
		}
		
		
		int count = SQLUtils.insertRows(con, schema , "stored_query_objects", records );
		con.close();
		return count;
	}
	
	
	//////////////////////////////////////////////////////
	// 
	// Main function for running either R or Stata scripts
	//
	//
	//
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
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			// R requires the data as column data
			Object[][] columnData = transpose(recordData);
			endTime = System.currentTimeMillis(); // end timer for data request
			recordData = null;
			time1 = startTime - endTime;
			
			// Run and time the script
			startTime = System.currentTimeMillis();
			resultData = runRScript(scriptName, columnData);
			endTime = System.currentTimeMillis();
			time2 = startTime - endTime;
			
		}
		else if(getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			endTime = System.currentTimeMillis(); // end timer for data request
			time1 = endTime - startTime;
			// Run and time the script
			startTime = System.currentTimeMillis();
			resultData = runStataScript(scriptName, recordData, programPath, tempDirPath, stataScriptsPath);
			endTime = System.currentTimeMillis();
			time2 = endTime - startTime;
		}
		
		ScriptResult result = new ScriptResult();
		
		result.data = resultData;
		result.times[0] = time1;
		result.times[1] = time2;
		
		return result;
	}
    
//	public int runStataScript() throws IOException {
//
//		Runtime run = Runtime.getRuntime();
//		Process proc = null;
//		proc = run.exec(new String[] { "stata-se", "-q" });
//		OutputStream stdin = proc.getOutputStream();
//		stdin.write(new String("/Users/franckamayou/Desktop/test.do")
//				.getBytes());
//		stdin.close();
//		BufferedReader stdout = new BufferedReader(new InputStreamReader(
//				proc.getInputStream()));
//		BufferedReader stderr = new BufferedReader(new InputStreamReader(
//				proc.getErrorStream()));
//
//		while (true) {
//			String line = null;
//			try {
//				// check both streams for new data
//				if (stdout.ready()) {
//					line = stdout.readLine();
//				} else if (stderr.ready()) {
//					line = stderr.readLine();
//				}
//
//				// print out data from stream
//				if (line != null) {
//					System.out.println(line);
//					continue;
//				}
//			} catch (IOException ioe) {
//				// stream error, get the return value of the process and return
//				// from this function
//				try {
//					return proc.exitValue();
//				} catch (IllegalThreadStateException itse) {
//					return -Integer.MAX_VALUE;
//				}
//			}
//			try {
//				// if process finished, return
//				return proc.exitValue();
//			} catch (IllegalThreadStateException itse) {
//				// process is still running, continue
//			}
//		}
//
//	}

	//////////////////////////////////////////
    //
    // Function for running R script
    //
    //
    
	public static Object runRScript(String scriptAbsPath, Object[][] dataSet) throws Exception
	{
		
		Object[] inputValues = {scriptAbsPath, dataSet};
		String[] inputNames = {"scriptAbsolutePath", "dataset"};

		String script = "scriptFromFile <- source(scriptAbsolutePath)\n" +
					         "scriptFromFile$value(dataset)"; 

		String[] outputNames = {};
		
		RConnection rConnection = RServiceUsingRserve.getRConnection();
		
		RResult[] results = null;
		Vector<RResult> resultVector = new Vector<RResult>();
		try
		{
			// ASSIGNS inputNames to respective Vector in R "like x<-c(1,2,3,4)"			
			RServiceUsingRserve.assignNamesToVector(rConnection,inputNames,inputValues);
			
			evaluateWithTypeChecking( rConnection, script, resultVector, false, false);
			
			for (int i = 0; i < outputNames.length; i++){// R Script to EVALUATE output Script
				String name = outputNames[i];						
				REXP evalValue = evalScript( rConnection, name, false);	
				resultVector.add(new RResult(name, RServiceUsingRserve.rexp2javaObj(evalValue)));					
			}
			// clear R objects
			evalScript( rConnection, "rm(list=ls())", false);
			
		}
		catch (Exception e)	{
			e.printStackTrace();
			System.out.println("printing error");
			System.out.println(e.getMessage());
			throw new RemoteException("Unable to run script", e);
		}
		finally
		{
			results = new RResult[resultVector.size()];
			resultVector.toArray(results);
			rConnection.close();
		}
		return results;
	}
	
	private static REXP evalScript(RConnection rConnection, String script, boolean showWarnings) throws REXPMismatchException,RserveException
	{
		
		REXP evalValue = null;
		
		if (showWarnings)			
			evalValue =  rConnection.eval("try({ options(warn=2) \n" + script + "},silent=TRUE)");
		else
			evalValue =  rConnection.eval("try({ options(warn=1) \n" + script + "},silent=TRUE)");
		
		return evalValue;
	}
	
	private static Vector<RResult> evaluateWithTypeChecking(RConnection rConnection, String script, Vector<RResult> newResultVector, boolean showIntermediateResults, boolean showWarnings ) throws ScriptException, RserveException, REXPMismatchException 
	{
		REXP evalValue= evalScript(rConnection, script, showWarnings);
		Object resultArray = RServiceUsingRserve.rexp2javaObj(evalValue);
		Object[] columns;
		if (resultArray instanceof Object[])
		{
			columns = (Object[])resultArray;
		}
		else
		{
			throw new ScriptException(String.format("Script result is not an Array as expected: \"%s\"", resultArray));
		}

		Object[][] final2DArray;//collecting the result as a two dimensional array 
		
		Vector<String> names = evalValue.asList().names;
		
	//try{
			//getting the rowCounter variable 
			int rowCounter = 0;
			/*picking up first one to determine its length, 
			all objects are different kinds of arrays that have the same length
			hence it is necessary to check the type of the array*/
			Object currentRow = columns[0];
			if(currentRow instanceof int[])
			{
				rowCounter = ((int[]) currentRow).length;
									
			}
			else if (currentRow instanceof Integer[])
			{
				rowCounter = ((Integer[]) currentRow).length;
				
			}
			else if(currentRow instanceof Double[])
			{
				rowCounter = ((Double[]) currentRow).length;
			}
			else if(currentRow instanceof double[])
			{
				rowCounter = ((double[]) currentRow).length;
			}
			else if(currentRow instanceof RFactor)
			{
				rowCounter = ((RFactor[]) currentRow).length;
			}
			else if(currentRow instanceof String[])
			{
				rowCounter = ((String[]) currentRow).length;
			}
			
			//handling single row, that is the currentColumn has only one record
			else if (currentRow instanceof Double)
			{
				rowCounter = 1;
			}
			
			else if(currentRow instanceof Integer)
			{
				rowCounter = 1;
			}
			
			else if(currentRow instanceof String)
			{
				rowCounter = 1; 
			}
			int columnHeadingsCount = 1;
			
			rowCounter = rowCounter + columnHeadingsCount;//we add an additional row for column Headings
			
			final2DArray = new Object[rowCounter][columns.length];
			
			//we need to push the first entry as column names to generate this structure
			/*[
			["k","x","y","z"]
			["k1",1,2,3]
			["k2",3,4,6]
			["k3",2,4,56]
			] */
		
			String [] namesArray = new String[names.size()];
			names.toArray(namesArray);
			final2DArray[0] = namesArray;//first entry is column names
			
			for( int j = 1; j < rowCounter; j++)
			{
				ArrayList<Object> tempList = new ArrayList<Object>();//one added for every column in 'columns'
				for(int f =0; f < columns.length; f++){
					//pick up one column
					Object currentCol = columns[f];
					//check its type
					if(currentCol instanceof int[])
					{
						//the second index in the new list should coincide with the first index of the columns from which values are being picked
						tempList.add(f, ((int[])currentCol)[j-1]);
					}
					else if (currentCol instanceof Integer[])
					{
						tempList.add(f,((Integer[])currentCol)[j-1]);
					}
					else if(currentCol instanceof double[])
					{
						tempList.add(f,((double[])currentCol)[j-1]);
					}
					else if(currentCol instanceof RFactor)
					{
						tempList.add(f,((RFactor[])currentCol)[j-1]);
					}
					else if(currentCol instanceof String[])
					{
						tempList.add(f,((String[])currentCol)[j-1]);
					}
					//handling single record
					else if(currentCol instanceof Double)
					{
						tempList.add(f, (Double)currentCol);
					}
					else if(currentCol instanceof String)
					{
						tempList.add(f, (String)currentCol);
					}
					else if(currentCol instanceof Integer)
					{
						tempList.add(f, (Integer)currentCol);
					}
					
				}
				Object[] tempArray = new Object[columns.length];
				tempList.toArray(tempArray);
				final2DArray[j] = tempArray;//after the first entry (column Names)
				
			}
			
			System.out.print(final2DArray);
			newResultVector.add(new RResult("endResult", final2DArray));
			//newResultVector.add(new RResult("timeLogString", timeLogString));
			

			return newResultVector;
			
	}
	
	////////////////////////////////////////////////
	//
	//	Functions that will run Stata Scripts
	//
	//
	//
	
	public Object runStataScript(String scriptName, Object[][] dataSet, String programPath, String tempDirPath, String scriptPath) throws Exception {

		int exitValue = -1;
		CSVParser parser = new CSVParser();
		//Gson jsonParser = new Gson();
		String tempScript = "";
		String[][] resultData;
		String[] args = null;
		File dataSetCSV = null;
		File tempScriptFile = null;
		File tempDirectory = new File(tempDirPath);
		if(!tempDirectory.exists() || !tempDirectory.isDirectory())
		{
			tempDirectory.mkdir();
		} 
		
		if(new File(tempDirectory.getAbsolutePath(), "result.csv").exists())
		{
			if(!new File(tempDirectory.getAbsolutePath(), "result.csv").delete()) {
				throw new RemoteException("Cannot delete result.csv");
			}
		}
		
		try 
		{
			dataSetCSV = new File(FilenameUtils.concat(tempDirectory.getCanonicalPath(), "data.csv"));
			BufferedWriter out = new BufferedWriter(new FileWriter(dataSetCSV));
			parser.createCSV(dataSet, true, out, true);
			//jsonParser.toJson(dataSet, out);
			out.close();
		} 

		catch( IOException e)
		{
			e.printStackTrace();
			throw new RemoteException("Error while trying to write dataset to file");
		}
		
		try 
		{
			tempScript += "insheet using " + dataSetCSV.getAbsolutePath() + ", clear" + "\n" +
					"global path=\"" + tempDirPath + "\"\n" +
					"cd \"$path/\" \n" +
					"noisily do " + new File(FilenameUtils.concat(scriptPath, scriptName)).getAbsolutePath() + "\n";
			
			tempScriptFile = new File(FilenameUtils.concat(tempDirectory.getAbsolutePath(), "tempScript.do"));
			BufferedWriter out = new BufferedWriter(new FileWriter(tempScriptFile));
			out.write(tempScript);
			out.close();
		} 
		catch( IOException e)
		{
			e.printStackTrace();
			throw new RemoteException("Error while trying to write script wrapper to file");
		}
		
		if(getOSType() == OS_TYPE.LINUX || getOSType() == OS_TYPE.OSX)
		{
			args = new String[] {programPath, "-b", "-q", "do", FilenameUtils.concat(tempDirPath, "tempScript.do")};
			
		}
		
		else if(getOSType() == OS_TYPE.WINDOWS)
		{
			args = new String[] {programPath, "/e", "/q", "do", FilenameUtils.concat(tempDirPath, "tempScript.do")};
		}
		
		else if(getOSType() == OS_TYPE.UNKNOWN)
		{
			throw new RemoteException("unsupported os type");
		}

		try {
			exitValue = CommandUtils.runCommand(args, null, tempDirectory);
			if(exitValue != 0)
			{
				throw new RemoteException("Stata terminated with exit value " + exitValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Since we cannot rely on the return value of the Stata process,
		// We will assume that the log file is erased at the end of the script.
		// Therefore if the log exists, the program did not terminate
		
		// remove extension .do extension and add .log
		File logFile = new File(FilenameUtils.removeExtension(tempScriptFile.getAbsolutePath()).concat(".log"));
		
		// for now we assume result is always in result.csv
		File scriptResult = new File(tempDirectory.getAbsolutePath(), "result.csv");
		
		if(scriptResult.exists()) {
			// parse log file for ouput only
			resultData = parser.parseCSV(scriptResult, true);
		} else {
			if(logFile.exists()) {
				String error = getErrorsFromStataLog(logFile);
				throw new RemoteException("Error while running Stata script: " + error);
			} else {
				throw new RemoteException("Script did not produce result.csv and no log file found.");
			}
		}
		return resultData;
	}
	
	/**
	 * This functions reads a stata .log log file and returns the outputs
	 * 
	 * @param filename
	 * @return the log outputs
	 */
	private String getErrorsFromStataLog(File file) throws Exception
	{
		String outputs = "";

		BufferedReader br = new BufferedReader(new FileReader(file));

		String line;
		while ((line = br.readLine()) != null) {
		   if (line.startsWith(".") || line.startsWith(">")|| line.startsWith("runn") || line.startsWith("\n")|| line.startsWith(" ")) // run skips output lines for do files
		   {
			   // this is a command input.. skip
			   continue;
		   }
		   else
		   {
			   outputs += line;
		   }
			// process the line.
		}
		br.close();

		return outputs;
	}
	
	//////////////////////////////////////////////////////////
	//
	//	Functions for managing the scripts on the server
	//
	//
	//
	public String getScript(String scriptName) throws Exception {
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return getScript(new File(rScriptsPath), scriptName);
		} else if( getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return getScript(new File(stataScriptsPath), scriptName);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
	public String[] getListOfScripts() throws Exception{
		
		File[] directories = {new File(rScriptsPath), new File(stataScriptsPath)};
		return getListOfScripts(directories);
	}
	
	public boolean saveScriptMetadata (String scriptName, JsonObject metadata) throws Exception {
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return saveScriptMetadata(new File(rScriptsPath), scriptName, metadata);
		} else if( getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return saveScriptMetadata(new File(stataScriptsPath), scriptName, metadata);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
	public Object getScriptMetadata (String scriptName) throws Exception {
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return getScriptMetadata(new File(rScriptsPath), scriptName);
		} else if( getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return getScriptMetadata(new File(stataScriptsPath), scriptName);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
	public boolean uploadNewScript(String scriptName, String content) throws Exception {
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return uploadNewScript(new File(rScriptsPath), scriptName, content);
		} else if( getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return uploadNewScript(new File(stataScriptsPath), scriptName, content);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
	public boolean uploadNewScript(String scriptName, String content, JsonObject metadata) throws Exception {
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return uploadNewScript(new File(rScriptsPath), scriptName, content, metadata);
		} else if( getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return uploadNewScript(new File(stataScriptsPath), scriptName, content, metadata);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
	public boolean deleteScript(String scriptName) throws Exception {
		
		if(getScriptType(scriptName) == SCRIPT_TYPE.R)
		{
			return deleteScript(new File(rScriptsPath), scriptName);
		} else if( getScriptType(scriptName) == SCRIPT_TYPE.STATA)
		{
			return deleteScript(new File(stataScriptsPath), scriptName);
		} else {
			throw new RemoteException("Unknown Script Type");
		}
	}
	
	/**
	  * 
	  * @param directory The location of the script
	  * @param scriptName The name of the script
	  *
	  * @return The script content as a string
	  * @throws Exception
	  */
	 private String getScript(File directory, String scriptName) throws Exception{
		
		String[] files = directory.list();
		
		String scriptContents = new String();
		
		BufferedReader bufr = null;
		
		for (int i = 0; i < files.length; i++)
		{
			if(scriptName.equalsIgnoreCase(files[i])){
				try {
					bufr = new BufferedReader(new FileReader(new File(directory, scriptName)));
					String contents = "";
					while((contents = bufr.readLine()) != null){
						scriptContents = scriptContents + contents + "\n";
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if(bufr != null){
							bufr.close();
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		return scriptContents;
	 }

	/**
	 * This function navigates all the given directories and return the list of all known scripts
	 *
	 * @param directories
	 * 
	 * @return Array of script names
	 */
	private String[] getListOfScripts(File[] directories) {

		List<String> listOfScripts = new ArrayList<String>();

		for(int i = 0; i < directories.length; i++)
		{
			File directory = directories[i];
			String[] files = directory.list();
			
			for (int j = 0; j < files.length; j++) 
			{
				if(getScriptType(files[j]) != SCRIPT_TYPE.UNKNOWN)
				{
					listOfScripts.add(files[j]);
				}
			}

		}
		return listOfScripts.toArray(new String[listOfScripts.size()]);
	}
	
	/**
	 * 
	 * This function saves the script metadata at the same location as the script.
	 * 
	 * @param directory The location of the script
	 * @param scriptName The script name
	 * @param scriptMetadata The metadata to be saved
	 * 
	 * @return Returns true if the metadata was saved.
	 * @throws Exception
	 */
	private boolean saveScriptMetadata(File directory, String scriptName, JsonObject scriptMetadata) throws Exception {
		
		// create json file name
		String jsonFileName = FilenameUtils.removeExtension(scriptName).concat(".json");
	
		File file = new File(directory, jsonFileName);
		
		if (!file.exists()){
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file.getAbsolutePath());
		BufferedWriter bw = new BufferedWriter(fw);
		Gson gson = new Gson();
		gson.toJson(scriptMetadata, bw);
		bw.close();

		return true;
	}
	
	/**
	 * 
	 * @param directory The directory where the script is located
	 * @param scriptName The script name relative
	 * 
	 * @return The script metadata as a Json object
	 * @throws Exception
	 */
	private Object getScriptMetadata(File directory, String scriptName) throws Exception {
		// this object will get the metadata from the json file
		Object scriptMetadata;
		Gson gson = new Gson();
		// we replace scriptname.R with scriptname.json
		String jsonFileName = FilenameUtils.removeExtension(scriptName).concat(".json");

		File metadataFile = new File(directory, jsonFileName);
		
		if(metadataFile.exists())
		{
			BufferedReader br = new BufferedReader(new FileReader(new File(directory, jsonFileName)));
			
			scriptMetadata = gson.fromJson(br, Object.class);
			return scriptMetadata;
		}
		else
		{
			throw new RemoteException("Could not find script metadata");
		}
	}
	

	/**
	 * 
	 * This function uploads a new script with a blank metadata file
	 * 
	 * @param directory
	 * @param scriptName
	 * @param content
	 * @return
	 * @throws Exception
	 */
	private Boolean uploadNewScript(File directory, String scriptName, String content) throws Exception
	{
		JsonObject metadata = new JsonObject();
		return uploadNewScript(directory, scriptName, content, metadata);
	}
	
	/**
	 * 
	 * This function uploads a new script with metadata
	 * 
	 * @param directory
	 * @param scriptName
	 * @param content
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	private Boolean uploadNewScript(File directory, String scriptName, String content, JsonObject metadata) throws Exception
	{
		File file = new File(directory, scriptName);
		
		try
		{
			file.createNewFile();
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.flush();
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		saveScriptMetadata(directory, scriptName, metadata);
		
		return true;
	}
	
	/**
	 * 
	 * Delete a script and its metadata
	 * 
	 * @param directory
	 * @param scriptName
	 * @return
	 * @throws RemoteException
	 */
	private Boolean deleteScript(File directory, String scriptName) throws RemoteException
	{
		File script = new File(directory, scriptName);
		File metadata = new File(directory, FilenameUtils.removeExtension(scriptName).concat(".json"));
		
		if(script.delete() && metadata.delete())
		{
			return true;
		}
		else
		{
			throw new RemoteException("Could not properly delete script and metadata");
		}
	}
	
	
	
	/////////////////////////////////////
	//
	//  Utility functions
	//
	//
	//
	
	public enum SCRIPT_TYPE
	{
		STATA, R, UNKNOWN
	}
	
	public enum OS_TYPE 
	{
		LINUX, OSX, WINDOWS, UNKNOWN
	}
	public static Object[][] transpose (Object[][] array) {
		  if (array == null || array.length == 0)//empty or unset array, nothing do to here
		    return array;

		  int width = array.length;
		  int height = array[0].length;

		  Object[][] array_new = new Object[height][width];

		  for (int x = 0; x < width; x++) {
		    for (int y = 0; y < height; y++) {
		      array_new[y][x] = array[x][y];
		    }
		  }
		  return array_new;
	}
	
	public static OS_TYPE getOSType()
	{
		String os = System.getProperty("os.name");
		
		if(os.toLowerCase().contains("windows"))
		{
			return OS_TYPE.WINDOWS;
		}
		else if (os.toLowerCase().contains("nix") || os.toLowerCase().contains("nux"))
		{
			return OS_TYPE.LINUX;
		}
		else if(os.toLowerCase().contains("mac"))
		{
			return OS_TYPE.OSX;
		}
		else
		{
			return OS_TYPE.UNKNOWN;
		}
	}
	
	public static SCRIPT_TYPE getScriptType(String scriptName)
	{
		String extension = FilenameUtils.getExtension(scriptName);

		//Use R as the computation engine
		if(extension.equalsIgnoreCase("R"))
		{
			return SCRIPT_TYPE.R;
		}
		
		//Use STATA as the computation engine
		if(extension.equalsIgnoreCase("do"))
		{
			return SCRIPT_TYPE.STATA;
		}
		else
		{
			return SCRIPT_TYPE.UNKNOWN;
		}
	}
}
