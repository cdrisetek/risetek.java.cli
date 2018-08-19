package risetek.jcli;

import java.io.IOException;
import java.util.List;

import risetek.jcli.JCli.cliState;

public interface CliCallback {
	cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException;
}
