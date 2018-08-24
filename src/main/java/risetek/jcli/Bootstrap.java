package risetek.jcli;

import risetek.jcli.ext.Syscast;
import risetek.jcli.utils.LogMonitor;
import risetek.jcli.utils.ParamSaver;

public class Bootstrap {

	public static void main(String[] args) {
		init();

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void init() {
		Cli_common.loadCommand(ParamSaver.class, Syscast.class);

		Cli_common.loadCommand(LogMonitor.class);

		CliSocketChannel.launch(2223);
	}
}
