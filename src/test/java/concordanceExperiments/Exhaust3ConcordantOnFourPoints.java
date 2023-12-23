package concordanceExperiments;

import java.util.Arrays;
import java.util.logging.Level;

import logging.GlobalLog;
import orientationStructures.ConcordanceExplorer;

/**
 * Count 3-concordant ranking systems on 4 points. In how many is {01, 12, 23, 03} a 4-cycle?
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class Exhaust3ConcordantOnFourPoints {

	ConcordanceExplorer r3c;
	final int n;

	/**
	 * Exhaust 3-concordant rankings on 4 points
	 */
	public Exhaust3ConcordantOnFourPoints() {
		GlobalLog.log(Level.INFO, "Instance of " + Exhaust3ConcordantOnFourPoints.class.getName());
		this.n = 4;
		r3c = new ConcordanceExplorer(n);

	}

	void printRankTable() {
		int[][] table = this.r3c.getRankTable();
		for (int r = 0; r < this.n; r++) {
			GlobalLog.log(Level.INFO, "Ranks from r: " + Arrays.toString(table[r]));
		}
	}

	void fourExhaust() {
		int[] fourExhaust = this.r3c.produceAll3ConcordantTables4Points();
		GlobalLog.log(Level.INFO, "Number of 3-concordant ranking tables on 4 points: " + fourExhaust[0]);
		GlobalLog.log(Level.INFO, "Of these, number in which {01, 12, 23, 03} is a 4-cycle: " + fourExhaust[1]);
		double p4cycle = (double) fourExhaust[1] / (double) fourExhaust[0];
		GlobalLog.log(Level.INFO,
				"Sampling a 3-simplex in a uniform 3-concordant ranking system: 4-cycle probability: " + p4cycle);
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
		test.r3c.generateConcordant();
		if (test.n < 10) {
			test.printRankTable();
		}
		test.fourExhaust();
	}

}
