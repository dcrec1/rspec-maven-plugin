package org.codehaus.mojo.rspec;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;

/**
 * Mojo to run Ruby Spec test
 * 
 * @author Michael Ward
 * @author Mauro Talevi
 * @author Diego Carrion
 * @author Andre Goncalves
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

	/**
	 * The flag to start selenium server (optional, defaults to "false")
	 * 
	 * @parameter expression="false"
	 */
	private boolean runSeleniumServer;

	/**
	 * The selenium server port (optional, defaults to "4444")
	 * 
	 * @parameter expression="4444"
	 */
	private int seleniumPort;

	
	private SeleniumServer server;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (runSeleniumServer) {
			startSelenium();
		}

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

		String reportFile = outputDirectory + "/" + reportName;
		StringBuilder script = new StringBuilder();
		try {
			script.append(handleClasspathElements(runtime));
		} catch (MalformedURLException e) {
			throw new MojoExecutionException(e.getMessage());
		}

		String e = "require 'rubygems'\nrequire 'rspec/core/rake_task'\n\nrequire 'rspec/core'\ndef run\nRSpec::Core::Runner.module_eval \"\"\"\n def self.autorun_with_args(args)\n return if autorun_disabled? || installed_at_exit? || running_in_drb?\n  @installed_at_exit = true \n   run(args, $stderr, $stdout)\n end\n\"\"\"\n"
				+ "\nRSpec::Core::Runner.autorun_with_args(['"
				+ sourceDirectory
				+ "', '-f', 'html', '-o', '"
				+ reportFile
				+ "'])\nend";
		System.out.println(script);
		RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
		IRubyObject o = evaler.eval(runtime, e);
		boolean result = ((RubyBoolean) JavaEmbedUtils.invokeMethod(runtime, o,
				"run", new Object[] {}, (Class<?>) RubyBoolean.class)).isTrue();

		if (!result) {
			String msg = "RSpec tests failed. See '" + reportFile
					+ "' for details.";
			getLog().warn(msg);
			if (!ignoreFailure) {
				throw new MojoFailureException(msg);
			}
		} else {
			String msg = "RSpec tests successful. See '" + reportFile
					+ "' for details.";
			getLog().info(msg);
		}

		if (runSeleniumServer) {
			stopSelenium();
		}
	}

	private void stopSelenium() {
		server.stop();
	}

	private void startSelenium() throws MojoExecutionException {
		try {
			RemoteControlConfiguration config = new RemoteControlConfiguration();
			config.setPort(seleniumPort);
			server = new SeleniumServer(config);
			server.start();
		} catch (Exception e2) {
			throw new MojoExecutionException("cannot start selenium server", e2);
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
