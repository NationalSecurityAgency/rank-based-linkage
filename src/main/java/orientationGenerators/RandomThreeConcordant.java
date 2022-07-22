package orientationGenerators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomThreeConcordant {
	/**
	 * Generates a random 3-concordant orientation of the line graph of the complete
	 * graph on n objects. Not guaranteed to generate such an object uniformly.
	 * Method is to start with an acyclic orientation induced by a random ordering
	 * of the points of the line graph. Then flip edges at random until about half
	 * the edges of the line graph have been flipped at least once.
	 * <p>
	 * Storage requirement: O(n^2)
	 * <p>
	 * Run time: O(n^3).
	 * 
	 * @param n size of set S of objects.
	 */
	private final int n;
	private final List<Integer> points;// Objects are treated as the set S:={0, 1, 2, ..., n-1}.
	/*
	 * rankTable[x][y] = k means y is the k-th nearest neighbor of x, according to
	 * x's ranking. The same relationship is expressed by orderedNeighbors[x][k] =
	 * y, where the objects are treated as the set S:={0, 1, 2, ..., n-1}, and ranks
	 * k are elements of {1, 2, ...,n-1}. By convention rankTable[x][x] = 0.
	 * 
	 */
	private int[][] rankTable, orderedNeighbors;
	private int numFlips;
	SplittableRandom g;

	public RandomThreeConcordant(int numPoints) {
		/*
		 * List of Integers 0, 1, 2, ... ,n-1
		 */
		this.n = numPoints;
		this.numFlips = 0;
		this.points = IntStream.range(0, n).boxed().collect(Collectors.toUnmodifiableList());
		this.rankTable = new int[n][n];
		this.orderedNeighbors = new int[n][n];
		g = new SplittableRandom();

	}

	/**
	 * Initializes rankTable and orderedNeighbors using a random acyclic
	 * orientation.
	 */
	public void initialAcyclicOrientation() {
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

	/*
	 * Execute given number of edge flips, none causing a 3-cycle. Typical value of
	 * nSteps will be half the number n(n-1)(n-2)/2 of 1-simplices. i.e.
	 * n(n-1)(n-2)/4
	 */
	public void edgeFlipProcess(int nSteps) {
		this.numFlips = 0;
		double start = (double) System.currentTimeMillis();
		this.initialAcyclicOrientation();
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		System.out.println("Initial orientation created in " + duration + " seconds.");
		start = (double) System.currentTimeMillis();
		while (this.numFlips < nSteps) {
			this.oneFlipUpdate();
		}
		duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		System.out.println(this.numFlips + " edge flips performed in " + duration + " seconds.");
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
		 * true if {xy, yz, xz} is a directed 3-cycle
		 */
		IntPredicate checkSimplex = m -> this.voter2SimplexIsCyclic(m);
		return IntStream.range(1, n * n * n).noneMatch(checkSimplex);
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
	 * @return true if x < y < z and {xy, yz, xz} is a directed 3-cycle
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
	 * @return the rank Table
	 */
	public int[][] getRankTable() {
		return rankTable;
	}

	/**
	 * @return the ordered Neighbors
	 */
	public int[][] getOrderedNeighbors() {
		return orderedNeighbors;
	}

	/**
	 * @return the number of Flips
	 */
	public int getNumFlips() {
		return numFlips;
	}
}
