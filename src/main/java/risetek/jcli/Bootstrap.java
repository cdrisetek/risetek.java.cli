package risetek.jcli;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Bootstrap {

	public static void main(String[] args) {
		System.out.println("hello risetek jcli");
		launchConsole();
	}

	public static void launchConsole() {
		JCli cli = new JCli();
		ReadableByteChannel in = Channels.newChannel(System.in);
		WritableByteChannel out = Channels.newChannel(System.out);
		cli.cli_schedule(in, out);
	}
}
