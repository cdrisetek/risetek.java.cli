package risetek.jcli;

import risetek.jcli.Cli_common.cliMode;
import risetek.jcli.JCli.Wilds_callback;

public class Cli_command implements ICli {
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
		    if(common == null)
		    	common = Cli_common.getInstance();
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
				// System.out.println("Regist: from parent:" + p.command + " next:" + c.command);
				if (p!=null) p.next = c;
			}
		    return c;
		}
	
	// TODO: FIXME: 在cli使用完成后，没有释放这个结构。
	static class lazy_no_cli {
		Cli_command command;
		cliMode	mode;
		int		privilege;
		lazy_no_cli next;
	};
	
	private static lazy_no_cli no_cli_header = null; //new lazy_no_cli();

	// 我们能够够实现lazy构造？
	private static Cli_command no_cli(cliMode mode, int privilege)
	{
		lazy_no_cli lazy_p = no_cli_header;
		while( lazy_p != null )
		{
			if( lazy_p.mode == mode && lazy_p.privilege == privilege)
				return lazy_p.command;
			lazy_p = lazy_p.next;
		}
		// 没有找到，立即生成一个。
		// diag_printf("new lazy mode: %d\r\n", mode);
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
