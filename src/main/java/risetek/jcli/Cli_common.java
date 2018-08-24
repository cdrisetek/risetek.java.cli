package risetek.jcli;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import risetek.jcli.JCli.cliState;
import risetek.jcli.JCli.cli_filter;

public class Cli_common extends Thread implements ICli {
	private static Cli_common instance = null;
	public static cliMode MODE_ANY = new cliMode(null, null);
	public static cliMode MODE_EXEC = new cliMode(null, null);
	public static cliMode MODE_CONFIG = new cliMode(MODE_EXEC, null);

	public static String hostname = null;
	public static String enable_password = null;

	public static void loadCommand(Class<?>... clet) {
		// ensure init cli common object.
		Cli_common.getInstance();

		for (Class<?> clazz : clet)
			try {
				Object instance = clazz.newInstance();
				if (instance instanceof HasRunningConf)
					Cli_common.runningConfigList.add((HasRunningConf) instance);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
	}
	
	
	public static List<HasRunningConf> runningConfigList = new Vector<>();
	
	static class _LogEntity {
		int level;
		String message;
	}

	public static LinkedList<_LogEntity> logQueue = new LinkedList<>();

	public static void monitor(int level, String format, Object... args) {
		if (logQueue.size() > 1000)
			return;
		_LogEntity entity = new _LogEntity();
		entity.level = level;
		entity.message = String.format(format, args);
		synchronized (logQueue) {
			logQueue.add(entity);
			logQueue.notify();
		}
	}

	@Override
	public void run() {
		_LogEntity entity;
		while (true) {
			synchronized (logQueue) {
				if (logQueue.size() > 0) {
					entity = logQueue.pop();
					for(JCli cli:cliInstances) {
						cli.DebugOutput(entity.message);
					}
				} else {
					try {
						logQueue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	static List<JCli> cliInstances = new Vector<>();
	public void AddCli(JCli cli) {
		cliInstances.add(cli);
	}
	public void RemoveCli(JCli cli) {
		cliInstances.remove(cli);
	}
	
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

	interface Regular_callback {
		cliState call(JCli cli);
	}

	Regular_callback regular_callback;

	Cli_command commands;
	cli_filter filters;

	private Cli_common() {
		super("cli common");
	}

	public static Cli_command show_cli;
	public static Cli_command debug_cli;
	public static Cli_command no_debug_cli;
	public static Cli_command service_cli;
	public static Cli_command no_service_cli;

	public static Cli_common getInstance() {
		if (instance == null) {
			instance = new Cli_common();
			instance.start();
			
			service_cli = Cli_command.cli_register_command(null, "service", null, PRIVILEGE_UNPRIVILEGED, MODE_CONFIG,
					"service");
			no_service_cli = Cli_command.cli_register_no_command("service", null, PRIVILEGE_PRIVILEGED, MODE_CONFIG, "negtive service");
			
			show_cli = Cli_command.cli_register_command(null, "show", null, PRIVILEGE_UNPRIVILEGED, MODE_EXEC,
					"show information");
			debug_cli = Cli_command.cli_register_command(null, "debug", null, PRIVILEGE_PRIVILEGED, MODE_EXEC,
					"Debugging functions");
			no_debug_cli = Cli_command.cli_register_command(null, "nodebug", null, PRIVILEGE_PRIVILEGED, MODE_EXEC,
					"Disable debugging functions");

			Cli_command.cli_register_command(null, "history", cli_int_history, PRIVILEGE_UNPRIVILEGED, MODE_EXEC,
					"Display the session command history");
			Cli_command.cli_register_command(null, "help", cli_int_help, PRIVILEGE_UNPRIVILEGED, MODE_EXEC,
					"Display the help of commands");
			Cli_command.cli_register_command(null, "enable", cli_int_enable, PRIVILEGE_UNPRIVILEGED, MODE_EXEC,
					"Turn on privileged commands");
			Cli_command.cli_register_command(null, "exit", cli_int_exit_exec, PRIVILEGE_PRIVILEGED, MODE_EXEC,
					"Exit from the EXEC");
			Cli_command.cli_register_command(null, "exit", cli_int_exit_conf, PRIVILEGE_PRIVILEGED, MODE_CONFIG,
					"Exit from configure mode");
			
			Cli_command temp = Cli_command.cli_register_command(null, "terminal", null,  PRIVILEGE_PRIVILEGED, MODE_EXEC, "Set terminal line parameters");
			Cli_command.cli_register_command(temp, "monitor", cmd_terminal_monitor, PRIVILEGE_PRIVILEGED, MODE_EXEC, "Copy debug output to the current terminal line");

			temp = Cli_command.cli_register_command(temp, "no", null, PRIVILEGE_PRIVILEGED, MODE_EXEC, " Negate a command or set its defaults");
			Cli_command.cli_register_command(temp, "monitor", cmd_no_terminal_monitor, PRIVILEGE_PRIVILEGED, MODE_EXEC, "Copy debug output to the current terminal line");
			

			temp = Cli_command.cli_register_command(null, "configure", null, PRIVILEGE_PRIVILEGED, MODE_EXEC, "Enter configuration mode");
			Cli_command.cli_register_command(temp, "terminal", cmd_configure_terminal, PRIVILEGE_PRIVILEGED, MODE_EXEC, "Configure from the terminal");
		
		}
		return instance;
	}

	static private CliCallback cmd_configure_terminal = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.cli_set_configmode(MODE_CONFIG, null, null);
			return null;
		}
	};
	static private CliCallback cmd_terminal_monitor = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			if(cli.debug_monitor == true)
				cli.print("the monitor is already opened\r\n");
			else {
				cli.debug_monitor = true;
				cli.print("the monitor is on\r\n");
			}
			return cliState.CLI_OK;
		}
		
	};

	static private CliCallback cmd_no_terminal_monitor = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			if(cli.debug_monitor == false)
				cli.print("the monitor is not opened\r\n");
			else {
				cli.debug_monitor = false;
				cli.print("the monitor is off\r\n");
			}
			return cliState.CLI_OK;
		}
		
	};	
	static CliCallback cli_int_history = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
		    cli.error("\nCommand history:");
		    for (int i = 0; i < JCli.MAX_HISTORY; i++)
		    {
		        if (cli.history[i] != null)
		            cli.error("%3d. %s", i, new String(cli.history[i]));
		    }

			return cliState.CLI_OK;
		}

	};

	static CliCallback cli_int_help = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("do command:" + command + " start with:" + start + " argc:" + argc);
			return cliState.CLI_OK;
		}

	};

	static CliCallback cli_int_enable = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			if (cli.privilege == PRIVILEGE_PRIVILEGED)
				return cliState.CLI_OK;

			if (enable_password == null && enable_callback == null) {
				/* no password required, set privilege immediately */
				cli.cli_set_privilege(PRIVILEGE_PRIVILEGED);
				cli.cli_set_configmode(MODE_EXEC, null, null);
			} else {
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
