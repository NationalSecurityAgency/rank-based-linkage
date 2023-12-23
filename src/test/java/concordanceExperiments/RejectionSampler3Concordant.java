package concordanceExperiments;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Random;
import java.util.logging.Level;

import logging.GlobalLog;
import orientationStructures.ConcordanceExplorer;

/**
 * Test rejection sampling methods of {@link ConcordanceExplorer} class. Used to
 * acquire knowledge about 3-concordant ranking systems.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class RejectionSampler3Concordant {

	ConcordanceExplorer rs;
	Random g;
	final int n;
	static final int[] numberOfFourSubsets = new int[] { 0, 0, 0, 0, 1, 5, 15, 35, 70, 630, 6300 };

	/**
	 * 
	 * @param num size of set of points
	 */
	public RejectionSampler3Concordant(int num) {
		GlobalLog.log(Level.INFO, "Instance of " + RejectionSampler3Concordant.class.getName());
		this.n = num;
		rs = new ConcordanceExplorer(n);
		g = new Random();

	}

	void printRankTable() {
		int[][] table = this.rs.getRankTable();
		for (int r = 0; r < this.n; r++) {
			GlobalLog.log(Level.INFO, "Ranks from r: " + Arrays.toString(table[r]));
		}
	}

	/**
	 * Reports result to logger
	 * 
	 * @param args none
	 */
	public static void main(String[] args) {
		int num = 9;
		int num4subsets = numberOfFourSubsets[num];
		RejectionSampler3Concordant test = new RejectionSampler3Concordant(num);

		/*
		 * Generate a series of truly uniform 3- concordant ranking systems, and check
		 * number of times a random 4-subset of objects gives a 4-cycle
		 */
		int numberOfRankingExemplars = 300;
		double[] waitingTimes = new double[numberOfRankingExemplars];
		double[] prop4cyc = new double[numberOfRankingExemplars];
		int counter = 0;
		long maxIter = Long.MAX_VALUE;
		GlobalLog.log(Level.INFO, "Rejection sampling for 3- concordant ranking systems on " + num + " points with "
				+ numberOfRankingExemplars + " repetitions.");
		GlobalLog.log(Level.INFO, "A sample of " + num4subsets + " 4-tuples with replacement will be taken for each.");
		while (counter < numberOfRankingExemplars) {
			OptionalLong trialsToSuccess = test.rs.rejectionSamplerUniform3Concordant(maxIter);
			if (trialsToSuccess.isPresent()) {
				/*
				 * GlobalLog.log(Level.
				 * INFO,"Uniform random 3-concordant ranking system generated after " +
				 * trialsToSuccess.getAsLong() + " trials.");
				 */
				waitingTimes[counter] = (double) trialsToSuccess.getAsLong();
				/*
				 * GlobalLog.log(Level.INFO,"Is it 3-concordant? " +
				 * test.rs.test3Concordance());
				 */
			} else {
				GlobalLog.log(Level.WARNING,
						"Failed to produce any 3-concordant ranking system after " + maxIter + " trials.");
			}
			int number4Cycles = test.rs.detect4CyclesFromSample(num4subsets);
			double proportion4Cycles = (double) number4Cycles / (double) num4subsets;
			prop4cyc[counter] = proportion4Cycles;
			// GlobalLog.log(Level.INFO,number4Cycles + " 4-cycles found. Percentage of
			// 4-cycles: " + (100.0 * proportion4Cycles));
			counter++;
		}
		OptionalDouble meanWaitingTime = Arrays.stream(waitingTimes).average();
		OptionalDouble meanProportion4Cycles = Arrays.stream(prop4cyc).average();
		GlobalLog.log(Level.INFO,
				"Mean number of ranking systems before 3-concordant: " + meanWaitingTime.getAsDouble());
		GlobalLog.log(Level.INFO,
				"Average rate of finding a 4-cycle in a given ordered 4-tuple: " + meanProportion4Cycles.getAsDouble());
		/*
		 * Last example of random rank table
		 */
		if (test.n < 12) {
			GlobalLog.log(Level.INFO, "Uniform 3-concordant random rank table, n =  " + num);
			test.printRankTable();
			GlobalLog.log(Level.INFO, "Is it 3-concordant? " + test.rs.test3Concordance());
		}

	}

}
