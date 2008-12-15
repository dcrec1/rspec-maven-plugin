package org.codehaus.mojo.rspec;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;

/**
 * Mojo to run Ruby Spec test
 * 
 * @author Michael Ward
 * @author Mauro Talevi
 * @author Diego Carrion
 * @goal spec
 */
public class RspecRunnerMojo extends AbstractMojo {

	/**
	 * The classpath elements of the project being tested.
	 * 
	 * @parameter expression="${project.testClasspathElements}"
	 * @required
	 * @readonly
	 */
	private List<String> classpathElements;

	/**
	 * The directory containing the RSpec source files
	 * 
	 * @parameter expression="${basedir}/spec"
	 * @required
	 */
	private String sourceDirectory;

	/**
	 * The directory where the RSpec report will be written to
	 * 
	 * @parameter expression="${basedir}/target"
	 * @required
	 */
	private String outputDirectory;

	/**
	 * The name of the RSpec report (optional, defaults to "rspec_report.html")
	 * 
	 * @parameter expression="rspec_report.html"
	 */
	private String reportName;

	/**
	 * The directory where JRuby is installed (optional, defaults to
	 * "${user.home}/.jruby")
	 * 
	 * @parameter expression="${env.JRUBY_HOME}"
	 */
	private String jrubyHome;

	/**
	 * The flag to ignore failures (optional, defaults to "false")
	 * 
	 * @parameter expression="false"
	 */
	private boolean ignoreFailure;

	/**
	 * The flag to skip tests (optional, defaults to "false")
	 * 
	 * @parameter expression="false"
	 */
	private boolean skipTests;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skipTests) {
			getLog().info("Skipping RSpec tests");
			return;
		}
		getLog().info("Running RSpec tests from " + sourceDirectory);

		if (jrubyHome == null) {
			throw new MojoExecutionException(
					"$JRUBY_HOME or jrubyHome directory not specified");
		}
		Ruby runtime = Ruby.newInstance();
		getLog().info("JRuby Home: " + jrubyHome);
		runtime.setJRubyHome(jrubyHome);
		runtime.getLoadService().init(classpathElements);

		// Build ruby script to run RSpec's
		StringBuilder script = new StringBuilder();
		try {
			script.append(handleClasspathElements(runtime));
		} catch (MalformedURLException e) {
			throw new MojoExecutionException(e.getMessage());
		}

		// Run all specs
		String reportPath = outputDirectory + "/" + reportName;
		script
				.append("require 'rubygems'\n")
				.append("require 'spec'\n")
				.append("spec_dir = '")
				.append(sourceDirectory)
				.append("'\n")
				.append("@report_file = '")
				.append(reportPath)
				.append("'\n")
				.append(
						"options = ::Spec::Runner::OptionParser.parse([spec_dir, '-f', \"html#{@report_file}\"], STDERR, STDOUT)\n")
				.append("::Spec::Runner::CommandLine.run(options)\n");

		runtime.evalScriptlet(script.toString());

		getLog().info("Verifying if there were failures");
		script = new StringBuilder();
		script.append(
				"if File.new(@report_file, 'r').read =~ /, 0 failures/ \n")
				.append(" false\n").append("else\n").append(" true\n").append(
						"end");

		RubyBoolean failure = (RubyBoolean) runtime.evalScriptlet(script
				.toString());

		if (failure.isTrue()) {
			String message = "RSpec tests failed. See '" + reportPath
					+ "' for details.";
			getLog().warn(message);
			if (!ignoreFailure) {
				throw new MojoFailureException(message);
			}
		} else {
			getLog().info(
					"RSpec tests successful. See '" + reportPath
							+ "' for details.");
		}
	}

	private String handleClasspathElements(Ruby runtime)
			throws MalformedURLException {
		StringBuilder script = new StringBuilder();
		for (String path : classpathElements) {
			if (path.endsWith(".jar")) {
				// handling jar files
				script.append("require '").append(path).append("'\n");
			} else {
				// handling directories
				getLog().info("Adding to Ruby Class Loader: " + path);
				runtime.getJRubyClassLoader().addURL(
						new URL("file:" + path + "/"));
			}
		}
		return script.toString();
	}

}
