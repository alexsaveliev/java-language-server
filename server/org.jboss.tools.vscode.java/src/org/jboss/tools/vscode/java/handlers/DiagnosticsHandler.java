package org.jboss.tools.vscode.java.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.jboss.tools.vscode.ipc.JsonRpcConnection;
import org.jboss.tools.vscode.java.JavaLanguageServerPlugin;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;

public class DiagnosticsHandler implements IProblemRequestor {
	
	private final List<IProblem> problems;
	private final IResource resource;
	private final JsonRpcConnection connection;
	
	public DiagnosticsHandler(JsonRpcConnection conn, IResource resource) {
		problems = new ArrayList<IProblem>();
		this.resource = resource;
		this.connection = conn;
	}

	@Override
	public void acceptProblem(IProblem problem) {
		JavaLanguageServerPlugin.logInfo("accept problem for "+ this.resource.getName());
		problems.add(problem);
	}

	@Override
	public void beginReporting() {
		JavaLanguageServerPlugin.logInfo("begin problem for "+ this.resource.getName());
		problems.clear();

	}

	@Override
	public void endReporting() {
		JavaLanguageServerPlugin.logInfo("end reporting for "+ this.resource.getName());
		JSONRPC2Notification notification = new JSONRPC2Notification("textDocument/publishDiagnostics");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("uri","file://" + this.resource.getLocationURI().getPath());
		params.put("diagnostics",toDiagnosticsArray());
		notification.setNamedParams(params);
		this.connection.sendNotification(notification);
	}

	private List<?> toDiagnosticsArray() {
		List<Map<String,Object>> array = new ArrayList<Map<String,Object>>();
		for (IProblem problem : problems) {
			Map<String, Object> diag = new HashMap<String,Object>();
			diag.put("range", convertRange(problem));
			diag.put("severity", convertSeverity(problem));
			diag.put("code", problem.getID());
			diag.put("source","Java");
			diag.put("message",problem.getMessage());
			array.add(diag);
		}
		return array;
	}

	private Integer convertSeverity(IProblem problem) {
		if(problem.isError())
			return new Integer(1);
		if(problem.isWarning())
			return new Integer(2);
		return new Integer(3);
	}

	private Map<String, Object> convertRange(IProblem problem) {
		Map<String, Object> range = new HashMap<String, Object>();
		Map<String, Object> start = new HashMap<String, Object>();
		Map<String, Object> end = new HashMap<String, Object>();
		start.put("line",problem.getSourceLineNumber()-1);// VSCode is 0-based
		if(problem instanceof DefaultProblem){
			start.put("character", ((DefaultProblem)problem).getSourceColumnNumber());
		}
		end.put("line",problem.getSourceLineNumber()-1);
		if(problem instanceof DefaultProblem){
			end.put("character", ((DefaultProblem)problem).getSourceColumnNumber());
		}
		range.put("start",start);
		range.put("end",end);
		return range;
	}

	@Override
	public boolean isActive() {
		return true;
	}

}
