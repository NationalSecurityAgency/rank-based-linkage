package orientationGeneratorsTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import orientationGenerators.RandomThreeConcordant;
import utilities.HTMLFormatter;

/**
 * Count 3-concordant on 4 points. In how many is {01, 12, 23, 03} a 4-cycle?
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class Exhaust3ConcordantOnFourPoints {

	static Logger logger = Logger.getLogger(Exhaust3ConcordantOnFourPoints.class.getName());
	RandomThreeConcordant r3c;
	final int n;

	/**
	 * 
	 * @param num size of set of points
	 */
	public Exhaust3ConcordantOnFourPoints() {
		this.n = 4;
		r3c = new RandomThreeConcordant(n);
		this.initializeHTMLLogger(System.currentTimeMillis());
	}

	void printRankTable() {
		int[][] table = this.r3c.getRankTable();
		for (int r = 0; r < this.n; r++) {
			logger.info("Ranks from r: " + Arrays.toString(table[r]));
		}
	}

	void fourExhaust() {
		int[] fourExhaust = this.r3c.produceAll3ConcordantTables4Points();
		logger.info("Number of 3-concordant ranking tables on 4 points: " + fourExhaust[0]);
		logger.info("Of these, number in which {01, 12, 23, 03} is a 4-cycle: " + fourExhaust[1]);
		double p4cycle = (double)fourExhaust[1] / (double)fourExhaust[0];
		logger.info("Sampling a 3-simplex in a uniform 3-concordant ranking system: 4-cycle probability: " + p4cycle);
	}

	/**
	 * Reports result to logger
	 * 
	 * @param args none
	 */
	public static void main(String[] args) {
		Exhaust3ConcordantOnFourPoints test = new Exhaust3ConcordantOnFourPoints();
		/*
		 * Check initialization
		 */
		test.r3c.initialAcyclicOrientation();
		if (test.n < 10) {
			test.printRankTable();
		}
		test.fourExhaust();
	}

	/**
	 * 
	 * @param time stamp
	 */
	private void initializeHTMLLogger(long timestamp) {
		try {
			LogManager lm = LogManager.getLogManager();
			FileHandler html_handler = new FileHandler(timestamp + "-log-r3c.html", true); // produced, readable
			lm.addLogger(logger);
			logger.setLevel(Level.ALL);
			html_handler.setFormatter(new HTMLFormatter());
			logger.addHandler(html_handler);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
