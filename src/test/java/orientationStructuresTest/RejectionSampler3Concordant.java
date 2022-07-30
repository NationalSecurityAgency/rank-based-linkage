package orientationStructuresTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import orientationStructures.RankingSystem;
import utilities.HTMLFormatter;

/**
 * Test rejection sampling methods of {@link RankingSystem} class. Used to
 * acquire knowledge about 3-concordant ranking systems.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class RejectionSampler3Concordant {

	static Logger logger = Logger.getLogger(RejectionSampler3Concordant.class.getName());
	RankingSystem rs;
	Random g;
	final int n;
	static final int[] numberOfFourSubsets = new int[] { 0, 0, 0, 0, 1, 5, 15, 35, 70, 630 };

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
		int num = 8;
		int num4subsets = numberOfFourSubsets[num];
		RejectionSampler3Concordant test = new RejectionSampler3Concordant(num);

		/*
		 * Generate a series of truly uniform 3- concordant ranking systems, and check
		 * number of times a random 4-subset of objects gives a 4-cycle
		 */
		int numberOfRankingExemplars = 1000;
		double[] waitingTimes = new double[numberOfRankingExemplars];
		double[] prop4cyc = new double[numberOfRankingExemplars];
 		int counter = 0;
		long maxIter = 1_000_000_000;
		logger.info("Rejection sampling for 3- concordant ranking systems on " + num + " points with " + numberOfRankingExemplars
				+ " repetitions.");
		logger.info("A sample of " + num4subsets + " 4-tuples with replacement will be taken for each.");
		while (counter < numberOfRankingExemplars) {
			OptionalLong trialsToSuccess = test.rs.rejectionSamplerUniform3Concordant(maxIter);
			if (trialsToSuccess.isPresent()) {
				/*
				 * logger.info("Uniform random 3-concordant ranking system generated after " +
				 * trialsToSuccess.getAsLong() + " trials.");
				 */
				waitingTimes[counter] = (double)trialsToSuccess.getAsLong();
				/* logger.info("Is it 3-concordant? " + test.rs.test3Concordance()); */
			} else {
				logger.warning("Failed to produce any 3-concordant ranking system after " + maxIter + " trials.");
			}
			int number4Cycles = test.rs.detect4CyclesFromSample(num4subsets);
			double proportion4Cycles = (double) number4Cycles / (double) num4subsets;
			prop4cyc[counter] = proportion4Cycles;
			//logger.info(number4Cycles + " 4-cycles found. Percentage of 4-cycles: " + (100.0 * proportion4Cycles));
			counter++;
		}
		OptionalDouble meanWaitingTime = Arrays.stream(waitingTimes).average();
		OptionalDouble meanProportion4Cycles = Arrays.stream(prop4cyc).average();
		logger.info("Mean number of ranking systems before 3-concordant: " + meanWaitingTime.getAsDouble());
		logger.info("Average rate of finding a 4-cycle in a given ordered 4-tuple: " + meanProportion4Cycles.getAsDouble());
		/*
		 * Last example of random rank table
		 */
		if (test.n < 10) {
			logger.info("Uniform 3-concordant random rank table ");
			test.printRankTable();
			logger.info("Is it 3-concordant? " + test.rs.test3Concordance());
		}
		
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
