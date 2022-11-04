/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package deprecated;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import algorithms.TwoCore;
import logging.GlobalLog;
import orientationStructures.NearestNeighborsDigraphSupplier;
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
public class OldDirectedToNNDigraph implements NearestNeighborsDigraphSupplier {

	private List<String> sourceTargetStrings; // list of distinct Strings in Source or Target position
	private Map<String, Integer> vertexIndex; // maps string s to integer i if sourceTargetStrings.get(i) = s
	private List<StringPairWithWeight> stwList; // stw means "source, target, weight"
	private List<Integer> nonCoreEdges;
	private Set<Integer> nonCoreVertices;
	private int nV, nE; // source vertex count, edge count
	private SparseIntDirectedWeightedGraph digraph;

	public OldDirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + OldDirectedToNNDigraph.class.getName());
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
		this.nE = this.digraph.edgeSet().size();
		GlobalLog.log(Level.INFO, "Directed graph built, with " + nE + " edges, and " + this.nV + " vertices.");
	}

	@Override
	public void determineTwoCore() {
		TwoCore<Integer, Integer> twoCore = new TwoCore<>(this.digraph);
		GlobalLog.log(Level.INFO, "Computing the two core, without regard to edge direction.");
		twoCore.computeTwoCore();
		GlobalLog.log(Level.INFO, "Edges removed in each round of leaf removal: "
				+ Arrays.toString(twoCore.getCountEdgesRemovedEachRound()));
		this.nonCoreEdges = twoCore.getAllNonCoreEdges();
		this.nonCoreVertices = twoCore.getNonCoreVertices();
		/*
		 * this.digraph is unmodifiable. Hence exclusion will have to occur at kNN cut
		 * down phase.
		 */
		GlobalLog.log(Level.INFO, "OUSIDE the two-core, there are " + this.nonCoreEdges.size() + " edges, and "
				+ this.nonCoreVertices.size() + " vertices.");

	}

	@Override
	public void buildKNearNbrDigraph(int k) {

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
	 * @return the number of source vertices
	 */
	public int getnV() {
		return nV;
	}

	/**
	 * @return the digraph
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph() {
		return digraph;
	}

}
