package weave.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.io.FilenameUtils;

public class AwsContextParams
{
	private static String awsConfigPath = "";
	private static String StataPath = "";
	private static String rScriptsPath = "";
	private static String stataScriptsPath = "";
	
	public static AwsContextParams getInstance(ServletContext context)throws ServletException{
		if (_instance == null)
			_instance = new AwsContextParams(context);
		return _instance;
		
	}
	
	private static AwsContextParams _instance ;
	
	private AwsContextParams(ServletContext  context) throws ServletException
	{
		awsConfigPath = context.getRealPath(context.getInitParameter("awsconfigPath")).replace('\\','/');
		StataPath = context.getRealPath(context.getInitParameter("StataPath")).replace('\\','/');
		rScriptsPath= FilenameUtils.concat(awsConfigPath, "RScripts");
		stataScriptsPath = FilenameUtils.concat(awsConfigPath, "StataScripts");
	}
	
	
	/**
	 * @return The path where aws config files are stored, ending in "/"
	 */
	public String getAwsConfigPath(){

		return awsConfigPath;
	}

	/**
	 * @return The path where aws config files are stored, ending in "/"
	 */
	public String getStataPath(){

		return StataPath;
	}
	
	/**
	 * @return The path where R scripts are uploaded ending in "/"
	 */
	public String getRScriptsPath() {
		return rScriptsPath;
	}

	/**
	 * @return The path where uploaded files are stored, ending in "/"
	 */
	public String getStataScriptsPath() {
		return stataScriptsPath;
	}
	
}
