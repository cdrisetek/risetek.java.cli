package risetek.jcli.utils;

import java.io.IOException;
import java.util.List;

import risetek.jcli.CliCallback;
import risetek.jcli.Cli_command;
import risetek.jcli.Cli_common;
import risetek.jcli.ICli;
import risetek.jcli.JCli;
import risetek.jcli.JCli.cliState;

public class Monitor implements ICli {
	public Monitor() {
		Cli_command temp = Cli_command.cli_register_command(Cli_common.debug_cli, "terminal", null,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "Set terminal line parameters");
		Cli_command.cli_register_command(temp, "monitor", cmd_terminal_monitor, PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "Copy debug output to the current terminal line");

		temp = Cli_command.cli_register_command(temp, "no", null, PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, " Negate a command or set its defaults");
		Cli_command.cli_register_command(temp, "monitor", cmd_no_terminal_monitor, PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "Copy debug output to the current terminal line");

	}
	
	private CliCallback cmd_terminal_monitor = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			return null;
		}
		
	};

	private CliCallback cmd_no_terminal_monitor = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			return null;
		}
		
	};
	
	interface CliLog {
		void call();
	}
	class Syslog_clients_chain {
		CliLog logger;
		Object context;
		Syslog_clients_chain next;
	}

	Syslog_clients_chain syslog_clients_chain;
	
}
