package orientationStructuresTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.OptionalLong;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import orientationStructures.RankingSystem;
import utilities.HTMLFormatter;

/**
 * Test rejection sampling methods of {@link RankingSystem} class.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class RejectionSampler3Concordant {

	static Logger logger = Logger.getLogger(RejectionSampler3Concordant.class.getName());
	RankingSystem rs;
	Random g;
	final int n;

	/**
	 * 
	 * @param num size of set of points
	 */
	public RejectionSampler3Concordant(int num) {
		this.n = num;
		rs = new RankingSystem(n);
		g = new Random();
		this.initializeHTMLLogger(System.currentTimeMillis());
	}

	void printRankTable() {
		int[][] table = this.rs.getRankTable();
		for (int r = 0; r < this.n; r++) {
			logger.info("Ranks from r: " + Arrays.toString(table[r]));
		}
	}

	/**
	 * Reports result to logger
	 * 
	 * @param args none
	 */
	public static void main(String[] args) {
		int num = 9;
		RejectionSampler3Concordant test = new RejectionSampler3Concordant(num);
		/*
		 * Generate a uniform random rank table
		 */
		test.rs.generateUniformRandomRankTable();
		if (test.n < 10) {
			logger.info("Uniform random rank table, not necessarily 3-concordant");
			test.printRankTable();
			logger.info("Is it 3-concordant? " + test.rs.test3Concordance());
		}
		long maxIter = 1_000_000_000;
		OptionalLong trialsToSuccess = test.rs.rejectionSamplerUniform3Concordant(maxIter);
		if (trialsToSuccess.isPresent()) {
			logger.info("Uniform random 3-concordant ranking system generated after " + trialsToSuccess.getAsLong() +
					" trials.");
			test.printRankTable();
			logger.info("Is it 3-concordant? " + test.rs.test3Concordance());
		} else {
			logger.info("Failed to produce any 3-concordant ranking system after " + maxIter + " trials.");
		}

		
		/*
		 * Test sampling without replacement
		 */
		int[] quad = test.g.ints(0, test.n).distinct().limit(4).toArray();
		logger.info("Sample, size 4, no repeats " + Arrays.toString(quad));
		int quadSampleSize = 1_000_000;

		int number4Cycles = test.rs.detect4CyclesFromSample(quadSampleSize);
		logger.info("In a sample of " + quadSampleSize + " 4-loops, " + number4Cycles + " 4-cycles were found.");
		double proportion4Cycles = (double) number4Cycles / (double) quadSampleSize;
		logger.info("Percentage of 4-cycles: " + (100.0 * proportion4Cycles));
	}

	/**
	 * 
	 * @param time stamp
	 */
	private void initializeHTMLLogger(long timestamp) {
		try {
			LogManager lm = LogManager.getLogManager();
			FileHandler html_handler = new FileHandler(timestamp + "-log-rs.html", true); // produced, readable
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
