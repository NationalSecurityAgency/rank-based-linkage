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
 * Test {@link RandomThreeConcordant} class.
 * Example: for n = 1000, 249M edge flips can be completed in 21 seconds.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TestRandomThreeConcordant {

	static Logger logger = Logger.getLogger(TestRandomThreeConcordant.class.getName());
	RandomThreeConcordant r3c;
	Random g;
	final int n, num1Simplices;
/**
 * 
 * @param num size of set of points
 */
	public TestRandomThreeConcordant(int num) {
		this.n = num;
		this.num1Simplices = n * (n - 1) * (n - 2) / 2;
		r3c = new RandomThreeConcordant(n);
		g = new Random();
		this.initializeHTMLLogger(System.currentTimeMillis());
	}

	void printRankTable() {
		int[][] table = this.r3c.getRankTable();
		for (int r = 0; r < this.n; r++) {
			logger.info("Ranks from r: " + Arrays.toString(table[r]));
		}
	}

	void report3Cycle() {
		OptionalInt c = IntStream.range(1, n * n * n).filter(m -> r3c.voter2SimplexIsCyclic(m)).findAny();
		if (c.isPresent()) {
			int m = c.getAsInt();
			int x = m % n; // e.g. n = 10, m = 875, x = 5
			int r = (m - x) / n; // e.g. 87
			int y = r % n; // e.g. 7
			int s = (r - y) / n; // e.g. 8
			int z = s % n; // e.g. 8
			logger.warning("2-simplex {" + x + ", " + y + ", " + z + "} is a directed cycle.");
		}
	}
/**
 * Reports result to logger
 * @param args none
 */
	public static void main(String[] args) {
		int num = 1000;
		TestRandomThreeConcordant test = new TestRandomThreeConcordant(num);
		/*
		 * Check initialization
		 */
		test.r3c.initialAcyclicOrientation();
		if (test.n < 10) {
			test.printRankTable();
		}
		boolean threeConcordant = test.r3c.test3Concordance();
		logger.info("3-concordant at initialization? " + threeConcordant);
		if (!threeConcordant) {
			test.report3Cycle();
		}
		/*
		 * Prepare for edge flip process
		 */
		double meanSteps = (double) test.num1Simplices / 2.0;
		double sdSteps = Math.sqrt(meanSteps);
		int numSteps = (int) Math.round(meanSteps + sdSteps * test.g.nextGaussian());
		logger.info("n = " + test.n + ": # 1-simplices = " + test.num1Simplices + "; random # of steps: " + numSteps);
		/*
		 * Perform edge flip process
		 */
		test.r3c.edgeFlipProcess(numSteps);
		logger.info("Number of edge flips performed: " + test.r3c.getNumFlips());
		if (test.n < 10) {
			test.printRankTable();
		}
		threeConcordant = test.r3c.test3Concordance();
		logger.info("3-concordant after edge flips? " + threeConcordant);
		
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
