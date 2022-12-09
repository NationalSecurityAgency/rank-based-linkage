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
 * Creates a static global logger to which various classes may append their
 * reports. The log file will be called
 * "Rank_Based_Linkage_Log(timestamp).html".
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class GlobalLog {
	private final String prefix = "Rank-Based_Linkage_Log";
	private static Logger logger;
	private Handler html_Handler;
	private LogManager lm;

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
 * 
 * @param level such as Level.INFO
 * @param msg string for report
 */
	public static void log(Level level, String msg) {
		getLogger().log(level, msg);
		System.out.println(msg);
	}
}
