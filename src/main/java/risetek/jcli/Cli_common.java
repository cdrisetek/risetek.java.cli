package risetek.jcli;

import java.io.IOException;
import java.util.List;

import risetek.jcli.JCli.cliState;
import risetek.jcli.JCli.cli_filter;

public class Cli_common {
	private static Cli_common instance = null;
	public static int PRIVILEGE_UNPRIVILEGED = 0; 
	public static int PRIVILEGE_PRIVILEGED = 1; 
	public static cliMode MODE_ANY = new cliMode(null, null);
	public static cliMode MODE_EXEC = new cliMode(null, null);
	public static cliMode MODE_CONFIG = new cliMode(MODE_EXEC,null);
	
	public static String hostname = null;
	public static String enable_password = null;
	
	interface Enable_callback {
		boolean call(byte[] command);
	}
	
	interface Auth_callback {
		cliState call(String username, String password);
	}
	Auth_callback auth_callback = null;
	
	class unp {
		String username;
		String password;
		unp next;
	}
	unp users = null;
	
	public static Enable_callback enable_callback = null;
	
	interface Regular_callback{
		cliState call(JCli cli);
	}
	Regular_callback regular_callback;
	
	
	Cli_command commands;
	cli_filter	filters;
	private Cli_common() {
	}

	public static Cli_command show_cli;
	public static Cli_command debug_cli;
	public static Cli_command no_debug_cli;
	
	public static Cli_common getInstance() {
		if(instance == null) {
			instance = new Cli_common();
			
		    show_cli = Cli_command.cli_register_command(null, "show", null,  PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "show information");
		    debug_cli = Cli_command.cli_register_command(null, "debug", null,  PRIVILEGE_PRIVILEGED, MODE_EXEC, "Debugging functions");
		    no_debug_cli = Cli_command.cli_register_command(null, "nodebug", null,  PRIVILEGE_PRIVILEGED, MODE_EXEC, "Disable debugging functions");

			
			Cli_command.cli_register_command(null, "history", cli_int_history, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Display the session command history");		
			Cli_command.cli_register_command(null, "help", cli_int_help, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Display the help of commands");		
			Cli_command.cli_register_command(null, "enable", cli_int_enable, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Turn on privileged commands");
			Cli_command.cli_register_command(null, "exit", cli_int_exit_exec, PRIVILEGE_PRIVILEGED, MODE_EXEC, "Exit from the EXEC");
			Cli_command.cli_register_command(null, "exit", cli_int_exit_conf, PRIVILEGE_PRIVILEGED, MODE_CONFIG, "Exit from configure mode");
			
			Cli_command.cli_register_command(null, "write", cmd_save_param,  PRIVILEGE_PRIVILEGED, MODE_EXEC, "Save runing-config");
			
		}
		return instance;
	}

	static CliCallback cmd_save_param = new CliCallback() {

		@Override
		public synchronized cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			
			cli.print("#Begin construct configure");

			/*
			cli_print_callback(cli, buffer_configure);
		    cli_print(cli, "\n#Configure:");

			param_commandtab_entry_t *t;
			int looper;
			for( looper = PARAM_PROCESS_LEVEL_FIRST; looper <= PARAM_PROCESS_LEVEL_LAST; looper++)
			{
			    for (t = &__PARAM_COMMANDS_TAB__[0]; t != &__PARAM_COMMANDS_TAB_END__; t++)
			    {
			        if( t->level == looper)
			        	t->param_process(cli);
			    }
			}
			cli_print_callback(cli, NULL);

			if( confiurebuffer != NULL ){
				cli_print(cli, "#Total configure length is %lu\r\n", (long unsigned int)strlen(confiurebuffer));

				if( saveconfig(cli, confiurebuffer) == CLI_OK ){
					cli_print(cli, "#Configure saved.");
				} else {
			    	cli_print(cli, "#Save configure failed.");
				}

				free(confiurebuffer);
			    confiurebuffer = NULL;
			    confiurebuffersize = 0;
			}
			*/
			return cliState.CLI_OK;
		}
		
	};
	
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

	static CliCallback cli_int_enable = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("do command:"+command);
			
		    if (cli.privilege == PRIVILEGE_PRIVILEGED)
		        return cliState.CLI_OK;

		    if (enable_password==null && enable_callback==null)
		    {
		        /* no password required, set privilege immediately */
		        cli.cli_set_privilege(PRIVILEGE_PRIVILEGED);
		        cli.cli_set_configmode(MODE_EXEC, null, null);
		    }
		    else
		    {
		        /* require password entry */
		        cli.state = JCli.State.STATE_ENABLE_PASSWORD;
		    }
			return cliState.CLI_OK;
		}
		
	};
	static CliCallback cli_int_exit_exec = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
		    cli.cli_set_privilege(PRIVILEGE_UNPRIVILEGED);
		    cli.cli_set_configmode(MODE_EXEC, null, null);
			return cliState.CLI_OK;
		}
		
	};
	static CliCallback cli_int_exit_conf = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.cli_set_configmode(MODE_EXEC, null, null);
			return cliState.CLI_OK;
		}
		
	};
	
	public static class cliMode {
		public cliMode(cliMode parent, String desc) {
			this.parent = parent;
			config_desc = desc;
		}
		public cliMode parent;
		public String config_desc;
	}

}
