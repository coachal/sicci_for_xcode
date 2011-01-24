package com.sic.bb.hudson.plugins.xcodeplugin.cli;


import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.RETURN_OK;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Vector;

import com.sic.bb.hudson.plugins.xcodeplugin.callables.OCUnitToJUnitWriterCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.io.ParsedOutputStream;
import com.sic.bb.hudson.plugins.xcodeplugin.ocunit.OCUnitTestSuite;

public class XcodebuildCommandCaller {
	public static final String XCODEBUILD_COMMAND = "/usr/bin/xcodebuild";

	private static XcodebuildCommandCaller instance;

	private String xcodebuildOutputTemp;
	private String workspaceTemp;

	public static XcodebuildCommandCaller getInstance() {
		if (instance == null)
			instance = new XcodebuildCommandCaller();

		return instance;
	}

	public void setWorkspaceTemp(String workspace) {
		if (this.workspaceTemp != null
				&& this.workspaceTemp.contains(workspace))
			this.workspaceTemp = null;
	}
	
	public boolean check(VirtualChannel channel, TaskListener listener) {
		try {
			if (new FilePath(channel, XCODEBUILD_COMMAND).exists())
				return true;
		} catch (Exception e) {
		}

		listener.fatalError(XCODEBUILD_COMMAND + ": "
				+ Messages.XcodebuildCommandCaller_check_commandNotFound());
		return false;
	}

	public boolean build(Launcher launcher, EnvVars envVars, TaskListener listener, FilePath workspace, boolean isUnitTest, List<String> args) {
		args.add("build");
		
		try {			
			if(!isUnitTest)
				return call(launcher, envVars, listener, workspace, args);
			
			ParsedOutputStream outputStream = new ParsedOutputStream(listener.getLogger());
			listener = new StreamTaskListener(outputStream);
			launcher = workspace.createLauncher(listener);
			
			boolean rcode = call(launcher, envVars, listener, workspace, args);
			
			Vector<OCUnitTestSuite> testSuites = outputStream.getParsedTests();
			
			if(testSuites.size() == 0)
				return rcode;
			
			FilePath testReports = workspace.child("test-reports");
			
			if(!testReports.exists())
				testReports.mkdirs();
			
			return testReports.act(new OCUnitToJUnitWriterCallable(testSuites));
		} catch (Exception e) {
		}

		return false;
	}

	public boolean clean(Launcher launcher, EnvVars envVars, TaskListener listener, FilePath workspace, List<String> args) {
		args.add("clean");
		
		try {
			return call(launcher, envVars, listener, workspace, args);
		} catch (Exception e) {
		}

		return false;
	}

	public String getOutput(FilePath workspace, String arg) {
		// TODO workspace.toString() will be called (deprecated)
		if (this.workspaceTemp != null
				&& this.workspaceTemp.equals(workspace + arg))
			return this.xcodebuildOutputTemp;
		else
			this.workspaceTemp = workspace + arg;

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();

		try {
			Launcher launcher = workspace
					.createLauncher(new StreamTaskListener(stdout));

			launcher.launch().stdout(stdout).pwd(workspace)
					.cmds(XCODEBUILD_COMMAND, arg).join();

		} catch (Exception e) {
		}

		this.xcodebuildOutputTemp = stdout.toString();

		return this.xcodebuildOutputTemp;
	}

	private boolean call(Launcher launcher, EnvVars envVars,
			TaskListener listener, FilePath workspace, List<String> args) {
		args.add(0, XCODEBUILD_COMMAND);

		try {
			int rcode = launcher.launch().envs(envVars).stdout(listener)
					.pwd(workspace).cmds(args).join();

			if (rcode == RETURN_OK)
				return true;
		} catch (Exception e) {
		}

		return false;
	}
}
