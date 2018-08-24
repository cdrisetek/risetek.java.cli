package risetek.jcli;

import risetek.jcli.Cli_common.cliMode;
import risetek.jcli.JCli.Wilds_callback;

public class Cli_command implements ICli {
	Cli_command children;
	Cli_command next;
	int push_command;

	int unique_len[][] = new int[MAX_CLI_PRIVILEGE][2];
	
    int	auto_hide;
	
	
	String command;
	Wilds_callback wilds_callback;
	CliCallback callback;
	int privilege;
	public cliMode mode;
	String help;
	
    long range_lower;
    long range_uper;
	
	private static Cli_common common = Cli_common.getInstance();
	
	public static Cli_command cli_register_command(Cli_command parent, String command,
			CliCallback callback, int privilege, cliMode mode, String help)
		{
			assert(common != null);
			assert(command != null);

			Cli_command c, p;
		    c = new Cli_command();
		    
		    c.callback = callback;
		    c.next = null;
			c.wilds_callback = null;
			c.command = command;
		    c.privilege = privilege;
		    c.mode = mode;
	    	c.help=help;

	    	Cli_command r = null;
	    	if(parent == null)
	    		if(common.commands == null)
	    			common.commands = c;
	    		else
	    			r = common.commands;
	    	else
	    		if(parent.children == null)
	    			parent.children = c;
	    		else
	    			r = parent.children;
	    	
	    	if(r != null)
			{
				for (p = r; p!=null && p.next!=null; p = p.next);
				if (p!=null) p.next = c;
			}
		    return c;
		}
	
	private static class lazy_no_cli {
		Cli_command command;
		cliMode	mode;
		int		privilege;
		lazy_no_cli next;
	};
	
	private static lazy_no_cli no_cli_header = null;
	private static Cli_command no_cli(cliMode mode, int privilege)
	{
		lazy_no_cli lazy_p = no_cli_header;
		for(; lazy_p != null; lazy_p = lazy_p.next)
		{
			if( lazy_p.mode == mode && lazy_p.privilege == privilege)
				return lazy_p.command;
		}

		lazy_p = new lazy_no_cli();
		lazy_p.command = cli_register_command(null, "no", null,  PRIVILEGE_PRIVILEGED, mode, "Negate a command or set its defaults");
		lazy_p.mode = mode;
		lazy_p.privilege = privilege;
		lazy_p.next = no_cli_header;
		no_cli_header = lazy_p;
		return no_cli_header.command;
	}
	
	public static Cli_command cli_register_no_command(String command,
			CliCallback callback, int privilege, cliMode mode, String help) {
		return cli_register_command(no_cli(mode, privilege), command, callback,  privilege, mode, help);
	}
}
