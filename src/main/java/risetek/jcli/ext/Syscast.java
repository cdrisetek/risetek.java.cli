package risetek.jcli.ext;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

import risetek.jcli.CliCallback;
import risetek.jcli.Cli_command;
import risetek.jcli.Cli_common;
import risetek.jcli.HasRunningConf;
import risetek.jcli.ICli;
import risetek.jcli.JCli;
import risetek.jcli.JCli.cliState;

public class Syscast extends Thread implements ICli, HasRunningConf {
	public Syscast() {
		Cli_command.cli_register_command(Cli_common.debug_cli, "syscast", cmd_syscast_debug,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
		Cli_command.cli_register_command(Cli_common.no_debug_cli, "syscast", cmd_syscast_debug_no,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
		Cli_command.cli_register_command(Cli_common.show_cli, "syscast", cmd_syscast_show,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
	
		Cli_command.cli_register_command(Cli_common.service_cli, "syscast", cmd_syscast_service,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_CONFIG, "run syscast");
		Cli_command.cli_register_command(Cli_common.no_service_cli, "syscast", cmd_syscast_service_no,  PRIVILEGE_PRIVILEGED, Cli_common.MODE_CONFIG, "stop syscast");
		start();
	}

	private boolean service = false;
	private CliCallback cmd_syscast_service = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			service = true;
			return cliState.CLI_OK;
		}
	};
	
	private CliCallback cmd_syscast_service_no = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			service = false;
			return cliState.CLI_OK;
		}
	};
	
	private boolean debug = false;
	private CliCallback cmd_syscast_debug = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			debug = true;
			return cliState.CLI_OK;
		}
	};

	private CliCallback cmd_syscast_debug_no = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			debug = false;
			return cliState.CLI_OK;
		}
	};
	
	private CliCallback cmd_syscast_show = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			return cliState.CLI_OK;
		}
	};

	@Override
	public void run() {
		DatagramSocket dgSocket = null;
		try {
			dgSocket = new DatagramSocket(80);
			receiveIP(dgSocket);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dgSocket != null)
				dgSocket.close();
		}
	}

	private void receiveIP(DatagramSocket dgSocket) throws Exception {
		XMLParser parser = new XMLParser();
		while (true) {
			byte[] by = new byte[1024];
			DatagramPacket packet = new DatagramPacket(by, by.length);
			dgSocket.receive(packet);

			String str = new String(packet.getData(), 0, packet.getLength());
			if(debug) {
				Cli_common.Debug(1, "Packet from: " + packet.getAddress() + " Size:" + str.length());
				Cli_common.Debug(1, str);
			}

			try {
				parser.parser(str);
			} catch (Exception e) {
				System.out.println("Parse Failed");
			}
		}
	}

	@Override
	public void processConfig(JCli cli) {
		if(service) {
			cli.print("!");
			cli.print("service syscast");
		}
	}
}
