/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package logging;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Lazily initialized global logger used across the project.
 * <p>
 * The first logging call creates a Java {@link Logger} configured with an
 * {@link HTMLFormatter} and a timestamped HTML output file named
 * {@code Rank-Based_Linkage_Log<timestamp>.html} in the current working
 * directory.
 * <p>
 * This class exposes a single static {@link #log(Level, String)} entry point so
 * callers do not need to manage logger instances directly.
 *
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public class GlobalLog {
	private final String prefix = "Rank-Based_Linkage_Log";
	private static Logger logger;
	private Handler html_Handler;
	private LogManager lm;

	/**
	 * Constructs and configures the backing logger and HTML file handler.
	 *
	 * @throws IOException if the log file handler cannot be created
	 */
	private GlobalLog() throws IOException {
		// instance the logger
		logger = Logger.getLogger(GlobalLog.class.getName());
		// instance the filehandler
		long timestamp = System.currentTimeMillis();
		String logfilename = prefix + timestamp + ".html";
		lm = LogManager.getLogManager();
		html_Handler = new FileHandler(logfilename, true);
		lm.addLogger(logger); // do we need this to create the html file?
		logger.setLevel(Level.ALL);
		html_Handler.setFormatter(new HTMLFormatter());
		logger.addHandler(html_Handler);
	}

	/**
	 * Returns the singleton logger instance, creating and configuring it on first
	 * access.
	 *
	 * @return initialized global {@link Logger}
	 */
	private static Logger getLogger() {

		if (logger == null) {
			try {
				new GlobalLog();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return logger;
	}

	/**
	 * Appends a message to the global logger and mirrors it to standard output.
	 *
	 * @param level log severity level (for example {@link Level#INFO})
	 * @param msg message text to record
	 */
	public static void log(Level level, String msg) {
		getLogger().log(level, msg);
		System.out.println(msg);
	}
}