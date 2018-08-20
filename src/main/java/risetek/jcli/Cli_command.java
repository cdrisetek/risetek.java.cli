package risetek.jcli;

import risetek.jcli.Cli_common.cliMode;
import risetek.jcli.JCli.Wilds_callback;

public class Cli_command {
	Cli_command children;
	Cli_command next;
	int push_command;

	int unique_len[][] = new int[2][2]; // MAX_CLI_PRIVILEGE = 2
	
    int	auto_hide;
	
	
	String command;
	Wilds_callback wilds_callback;
	CliCallback callback;
	int privilege;
	public cliMode mode;
	String help;
	
    long range_lower;
    long range_uper;
	
	public Cli_command() {

	}

	private static Cli_common common = Cli_common.getInstance();
	
	public static Cli_command cli_register_command(Cli_command parent, String command,
			CliCallback callback, int privilege, cliMode mode, String help)
		{
		    Cli_command c, p;

		    if (command == null) return null;
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
	
}
