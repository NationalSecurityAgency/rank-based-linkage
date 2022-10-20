/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Takes a List of {@link readWrite.StringPairWithWeight} records, indexes the
 * source and target objects by integers 0 to n &minus; 1, and builds a
 * {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph} object or
 * {@link org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph} object,
 * as wished.
 * <p>
 * Tested 10.20.2022
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class RankingSystemBuilder {

	private List<String> sourceStrings; // list of distinct elements
	private Map<String, Integer> vertexIndex; // maps string s to integer i if sourceStrings.get(i) = s
	private List<StringPairWithWeight> stwList; // stw means "source, target, weight"
	private int nV; // source vertex count
	private SparseIntDirectedWeightedGraph digraph; // option 1
	private SparseIntUndirectedWeightedGraph graph; // option 2

	public RankingSystemBuilder() {
		GlobalLog.log(Level.INFO, "Instance of " + RankingSystemBuilder.class.getName());
	}

	/**
	 * First method to invoke.
	 * 
	 * @param edgeRecords list of "source, target, weight" triples
	 */
	public void setStwList(List<StringPairWithWeight> edgeRecords) {
		this.stwList = edgeRecords;
		;
	}

	/*
	 * Preparation for making a graph with vertices indexed by integers 0 to n
	 * &minus 1
	 */
	private void buildObjectIndex() {
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
	 * Second method to invoke. Creates a directed or undirected graph, depending on
	 * the boolean argument.
	 * 
	 * @param isDirected refers to whether the "source, target, weight" records are
	 *                   directed.
	 */
	public void buildSparseDirectedGraph(boolean isDirected) {
		this.buildObjectIndex();
		GlobalLog.log(Level.INFO, "Edge list built, with " + nV + " entries.");
		if (isDirected) {
			this.digraph = new SparseIntDirectedWeightedGraph(this.nV, this.buildEdgeList());
			GlobalLog.log(Level.INFO, "Directed graph built, with " + this.digraph.edgeSet().size() + " edges");
		} else {
			this.graph = new SparseIntUndirectedWeightedGraph(this.nV, this.buildEdgeList());
			GlobalLog.log(Level.INFO, "Undirected graph built, with " + this.graph.edgeSet().size() + " edges");
		}
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

	/**
	 * @return the graph
	 */
	public SparseIntUndirectedWeightedGraph getGraph() {
		return graph;
	}

}
