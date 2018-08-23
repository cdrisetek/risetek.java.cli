package risetek.jcli;

import java.io.IOException;

import risetek.jcli.ext.Syscast;
import risetek.jcli.utils.ParamSaver;

public class Bootstrap {

	public static void main(String[] args) {
		// ensure init cli common object.
		Cli_common.getInstance();
		
		loadCommand(ParamSaver.class);
		loadCommand(Syscast.class);

		try {
			new CliSocketChannel().startServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadCommand(Class<?> applet) {
		try {
			Object app = applet.newInstance();
			if(app instanceof HasRunningConf)
				Cli_common.runningConfigList.add((HasRunningConf)app);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
