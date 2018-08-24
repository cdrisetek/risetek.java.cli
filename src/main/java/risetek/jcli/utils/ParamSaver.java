package risetek.jcli.utils;

import java.io.IOException;
import java.util.List;

import risetek.jcli.CliCallback;
import risetek.jcli.Cli_command;
import risetek.jcli.Cli_common;
import risetek.jcli.HasRunningConf;
import risetek.jcli.ICli;
import risetek.jcli.JCli;
import risetek.jcli.JCli.IHelper_print;
import risetek.jcli.JCli.cliState;

public class ParamSaver implements ICli {
	public ParamSaver() {
		Cli_command.cli_register_command(Cli_common.show_cli, "running-config", cmd_show_param,  PRIVILEGE_PRIVILEGED, 
				Cli_common.MODE_EXEC, "Current operating configuration");
		Cli_command.cli_register_command(null, "write", cmd_save_param,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "Save runing-config");
	}

	CliCallback cmd_show_param = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("#Begin construct configure");
			for(HasRunningConf processer:Cli_common.runningConfigList)
				processer.processConfig(cli);

			return cliState.CLI_OK;
		}
	};
	
	private StringBuffer confiurebuffer = null;

	CliCallback cmd_save_param = new CliCallback() {

		@Override
		public synchronized cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			
			cli.print("#Begin construct configure");

			cli.cli_print_callback(buffer_configure);
		    cli.print("\n#Configure:");
			for(HasRunningConf processer:Cli_common.runningConfigList)
				processer.processConfig(cli);

			cli.cli_print_callback(null);

			if( confiurebuffer != null ){
				cli.print("#Total configure length is %d\r\n", confiurebuffer.length());

				if( saveconfig(confiurebuffer.toString()) == cliState.CLI_OK ){
					cli.print("#Configure saved.");
				} else {
					cli.print("#Save configure failed.");
				}

			    confiurebuffer = null;
			}

			return cliState.CLI_OK;
		}
		
	};

	IHelper_print buffer_configure = new IHelper_print() {

		@Override
		public void print(String command) throws IOException {
			if(confiurebuffer==null) {
				confiurebuffer = new StringBuffer();
			}
			confiurebuffer.append(command);
			confiurebuffer.append("\r\n");
		}
	};
	
	private cliState saveconfig(String command) {
		// 首先删除文件
		/*
		String sys_filename = "./system.ini";
		unlink(sys_filename);
	    int fd = open( sys_filename, O_WRONLY | O_CREAT );
	    if( fd > 0 ){
		    write(fd, command, strlen( command ) );
			close(fd);
	    }
	    */
		System.out.println(command);
		return cliState.CLI_OK;
	}
	
}
