/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithms;

import java.util.logging.Level;

import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;
import orientationStructures.RankingSystemBuilder;

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
	private SparseIntUndirectedWeightedGraph linkageGraph; // output
	private SparseIntDirectedWeightedGraph dataDigraph; // option 1 for input
	private SparseIntUndirectedWeightedGraph dataGraph; // option 2 for input
	private final boolean inputGraphIsDirected;
	
	/**
	 * 
	 */
	public LinkageFromWeightedGraph(boolean directedInput) {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageFromWeightedGraph.class.getName());
		this.inputGraphIsDirected = directedInput;
	}

}
