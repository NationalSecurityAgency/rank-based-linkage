/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithms;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.jgrapht.alg.util.UnorderedPair;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;

/**
 * Takes {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph}
 * object, with vertex set {0, 1, 2, ..., n &minus; 1}. Even when rankings are
 * built from edge weights of an undirected graph, the relation "is one of the k
 * nearest neighbors of" is not symmetric. Therefore a directed weighted graph
 * type is needed for input storage.
 * <p>
 * Call z a friend of x it is one of the k nearest neighbors of x.
 * <p>
 * Starts by computing the graph whose edges are pairs of mutual friends. Take
 * connected components. A linkage graph is computed on each component. This
 * means computing in-sway for each pair of mutual friends.
 * <p>
 * Turn the pairs of mutual friends {x, z} into a stream, and count for each {x,
 * z} the number of 2-simplices (xz, xy, yz} in which xz is the source. Here at
 * least one of x and z must be a friend of y.
 * <p>
 * Computation of ranks is unnecessary; just compare neighbors of vertex v
 * according to weight of outgoing edges from v.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class LinkageFromWeightedGraph {
	private SparseIntDirectedWeightedGraph dataDigraph;
	private final boolean higherWeightMoreSimilar;
	private SparseIntUndirectedGraph mutualFriendGraph;

	/*
	 * Insway is a mutable map to store the counts of 2-simplices where {x,y}
	 * (unordered) is a source.
	 */
	private Map<UnorderedPair<Integer, Integer>, Double> insway;
	/*
	 * Graph to be built from links of positive insway.
	 */
	private SparseIntUndirectedWeightedGraph linkageGraph; // output

	/**
	 * 
	 * @param higherWeightMoreSimilar true if the higher of two weights of outgoing
	 *                                edges from vertex x points to vertex more
	 *                                similar to vertex x.
	 */
	public LinkageFromWeightedGraph(SparseIntDirectedWeightedGraph digraph, boolean higherWeightMoreSimilar) {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageFromWeightedGraph.class.getName());
		this.dataDigraph = digraph;
		this.higherWeightMoreSimilar = higherWeightMoreSimilar;
		this.insway = new HashMap<>();
	}

	/**
	 * WORKS FOR ALL:
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
		wij = this.dataDigraph.getEdgeWeight(this.dataDigraph.getEdge(i, j));
		wik = this.dataDigraph.getEdgeWeight(this.dataDigraph.getEdge(i, k));
		if (this.higherWeightMoreSimilar) {
			return wij > wik;
		} else {
			return wij < wik;
		}
	}

	/*
	 * 
	 * True if xz is the source in the 2-simplex {xz, yz, xy}. Invoked only when x
	 * and z are mutual friends; however y need be a friend of neither x nor z.
	 */
	private boolean xzIsSource(UnorderedPair<Integer, Integer> xz, int y) {
		int x = xz.getFirst();
		int z = xz.getSecond();
		boolean xzBeatsxy = (!this.dataDigraph.containsEdge(x, y)) || this.moreSimilarTo(x, z, y);
		boolean xzBeatsyz = (!this.dataDigraph.containsEdge(z, y)) || this.moreSimilarTo(z, x, y);
		return xzBeatsxy && xzBeatsyz;
	}

}
