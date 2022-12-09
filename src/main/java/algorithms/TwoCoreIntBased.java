/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jgrapht.graph.AbstractGraph;

/**
 * Computes the vertices and edges in the two core of a graph, WITHOUT
 * constructing a new graph. Process: recognize edges incident to degree one
 * vertices, and update vertex degrees. Modified from trubbl-core project so
 * vertices and edges are both of Integer type. This allows passing results back
 * in the form of boolean arrays.
 * <p>
 * Applicable to SparseIntDirectedWeightedGraph and
 * SparseIntUnirectedWeightedGraph objects.
 * 
 * 
 * Example
 * <p>
 * Edge list of random graph: {h, b}, {e, b}, {g, h}, {g, d}, {d, f}, {a, k},
 * {g, e}, {b, k}, {a, g}, Two core vertices: a, b, e, g, h, k, Two core edges:
 * {h, b}, {b, k}, {a, k}, {g, h}, {g, e}, {e, b}, {a, g}, Numbers of edges
 * removed per round: 1, 1, 0.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TwoCoreIntBased {
	/*
	 * The linkageGraph will not be changed. We will only reduce the effective
	 * degree.
	 */
	AbstractGraph<Integer, Integer> graph;
	final int nV, nE;
	Map<Integer, Integer> currentDegree;
	Predicate<Integer> isLeafVertex;
	Predicate<Integer> isOuterEdge; // means it has an end point of degree 1.
	Set<Integer> twoCoreVertices;
	Set<Integer> twoCoreEdges;
	List<Integer> edgesOutsideTwoCore;
	/*
	 * Array-based versions of outcome are needed to communicate with sparse arrays
	 * later.
	 */
	private boolean[] vertexIsInTwoCore, edgeIsInTwoCore;
	int[] countEdgesRemovedEachRound;

	/**
	 * @param myGraph - could have isolated vertices
	 */
	public TwoCoreIntBased(AbstractGraph<Integer, Integer> myGraph) {
		this.graph = myGraph;
		this.currentDegree = this.graph.vertexSet().parallelStream()
				.collect(Collectors.toMap(Function.identity(), v -> Integer.valueOf(this.graph.degreeOf(v))));
		this.nV = this.graph.vertexSet().stream().mapToInt(v -> v.intValue()).max().orElse(0) + 1;
		this.vertexIsInTwoCore = new boolean[nV];
		Arrays.fill(this.vertexIsInTwoCore, false);
		this.nE = this.graph.edgeSet().stream().mapToInt(e -> e.intValue()).max().orElse(0) + 1;
		this.edgeIsInTwoCore = new boolean[nE];
		Arrays.fill(this.edgeIsInTwoCore, false);
		/*
		 * Detect leaf vertex, with respect to current vertex degrees
		 */
		this.isLeafVertex = v -> (this.currentDegree.get(v).intValue() == 1);
		/*
		 * Detect edge incident to a leaf, with respect to current vertex degrees
		 */
		this.isOuterEdge = e -> (this.isLeafVertex.test(this.graph.getEdgeSource(e))
				|| this.isLeafVertex.test(this.graph.getEdgeTarget(e)));
		/*
		 * Collections to be modified, while leaving linkageGraph unchanged.
		 */
		this.twoCoreEdges = this.graph.edgeSet().stream().collect(Collectors.toSet()); // fresh copy
		this.edgesOutsideTwoCore = new ArrayList<>();
	}

	/**
	 * 
	 * Compute the two-core subgraph.
	 */
	public void computeTwoCore() {
		/*
		 * Initialize set of edges NOT in 2-core.
		 */
		Set<Integer> outerEdges = this.twoCoreEdges.stream().filter(isOuterEdge).collect(Collectors.toSet());
		this.edgesOutsideTwoCore.addAll(outerEdges);
		Integer s, t;
		int ds, dt;
		List<Integer> countEdgesRemoved = new ArrayList<>();
		countEdgesRemoved.add(Integer.valueOf(outerEdges.size()));
		while (!outerEdges.isEmpty()) {
			for (Integer e : outerEdges) {
				s = this.graph.getEdgeSource(e);
				ds = this.currentDegree.get(s);
				this.currentDegree.put(s, Integer.valueOf(ds - 1));
				t = this.graph.getEdgeTarget(e);
				dt = this.currentDegree.get(t);
				this.currentDegree.put(t, Integer.valueOf(dt - 1));
				this.twoCoreEdges.remove(e);
			}
			/*
			 * Refresh the outer edges with respect to NEW currentDegree map
			 */
			outerEdges.clear();
			outerEdges.addAll(this.twoCoreEdges.stream().filter(isOuterEdge).collect(Collectors.toSet()));
			countEdgesRemoved.add(Integer.valueOf(outerEdges.size()));
		}
		/*
		 * twoCoreVertices are those whose degree is at least two after 'while" loop.
		 */
		this.twoCoreVertices = this.currentDegree.entrySet().stream().filter(e -> (e.getValue() > 1))
				.map(e -> e.getKey()).collect(Collectors.toSet());
		/*
		 * Reproduce the results in the form of boolean arrays, usable by consuming
		 * classes.
		 */
		for (Integer v : this.twoCoreVertices) {
			this.vertexIsInTwoCore[v.intValue()] = true;
		}
		for (Integer e : this.twoCoreEdges) {
			this.edgeIsInTwoCore[e.intValue()] = true;
		}
		/*
		 * Record what happened.
		 */
		this.countEdgesRemovedEachRound = new int[countEdgesRemoved.size()];
		Arrays.setAll(this.countEdgesRemovedEachRound, i -> countEdgesRemoved.get(i));
	}

	/**
	 * 
	 * @return the two core vertices
	 */
	public Set<Integer> getTwoCoreVertices() {
		return twoCoreVertices;
	}

	/**
	 * 
	 * @return the two core edges
	 */
	public Set<Integer> getTwoCoreEdges() {
		return twoCoreEdges;
	}

	/**
	 * @return the edges Removed Each Round, as an array indexed by the rounds
	 * 
	 */
	public int[] getCountEdgesRemovedEachRound() {
		return countEdgesRemovedEachRound;
	}

	/**
	 * 
	 * @return proportion of vertices in the two core
	 */
	public double twoCoreVertexProportion() {
		return (double) twoCoreVertices.size() / (double) this.graph.vertexSet().size();
	}

	/**
	 * @return the vertexIsInTwoCore
	 */
	public boolean[] getVertexIsInTwoCore() {
		return vertexIsInTwoCore;
	}

	/**
	 * @return the edgeIsInTwoCore
	 */
	public boolean[] getEdgeIsInTwoCore() {
		return edgeIsInTwoCore;
	}

}
