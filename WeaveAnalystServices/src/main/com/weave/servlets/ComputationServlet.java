package com.weave.servlets;

import static weave.config.WeaveConfig.initWeaveConfig;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import weave.config.WeaveContextParams;
import weave.servlets.WeaveServlet;
import weave.utils.SQLUtils.WhereClause.NestedColumnFilters;

import com.weave.config.AwsContextParams;
import com.weave.models.computations.ComputationEngineBroker;
import com.weave.models.computations.ScriptResult;

public class ComputationServlet extends WeaveServlet
{	
	ComputationServlet()
	{
	}
	private String programPath = "";
	private String tempDirPath = "";
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		initWeaveConfig(WeaveContextParams.getInstance(config.getServletContext()));
		programPath = WeaveContextParams.getInstance(config.getServletContext()).getRServePath();
		tempDirPath = AwsContextParams.getInstance(config.getServletContext()).getAwsConfigPath() + "temp";
	}

	private static final long serialVersionUID = 1L;

	
	public ScriptResult runScript(String scriptName, int[] ids, NestedColumnFilters filters) throws Exception
	{
		
		ScriptResult result = new ScriptResult();

		ComputationEngineBroker broker = new ComputationEngineBroker();
		result = broker.decideComputationEngine(scriptName, ids, filters, programPath, tempDirPath);

		return result;
	}
}