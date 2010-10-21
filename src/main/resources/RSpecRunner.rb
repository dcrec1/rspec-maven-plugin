require 'rubygems'
require 'rspec/core'

def run(sourceDir, reportFile)
	RSpec::Core::Runner.module_eval """
	 	def self.autorun_with_args(args)
			return if autorun_disabled? || installed_at_exit? || running_in_drb?
			@installed_at_exit = true 
			run(args, $stderr, $stdout)
		end"""
	RSpec::Core::Runner.autorun_with_args([sourceDir, '-f', 'html', '-o', reportFile])
end
