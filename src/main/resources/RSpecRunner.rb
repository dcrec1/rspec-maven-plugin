require 'rubygems'
require 'rspec/core'

def run(sourceDir, requiredMod, reportFile)
	RSpec::Core::Runner.module_eval """
	 	def self.autorun_with_args(args)
			return if autorun_disabled? || installed_at_exit? || running_in_drb?
			@installed_at_exit = true 
			run(args, $stderr, $stdout)
		end"""
	opts = [sourceDir, '-f', 'html', '-o', reportFile]
	if requiredMod
		opts = ['-r'].product(requiredMod.split(',')).flatten + opts
	end
	RSpec::Core::Runner.autorun_with_args(opts)
end
