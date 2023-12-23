
package logging;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * 
 * Generic code snippet used by LoggingHTMLFormatter class.
 *
 */
public class HTMLFormatter extends Formatter {
	/**
	 * Generic
	 */
	public HTMLFormatter() {
	}

	/**
	 * Row of table
	 */
	public String format(LogRecord record) {
		return ("<tr><td>" + (new Date(record.getMillis())).toString() + "</td><td>" + record.getMessage()
				+ "</td></tr>\n");
	}

	/**
	 * Border of table
	 */
	public String getHead(Handler h) {
		return ("<html>\n  <body>\n" + "<Table border>\n<tr><td>Time</td><td>Log Message</td></tr>\n");
	}

	/**
	 * End of table
	 */
	public String getTail(Handler h) {
		return ("</table>\n</body>\n</html>");
	}
}
