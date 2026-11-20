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
 * Builds a directed weighted k-nearest-neighbor digraph from undirected
 * weighted edge data.
 * <p>
 * Compared with {@link DirectedToNNDigraph}, this class first builds an
 * undirected sparse graph, computes its 2-core, then selects top-{@code k}
 * neighbors for each eligible vertex and emits those selections into a directed
 * weighted output digraph.
 * <p>
 * The interpretation of edge weight ordering is controlled by the
 * {@code higherBetter} constructor flag.
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
	 * Creates a converter from undirected weighted graph data to a directed k-NN
	 * digraph.
	 *
	 * @param higherBetter true if larger edge weight means greater similarity;
	 *                     false if smaller weight means greater similarity
	 */
	public UndirectedToNNDigraph(boolean higherBetter) {
		GlobalLog.log(Level.INFO, "Instance of " + UndirectedToNNDigraph.class.getName());
		this.higherWeightIsBetter = higherBetter;
	}

	/**
	 * Builds the raw sparse undirected weighted graph from edge records and prepares
	 * the similarity comparator.
	 * <p>
	 * This method initializes inherited source/target indexing and then constructs
	 * the graph used by subsequent 2-core and k-NN steps.
	 *
	 * @param edgeRecords input weighted edge records
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

	/**
	 * Computes 2-core membership on the current raw undirected graph.
	 */
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
	 * @param k maximum number of neighbors to return
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
	 *
	 * @param k number of outgoing nearest neighbors retained per vertex
	 */
	@Override
	public void buildKNearNbrDigraph(int k) {
		GlobalLog.log(Level.INFO, "Building K-NN digraph for use by rank-based linkage.");
		this.knnDigraph = new SparseIntDirectedWeightedGraph(super.nV,
				this.graph.vertexSet().parallelStream().flatMap(v -> kNearNbrOfVertex(v, k)).toList());
	}

	/**
	 * Returns the constructed directed weighted k-nearest-neighbor digraph.
	 *
	 * @return k-NN digraph
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph() {
		return this.knnDigraph;
	}

	/**
	 * Returns the raw sparse undirected weighted graph built from input records.
	 *
	 * @return raw undirected graph used as k-NN source
	 */
	public SparseIntUndirectedWeightedGraph getGraph() {
		return this.graph;
	}

}
