/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import logging.GlobalLog;
import main.ClusteringByInsway;

/**
 * Research tool with test classes for generating 3-concordant ranking systems.
 * Provides array-based data structures {@link getRankTable} and
 * {@link getOrderedNeighbors}, utility methods for ranking systems, and methods
 * for testing for 3-concordance.
 * <p>
 * Used to generate examples for rank-based linkage.
 * <p>
 * Includes {@link rejectionSamplerUniform3Concordant} (for up to ten objects),
 * and {@link edgeFlipProcess} (for thousands of objects).
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class ConcordanceExplorer {

	private final int n;
	private final List<Integer> points;// Objects are treated as the set S:={0, 1, 2, ..., n-1}.
	private final List<Integer> ranks; // {1, 2, ..., n}
	/*
	 * see getRankTable() method
	 */
	private int[][] rankTable, orderedNeighbors;
	private int numFlips;
	SplittableRandom g;

	/**
	 * 
	 * @param numPoints number of objects in ranking system
	 */
	public ConcordanceExplorer(int numPoints) {
		GlobalLog.log(Level.INFO, "Instance of " + ConcordanceExplorer.class.getName());
		/*
		 * List of Integers 0, 1, 2, ... ,n-1
		 */
		this.n = numPoints;
		this.numFlips = 0;
		this.points = IntStream.range(0, n).boxed().collect(Collectors.toUnmodifiableList());
		this.ranks = IntStream.range(1, n).boxed().collect(Collectors.toUnmodifiableList());
		this.rankTable = new int[n][n];
		this.orderedNeighbors = new int[n][n];
		g = new SplittableRandom();

	}

	/**
	 * Fills each row of this.rankTable with a permutation of {0, 1, 2, ..., n-1},
	 * with subtlety that this.rankTable[j][j] = 0 for all j. Hence shuffling n-1
	 * integers, not n.
	 */
	public void generateUniformRandomRankTable() {
		for (int i = 0; i < n; i++) {
			/*
			 * Integer 0 is missing from permutation
			 */
			List<Integer> permutation = new ArrayList<>(this.ranks);
			Collections.shuffle(permutation);
			/*
			 * Object i assigns rank 0 to itself, making permutation a list of size n.
			 */
			permutation.add(i, Integer.valueOf(0));
			Arrays.setAll(this.rankTable[i], y -> permutation.get(y));
		}
	}

	/**
	 * 
	 * @param maxTrials limit of number iterations
	 * @return OptionalLong containing number of iterations to success if a
	 *         3-concordant ranking table is found, or empty if not.
	 */
	public OptionalLong rejectionSamplerUniform3Concordant(long maxTrials) {
		long nTrials = 0L;
		boolean threeConcordantRanktable = false;
		while (!threeConcordantRanktable && nTrials < maxTrials) {
			this.generateUniformRandomRankTable();
			threeConcordantRanktable = this.test3Concordance();
			nTrials++;
		}
		if (threeConcordantRanktable) {
			return OptionalLong.of(nTrials);
		} else {
			return OptionalLong.empty();
		}
	}

	/**
	 * Initializes orderedNeighbors and orderedNeighbors using a random acyclic
	 * orientation.
	 */
	public void generateConcordant() {
		/*
		 * I.I.D. Uniform random variables fill the super-diagonal of an n &times; n
		 * array. Then reflect to give symmetric random matrix (eta_{i,j}).
		 */
		double[][] eta = new double[n][n];
		for (int i = 0; i < n - 1; i++) {
			eta[i][i] = 0.0;
			for (int j = i + 1; j < n; j++) {
				eta[i][j] = g.nextDouble();
				/*
				 * Symmetrize, giving a random concordant ranking
				 */
				eta[j][i] = eta[i][j];
			}
		}
		/*
		 * For each row i, convert array eta[i] into a list, and sort according to
		 * j->eta[i][j]
		 */
		IntFunction<Comparator<Integer>> sortByEta = i -> ((y, z) -> Double.compare(eta[i][y], eta[i][z]));
		/* List<List<Integer>> rowRanks = new ArrayList<>(); */
		int rowCounter = 0;
		while (rowCounter < n) {
			/*
			 * Make a new copy of list of Integers 0, 1, 2, ... ,n-1, and sort row by
			 * Comparator specific to this row. No effect on the points list.
			 */
			List<Integer> row = new ArrayList<>(this.points);
			row.sort(sortByEta.apply(rowCounter));
			/*
			 * Transfer to this.orderedNeighbors, row index = rowCounter. According to x =
			 * rowCounter, the y in position k of the row is assigned rank k.
			 */

			this.orderedNeighbors[rowCounter] = row.stream().mapToInt(Integer::intValue).toArray();
			for (int k = 0; k < n; k++) {
				this.rankTable[rowCounter][this.orderedNeighbors[rowCounter][k]] = k;
			}
			rowCounter++;
		}

	}

	/*
	 * Select point x uniformly at random. Then select rank k uniformly from {1, 2,
	 * ..., n-2}. Take y at rank k, and z at rank k+1, according to x. Switch their
	 * ranks, if no directed 3-cycle results.
	 */
	private void oneFlipUpdate() {
		int x = g.nextInt(n);
		int k = g.nextInt(n - 2) + 1; // minimum = 1, maximum = n-2
		int y = this.orderedNeighbors[x][k];
		int z = this.orderedNeighbors[x][k + 1];
		if (!this.voter2SimplexIsCyclicAfterSwitch(x, y, z)) {
			/*
			 * Switch ranks of y and z, according to x. Rankings according to y and z are
			 * not affected.
			 */
			this.rankTable[x][y] = k + 1;
			this.rankTable[x][z] = k;
			this.orderedNeighbors[x][k] = z;
			this.orderedNeighbors[x][k + 1] = y;
			this.numFlips++;
		}
	}

	/**
	 * Generates a random 3-concordant orientation of the line linkageGraph of the
	 * complete linkageGraph on n objects. Not guaranteed to generate such an object
	 * uniformly.
	 * <p>
	 * Performs a random walk on ranking systems (i.e. rank tables), where two rank
	 * tables are adjacent if one object x switches the ranks of objects y and z
	 * whose ranks differ by one.
	 * <p>
	 * Start with an acyclic orientation induced by a random ordering of the points
	 * of the line linkageGraph. In particular this is 3-concordant.
	 * <p>
	 * The random walk is "reflected at the boundary" of the 3-concordant ranking
	 * systems, in the sense that a step of the walk is disallowed if it makes some
	 * voter triangle into a directed 3-cycle.
	 * <p>
	 * Doubts about whether O(n^4) or O(n^3 log(n)) edge flips are needed. The state
	 * space of the random walk has about (n!)^n elements.
	 * <p>
	 * Tested in {@link concordanceExperiments.Test3ConcordantRandomWalk}
	 * class.
	 * <p>
	 * Storage requirement: O(n^2)
	 * <p>
	 * Run time: O(n^3). Twenty seconds at n = 1000.
	 * 
	 * @param nSteps # flips to perform before termination
	 */
	public void edgeFlipProcess(int nSteps) {
		this.numFlips = 0;
		this.generateConcordant();
		while (this.numFlips < nSteps) {
			this.oneFlipUpdate();
		}
	}

	/**
	 * DIAGNOSTIC: checks all n(n-1)(n-2)/6 2-simplices to check none are directed
	 * cycles. Method applies one-argument {@link voter2SimplexIsCyclic} method to
	 * all integers less than n^3, stopping if a directed 3-cycle is found.
	 * 
	 * @return true if ranking system is 3-concordant.
	 */
	public boolean test3Concordance() {
		/*
		 * checkSimplex returns true if {xy, yz, xz} is a directed 3-cycle
		 */
		IntPredicate checkSimplex = m -> this.voter2SimplexIsCyclic(m);
		boolean cycleFree = IntStream.range(1, n * n * n).parallel().noneMatch(checkSimplex);
		return cycleFree;
	}

	/**
	 * Tests whether the 2-simplex {xy, yz, xz} is a directed 3-cycle, using rank
	 * table. Returns false when {x, y, z} has less than three distinct elements.
	 * 
	 * @param x point
	 * @param y point, different to x
	 * @param z point, different to x and y
	 * @return true if xy &rarr; xz &rarr; yz &rarr; xy or xy &larr; xz &larr; yz
	 *         &larr; xy
	 */
	public boolean voter2SimplexIsCyclic(int x, int y, int z) {
		if (x == y || y == z || x == z) {
			return false;
		} else {
			boolean xyARROWxz = (this.rankTable[x][y] < this.rankTable[x][z]); // xy &rarr; xz
			boolean yzARROWyx = (this.rankTable[y][z] < this.rankTable[y][x]); // yz &rarr; xy
			boolean zxARROWzy = (this.rankTable[z][x] < this.rankTable[z][y]); // xz &rarr; yz
			return (xyARROWxz && yzARROWyx && zxARROWzy) || (!xyARROWxz && !yzARROWyx && !zxARROWzy);
		}
	}

	/**
	 * Applies {@link voter2SimplexIsCyclic}(x, y, z) method to x + n*y + n*n*z.
	 * Trick to allow use of IntPredicate in {@link test3Concordance}.
	 * 
	 * @param m &lt; n^3
	 * @return true if x &lt; y &lt; z and {xy, yz, xz} is a directed 3-cycle
	 */
	public boolean voter2SimplexIsCyclic(int m) {
		int x = m % n; // e.g. n = 10, m = 875, x = 5
		int r = (m - x) / n; // e.g. 87
		int y = r % n; // e.g. 7
		int s = (r - y) / n; // e.g. 8
		int z = s % n; // e.g. 8
		if ((x > y) || (y > z)) {
			return false;
		}
		/*
		 * Only check 2-simplex when x &lt; y &lt; z
		 */
		else {
			return voter2SimplexIsCyclic(x, y, z);
		}
	}

	/**
	 * Tests whether switching the ranks (under x) of points y and z will cause a
	 * directed 3-cycle
	 * 
	 * @param x base point
	 * @param y point at some rank k, according to x
	 * @param z point at rank k+1, according to x
	 * @return true if a directed 3-cycle will occur after switching the ranks
	 *         (under x) of points y and z.
	 */
	public boolean voter2SimplexIsCyclicAfterSwitch(int x, int y, int z) {
		boolean xyARROWxz = (this.rankTable[x][y] < this.rankTable[x][z]); // xy &rarr; xz
		boolean switchArrow = !xyARROWxz; // switching the ranks (under x) of points y and z.
		boolean yzARROWyx = (this.rankTable[y][z] < this.rankTable[y][x]); // yz &rarr; xy
		boolean zxARROWzy = (this.rankTable[z][x] < this.rankTable[z][y]); // xz &rarr; yz
		return (switchArrow && yzARROWyx && zxARROWzy) || (!switchArrow && !yzARROWyx && !zxARROWzy);
	}

	/**
	 * Tests whether the 3-simplex {tx, xy, yz, zt} is a directed 4-cycle, using
	 * rank table. Returns false when {x, y, z} has less than three distinct
	 * elements.
	 * <p>
	 * False unless points t, x, y, z are all different.
	 * 
	 * @param t point
	 * @param x point
	 * @param y point
	 * @param z point
	 * @return true if zt &rarr; tx &rarr; xy &rarr; yz &rarr; zt (or same with
	 *         arrows reversed)
	 */
	public boolean quadrilateralIsCyclic(int t, int x, int y, int z) {
		Set<Integer> quad = new HashSet<>(4);
		Collections.addAll(quad, t, x, y, z);
		if (quad.size() < 4) {
			return false;
		} else {
			boolean tzARROWtx = (this.rankTable[t][z] < this.rankTable[t][x]);
			boolean xtARROWxy = (this.rankTable[x][t] < this.rankTable[x][y]);
			boolean yxARROWyz = (this.rankTable[y][x] < this.rankTable[y][z]);
			boolean zyARROWzt = (this.rankTable[z][y] < this.rankTable[z][t]);
			if ((tzARROWtx && xtARROWxy && yxARROWyz && zyARROWzt)
					|| (!tzARROWtx && !xtARROWxy && !yxARROWyz && !zyARROWzt)) {
				return true;
			} else {
				return false;
			}
		}

	}

	/**
	 * Checks relative of frequency of 4-cycles; used after {@link edgeFlipProcess}
	 * to verify that orientation is no longer acyclic.
	 * 
	 * @param sampleSize how many 4-tuples to sample
	 * @return number of 4-cycles detected in random sample of 3-simplices
	 */
	public int detect4CyclesFromSample(int sampleSize) {
		int counter = 0;
		int fourCycleCount = 0;
		while (counter < sampleSize) {
			/*
			 * Sample four times from {0, 1, ..,n-1} without replacement.
			 */
			int[] q = g.ints(0, n).distinct().limit(4).toArray();
			if (this.quadrilateralIsCyclic(q[0], q[1], q[2], q[3])) {
				fourCycleCount++;
			}
			counter++;
		}
		return fourCycleCount;
	}

	/**
	 * Exhaustive enumeration of 3-concordant ranking systems when n = 4. Called by
	 * {@link concordanceExperiments.Exhaust3ConcordantOnFourPoints} test class.
	 * <p>
	 * Shows there are 450 3-concordant ranking tables on a set of four points, and
	 * 8 of them make {01, 12, 23, 03} a 4-cycle.
	 * 
	 * @return array containing count of 3-Concordant, and count of 3-Concordant
	 *         with four-cycle {01, 12, 23, 03}
	 */
	public int[] produceAll3ConcordantTables4Points() {
		int count3Concordant = 0;
		int count3ConcordantWithFourCycle0123 = 0;
		/*
		 * List six permutations of 3 objects. For cut and paste.
		 */
		int[][] perm = new int[][] { { 1, 2, 3 }, { 1, 3, 2 }, { 2, 1, 3 }, { 2, 3, 1 }, { 3, 1, 2 }, { 3, 2, 1 } };
		/*
		 * How they appear as rows of ranking table, in view of autosimilarity
		 */
		int[][][] perms = new int[4][6][4];
		perms[0] = new int[][] { { 0, 1, 2, 3 }, { 0, 1, 3, 2 }, { 0, 2, 1, 3 }, { 0, 2, 3, 1 }, { 0, 3, 1, 2 },
				{ 0, 3, 2, 1 } };
		perms[1] = new int[][] { { 1, 0, 2, 3 }, { 1, 0, 3, 2 }, { 2, 0, 1, 3 }, { 2, 0, 3, 1 }, { 3, 0, 1, 2 },
				{ 3, 0, 2, 1 } };
		perms[2] = new int[][] { { 1, 2, 0, 3 }, { 1, 3, 0, 2 }, { 2, 1, 0, 3 }, { 2, 3, 0, 1 }, { 3, 1, 0, 2 },
				{ 3, 2, 0, 1 } };
		perms[3] = new int[][] { { 1, 2, 3, 0 }, { 1, 3, 2, 0 }, { 2, 1, 3, 0 }, { 2, 3, 1, 0 }, { 3, 1, 2, 0 },
				{ 3, 2, 1, 0 } };
		int num4RankingSystems = 6 * 6 * 6 * 6;
		int[] indices = new int[4];
		for (int m = 0; m < num4RankingSystems; m++) {
			indices[0] = m % 6;
			int r0 = (m - indices[0]) / 6;
			indices[1] = r0 % 6;
			int r1 = (r0 - indices[1]) / 6;
			indices[2] = r1 % 6;
			int r2 = (r1 - indices[2]) / 6;
			indices[3] = r2 % 6;
			for (int row = 0; row < 4; row++) {
				this.rankTable[row] = perms[row][indices[row]]; // 4-tuple such as {0,1,3,2}
			}
			if (this.test3Concordance()) {
				count3Concordant++;
				if (this.quadrilateralIsCyclic(0, 1, 2, 3)) {
					count3ConcordantWithFourCycle0123++;
				}
			}
		}

		return new int[] { count3Concordant, count3ConcordantWithFourCycle0123 };
	}

	/**
	 * @return the rank Table
	 *         <p>
	 *         orderedNeighbors[x][y] = k means y is the k-th nearest neighbor of x,
	 *         according to x's ranking. The same relationship is expressed by
	 *         orderedNeighbors[x][k] = y, where the objects are treated as the set
	 *         S:={0, 1, 2, ..., n-1}, and ranks k are elements of {1, 2, ...,n-1}.
	 *         By convention orderedNeighbors[x][x] = 0.
	 */
	public int[][] getRankTable() {
		return rankTable;
	}

	/**
	 * @return the ordered Neighbors. Explained under {@link getRankTable}
	 */
	public int[][] getOrderedNeighbors() {
		return orderedNeighbors;
	}
/**
 * Logging of whether ranks and neighbor orders are consistent
 */
	public void checkConsistencyOfRanksWithNbrs() {
		int index = this.g.nextInt(this.n);
		int rank = this.g.nextInt(this.n - 1);
		int objectAtRank = this.orderedNeighbors[index][rank];
		int rankOfObject = this.rankTable[index][objectAtRank];
		GlobalLog.log(Level.INFO, "0 position among neighbors of " + index + " is " + this.orderedNeighbors[index][0]);
		GlobalLog.log(Level.INFO, "For object " + index + ", object at rank " + rank + " is " + objectAtRank);
		GlobalLog.log(Level.INFO, "Rank of this object in table is " + rankOfObject);

	}

	/**
	 * @return the number of Flips
	 */
	public int getNumFlips() {
		return numFlips;
	}

	/**
	 * 
	 * @param rankTable rank Table to be set
	 */
	public void setRankTable(int[][] rankTable) {
		this.rankTable = rankTable;
	}
}
