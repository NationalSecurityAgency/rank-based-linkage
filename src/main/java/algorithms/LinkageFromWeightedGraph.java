/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithms;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.jgrapht.alg.util.UnorderedPair;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;

/**
 * Vertex set of the linkage graph is integers {0, 1, 2, ..., n &minus; 1},
 * which is the vertex set of a
 * {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph} or
 * {@link org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph},
 * supplied as input.
 * <p>
 * Computation of ranks is unnecessary; just compare neighbors of vertex v
 * according to weight of outgoing edges from v.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class LinkageFromWeightedGraph {
	/*
	 * Insway is a mutable map to store the counts of 2-simplices where {x,y}
	 * (unordered) is a source.
	 */
	private Map<UnorderedPair<Integer, Integer>, Double> insway;
	/*
	 * Graph to be built from links of positive insway.
	 */
	private SparseIntUndirectedWeightedGraph linkageGraph; // output
	/*
	 * Only one of these input graphs will be used.
	 */
	private SparseIntDirectedWeightedGraph dataDigraph; // option 1 for input
	private SparseIntUndirectedWeightedGraph dataGraph; // option 2 for input
	private final boolean inputGraphIsDirected, higherWeightMoreSimilar;
	/*
	 * f[i] is a binary operator on integers. <p> f[i][j][k] &lt; 0 if ranks satisfy
	 * r_i[j] &lt; r_i[k] <p> f[i][j][k] &gt; 0 if ranks satisfy r_i[j] &gt; r_i[k]
	 */
	// public IntFunction<IntBinaryOperator> moreSimilarThan;

	/**
	 * 
	 * @param inputGraphIsDirected    true if input triples were DIRECTED edges.
	 * @param higherWeightMoreSimilar true if the higher of two weights of outgoing
	 *                                edges from vertex x points to vertex more
	 *                                similar to vertex x.
	 */
	public LinkageFromWeightedGraph(boolean inputGraphIsDirected, boolean higherWeightMoreSimilar) {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageFromWeightedGraph.class.getName());
		this.inputGraphIsDirected = inputGraphIsDirected;
		this.higherWeightMoreSimilar = higherWeightMoreSimilar;
		this.insway = new HashMap<>();
	}

	/**
	 * DIRECTED case
	 * 
	 * @param digraph for input
	 * @throws IOException if an undirected graph was expected
	 */
	public void loadDataDigraph(SparseIntDirectedWeightedGraph digraph) throws IOException {
		if (this.inputGraphIsDirected) {
			this.dataDigraph = digraph;
		} else {
			throw new IOException("An undirected weighted graph is expected.");
		}
	}

	/**
	 * UNDIRECTED case
	 * 
	 * @param graph for input
	 * @throws IOException if a directed graph was expected
	 */
	public void loadDataGraph(SparseIntUndirectedWeightedGraph graph) throws IOException {
		if (!this.inputGraphIsDirected) {
			this.dataGraph = graph;
		} else {
			throw new IOException("A directed weighted graph is expected.");
		}
	}

	/**
	 * WORKS FOR ALL:
	 * <p>
	 * Directed or undirected graph.
	 * <p>
	 * Weights indicate similarity or dissimilarity.
	 * 
	 * @param i base vertex
	 * @param j neighbor of i
	 * @param k neighbor of k
	 * @return true if j is more similar to i than k is to i
	 */
	public boolean moreSimilarTo(int i, int j, int k) {
		double wij, wik;
		if (this.inputGraphIsDirected) {
			wij = this.dataDigraph.getEdgeWeight(this.dataDigraph.getEdge(i, j));
			wik = this.dataDigraph.getEdgeWeight(this.dataDigraph.getEdge(i, k));
		} else {
			wij = this.dataGraph.getEdgeWeight(this.dataGraph.getEdge(i, j));
			wik = this.dataGraph.getEdgeWeight(this.dataGraph.getEdge(i, k));
		}
		if (this.higherWeightMoreSimilar) {
			return wij > wik;
		} else {
			return wij < wik;
		}
	}

	/*
	 * IN A MUDDLE!
	 * True if xz is the source in the 2-simplex {xz, yz, xy}. Invoked only when x
	 * and z are mutual friends; however y need be a friend of neither x nor z.
	 */
	private boolean firstElementIsSource(UnorderedPair<Integer, Integer> xz, int y) {
		int x = xz.getFirst();
		int z = xz.getSecond();
		boolean yFriendOfx;
		boolean yFriendOfz;
		//SEPARATE METHOD HERE
		if (this.inputGraphIsDirected) {
			yFriendOfx = this.dataDigraph.containsEdge(x,y);
			yFriendOfz = this.dataDigraph.containsEdge(z,y);
		} else {
			yFriendOfx = this.dataGraph.containsEdge(x,y);
			yFriendOfz = this.dataGraph.containsEdge(z,y);
		}
		boolean xzBeatsxy;
		boolean xzBeatsyz;
		return false;
	}

	/**
	 * Rank-based linkage is implemented here.
	 */
	public void computeInsway() {
		/*
		 * Temporary store of {x, z} pairs visited for this y
		 */
		Set<UnorderedPair<Integer, Integer>> visitedPairs = new HashSet<>();

	}

}
