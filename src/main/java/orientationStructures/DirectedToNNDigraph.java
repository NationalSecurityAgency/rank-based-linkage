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
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import algorithms.TwoCore;
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
 * (1) 2-core extraction.
 * <p>
 * (2) deletion of all but K-NN
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class DirectedToNNDigraph implements NearestNeighborsDigraphSupplier {

	private List<String> sourceStrings; // list of distinct elements
	private Map<String, Integer> vertexIndex; // maps string s to integer i if sourceStrings.get(i) = s
	private List<StringPairWithWeight> stwList; // stw means "source, target, weight"
	private int nV; // source vertex count
	private SparseIntDirectedWeightedGraph digraph;

	public DirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + DirectedToNNDigraph.class.getName());
	}

	/**
	 * First method to invoke.
	 * 
	 * @param edgeRecords list of "source, target, weight" triples
	 */
	public void setSourceTargetWeightList(List<StringPairWithWeight> edgeRecords) {
		this.stwList = edgeRecords;

	}

	/**
	 * Preparation for making a graph with vertices indexed by integers 0 to n
	 * &minus 1
	 */
	public void buildObjectIndex() {
		this.sourceStrings = this.stwList.stream().map(r -> r.source()).distinct()
				.collect(Collectors.toUnmodifiableList());
		this.nV = this.sourceStrings.size();
		this.vertexIndex = IntStream.range(0, nV).boxed()
				.collect(Collectors.toUnmodifiableMap(i -> this.sourceStrings.get(i), i -> i));
		GlobalLog.log(Level.INFO, "Vertex index built, with " + nV + " entries.");
	}

	/*
	 * Purpose: input file for the {@link
	 * org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph} constructor.
	 */
	private List<Triple<Integer, Integer, Double>> buildEdgeList() {
		/*
		 * Conversion from "source string, target string, weight" to
		 * "source index, target index, weight"
		 */
		Function<StringPairWithWeight, Triple<Integer, Integer, Double>> convert = stw -> new Triple<>(
				vertexIndex.get(stw.source()), vertexIndex.get(stw.target()), stw.weight());
		return this.stwList.stream().parallel().map(convert::apply).toList();
	}

	/**
	 * Second method to invoke. Creates a directed graph.
	 */
	public void buildSparseDirectedGraph() {
		this.buildObjectIndex();
		this.digraph = new SparseIntDirectedWeightedGraph(this.nV, this.buildEdgeList());
		int nE = this.digraph.edgeSet().size();
		GlobalLog.log(Level.INFO, "Directed graph built, with " + nE + " edges");
	}
	@Override
	public void extractTwoCore() {
		TwoCore<Integer, Integer> twoCore = new TwoCore<>(this.digraph);
		GlobalLog.log(Level.INFO, "Computing the two core, without regard to edge direction.");
		twoCore.computeTwoCore();
		GlobalLog.log(Level.INFO, "Edges removed in each round of leaf removal: "
				+ Arrays.toString(twoCore.getCountEdgesRemovedEachRound()));
		/*
		 * java.lang.UnsupportedOperationException: this graph is unmodifiable
		 * Hence exclusion will have to occur at kNN cut down phase.
		 */
		this.digraph.removeAllEdges(twoCore.getAllNonCoreEdges());
		this.digraph.removeAllVertices(twoCore.getNonCoreVertices());
		GlobalLog.log(Level.INFO, "Two core contains " + this.digraph.edgeSet().size() + " edges, and "
				+ this.digraph.vertexSet().size() + " vertices.");
	}

	@Override
	public void trimToKNearNbrDigraph(int k) {

	}

	/**
	 * @return the sourceStrings as list of distinct objects
	 */
	public List<String> getSourceStrings() {
		return sourceStrings;
	}

	/**
	 * @return the vertexIndex lookup map
	 */
	public Map<String, Integer> getVertexIndex() {
		return vertexIndex;
	}

	/**
	 * @return the number of source vertices
	 */
	public int getnV() {
		return nV;
	}

	/**
	 * @return the digraph
	 */
	public SparseIntDirectedWeightedGraph getDigraph() {
		return digraph;
	}

}
