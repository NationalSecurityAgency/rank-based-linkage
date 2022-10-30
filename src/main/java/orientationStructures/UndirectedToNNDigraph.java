package orientationStructures;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Differs from the directed version in that we build an undirected graph first,
 * take its 2 -core, compute K-NN of each vertex, and write to a directed
 * gweighted graph.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class UndirectedToNNDigraph implements NearestNeighborsDigraphSupplier {

	private List<String> vertexStrings; // list of distinct elements
	private Map<String, Integer> vertexIndex; // maps string s to integer i if sourceStrings.get(i) = s
	private List<StringPairWithWeight> stwList; // stw means "source, target, weight"
	private int nV; // source vertex count
	private SparseIntUndirectedWeightedGraph graph;
	private SparseIntDirectedWeightedGraph digraph; // for output

	public UndirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + UndirectedToNNDigraph.class.getName());
	}

	@Override
	public void setSourceTargetWeightList(List<StringPairWithWeight> edgeRecords) {
		// TODO Auto-generated method stub

	}

	@Override
	public void extractTwoCore() {
		// TODO Auto-generated method stub

	}

	@Override
	public void buildObjectIndex() {
		// TODO Auto-generated method stub

	}

	@Override
	public void buildSparseDirectedGraph() {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Integer> getVertexIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SparseIntDirectedWeightedGraph getDigraph() {
		// TODO Auto-generated method stub
		return null;
	}

}
