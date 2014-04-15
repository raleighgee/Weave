package com.weave.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.io.FilenameUtils;

import weave.config.WeaveContextParams;
import weave.servlets.WeaveServlet;

import com.google.gson.Gson;
import com.weave.config.AwsContextParams;
import com.weave.utils.AWSUtils;
import com.weave.utils.AWSUtils.SCRIPT_TYPE;

public class ScriptManagerService {

	public ScriptManagerService(){
		
	}
	
	 /**
	  * 
	  * Gives an object containing the script contents
	  * 
	  * @param scriptName
	  * @return
	  * @throws Exception
	  */
	 public static String getScript(File directory, String scriptName) throws Exception{
		
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
	 * This function navigates all the given directories and return
	 * All the script types
	 * @param directories
	 * @return
	 */
	public static String[] getListOfScripts(File[] directories) {

		List<String> listOfScripts = new ArrayList<String>();

		for(int i = 0; i < directories.length; i++)
		{
			File directory = directories[i];
			String[] files = directory.list();
			
			for (int j = 0; j < files.length; j++) 
			{
				if(AWSUtils.getScriptType(files[j]) != AWSUtils.SCRIPT_TYPE.UNKNOWN)
				{
					listOfScripts.add(files[j]);
				}
			}

		}
		return listOfScripts.toArray(new String[listOfScripts.size()]);
	}

	public static void saveScriptMetadata(String scriptAbsName, Gson scriptMetadata) throws Exception {
		
		// create json file name
		String jsonFileName = FilenameUtils.removeExtension(scriptAbsName).concat(".json");
	
		File file = new File(jsonFileName);
		
		if (!file.exists()){
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file.getAbsolutePath());
		BufferedWriter bw = new BufferedWriter(fw);
		Gson gson = new Gson();
		gson.toJson(scriptMetadata, bw);
		bw.close();

		return;
	}
	
	/**
	 * 
	 * @param directory The directory where the script is located
	 * @param scriptName The script name relative
	 * 
	 * @return The script metadata in Gson format
	 * @throws Exception
	 */
	public static Gson getScriptMetadata(File directory, String scriptName) throws Exception {
		
		String[] files = directory.list();

		int filecount = 0;
		// this object will get the metadata from the json file
		Object scriptMetadata = new Object();
		
		// we replace scriptname.R with scriptname.json
		String jsonFileName = scriptName.substring(0, scriptName.lastIndexOf('.')).concat(".json");

		// we will check if there is a json file with the same name in the directory.
		for (int i = 0; i < files.length; i++)
		{
			if (jsonFileName.equalsIgnoreCase(files[i]))
			{
				filecount++;
				// do the work
				Gson gson = new Gson();
				
				if(filecount > 1) {
					throw new RemoteException("multiple copies of " + jsonFileName + "found!");
				}
				
				try {
					
					BufferedReader br = new BufferedReader(new FileReader(new File(directory, jsonFileName)));
					
					scriptMetadata = gson.fromJson(br, Object.class);
					
					//System.out.println(scriptMetadata);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// 
		if(filecount == 0) {
			throw new RemoteException("Could not find the file " + jsonFileName + "!");
		}
		
		return scriptMetadata;
	}
	
	public static Boolean uploadNewScript(Map<String, Object> params){
		String scriptName = params.get("scriptName").toString();
		Object fileObject = params.get("fileObject");
		File file = new File(awsConfigPath + "RScripts", scriptName);
		if (!file.exists()){
			try{
				file.createNewFile();
				FileWriter fw = new FileWriter(file.getAbsolutePath());
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write( (String) fileObject);
				bw.flush();

				bw.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		String jsonFileName = scriptName.substring(0, scriptName.lastIndexOf('.')).concat(".json");
		file = new File(awsConfigPath + "RScripts", jsonFileName);
		if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		return true;
	}
	
	public Boolean deleteScript(String scriptName, String password) throws RemoteException
	{
//		if(authenticate()){
//			File file = new File(awsConfigPath + "RScripts", scriptName);
//			file.delete();
//		}else{
//			throw new RemoteException("Authentication Failure");
//		}
		return false;
	}
}
