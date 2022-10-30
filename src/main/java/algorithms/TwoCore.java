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
 * constructing e new graph. Process: delete edges incident to degree one
 * vertices, and update vertex degrees.
 * 
 * * Example
 * <p>
 * Edge list of random graph: {h, b}, {e, b}, {g, h}, {g, d}, {d, f}, {a, k},
 * {g, e}, {b, k}, {a, g}, Two core vertices: a, b, e, g, h, k, Two core edges:
 * {h, b}, {b, k}, {a, k}, {g, h}, {g, e}, {e, b}, {a, g}, Numbers of edges
 * removed per round: 1, 1, 0,
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public class TwoCore<V, E> {
	/*
	 * The graph will not be changed. We will only reduce the effective degree.
	 */
	AbstractGraph<V, E> graph;
	Map<V, Integer> currentDegree;
	Predicate<V> isLeafVertex;
	Predicate<E> isOuterEdge; // means it has an end point of degree 1.
	Set<V> twoCoreVertices;
	Set<E> twoCoreEdges;
	int[] edgesRemovedEachRound;

	/**
	 * @param myGraph - could have isolated vertices
	 */
	public TwoCore(AbstractGraph<V, E> myGraph) {
		this.graph = myGraph;
		this.currentDegree = this.graph.vertexSet().parallelStream()
				.collect(Collectors.toMap(Function.identity(), v -> Integer.valueOf(this.graph.degreeOf(v))));
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
		 * Collections to be modified, while leaving graph unchanged.
		 */
		this.twoCoreEdges = this.graph.edgeSet().stream().collect(Collectors.toSet()); // fresh copy
	}

	/**
	 * 
	 * Compute the two-core subgraph.
	 */
	public void computeTwoCore() {
		/*
		 * Initialize set of edges NOT in 2-core.
		 */
		Set<E> outerEdges = this.twoCoreEdges.stream().filter(isOuterEdge).collect(Collectors.toSet());
		V s, t;
		int ds, dt;
		List<Integer> countEdgesRemoved = new ArrayList<>();
		countEdgesRemoved.add(Integer.valueOf(outerEdges.size()));
		while (!outerEdges.isEmpty()) {
			for (E e : outerEdges) {
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
		 * Record what happened.
		 */
		this.edgesRemovedEachRound = new int[countEdgesRemoved.size()];
		Arrays.setAll(this.edgesRemovedEachRound, i -> countEdgesRemoved.get(i));
	}

	/**
	 * @return the two core vertices
	 */
	public Set<V> getTwoCoreVertices() {
		return twoCoreVertices;
	}

	/**
	 * @return the two core edges
	 */
	public Set<E> getTwoCoreEdges() {
		return twoCoreEdges;
	}

	/**
	 * @return the edges Removed Each Round, as an array indexed by the rounds
	 * 
	 */
	public int[] getEdgesRemovedEachRound() {
		return edgesRemovedEachRound;
	}

	/**
	 * 
	 * @return proportion of vertices in the two core
	 */
	public double twoCoreVertexProportion() {
		return (double) twoCoreVertices.size() / (double) this.graph.vertexSet().size();
	}

}
