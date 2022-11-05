/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.AbstractGraph;

import algorithms.TwoCore;
import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Contains common methods of the classes {@link OldDirectedToNNDigraph} and
 * {@link UndirectedToNNDigraph}, such as building a vertex index, but no graphs
 * are built yet. The two core construction occurs with a generic AbstractGraph
 * as parameter.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public abstract class SparseGraphDataPreparer {
	private List<String> sourceTargetStrings; // list of distinct Strings in Source or Target position
	private Map<String, Integer> vertexIndex; // maps string s to integer i if sourceTargetStrings.get(i) = s
	private List<StringPairWithWeight> stwList; // stw means "source, target, weight"
	public int nV, nE; // source vertex count, edge count
	public Set<Integer> nonCoreEdges;//Visibility issue? Use getter?
	public Set<Integer> nonCoreVertices;

	/**
	 * 
	 */
	public SparseGraphDataPreparer() {
		GlobalLog.log(Level.INFO, "Instance of " + SparseGraphDataPreparer.class.getName());
	}

	/**
	 * First method to invoke.
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
	 * Build an index for all the source and target strings, which refer to
	 * vertices.
	 * <p>
	 * WARNING: Not enough to include sources only. Possibly some vertex has
	 * out-degree zero, but positive in-degree.
	 * <p>
	 * Preparation for making a graph with vertices indexed by integers 0 to n
	 * &minus 1
	 */
	public void buildObjectIndex() {
		this.sourceTargetStrings = this.stwList.stream().flatMap(r -> r.getSourceAndTarget().stream()).distinct()
				.collect(Collectors.toUnmodifiableList());
		this.nV = this.sourceTargetStrings.size();
		this.vertexIndex = IntStream.range(0, nV).boxed()
				.collect(Collectors.toUnmodifiableMap(i -> this.sourceTargetStrings.get(i), i -> i));
		GlobalLog.log(Level.INFO, "Vertex index built, with " + nV + " entries.");
	}

	/**
	 * Purpose: input file for the
	 * {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph}
	 * constructor.
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
	 *
	 * @param abstractGraph will be a SparseIntDirectedWeightedGraph or
	 *                      SparseIntUndirectedWeightedGraph in an extending class.
	 */
	public void determineTwoCore(AbstractGraph<Integer, Integer> abstractGraph) {
		TwoCore<Integer, Integer> twoCore = new TwoCore<>(abstractGraph);
		GlobalLog.log(Level.INFO, "Computing the two core, without regard to edge direction.");
		twoCore.computeTwoCore();
		GlobalLog.log(Level.INFO, "Edges removed in each round of leaf removal: "
				+ Arrays.toString(twoCore.getCountEdgesRemovedEachRound()));
		this.nonCoreEdges = new HashSet<>(twoCore.getAllNonCoreEdges());
		this.nonCoreVertices = twoCore.getNonCoreVertices();
		GlobalLog.log(Level.INFO, "OUSIDE the two-core, there are " + this.nonCoreEdges.size() + " edges, and "
				+ this.nonCoreVertices.size() + " vertices.");
	}

	/**
	 * @return the sourceTargetStrings as list of distinct objects
	 */
	public List<String> getSourceTargetStrings() {
		return sourceTargetStrings;
	}

	/**
	 * @return the vertexIndex lookup map
	 */
	public Map<String, Integer> getVertexIndex() {
		return vertexIndex;
	}

	/**
	 * @return the number of source and target vertices
	 */
	public int getnV() {
		return nV;
	}

	/**
	 * @return the number of edges
	 */
	public int getnE() {
		return nE;
	}

}
