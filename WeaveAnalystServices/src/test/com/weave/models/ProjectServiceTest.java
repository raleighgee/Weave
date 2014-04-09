package com.weave.models;

import java.rmi.RemoteException;
import java.sql.SQLException;

import junit.framework.TestCase;
import com.weave.models.ProjectService;

public class ProjectServiceTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		
	}

	public void testProjectService() {
		fail("Not yet implemented");
	}

	public void testGetProjectFromDatabase() {
		fail("Not yet implemented");
		
	}

	public void testGetQueryObjectsFromDatabase() {
		fail("Not yet implemented");
		
		
	}

	public void testDeleteProjectFromDatabase() {
		
		 int expected = 1;
		 ProjectService proj_serv_object = new ProjectService();
		 
		
		
		try {
		
			assertEquals(expected, proj_serv_object.deleteProjectFromDatabase("hello"));
		
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

	public void testDeleteQueryObjectFromProjectFromDatabase() {
		fail("Not yet implemented");
	}

	public void testInsertQueryObjectInProjectFromDatabase() {
		fail("Not yet implemented");
	}

	public void testInsertMultipleQueryObjectInProjectFromDatabase() {
		fail("Not yet implemented");
	}

}
