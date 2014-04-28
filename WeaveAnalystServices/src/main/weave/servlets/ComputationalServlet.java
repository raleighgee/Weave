package weave.servlets;

import static weave.config.WeaveConfig.initWeaveConfig;

import java.rmi.RemoteException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.io.FilenameUtils;

import weave.beans.RResult;
import weave.config.AwsContextParams;
import weave.config.WeaveContextParams;
import weave.models.computations.AwsRService;
import weave.models.computations.AwsStataService;
import weave.models.computations.ScriptResult;
import weave.utils.AWSUtils;
import weave.utils.AWSUtils.SCRIPT_TYPE;
import weave.utils.SQLUtils.WhereClause.NestedColumnFilters;

public class ComputationalServlet extends WeaveServlet
{	
	public ComputationalServlet()
	{
	}
	
	private static String awsConfigPath = "";
	private String programPath = "";
	private String tempDirPath = "";
	private String stataScriptsPath = "";
	private String rScriptsPath = "";
	
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		initWeaveConfig(WeaveContextParams.getInstance(config.getServletContext()));
		programPath = AwsContextParams.getInstance(config.getServletContext()).getStataPath();
		tempDirPath = AwsContextParams.getInstance(config.getServletContext()).getAwsConfigPath() + "temp";
		awsConfigPath = AwsContextParams.getInstance(config.getServletContext()).getAwsConfigPath();
		stataScriptsPath = AwsContextParams.getInstance(config.getServletContext()).getStataScriptsPath();
		rScriptsPath = AwsContextParams.getInstance(config.getServletContext()).getRScriptsPath();
	}

	private static final long serialVersionUID = 1L;

	
	public ScriptResult runScript(String scriptName, int[] ids, NestedColumnFilters filters) throws Exception
	{
		
		//String scriptPath = "";//"C:\\Tomcat\\webapps\\aws-config\\RScripts\\AWS_ParallelVersion2.R";
		//File newpath = new File(awsConfigPath);
		//String parentOfnewPath = newpath.getParent();
		//scriptPath = parentOfnewPath + File.separator + "RScripts" + File.separator + scriptName;
		
		Object resultData = null;
		ScriptResult result = new ScriptResult();

		long startTime = 0; 
		long endTime = 0;
		long time1 = 0;
		long time2 = 0;
		
		// Start the timer for the data request
		startTime = System.currentTimeMillis();
		Object[][] recordData;
			recordData = DataService.getFilteredRows(ids, filters, null).recordData;
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
		try {
			resultData = AwsRService.runScript(FilenameUtils.concat(rScriptsPath, scriptName), columnData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		endTime = System.currentTimeMillis();
		time2 = startTime - endTime;

	}
	else if(AWSUtils.getScriptType(scriptName) == SCRIPT_TYPE.STATA)
	{
		endTime = System.currentTimeMillis(); // end timer for data request
		time1 = endTime - startTime;
		// Run and time the script
		startTime = System.currentTimeMillis();
		resultData = AwsStataService.runScript(scriptName, recordData, programPath, tempDirPath, stataScriptsPath);
		endTime = System.currentTimeMillis();
		time2 = endTime - startTime;
		
	}

	result.data = (RResult[]) resultData;
	result.times[0] = time1;
	result.times[1] = time2;
	
	return result;
	}
}