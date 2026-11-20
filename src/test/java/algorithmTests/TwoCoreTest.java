/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedGraph;

import algorithms.TwoCoreIntBased;

/**
 * Console-based test driver for {@link TwoCoreIntBased} on a random
 * {@link SparseIntUndirectedGraph}.
 * <p>
 * The test builds a random graph, computes its 2-core, and prints graph
 * neighborhoods plus 2-core vertices/edges and per-round edge-removal counts.
 *
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public class TwoCoreTest {

	SparseIntUndirectedGraph graph;
	TwoCoreIntBased tc;
	Random g;

	/**
	 * Creates a random graph test instance and immediately computes its 2-core.
	 *
	 * @param n number of vertices in the random undirected graph
	 * @param m number of random edges generated
	 */
	public TwoCoreTest(int n, int m) {
		g = new Random();
		this.graph = new SparseIntUndirectedGraph(n, this.edgeList(n, m));
		this.tc = new TwoCoreIntBased(this.graph);
		this.tc.computeTwoCore();

	}

	/**
	 * Generates {@code m} random unordered vertex pairs for an undirected graph on
	 * {@code n} vertices.
	 * <p>
	 * Self-loops are avoided; duplicate pairs may occur.
	 *
	 * @param n number of vertices
	 * @param m number of edge pairs to generate
	 * @return list of random endpoint pairs
	 */
	List<Pair<Integer, Integer>> edgeList(int n, int m) {
		List<Pair<Integer, Integer>> randomPairs = new ArrayList<>();
		int s, t;
		for (int j = 0; j < m; j++) {
			s = g.nextInt(n);
			t = g.nextInt(n - 1);
			if (t >= s) {
				t++;
			}
			randomPairs.add(new Pair<Integer, Integer>(Integer.valueOf(s), Integer.valueOf(t)));
		}
		return randomPairs;
	}

	/**
	 * Runs one random-graph experiment and prints graph and 2-core diagnostics.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		int n = 200;
		int m = 201;
		System.out.println();
		TwoCoreTest test = new TwoCoreTest(n, m);
		test.reportGraph();
		test.reportCore();
	}

	/**
	 * Prints each vertex and its neighbor list to standard output.
	 */
	public void reportGraph() {
		for (Integer v : this.graph.vertexSet()) {
			System.out.print("Neighbors of vertex " + v + ": ");
			for (Integer u : Graphs.neighborListOf(this.graph, v)) {
				System.out.print(u + ", ");
			}
			System.out.println();
		}
		System.out.println();
	}

	/**
	 * Prints computed 2-core vertices/edges and edge-removal counts by round.
	 */
	public void reportCore() {
		System.out.println("Two-core vertices:");
		for (Integer v : this.tc.getTwoCoreVertices()) {
			System.out.print(v + ", ");
		}
		System.out.println();
		System.out.println("Two-core edges:");
		for (Integer e : this.tc.getTwoCoreEdges()) {
			System.out.println("{" + this.graph.getEdgeSource(e) + ", " + this.graph.getEdgeTarget(e) + "}, ");
		}
		System.out.println();
		System.out.println("Numbers of edges removed per round:");
		for (int i : this.tc.getCountEdgesRemovedEachRound()) {
			System.out.print(i + ", ");
		}
		System.out.println();
	}

}