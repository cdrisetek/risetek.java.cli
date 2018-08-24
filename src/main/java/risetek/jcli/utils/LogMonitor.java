package risetek.jcli.utils;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LogMonitor extends Thread {
	private final static Logger logger = LogManager.getLogger(LogMonitor.class);
	
	public LogMonitor() {
		super("logmonitor");
		LogManager.getRootLogger().setLevel(Level.DEBUG);
		LogManager.getRootLogger().addAppender(new Log4jMonitorAppender());
		start();
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	
			// Cli_common.Debug(1, "LogMonitor example output\r\n");
			logger.debug("LogMonitor logger simple output\r\n");
			logger.log(Level.DEBUG, "LogMonitor logger output\r\n");
		}
	}
}
