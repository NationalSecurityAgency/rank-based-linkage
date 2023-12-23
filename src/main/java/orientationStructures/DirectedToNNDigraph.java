/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Takes a List of {@link readWrite.StringPairWithWeight} records, indexes the
 * source and target objects by integers 0 to n &minus; 1, and builds a
 * {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph} object.
 * After extracting the two core, build K-NN digraph.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class DirectedToNNDigraph extends SparseGraphDataPreparer implements NearestNeighborsDigraphSupplier {

	private SparseIntDirectedWeightedGraph digraph, knnDigraph;
	private boolean higherWeightIsBetter;
	/**
	 * Comparator ranks edge e before edge f if weight(f) &lt; weight(e), assuming
	 * higherWeightIsBetter is true, and vice versa.
	 */
	public Comparator<Integer> edgeMoreSimilarThan;
/**
 * 
 * @param higherBetter true if higher edge weight means more similar
 */
	public DirectedToNNDigraph(boolean higherBetter) {
		GlobalLog.log(Level.INFO, "Instance of " + DirectedToNNDigraph.class.getName());
		this.higherWeightIsBetter = higherBetter;
	}

	/**
	 * Creates a sparse directed linkageGraph.
	 */
	@Override
	public void buildRawSparseGraph(List<StringPairWithWeight> edgeRecords) {
		super.setSourceTargetWeightList(edgeRecords);
		super.buildObjectIndex();
		this.digraph = new SparseIntDirectedWeightedGraph(super.nV, super.buildEdgeList());// immutable
		GlobalLog.log(Level.INFO,
				"Directed linkageGraph built, with " + super.nE + " edges, and " + super.nV + " vertices.");
		GlobalLog.log(Level.INFO,"Object-string lookup built: first string is " + this.getObjectStringLookup().get(0));
		/*
		 * Comparator ranks edge e before edge f if weight(f) &lt; weight(e) if
		 * higherWeightIsBetter is true
		 */
		if (this.higherWeightIsBetter) {
			this.edgeMoreSimilarThan = (e, f) -> Double.compare(this.digraph.getEdgeWeight(f),
					this.digraph.getEdgeWeight(e));
		} else {
			this.edgeMoreSimilarThan = (e, f) -> Double.compare(this.digraph.getEdgeWeight(e),
					this.digraph.getEdgeWeight(f));
		}
	}

	

	@Override
	public void determineTwoCore() {
		super.determineTwoCore(this.digraph);
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
			Iterator<Integer> edgeIterator = this.digraph.outgoingEdgesOf(v).iterator();
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
			 * Convert edge indices into stream of source, target, weight triples.
			 */
			return runningK.stream().map(e -> new Triple<Integer, Integer, Double>(this.digraph.getEdgeSource(e),
					this.digraph.getEdgeTarget(e), this.digraph.getEdgeWeight(e)));
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
				this.digraph.vertexSet().parallelStream().flatMap(v -> kNearNbrOfVertex(v, k)).toList());
	}

	/**
	 * @return the N-NN digraph
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph() {
		return this.knnDigraph;
	}

	/**
	 * @return the raw input digraph
	 */
	public SparseIntDirectedWeightedGraph getDigraph() {
		return digraph;
	}

}
