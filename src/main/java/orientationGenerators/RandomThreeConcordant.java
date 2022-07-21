package orientationGenerators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
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

	/*
	 * rankTable[x][y] = k means y is the k-th nearest neighbor of x, according to
	 * x's ranking. The same relationship is expressed by orderedNeighbors[x][k] =
	 * y. Here the objects are treated as the set S:={0, 1, 2, ..., n-1}, and ranks
	 * k are elements of {1, 2, ...,n-1}. By convention rankTable[x][x] = 0.
	 * 
	 */
	private int[][] rankTable, orderedNeighbors;
	private List<Integer> points;
	SplittableRandom g;

	public RandomThreeConcordant(int n) {
		/*
		 * List of Integers 0, 1, 2, ... ,n-1
		 */
		points = IntStream.range(0, n).boxed().collect(Collectors.toUnmodifiableList());
		this.rankTable = new int[n][n];
		this.orderedNeighbors = new int[n][n];
		g = new SplittableRandom();
		
	}

	private void initialAcyclicOrientation(int n) {
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
		List<List<Integer>> rowRanks = new ArrayList<>();
		int rowCounter = 0;
		while (rowCounter < n) {
			/*
			 * New copy of list of Integers 0, 1, 2, ... ,n-1
			 */
			List<Integer> row = new ArrayList<>(this.points);
			/*
			 * Sort row by Comparator specific to this row. No effect on the points list.
			 */
			row.sort(sortByEta.apply(rowCounter));
			/*
			 * 
			 */
			rowRanks.add(rowCounter, row);
			rowCounter++;
		}
		/*
		 * More convenient and accessible array encoding
		 * TODO
		 */

	}

}
