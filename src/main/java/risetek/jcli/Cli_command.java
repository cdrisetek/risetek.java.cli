package risetek.jcli;

import risetek.jcli.JCli.CliCallback;
import risetek.jcli.JCli.Wilds_callback;
import risetek.jcli.JCli.cliMode;

public class Cli_command {
	Cli_command children;
	Cli_command next;
	int push_command;

	int unique_len[][] = new int[10][2];
	
	String command;
	Wilds_callback wilds_callback;
	CliCallback callback;
	int privilege;
	public cliMode mode;
	String help;
	
	public Cli_command() {

	}

}
