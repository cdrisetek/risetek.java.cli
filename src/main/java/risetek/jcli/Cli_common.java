package risetek.jcli;

import java.io.IOException;
import java.util.List;

import risetek.jcli.JCli.cliState;
import risetek.jcli.JCli.cli_filter;

public class Cli_common {
	private static Cli_common instance = null;
	public static int PRIVILEGE_UNPRIVILEGED = 0; 
	public static int PRIVILEGE_PRIVILEGED = 1; 
	public static cliMode MODE_ANY = new cliMode();
	public static cliMode MODE_EXEC = new cliMode();
	public static cliMode MODE_CONFIG = new cliMode();
	
	
	Cli_command commands;
	cli_filter	filters;
	private Cli_common() {
	}

	public static Cli_common getInstance() {
		if(instance == null) {
			instance = new Cli_common();
			Cli_command.cli_register_command(null, "history", cli_int_history, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Display the session command history");		
			Cli_command.cli_register_command(null, "help", cli_int_help, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Display the help of commands");		
		}
		return instance;
	}
	
	static CliCallback cli_int_history = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("do command:"+command);
			return cliState.CLI_OK;
		}
		
	};
	
	static CliCallback cli_int_help = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("do command:"+command + " start with:" + start + " argc:" + argc);
			return cliState.CLI_OK;
		}
		
	};
	
	public static class cliMode {
		cliMode parent;
	}

}
