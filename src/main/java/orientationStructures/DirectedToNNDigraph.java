/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.List;
import java.util.logging.Level;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Takes a List of {@link readWrite.StringPairWithWeight} records, indexes the
 * source and target objects by integers 0 to n &minus; 1, and builds a
 * {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph} object.
 * <p>
 * Tested 10.20.2022
 * <p>
 * TODO
 * <p>
 * (1) 2-core determination.
 * <p>
 * (2) deletion of all but K-NN
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class DirectedToNNDigraph extends SparseGraphDataPreparer implements NearestNeighborsDigraphSupplier {

	private SparseIntDirectedWeightedGraph digraph, knnDigraph;

	public DirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + DirectedToNNDigraph.class.getName());
	}

	/**
	 * Creates a sparse directed graph.
	 */
	@Override
	public void buildRawSparseGraph(List<StringPairWithWeight> edgeRecords) {
		super.setSourceTargetWeightList(edgeRecords);
		super.buildObjectIndex();
		this.digraph = new SparseIntDirectedWeightedGraph(super.nV, super.buildEdgeList());// immutable
		GlobalLog.log(Level.INFO, "Directed graph built, with " + super.nE + " edges, and " + super.nV + " vertices.");
	}

	@Override
	public void determineTwoCore() {
		super.determineTwoCore(this.digraph);
	}
	public List<Triple<Integer, Integer, Double>> buildKNearNbrEdgeList(int k) {
		GlobalLog.log(Level.INFO,"Building K-NN edge list.");
		//IntFunction<Triple<Integer, Integer, Double> > selectVertexKNN = 
	}
	/**
	 * For every vertex in the two core, the k OUTGOING two core edges of highest
	 * weight will be placed in a list, from which a new sparse directed graph,
	 * this.knnDigraph, is built. We cannot delete some edges from this.digraph,
	 * which is immutable.
	 */
	@Override
	public void buildKNearNbrDigraph(int k) {
		GlobalLog.log(Level.INFO,"Building K-NN digraph for use by rank-based linkage.");
		this.knnDigraph = new SparseIntDirectedWeightedGraph(super.nV, this.buildKNearNbrEdgeList(k));
	}
	/**
	 * @return the digraph
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph() {
		return digraph;
	}

}
