/**
 * @since 8.17.2021
 * Used by LoggingHTMLFormatter class
 */

package utilities;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class HTMLFormatter extends Formatter {

	public HTMLFormatter() {
		// TODO Auto-generated constructor stub
	}

	public String format(LogRecord record) {
		return ("<tr><td>" + (new Date(record.getMillis())).toString() + "</td><td>" + record.getMessage()
				+ "</td></tr>\n");
	}

	public String getHead(Handler h) {
		return ("<html>\n  <body>\n" + "<Table border>\n<tr><td>Time</td><td>Log Message</td></tr>\n");
	}

	public String getTail(Handler h) {
		return ("</table>\n</body>\n</html>");
	}
}


