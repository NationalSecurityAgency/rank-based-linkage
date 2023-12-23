/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.IntUnaryOperator;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Differs from the directed version {@link DirectedToNNDigraph} in that we
 * build an undirected linkageGraph first, take its two core, compute K-NN of
 * each vertex, and write to a directed weighted linkageGraph.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class UndirectedToNNDigraph extends SparseGraphDataPreparer implements NearestNeighborsDigraphSupplier {

	private SparseIntUndirectedWeightedGraph graph;
	private SparseIntDirectedWeightedGraph knnDigraph; // for output
	/**
	 * Comparator ranks edge e before edge f if weight(f) &lt; weight(e).
	 */
	private boolean higherWeightIsBetter;
	private Comparator<Integer> edgeMoreSimilarThan;

	/**
	 * 
	 * @param higherBetter true if higher weight means more similar
	 */
	public UndirectedToNNDigraph(boolean higherBetter) {
		GlobalLog.log(Level.INFO, "Instance of " + UndirectedToNNDigraph.class.getName());
		this.higherWeightIsBetter = higherBetter;
	}

	/**
	 * Creates a sparse UNdirected linkageGraph.
	 */
	@Override
	public void buildRawSparseGraph(List<StringPairWithWeight> edgeRecords) {
		super.setSourceTargetWeightList(edgeRecords);
		super.buildObjectIndex();
		this.graph = new SparseIntUndirectedWeightedGraph(super.nV, super.buildEdgeList());
		GlobalLog.log(Level.INFO,
				"Undirected linkageGraph built, with " + super.nE + " edges, and " + super.nV + " vertices.");
		GlobalLog.log(Level.INFO, "Object-string lookup built: first string is " + this.getObjectStringLookup().get(0));
		/*
		 * Comparator ranks edge e before edge f if weight(f) &lt; weight(e) if
		 * higherWeightIsBetter is true
		 */
		if (this.higherWeightIsBetter) {
			this.edgeMoreSimilarThan = (e, f) -> Double.compare(this.graph.getEdgeWeight(f),
					this.graph.getEdgeWeight(e));
		} else {
			this.edgeMoreSimilarThan = (e, f) -> Double.compare(this.graph.getEdgeWeight(e),
					this.graph.getEdgeWeight(f));
		}
	}

	@Override
	public void determineTwoCore() {
		super.determineTwoCore(this.graph);
	}

	/**
	 * 
	 * Method to return source, target, weight triples for the k outgoing edges from
	 * source v with largest weight. If v has out-degree less than k, all out edges
	 * are returned. These streams are flattened, over all source vertices in the
	 * two core, in the {@link buildKNearNbrDigraph} method.
	 * <p>
	 * Excludes non-two core edges.
	 * 
	 * @param v source vertex
	 * @param k number to be returned
	 * @return k outgoing two core edges from source v with largest weight., or
	 *         empty stream if v is not in the two core.
	 */
	public Stream<Triple<Integer, Integer, Double>> kNearNbrOfVertex(int v, int k) {
		NavigableSet<Integer> runningK = new TreeSet<Integer>(this.edgeMoreSimilarThan);
		if (!super.vertexIsInTwoCore[v]) {
			return Stream.empty();
		} else {
			Iterator<Integer> edgeIterator = this.graph.edgesOf(v).iterator();
			int counter = 0;
			/*
			 * Maybe vertex v has no more than k outgoing edges. Insert them in sorted
			 * order, highest weight first, but exclude edges outside the two core.
			 */
			Integer f;
			while (counter < k && edgeIterator.hasNext()) {
				f = edgeIterator.next();
				if (super.edgeIsInTwoCore[f.intValue()]) {
					runningK.add(f);
					counter++;
				}
			}
			/*
			 * If there are more than k outgoing edges, iterate through remaining outgoing
			 * edges from v, inserting them if weight exceeds lowest weight among the k
			 * running best, but excluding edges outside the two core.
			 */
			while (edgeIterator.hasNext()) {
				f = edgeIterator.next();
				if (this.edgeMoreSimilarThan.compare(f, runningK.last()) < 0 && super.edgeIsInTwoCore[f.intValue()]) {
					runningK.remove(runningK.last());
					runningK.add(f);
				}
			}
			/*
			 * For top ranking k edges incident to v, convert into a stream of source,
			 * target, weight triples. Needs the notV function to detect the end point
			 * opposite to v in edge e.
			 */
			IntUnaryOperator notV = e -> (Integer.compare(this.graph.getEdgeSource(e).intValue(), v) == 0)
					? this.graph.getEdgeTarget(e)
					: this.graph.getEdgeSource(e);
			return runningK.stream()
					.map(e -> new Triple<Integer, Integer, Double>(v, notV.applyAsInt(e), this.graph.getEdgeWeight(e)));
		}
	}

	/**
	 * For every vertex in the two core, the k OUTGOING two core edges of highest
	 * weight will be placed in a list, from which a new sparse directed
	 * linkageGraph, this.knnDigraph, is built. We cannot delete some edges from
	 * this.digraph, which is immutable.
	 */
	@Override
	public void buildKNearNbrDigraph(int k) {
		GlobalLog.log(Level.INFO, "Building K-NN digraph for use by rank-based linkage.");
		this.knnDigraph = new SparseIntDirectedWeightedGraph(super.nV,
				this.graph.vertexSet().parallelStream().flatMap(v -> kNearNbrOfVertex(v, k)).toList());
	}

	/**
	 * @return the N-NN digraph
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph() {
		return this.knnDigraph;
	}

	/**
	 * @return the raw undirected linkageGraph
	 */
	public SparseIntUndirectedWeightedGraph getGraph() {
		return this.graph;
	}

}
