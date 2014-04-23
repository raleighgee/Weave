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
package weave.tests;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import weave.beans.RResult;
import weave.servlets.AWSRService;
import weave.servlets.RService;

public class test
{

	static RService ws = null;
	static AWSRService aws= null;
	
	public static Map<String, String> connectionObject = new HashMap<String, String>();
	public static Map<String, Object> requestObject = new HashMap<String, Object>();
		
	public static void call(String[] keys, String[]inputNames, Object[]inputValues, String[]resultNames, String script, String plotScript, boolean showIntermediateResults, boolean showWarnings, boolean useColumnAsList)
	throws Exception
	{
		Connection con = null;

	    for(int i = 0; i < 4; i++)
	    {
			
				RResult[] scriptResult = null;
				
				Object [] allResults = new Object[3];
				
				System.out.println(System.getProperty("user.dir"));
			try {
				
				scriptResult = ws.runScript(null, inputNames, inputValues, resultNames, script, plotScript, showIntermediateResults, showWarnings, useColumnAsList);
				
				} 
//				catch (RemoteException e) {
//					e.printStackTrace();
//				}
				finally
				{
					System.out.println(Arrays.asList(scriptResult));
				}
				allResults[i] = scriptResult;
	    }
	}
	
	

	public static void main(String[] args) throws Exception
	{
		
//		HashMap<String, Object> algorithmObject = new HashMap<String, Object>();
//		algorithmObject.put("startClusterNumber", 4);
//		algorithmObject.put("stopClusterNumber", 10);
//		algorithmObject.put("ClusterInterval", 2);
//		algorithmObject.put("startIterationsNumber", 1000);
//		algorithmObject.put("stopIterationsNumber", 10000);
//		algorithmObject.put("iterationsInterval", 500);
//		
		
		
		ArrayList<String> columns = new ArrayList<String>();
		columns.add("X_STATE");
		columns.add("X_PSU");
		columns.add("X_FINALWT");
		columns.add("X_STSTR");
		columns.add("DIABETE2");
		
//		columns.add("PercentObese2002");
//		columns.add("PercentObese2003");
//		columns.add("PercentObese2004");
//		columns.add("PercentObese2005");
//		columns.add("PercentObese2006");
		
		connectionObject.put("connectionType", "RODBC");
		connectionObject.put("user", "root");
		connectionObject.put("password", "shweta");
		connectionObject.put("schema", "data");
		connectionObject.put("host", "localhost");
		connectionObject.put("dsn", "myCDC");
		
		requestObject.put("scriptName", "TestingAWS_RODBC.R");
		requestObject.put("scriptPath", "C:\\RScripts\\");
		requestObject.put("columnsToBeRetrieved",columns);
		requestObject.put("dataset", "sdoh2010q");
		
		//Object[] array1 = {10,10,20,30,22,50,60,55,89,33,44,54,21};
		//Object[] array2 = {10,20,44,52,34,87,45,65,76,87,23,12,34};
		//Object[] myMatrix = new Object[]{array1, array2};
		//String scriptPath = "C:\\RScripts\\TestingAWS_RODBC.R";
		//String [] inputNames = {"cannedScriptPath", "params"};
		//Object[] inputValues = new Object[0];
		//Object[] inputValues = {scriptPath, columns };
		
		//String script = "scriptFromFile <- source(cannedScriptPath)\n" +
		 //"returnedColumns <- scriptFromFile$value(dataMatrix)\n";
		//String[] resultNames = {};
		
		
		
		//call(null, inputNames, inputValues, resultNames, script, "", false, false, false);
		
		
		
		//Object[] inputValues1 = {};	
		//Object[] inputValues = {};
		//String plotscript = "";
		//Object[] parameters = {};
		//String [] resultNames = {};	
		//String scriptFilePath = "C:\\RScripts\\TestingAWS_RODBC.R";
		//String csvPath = "C:\\Users\\Shweta\\Desktop\\SDoH2010Q.csv";
//		String user = "root";
//		String password = "shweta";
//		String hostName = "localhost";
//		String schemaName = "data";
//		String dsn = "myCDC";
	//	String [] columns = {"X_STATE", "X_PSU", "X_STSTR", "X_FINALWT", "DIABETE2"};
		//String query = "select `X_STATE`,`X_PSU`,`X_STSTR`,`X_FINALWT`,DIABETE2 from sdoh2010q";
		
//		 Object[] inputValues = {scriptFilePath, query, columns, user, password, hostName, schemaName, dsn};
//		 String[] inputNames = {"cannedScriptPath", "query", "params", "myuser", "mypassword", "myhostName", "myschemaName", "mydsn"};
//		
//		String script =  "scriptFromFile <- source(cannedScriptPath)\n" +
//		  "library(RMySQL)\n" +
//		  "con <- dbConnect(dbDriver(\"MySQL\"), user = myuser , password = mypassword, host = myhostName, port = 3306, dbname =myschemaName)\n" +
//		  "library(survey)\n" +
//		  "getColumns <- function(query)\n" +
//		  "{\n" +
//		  "return(dbGetQuery(con, paste(query)))\n" +
//		  "}\n" +
//		  "returnedColumnsFromSQL <- scriptFromFile$value(query, params)\n";
		
			//String[] inputNames  = {"cannedScriptPath","query", "mystate", "mypsu","myststr","myfinalwt", "myindicator"};
		//Object[] inputValues = {checkstrings};
//		Object[] array1 = {0,10,20,30,22,50,60,55,89,33,44,54,21};
//		Object[] array2 = {10,20,44,52,34,87,45,65,76,87,23,12,34};
//		Object[] array3 = {10,20,44,52,34,87,45,65,76,87,23,12,34};
		//String []inputValues1 = {scriptFilePath,query,mystate, mypsu,myststr, myfinalwt,myindicator};
		//inputNames = new String[]{"scriptPath", "csvPath"};
		//works
//		script = "scriptFromFile <- source(scriptPath)\n" +
//				"answer <- scriptFromFile$value(col1, col2)\n";
		
		//testing
//		script1 ="params <- c(mystate, mypsu, myststr, myfinalwt, myindicator)\n" +
//		"scriptFromFile <- source(cannedScriptPath)\n" +
//		"library(RMySQL)\n" +
//		"con <- dbConnect(dbDriver(\"MySQL\"), user = \"root\", password = \"shweta\", host = \"localhost\", port = 3306, dbname = \"data\")\n" +
//		"library(survey)\n" +
//		"returnedColumnsFromSQL <- scriptFromFile$value(query,params)\n";
		
		
		
		//inputValues1 = new Object[0];
		//inputNames = new String[]{"myMatrix"};
		//Object[] array4 = {"ji", "hello"};
		//Object[] array5 = {0,1,2,3,4};
		//inputNames = new String []{"inputColumns","EMModel"};
		//Object[][] myMatrix = new Object[][]{array1, array2, array3};
		//String[] EMModel = {"VII"};
		//inputValues1 = new Object[]{myMatrix};
		//script = "y <- max(x)";
//		script = " frame <- data.frame(myMatrix)\n" +
//				"normandBin <- function(frame)\n" +  
//                               "structure(list(counts = getCounts(frame), breaks = getBreaks(frame)), class = \"normandBin\");\n"+
//				"getNorm <- function(frame){\n" +				
//			         "myRows <- nrow(frame)\n" +
//					 "myColumns <- ncol(frame)\n" +
//					 "for (z in 1:myColumns ){\n" +
//			         "maxr <- max(frame[z])\n" +
//			         "minr <- min(frame[z])\n" +
//			         "for(i in 1:myRows ){\n" +
//			         "frame[i,z] <- (frame[i,z] - minr) / (maxr - minr)\n" +
//			         " }\n" +
//			         "}\n" +
//					 "return(frame)\n" +
//					 "}\n" +
//		 "getCounts <- function(normFrame){\n" +
//					   "normFrame <- getNorm(frame)\n" +
//						"c <- ncol(normFrame)\n" +
//						"histoInfo <- list()\n" +
//						"answerCounts <- list()\n" +
//						"for( s in 1:c){\n" + 
//					    "histoInfo[[s]] <- hist(normFrame[[s]], plot = FALSE)\n" + 
//						"answerCounts[[s]] <- histoInfo[[s]]$counts\n" +
//						"}\n" +
//						"return(answerCounts)\n" +
//						"}\n" +
//		"getBreaks <- function(frame){\n" +
//		              "normFrame <- getNorm(frame)\n" +
//					  " c <- ncol(normFrame)\n" +
//					  "histoInfo <- list()\n" +
//					  "answerBreaks <- list()\n" +
//					  "for( i in 1:c){\n" +
//					  "histoInfo[[i]] <- hist(normFrame[[i]], plot = FALSE)\n" +
//					  "answerBreaks[[i]] <- histoInfo[[i]]$breaks\n" +
//					  "}\n" +
//					  "return(answerBreaks)\n" +
//					  "}\n" +
//					
//		"finalResult <- normandBin(frame)\n";
//		
		
//		script = "library(mclust)\n" +
//		"hello1 <- function(inputColumns, EMModel){\n" +
//		"Clusters = list()\n" + 
//		"for(i in 1: length(EMModel)){\n" +
//		"BIC <- NA\n" +
//		"try(BIC <- Mclust(inputColumns, 7, EMModel[i]))\n" +
//		"Results <- NA\n" +
//		"try( Results <- summary( BIC, inputColumns ) )\n" +
//		"Clusters [[i]]<- Results$classification\n" + 
//		"return(Clusters)}}\n" +
//		"ans <- hello1(inputColumns, EMModel)\n";
		
			
//		
	}	
}		
		
		
		
		
		
		
		//String[] keys = {"a","b","c","d","e","f"};
//		//Object[] array3 = {"aa","bb","cc","dd","ee","ff"};
//		Number number_of_clusters = 3;
//		Number number_of_Iterations = 10;
//		Number fnumber_of_clusters = 3;
//		Number fnumber_of_Iterations = 500;
//		Number randomsets = 1;
//		String algorithm = "Forgy";
//		String dmetric = "manhattan";
		

		//String docrootPath  = "";
		//inputNames = new String []{"inputColumns", "number_of_clusters", "number_of_Iterations", "randomsets", "algorithm"};//for operations on matrices
		
		//inputNames = new String []{"inputColumns", "knumber_of_clusters","knumber_of_Iterations","randomsets","algorithm","fnumberOfClusters","dmetric","fnumberOfIterations"};//for operations on matrices
		//inputNames =  new String []{"myVector"};//for operations on vectors
	//inputValues = new Object[][]{array1,array2};
	//inputValues1 = new Object[]{ array3 };
	//inputValues1 = new Object[]{myMatrix,number_of_clusters,number_of_Iterations,randomsets,algorithm,fnumber_of_clusters,dmetric,fnumber_of_Iterations};

		
//	script = "frame <- data.frame(inputColumns)\n" +
//	"kMeansResult <- kmeans(frame,knumber_of_clusters,knumber_of_Iterations, nstart = randomsets, algorithm)\n" +
//	"library(cluster)\n" +
//	"fuzzkMeansResult <- fanny(frame,fnumberOfClusters, metric = dmetric, maxit = fnumberOfIterations)\n"; 


	//script = "answer <- cor(mymatrix, use = \"everything\", method = \"pearson\")";
		//resultNames =  new String []{"answer"};//test for matrix
	//resultNames = new String[]{"2"};
	//resultNames = new String[]{"pcaResult$loadings", "pcaResult$sd^2"};
	//plotscript = "plot(x <- c(1,2,3,4))";
	
//	plotscript = "for(i in 1:3)\n" +
//			"{" +
//			"hist(myVector)\n" +
//			"}";
//	plotscript =  "for(i in 1:l)\n" +
//	"{\n" +
//	"hist(myVector, xlab = shweta)\n" +
//	"}\n";
	//script = "s <- data.frame(Vec1)";
		//script = "Vec1 <- as.matrix(Vec1)\n";
		//resultNames = new String[]{"CluResult$cluster", "CluResult$centers", "CluResult$size", "CluResult$withinss"};
	//script = "d <- is.vector(mymatrix)";
//	script = "frame <- data.frame(mymatrix)\n" +
//	"pcaResult <- princomp(frame)\n";
		
		
//		@SuppressWarnings("unused")
//		public static void main(String[] args) throws Exception
//		{
//			
//			for (String fileName : Arrays.asList(
//					"/home/pkovac/dload/cfg/sqlconfig.xml",
//					"d:/tomcat/webapps/weave-config/sqlconfig.xml",
//					"c:/tomcat/webapps/weave-config/sqlconfig.xml"))
//			{
//				File file = new File(fileName);
//				if (file.exists())
//				{
//					ConnectionConfig connConfig = new ConnectionConfig(file);
//					if (connConfig.migrationPending())
//					{
//						ProgressPrinter pp = new ProgressPrinter(System.out);
//						DataConfig dataConfig = connConfig.initializeNewDataConfig(pp.getProgressManager());
//					}
//					else
//					{
//						DataConfig dataConfig = new DataConfig(connConfig);
////						dataConfig.buildHierarchy(135616, 135616, 1);
//					}
//					break;
//				}
//			}
		
//			ConnectionInfo connInfo = new ConnectionInfo();
//			connInfo.connectString = "hello";
//			connInfo.name = "myname";
//			connInfo.dbms = "what";
//			connInfo.pass = "PPPASS";
//			connConfig.addConnectionInfo(connInfo);
//			
//			DatabaseConfigInfo dbInfo = new DatabaseConfigInfo();
//			dbInfo.connection = "myname";
//			dbInfo.schema = "weave";
//			connConfig.setDatabaseConfigInfo(dbInfo);
			
//			if (true)
//				return;s
	//}
			
//			Connection conn = SQLUtils.getConnection(SQLUtils.getDriver(SQLUtils.MYSQL), "jdbc:mysql://localhost/weave?user=root&password=boolpup");
//			SQLConfig sqlcfg = new SQLConfig(new SQLConfigXML("C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 6.0\\webapps\\weave-config\\sqlconfig.xml"));
//			SQLConfig sqlcfg = new SQLConfig(new SQLConfigXML("sqlconfig.xml"));
//			System.out.println(sqlcfg.addEntry("Hello", null));
			// TODO Auto-generated method stub

