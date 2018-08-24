package risetek.jcli;

import risetek.jcli.ext.Syscast;
import risetek.jcli.utils.LogMonitor;
import risetek.jcli.utils.ParamSaver;

public class Bootstrap {

	public static void main(String[] args) {
		loadCommand(ParamSaver.class);
		loadCommand(Syscast.class);

		loadCommand(LogMonitor.class);

		CliSocketChannel.launch(2223);

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void loadCommand(Class<?>... clet) {
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
}
