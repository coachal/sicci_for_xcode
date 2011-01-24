package com.sic.bb.hudson.plugins.xcodeplugin.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import com.sic.bb.hudson.plugins.xcodeplugin.ocunit.OCUnitTestSuite;

import hudson.console.LineTransformationOutputStream;

public class ParsedOutputStream extends LineTransformationOutputStream {
	private final OCUnitOutputParser ocUnitParser;
	private final OutputStream logger;
	
	public ParsedOutputStream(OutputStream logger) {
		this.ocUnitParser = new OCUnitOutputParser();
		this.logger = logger;
	}
	
	public Vector<OCUnitTestSuite> getParsedTests() {
		return this.ocUnitParser.getParsedTests();
	}
	
	protected void eol(byte[] bytes, int len) throws IOException {
	    String line = new String(bytes, 0, len);
	    this.ocUnitParser.parse(line);
	    this.logger.write(line.getBytes());
	}

}
