package risetek.jcli.utils;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import risetek.jcli.Cli_common;

public class Log4jMonitorAppender implements Appender {

	@Override
	public void addFilter(Filter newFilter) {
	}

	@Override
	public Filter getFilter() {
		return null;
	}

	@Override
	public void clearFilters() {
	}

	@Override
	public void close() {
	}

	@Override
	public void doAppend(LoggingEvent event) {
		Object o = event.getMessage();
		if(!(o instanceof String))
			return;

		Cli_common.Debug(1, (String)o);
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setErrorHandler(ErrorHandler errorHandler) {
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return null;
	}

	@Override
	public void setLayout(Layout layout) {
	}

	@Override
	public Layout getLayout() {
		return null;
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

}
