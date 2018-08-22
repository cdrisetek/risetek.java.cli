package risetek.jcli;

import java.io.IOException;

public class Bootstrap {

	public static void main(String[] args) {
		System.out.println("hello risetek jcli");
		new ParamSaver();
		new Syscast();
		try {
			new CliSocketChannel().startServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
