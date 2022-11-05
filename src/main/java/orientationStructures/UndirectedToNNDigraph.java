package orientationStructures;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Differs from the directed version in that we build an undirected graph first,
 * take its 2 -core, compute K-NN of each vertex, and write to a directed
 * weighted graph.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class UndirectedToNNDigraph extends SparseGraphDataPreparer implements NearestNeighborsDigraphSupplier {

	private SparseIntUndirectedWeightedGraph graph;
	private SparseIntDirectedWeightedGraph knnDigraph; // for output

	public UndirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + UndirectedToNNDigraph.class.getName());
	}

	/**
	 * Creates a sparse UNdirected graph.
	 */
	@Override
	public void buildRawSparseGraph(List<StringPairWithWeight> edgeRecords) {
		super.setSourceTargetWeightList(edgeRecords);
		super.buildObjectIndex();
		this.graph = new SparseIntUndirectedWeightedGraph(super.nV, super.buildEdgeList());
		GlobalLog.log(Level.INFO,
				"Undirected graph built, with " + super.nE + " edges, and " + super.nV + " vertices.");
	}

	@Override
	public void determineTwoCore() {
		super.determineTwoCore(this.graph);
	}

	public List<Triple<Integer, Integer, Double>> buildKNearNbrEdgeList(int k) {
		return null;//TODO
	}

	/**
	 * For every vertex in the two core, the k OUTGOING two core edges of highest
	 * weight will be placed in a list, from which a new sparse directed graph,
	 * this.knnDigraph, is built. We cannot select some edges from this.graph, which
	 * is immutable.
	 */
	@Override
	public void buildKNearNbrDigraph(int k) {
		this.knnDigraph = new SparseIntDirectedWeightedGraph(super.nV, this.buildKNearNbrEdgeList(k));
	}

	@Override
	public Map<String, Integer> getVertexIndex() {
		// TODO Auto-generated method stub
		return super.getVertexIndex();
	}

	@Override
	public SparseIntDirectedWeightedGraph getKNNDigraph() {
		return this.knnDigraph;
	}

}
