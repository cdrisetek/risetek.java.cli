package risetek.jcli.ext;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

import risetek.jcli.CliCallback;
import risetek.jcli.Cli_command;
import risetek.jcli.Cli_common;
import risetek.jcli.JCli;
import risetek.jcli.JCli.cliState;

public class Syscast extends Thread {
	public Syscast() {
		Cli_command.cli_register_command(Cli_common.debug_cli, "syscast", cmd_syscast_debug,  Cli_common.PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
		Cli_command.cli_register_command(Cli_common.no_debug_cli, "syscast", cmd_syscast_debug_no,  Cli_common.PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
		Cli_command.cli_register_command(Cli_common.show_cli, "syscast", cmd_syscast_show,  Cli_common.PRIVILEGE_PRIVILEGED, Cli_common.MODE_EXEC, "debug syscast");
	
		start();
	}

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
			Cli_common.Debug(1, "debug syscast is supported.");
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
			System.out.println("Packet from: " + packet.getAddress() + " Size:" + str.length());
			System.out.println(str);

			try {
				parser.parser(str);
			} catch (Exception e) {
				System.out.println("Parse Failed");
			}
		}
	}
}
