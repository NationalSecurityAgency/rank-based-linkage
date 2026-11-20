/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.AbstractGraph;

import algorithms.TwoCoreIntBased;
import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Base helper for preparing sparse-graph data used by
 * {@link DirectedToNNDigraph} and {@link UndirectedToNNDigraph}.
 * <p>
 * This class stores the input edge records, builds object-to-index lookup
 * structures, converts string-labeled edges to integer-labeled triples, and
 * computes membership in the 2-core on a supplied graph instance.
 * <p>
 * Concrete subclasses are responsible for constructing the final directed or
 * undirected graph objects.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public abstract class SparseGraphDataPreparer {
	private List<String> objectStringLookup; // list of distinct Strings in Source or Target position
	private Map<String, Integer> vertexIndex; // maps string s to integer i if objectStringLookup.get(i) = s
	private List<StringPairWithWeight> stwList; // stw means "source, target, weight"
	int nV, nE; // One more than highest numbered vertex (resp. edge)
	boolean[] vertexIsInTwoCore, edgeIsInTwoCore;

	/**
	 * Creates a new preparer instance.
	 */
	public SparseGraphDataPreparer() {
		GlobalLog.log(Level.INFO, "Instance of " + SparseGraphDataPreparer.class.getName());
	}

	/**
	 * Sets the source-target-weight records used by subsequent preparation steps.
	 * <p>
	 * This is typically the first method to invoke before
	 * {@link #buildObjectIndex()} and {@link #buildEdgeList()}.
	 * 
	 * @param edgeRecords list of "source, target, weight" triples. In extensions of
	 *                    this abstract class, edges will be declared as directed or
	 *                    undirected.
	 */
	public void setSourceTargetWeightList(List<StringPairWithWeight> edgeRecords) {
		this.stwList = edgeRecords;
		this.nE = edgeRecords.size();
	}

	/**
	 * Builds an index over all source and target labels that appear in the stored
	 * edge records.
	 * <p>
	 * Both source and target columns are used because a vertex may have out-degree
	 * zero while still having positive in-degree.
	 * <p>
	 * The method populates:
	 * <ul>
	 * <li>{@code objectStringLookup}: index-to-label map via list position</li>
	 * <li>{@code vertexIndex}: label-to-index map</li>
	 * <li>{@code nV}: number of distinct vertices</li>
	 * </ul>
	 */
	public void buildObjectIndex() {
		this.objectStringLookup = this.stwList.stream().flatMap(r -> r.getSourceAndTarget().stream()).distinct()
				.collect(Collectors.toUnmodifiableList());
		this.nV = this.objectStringLookup.size();
		this.vertexIndex = IntStream.range(0, nV).boxed()
				.collect(Collectors.toUnmodifiableMap(i -> this.objectStringLookup.get(i), i -> i));
		GlobalLog.log(Level.INFO, "Vertex index built, with " + nV + " entries.");
	}

	/**
	 * Converts the stored string-labeled edge list into integer-indexed weighted
	 * triples.
	 * <p>
	 * Each output triple has the form
	 * {@code (sourceIndex, targetIndex, weight)} and is suitable for sparse-graph
	 * constructors that accept primitive integer vertex identifiers.
	 *
	 * @return list of triples suitable for input file for the
	 *         {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph}
	 *         constructor.
	 */
	public List<Triple<Integer, Integer, Double>> buildEdgeList() {
		/*
		 * Conversion from "source string, target string, weight" to
		 * "source index, target index, weight"
		 */
		Function<StringPairWithWeight, Triple<Integer, Integer, Double>> convert = stw -> new Triple<>(
				vertexIndex.get(stw.source()), vertexIndex.get(stw.target()), stw.weight());
		return this.stwList.stream().parallel().map(convert::apply).toList();
	}

	/**
	 * Construct lists of edges and vertices outside the two core, without regards
	 * to whether edges are directed or not.
	 * <p>
	 * Results are stored in {@code vertexIsInTwoCore} and
	 * {@code edgeIsInTwoCore} for later filtering by subclasses.
	 *
	 * @param abstractGraph graph on integer vertex and edge identifiers; typically a
	 *                      sparse directed or sparse undirected weighted graph in an
	 *                      extending class
	 */
	public void determineTwoCore(AbstractGraph<Integer, Integer> abstractGraph) {
		TwoCoreIntBased twoCore = new TwoCoreIntBased(abstractGraph);
		GlobalLog.log(Level.INFO, "Computing the two core, without regard to edge direction.");
		twoCore.computeTwoCore();
		GlobalLog.log(Level.INFO, "Edges removed in each round of leaf removal: "
				+ Arrays.toString(twoCore.getCountEdgesRemovedEachRound()));
		this.vertexIsInTwoCore = twoCore.getVertexIsInTwoCore();
		GlobalLog.log(Level.INFO, "Boolean array vertexIsInTwoCore has length " + this.vertexIsInTwoCore.length);
		this.edgeIsInTwoCore = twoCore.getEdgeIsInTwoCore();
		GlobalLog.log(Level.INFO, "Boolean array edgeIsInTwoCore has length " + this.edgeIsInTwoCore.length);
		GlobalLog.log(Level.INFO, "In the two-core, there are " + twoCore.getTwoCoreEdges().size() + " edges, and "
				+ twoCore.getTwoCoreVertices().size() + " vertices.");
	}

	/**
	 * Returns the distinct object labels seen in source/target positions.
	 *
	 * @return immutable list mapping internal vertex indices to original labels
	 */
	public List<String> getObjectStringLookup() {
		return objectStringLookup;
	}

	/**
	 * Returns the object-label to vertex-index lookup map.
	 *
	 * @return map from original object label to internal integer vertex index
	 */
	public Map<String, Integer> getVertexIndex() {
		return vertexIndex;
	}

	/**
	 * Returns the number of distinct vertices observed in the input records.
	 *
	 * @return number of vertices
	 */
	public int getnV() {
		return nV;
	}

	/**
	 * Returns the number of input edge records.
	 *
	 * @return number of edges
	 */
	public int getnE() {
		return nE;
	}

}
