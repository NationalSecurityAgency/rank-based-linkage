/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.alg.util.UnionFind;
import org.jgrapht.alg.util.UnorderedPair;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;

/**
 * Builds in-sway weighted linkage graphs from a directed k-nearest-neighbor
 * digraph.
 * <p>
 * Input is a
 * {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph} on
 * vertices {@code 0,1,...,n-1}. Even if the original similarity relation is
 * symmetric, nearest-neighbor relations are generally directed, so directed
 * input storage is required.
 * <p>
 * Say {@code z} is a friend of {@code x} if {@code z} is among {@code x}'s
 * k-nearest outgoing neighbors.
 * <p>
 * The workflow is: identify mutual-friend pairs, compute in-sway for each pair,
 * build an undirected linkage graph weighted by in-sway, and then analyze
 * thresholded subgraphs and connected components.
 * <p>
 * For each mutual pair {@code {x,z}}, in-sway is the number of eligible
 * 2-simplices where edge {@code xz} is the source edge in the local ordering.
 * <p>
 * Explicit rank arrays are unnecessary; comparisons are made directly from
 * outgoing edge weights.
 * <p>
 * The class can also find threshold values of in-sway to control maximum
 * connected-component size.
 * <p>
 * Tested via {@code TestLinkageGraphBuilder} on a variety of directed graphs.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class LinkageGraphBuilder {
	private SparseIntDirectedWeightedGraph dataDigraph;
	private int nO; // number of objects
	private int numSingletons; // count isolated objects after connected components
	private int maxInsway, criticalInsway; // See getters for meaning
	private final boolean higherWeightMoreSimilar;
	private List<UnorderedPair<Integer, Integer>> mutualFriends;
	private SparseIntUndirectedWeightedGraph linkageGraph, linkageSubgraph, subCriticalGraph;// See getters for
																								// meaning
	private int[] whichComponent; // used when writing edges of subcritical graph to file
	private boolean[] isIsolatedObject;
	private IntFunction<Triple<Integer, Integer, Double>> pickEdge;
	/*
	 * Component i of this array is the number of links whose in-sway is exactly i.
	 */
	private int[] linkCountByInswayValue, linkCountAboveInswayValue;
	/*
	 * Needed for Union Find
	 */
	private Map<Integer, List<Integer>> linksGroupedByInsway;
	/*
	 * In the linkage graph containing links of ins-sway at least k, what is the
	 * size of the largest component?
	 */
	private Map<Integer, Integer> maxComponentSizeByInsway;

	/**
	 * Creates a linkage-graph builder.
	 * <p>
	 * The {@code higherWeightMoreSimilar} flag controls all neighbor comparisons in
	 * methods such as {@link #moreSimilarTo(int, int, int)}.
	 *
	 * @param higherWeightMoreSimilar true if the higher of two weights of outgoing
	 *                                edges from vertex x points to vertex more
	 *                                similar to vertex x.
	 */
	public LinkageGraphBuilder(boolean higherWeightMoreSimilar) {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageGraphBuilder.class.getName());
		this.higherWeightMoreSimilar = higherWeightMoreSimilar;
		this.maxComponentSizeByInsway = new HashMap<>();
	}

	/**
	 * Sets the input data digraph and initializes vertex-dependent arrays.
	 * 
	 * @param digraph used as a representative of out-ordered digraph equivalence
	 *                class
	 */
	public void setDigraph(SparseIntDirectedWeightedGraph digraph) {
		this.dataDigraph = digraph;
		this.nO = digraph.vertexSet().size();
		this.numSingletons = this.nO;
		this.whichComponent = new int[nO];
		this.isIsolatedObject = new boolean[nO];
	}

	/**
	 * WORKS whether weights indicate similarity or dissimilarity.
	 * 
	 * @param i base vertex
	 * @param j neighbor of i
	 * @param k another neighbor of i
	 * @return true if j is more similar to i than k is to i
	 */
	public boolean moreSimilarTo(int i, int j, int k) {
		double wij, wik;
		wij = this.dataDigraph.getEdgeWeight(this.dataDigraph.getEdge(i, j));
		wik = this.dataDigraph.getEdgeWeight(this.dataDigraph.getEdge(i, k));
		if (this.higherWeightMoreSimilar) {
			return wij > wik;
		} else {
			return wij < wik;
		}
	}

	/*
	 * 
	 * True if xz is the source in the 2-simplex {xz, yz, xy}. Invoked only when x
	 * and z are mutual friends; however y need be a friend of neither x nor z.
	 */
	private boolean xzIsSource(Integer x, Integer z, Integer y) {
		boolean xzBeatsxy = (!this.dataDigraph.containsEdge(x, y)) || this.moreSimilarTo(x, z, y);
		boolean xzBeatsyz = (!this.dataDigraph.containsEdge(z, y)) || this.moreSimilarTo(z, x, y);
		return xzBeatsxy && xzBeatsyz;
	}

	/*
	 * To be applied when dataDigraph already contains arc (s, t). We only want to
	 * store each unordered pair {s, t} once, although there exist arcs in both
	 * directions. The integer comparison makes the arbitrary choice to recognize
	 * the mutual friends when s &lt; t.
	 */
	private boolean areMutualFriends(Integer s, Integer t) {
		return (s.compareTo(t) < 0) && this.dataDigraph.containsEdge(t, s);
	}

	/*
	 * Computes in-sway of object pair {x, z}, assumed to be mutual friends.
	 */
	private double insway(Integer x, Integer z) {
		/*
		 * Determine objects y such that y is source of arc pointing to object x.
		 */
		Set<Integer> yVertices = this.dataDigraph.incomingEdgesOf(x).stream()
				.map(e -> this.dataDigraph.getEdgeSource(e)).collect(Collectors.toSet());
		/*
		 * Add in objects y such that y is source of arc pointing to object z.
		 */
		yVertices.addAll(this.dataDigraph.incomingEdgesOf(z).stream().map(e -> this.dataDigraph.getEdgeSource(e))
				.collect(Collectors.toSet()));
		/*
		 * Count y with {x, z} intersecting Gamma(y), s.t. xz is source of 2-simplex
		 * {xz, xy, yz}.
		 */
		long sourceCount = yVertices.stream().filter(y -> this.xzIsSource(x, z, y)).count();
		return (double) sourceCount;
	}

	/**
	 * First builds mutual friend pairs via a parallel stream.
	 * <p>
	 * Build weighted edges set of linkage graph via a parallel stream.
	 * <p>
	 * Finally create the linkage graph, by computing all the in-sways.
	 */
	public void buildLinkageGraph() {
		this.mutualFriends = this.dataDigraph.edgeSet().parallelStream()
				.filter(e -> this.areMutualFriends(this.dataDigraph.getEdgeSource(e), dataDigraph.getEdgeTarget(e)))
				.map(e -> new UnorderedPair<>(this.dataDigraph.getEdgeSource(e), dataDigraph.getEdgeTarget(e)))
				.toList();
		GlobalLog.log(Level.INFO, "Mutual friends list built with " + this.mutualFriends.size() + " pairs.");
		// Collections.shuffle(this.mutualFriends); // not allowed for immutable
		// collection
		/*
		 * Edges of the linkage graph and their insway are built as a stresm
		 */
		double start = (double) System.currentTimeMillis();
		this.linkageGraph = new SparseIntUndirectedWeightedGraph(this.nO,
				this.mutualFriends.parallelStream().map(xz -> new Triple<Integer, Integer, Double>(xz.getFirst(),
						xz.getSecond(), this.insway(xz.getFirst(), xz.getSecond()))).toList());
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO,
				"Linkage graph built in " + duration + " secs, with " + this.linkageGraph.edgeSet().size() + " links.");
		/*
		 * Convenience method used later
		 */
		this.pickEdge = e -> new Triple<>(this.linkageGraph.getEdgeSource(e), this.linkageGraph.getEdgeTarget(e),
				this.linkageGraph.getEdgeWeight(e));
		/*
		 * The in-sway of an edge is a primitive double. It must be converted to Integer
		 * object.
		 */
		IntFunction<Integer> inswayInt = e -> Integer.valueOf((int) this.linkageGraph.getEdgeWeight(e));
		this.linksGroupedByInsway = this.linkageGraph.edgeSet().stream()
				.collect(Collectors.groupingBy(inswayInt::apply));
		/*
		 * Maximum value of insway over all links
		 */
		this.maxInsway = this.linksGroupedByInsway.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
		this.linkCountByInswayValue = new int[1 + maxInsway];
		/*
		 * For each value of insway, count the links where insway takes this value, and
		 * store in array.
		 */
		for (Integer insw : this.linksGroupedByInsway.keySet()) {
			this.linkCountByInswayValue[insw.intValue()] = this.linksGroupedByInsway.get(insw).size();
		}
		/*
		 * Counts links whose insway exceeds value of index
		 */
		this.linkCountAboveInswayValue = new int[1 + maxInsway];
		this.linkCountAboveInswayValue[maxInsway] = 0;
		this.criticalInsway = this.maxInsway;
		for (int j = maxInsway - 1; j > -1; j--) {
			this.linkCountAboveInswayValue[j] = this.linkCountAboveInswayValue[j + 1]
					+ this.linkCountByInswayValue[j + 1];
			if (this.linkCountAboveInswayValue[j] < this.nO) {
				this.criticalInsway--;
			}
		}
		GlobalLog.log(Level.INFO, "Critical insway: " + criticalInsway + " # links with insway above this: "
				+ linkCountAboveInswayValue[criticalInsway]);
	}

	/**
	 * Finds the smallest in-sway threshold whose retained-link graph has maximum
	 * component size at most {@code m}.
	 * <p>
	 * Let {@code L_t} be the linkage graph containing edges with in-sway at least
	 * {@code t}. This method returns the smallest threshold {@code t} such that the
	 * largest component of {@code L_t} has size no greater than {@code m}.
	 *
	 * @param m maximum acceptable size of a cluster
	 * @return minimum value of t so that links with in-sway at least t form a graph
	 *         whose largest connected component is not more than m
	 */
	public int unionFindUpToClusterThreshold(int m) {
		UnionFind<Integer> uf = new UnionFind<>(this.linkageGraph.vertexSet());
		int maxSize = 1;
		int inSway = this.maxInsway + 1;
		this.maxComponentSizeByInsway.put(inSway, maxSize);
		while (maxSize < m + 1 && inSway > 0) {
			inSway--;
			/*
			 * Declare all pairs of mutual friends with this in-sway value to be in same
			 * component. NULL POINTER EXCEPTION!
			 */
			if (this.linksGroupedByInsway.containsKey(inSway)) {
				for (Integer edge : this.linksGroupedByInsway.get(inSway)) {
					uf.union(this.linkageGraph.getEdgeSource(edge), this.linkageGraph.getEdgeTarget(edge));
				}
			}
			/*
			 * Maximum cluster size after inserting links of this in-sway value and greater
			 */
			maxSize = this.linkageGraph.vertexSet().stream().collect(Collectors.groupingBy(s -> uf.find(s))).values()
					.stream().mapToInt(c -> c.size()).max().orElse(1);
			this.maxComponentSizeByInsway.put(inSway, maxSize);
		}
		GlobalLog.log(Level.INFO,
				"Including all links with in-sway at least " + inSway + " gives maximum component size = " + maxSize);
		GlobalLog.log(Level.INFO, "Including all links at least " + (inSway + 1) + " gives maximum component size = "
				+ this.maxComponentSizeByInsway.get(inSway + 1));
		return (inSway + 1);
	}

	/**
	 * Builds the linkage subgraph containing all links with in-sway at least
	 * {@code inSwayLowerBound}.
	 * <p>
	 * Usually called after {@link #unionFindUpToClusterThreshold(int)} has been
	 * used to choose a threshold.
	 * 
	 * @param inSwayLowerBound is minimum value of in-sway for links in the
	 *                         linkageSubgraph.
	 */
	public void buildGraphInswayNotLessThan(int inSwayLowerBound) {
		/*
		 * List of indices of links whose in-sway is not less than the critical in-sway.
		 */
		List<Integer> criticalEdges = this.linksGroupedByInsway.entrySet().stream()
				.filter(s -> s.getKey().compareTo(inSwayLowerBound) >= 0).flatMap(s -> s.getValue().stream()).parallel()
				.toList();
		this.linkageSubgraph = new SparseIntUndirectedWeightedGraph(this.nO,
				criticalEdges.parallelStream().map(pickEdge::apply).toList());
	}

	/**
	 * Builds the subcritical linkage graph.
	 * <p>
	 * Call {@link #buildLinkageGraph()} first. This method restricts the linkage
	 * graph to edges whose
	 * in-sway exceeds critical value, i.e. fewer links than object.
	 */
	public void buildSubCriticalGraph() {
		/*
		 * List of indices of links whose in-sway exceeds the critical in-sway.
		 */
		List<Integer> subCriticalEdges = this.linksGroupedByInsway.entrySet().stream()
				.filter(s -> s.getKey().compareTo(this.criticalInsway) > 0).flatMap(s -> s.getValue().stream())
				.parallel().toList();
		this.subCriticalGraph = new SparseIntUndirectedWeightedGraph(this.nO,
				subCriticalEdges.parallelStream().map(pickEdge::apply).toList());
	}

	/**
	 * Detects connected components of a supplied linkage subgraph.
	 * <p>
	 * Call {@link #buildLinkageGraph()} first. Can be called more than once, for
	 * different subgraphs.
	 * 
	 * @param subGraph must have same vertex set as this.linkageGraph, and is
	 *                 expected to be a subgraph of the latter.
	 * @return size of largest component.
	 */
	public int detectComponentsOf(SparseIntUndirectedWeightedGraph subGraph) {
		ConnectivityInspector<Integer, Integer> inspector = new ConnectivityInspector<>(subGraph);
		/*
		 * Components which are not singletons
		 */
		List<Set<Integer>> componentsGT1 = inspector.connectedSets().stream().filter(c -> c.size() > 1).toList();
		int maxComponentSize = componentsGT1.stream().mapToInt(c -> c.size()).max().orElse(1);
		GlobalLog.log(Level.INFO,
				"Graph components detected: " + componentsGT1.size() + " components of size two or more.");

		Arrays.fill(this.isIsolatedObject, true); // switch to false for each non-singleton
		this.numSingletons = this.nO; // deduct one for each non-singleton
		for (int c = 0; c < componentsGT1.size(); c++) {
			for (Integer vertex : componentsGT1.get(c)) {
				this.whichComponent[vertex.intValue()] = c;
				this.numSingletons--;
				this.isIsolatedObject[vertex.intValue()] = false;
			}
		}
		return maxComponentSize;
	}

	/**
	 * Returns the list of mutual-friend object pairs identified in the input
	 * directed k-NN digraph.
	 *
	 * @return the mutual friends, i.e. mutual k-NN
	 */
	public List<UnorderedPair<Integer, Integer>> getMutualFriends() {
		return mutualFriends;
	}

	/**
	 * Returns the full in-sway weighted linkage graph built from mutual-friend
	 * pairs.
	 *
	 * @return the linkage graph, whose edge weights are the in-sway between mutual
	 *         friends
	 */
	public SparseIntUndirectedWeightedGraph getLinkageGraph() {
		return linkageGraph;
	}

	/**
	 * Returns the count of links for each exact in-sway value.
	 *
	 * @return array where index {@code i} stores the number of links with in-sway
	 *         exactly {@code i}
	 */
	public int[] getLinkCountByInswayValue() {
		return linkCountByInswayValue;
	}

	/**
	 * Returns linkage-graph edges grouped by their integer in-sway value.
	 *
	 * @return map from in-sway value to edge-id list in the linkage graph
	 */
	public Map<Integer, List<Integer>> getLinksGroupedByInsway() {
		return linksGroupedByInsway;
	}

	/**
	 * Returns cumulative counts of links above each in-sway level.
	 *
	 * @return array where index {@code i} stores the number of links with in-sway
	 *         strictly greater than {@code i}
	 */
	public int[] getLinkCountAboveInswayValue() {
		return linkCountAboveInswayValue;
	}

	/**
	 * Returns the maximum in-sway value observed among all linkage-graph edges.
	 *
	 * @return the maximum value of in-sway over all links
	 */
	public int getMaxInsway() {
		return maxInsway;
	}

	/**
	 * Returns the critical in-sway cutoff used for the subcritical-edge notion in
	 * this class.
	 *
	 * @return smallest value of j such that the number of links with in-sway
	 *         greater than j is less than the number of objects.
	 */
	public int getCriticalInsway() {
		return criticalInsway;
	}

	/**
	 * Returns the graph containing only links whose in-sway is above the critical
	 * threshold.
	 * <p>
	 * Call {@link #buildSubCriticalGraph()} first.
	 * 
	 * @return the subCriticalGraph
	 */
	public SparseIntUndirectedWeightedGraph getSubCriticalGraph() {
		return subCriticalGraph;
	}

	/**
	 * Returns the number of objects (vertices) in the input digraph.
	 *
	 * @return number of objects
	 */
	public int getNumberObjects() {
		return nO;
	}

	/**
	 * Returns the component index for each vertex from the most recent component
	 * detection.
	 * <p>
	 * Must call {@link #detectComponentsOf(SparseIntUndirectedWeightedGraph)} first.
	 * 
	 * @return which component each vertex belongs to, where minus 1 refers to
	 *         singletons
	 */
	public int[] getWhichComponent() {
		return whichComponent;
	}

	/**
	 * Returns the number of isolated vertices from the most recent component
	 * detection.
	 * <p>
	 * Must call {@link #detectComponentsOf(SparseIntUndirectedWeightedGraph)} first.
	 * 
	 * @return count isolated objects after connected components
	 */
	public int getNumSingletons() {
		return numSingletons;
	}

	/**
	 * Returns a boolean mask identifying isolated objects.
	 * <p>
	 * Must call {@link #detectComponentsOf(SparseIntUndirectedWeightedGraph)} first.
	 * 
	 * @return the isIsolatedObject
	 */
	public boolean[] getIsIsolatedObject() {
		return isIsolatedObject;
	}

	/**
	 * Returns the linkage graph containing links whose in-sway is at least a chosen
	 * lower bound.
	 * <p>
	 * Call {@link #buildGraphInswayNotLessThan(int)} first.
	 * 
	 * @return the modest Linkage Graph, namely a linkage graph excluding links of
	 *         in-sway less than user-specified value, as in
	 */
	public SparseIntUndirectedWeightedGraph getLinkageSubgraph() {
		return linkageSubgraph;
	}

	/**
	 * Returns the maximum component size as a function of in-sway threshold.
	 * <p>
	 * Must call {@link #unionFindUpToClusterThreshold(int)} first.
	 * 
	 * @return map whose key is an integer value of in-sway, and whose value is the
	 *         maximum component size in linkage graph including links of in-sway at
	 *         least key.
	 * 
	 */
	public Map<Integer, Integer> getMaxComponentSizeByInsway() {
		return maxComponentSizeByInsway;
	}

}
