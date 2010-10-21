package org.codehaus.mojo.rspec;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;

/**
 * Just holds mojo properties to make Mojo real code cleaner
 * @author andre
 *
 */
public abstract class AbstractRspecMojo extends AbstractMojo{
	/**
	 * The classpath elements of the project being tested.
	 * 
	 * @parameter expression="${project.testClasspathElements}"
	 * @required
	 * @readonly
	 */
	protected List<String> classpathElements;

	/**
	 * The directory containing the RSpec source files
	 * 
	 * @parameter expression="${basedir}/spec"
	 * @required
	 */
	protected String sourceDirectory;

	/**
	 * The directory where the RSpec report will be written to
	 * 
	 * @parameter expression="${basedir}/target"
	 * @required
	 */
	protected String outputDirectory;

	/**
	 * The name of the RSpec report (optional, defaults to "rspec_report.html")
	 * 
	 * @parameter expression="rspec_report.html"
	 */
	protected String reportName;

	/**
	 * The directory where JRuby is installed (optional, defaults to
	 * "${user.home}/.jruby")
	 * 
	 * @parameter expression="${env.JRUBY_HOME}"
	 */
	protected String jrubyHome;

	/**
	 * The flag to ignore failures (optional, defaults to "false")
	 * 
	 * @parameter expression="false"
	 */
	protected boolean ignoreFailure;

	/**
	 * The flag to skip tests (optional, defaults to "false")
	 * 
	 * @parameter expression="false"
	 */
	protected boolean skipTests;
}
