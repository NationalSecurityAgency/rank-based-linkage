package concordanceExperiments;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.IntStream;

import logging.GlobalLog;
import orientationStructures.ConcordanceExplorer;
import readWrite.OrderedNeighborsToEdgesCSVWriter;

/**
 * Experiment driver for the 3-concordant random-walk generator in
 * {@link ConcordanceExplorer}.
 * <p>
 * The program initializes rankings, verifies 3-concordance, runs the edge-flip
 * process, performs consistency and cycle diagnostics, and optionally exports a
 * k-nearest-neighbor edge list to CSV for downstream rank-based linkage.
 * <p>
 * Example runs indicate high throughput (for example, hundreds of millions of
 * flips at {@code n = 1000} on suitable hardware).
 * <p>
 * WARNING: integer-overflow risks appear for larger {@code n} when expressions
 * involving {@code n^3} are evaluated with {@code int} arithmetic.
 *
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public class Test3ConcordantRandomWalk {

	ConcordanceExplorer r3c;
	Random g;
	final int n;
	final double num1Simplices;

	/**
	 * Creates a test runner for a fixed number of objects.
	 *
	 * @param num size of the point/object set used in the ranking system
	 */
	public Test3ConcordantRandomWalk(int num) {
		GlobalLog.log(Level.INFO, "Instance of " + Test3ConcordantRandomWalk.class.getName());
		this.n = num;
		double nd = (double) n;
		this.num1Simplices = nd * (nd - 1.0) * (nd - 2.0) / 2.0;
		r3c = new ConcordanceExplorer(n);
		g = new Random();
	}

	/**
	 * Logs the current rank table row-by-row.
	 */
	void printRankTable() {
		int[][] table = this.r3c.getRankTable();
		for (int r = 0; r < this.n; r++) {
			GlobalLog.log(Level.INFO, "Ranks from r: " + Arrays.toString(table[r]));
		}
	}

	/**
	 * Searches for and logs one directed 3-cycle if present.
	 * <p>
	 * Intended as a diagnostic helper when a ranking system fails the
	 * 3-concordance test.
	 */
	void report3Cycle() {
		OptionalInt c = IntStream.range(1, n * n * n).parallel().filter(m -> r3c.voter2SimplexIsCyclic(m)).findAny();
		if (c.isPresent()) {
			int m = c.getAsInt();
			int x = m % n; // e.g. n = 10, m = 875, x = 5
			int r = (m - x) / n; // e.g. 87
			int y = r % n; // e.g. 7
			int s = (r - y) / n; // e.g. 8
			int z = s % n; // e.g. 8
			GlobalLog.log(Level.WARNING, "2-simplex {" + x + ", " + y + ", " + z + "} is a directed cycle.");
		}
	}

	/**
	 * Runs the end-to-end random-walk experiment and reports results to the logger.
	 * <p>
	 * Workflow:
	 * <ol>
	 * <li>Generate and optionally print random rank tables.</li>
	 * <li>Initialize a concordant ranking and verify 3-concordance.</li>
	 * <li>Run an edge-flip process for a random number of steps.</li>
	 * <li>Perform consistency checks and cycle diagnostics.</li>
	 * <li>Write the first {@code k} ordered neighbors per vertex to a CSV edge
	 * file.</li>
	 * <li>Estimate 4-cycle frequency from random 4-vertex samples.</li>
	 * </ol>
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		int num = 1000;
		Test3ConcordantRandomWalk test = new Test3ConcordantRandomWalk(num);
		/*
		 * Generate a uniform random rank table
		 */
		test.r3c.generateUniformRandomRankTable();
		if (test.n < 10) {
			GlobalLog.log(Level.INFO, "Uniform random rank table, not necessarily 3-concordant");
			test.printRankTable();
			GlobalLog.log(Level.INFO, "Is it 3-concordant? " + test.r3c.test3Concordance());
		}
		/*
		 * Check initialization
		 */
		test.r3c.generateConcordant();
		if (test.n < 10) {
			GlobalLog.log(Level.INFO, "Rank table of random topological sort of 0-simplices");
			test.printRankTable();
		}
		double start = (double) System.currentTimeMillis();
		boolean threeConcordant = test.r3c.test3Concordance();
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "Time to check all voter triangles are cycle-free: " + duration + " seconds.");
		GlobalLog.log(Level.INFO, "3-concordant at initialization? " + threeConcordant);
		if (!threeConcordant) {
			test.report3Cycle();
		}
		/*
		 * Prepare for edge flip process
		 */
		double meanSteps = (double) test.num1Simplices / 2.0;
		double sdSteps = Math.sqrt(meanSteps);
		int numSteps = (int) Math.round(meanSteps + sdSteps * test.g.nextGaussian());
		GlobalLog.log(Level.INFO,
				"n = " + test.n + ": # 1-simplices = " + test.num1Simplices + "; random # of steps: " + numSteps);
		/*
		 * Perform edge flip process
		 */
		start = (double) System.currentTimeMillis();
		test.r3c.edgeFlipProcess(numSteps);
		duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, test.r3c.getNumFlips() + " edge flips performed in " + duration + " seconds.");
		if (test.n < 10) {
			test.printRankTable();
		}
		/*
		 * Routine checks of ranking system result.
		 */
		test.r3c.checkConsistencyOfRanksWithNbrs();
		boolean threeConcordantAfter = test.r3c.test3Concordance();
		GlobalLog.log(Level.INFO, "3-concordant after edge flips? " + threeConcordantAfter);
		/*
		 * Write rank table to CSV file
		 */
		int numNbrs = 64;
		OrderedNeighborsToEdgesCSVWriter rankTableWriter = new OrderedNeighborsToEdgesCSVWriter();
		rankTableWriter.setOrderedNeighbors(test.r3c.getRankTable());
		String outFileName = num + "-objects-" + test.r3c.getNumFlips() + "-edge-flips-" + numNbrs + "-NN" + ".csv";
		rankTableWriter.writeFirstKNeighborsToEdgeFile(numNbrs, outFileName);
		/*
		 * Test sampling without replacement
		 */
		int[] quad = test.g.ints(0, test.n).distinct().limit(4).toArray();
		GlobalLog.log(Level.INFO, "Sample, size 4, no repeats " + Arrays.toString(quad));
		int quadSampleSize = 1000000;

		int number4Cycles = test.r3c.detect4CyclesFromSample(quadSampleSize);
		GlobalLog.log(Level.INFO,
				"In a sample of " + quadSampleSize + " 3-simplices, " + number4Cycles + " 4-cycles were found.");
		double proportion4Cycles = (double) number4Cycles / (double) quadSampleSize;
		GlobalLog.log(Level.INFO, "Percentage of 4-cycles: " + (100.0 * proportion4Cycles));

	}

}