package com.weave.utils;

public class AWSUtils {

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
	
}
