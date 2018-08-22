package risetek.jcli;

import java.io.IOException;
import java.util.List;

import risetek.jcli.JCli.cliState;

public class Syscast {
	public Syscast() {
		Cli_command.cli_register_command(Cli_common.debug_cli, "syscast", cmd_syscast_debug,  Cli_common.PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
		Cli_command.cli_register_command(Cli_common.show_cli, "syscast", cmd_syscast_show,  Cli_common.PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
	}

	private CliCallback cmd_syscast_debug = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("enable debug syscast");
			return cliState.CLI_OK;
		}
	};
	
	private CliCallback cmd_syscast_show = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("debug syscast is supported.");
			return cliState.CLI_OK;
		}
	};
	
}
