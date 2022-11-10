/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.alg.util.UnorderedPair;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;

/**
 * Takes {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph}
 * object, with vertex set {0, 1, 2, ..., n &minus; 1}. Even when rankings are
 * built from edge weights of an undirected graph, the relation "is one of the k
 * nearest neighbors of" is not symmetric. Therefore a directed weighted graph
 * type is needed for input storage.
 * <p>
 * Call z a friend of x it is one of the k nearest neighbors of x.
 * <p>
 * Starts by computing the graph whose edges are pairs of mutual friends. Take
 * connected components. A linkage graph is computed on each component. This
 * means computing in-sway for each pair of mutual friends.
 * <p>
 * Turn the pairs of mutual friends {x, z} into a stream, and count for each {x,
 * z} the number of 2-simplices (xz, xy, yz} in which xz is the source. Here at
 * least one of x and z must be a friend of y.
 * <p>
 * Computation of ranks is unnecessary; just compare neighbors of vertex v
 * according to weight of outgoing edges from v.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class LinkageGraphBuilder {
	private SparseIntDirectedWeightedGraph dataDigraph;
	private int nO; // number of objects
	private final boolean higherWeightMoreSimilar;
	private List<UnorderedPair<Integer, Integer>> mutualFriends;
	private SparseIntUndirectedWeightedGraph linkageGraph;
	/*
	 * Component i of this array is the number of links whose in-sway is exactly i.
	 */
	private int[] linkCountForInswayValue;
	/*
	 * Not sure if this map is useful or necessary
	 */
	private Map<Integer, List<Integer>> linksGroupedByInsway;
	/*
	 * Graph to be built from links of positive insway.
	 */

	/**
	 * 
	 * @param higherWeightMoreSimilar true if the higher of two weights of outgoing
	 *                                edges from vertex x points to vertex more
	 *                                similar to vertex x.
	 */
	public LinkageGraphBuilder(boolean higherWeightMoreSimilar) {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageGraphBuilder.class.getName());
		this.higherWeightMoreSimilar = higherWeightMoreSimilar;

	}

	public void setDigraph(SparseIntDirectedWeightedGraph digraph) {
		this.dataDigraph = digraph;
		this.nO = digraph.vertexSet().size();
	}

	/**
	 * WORKS whether weights indicate similarity or dissimilarity.
	 * 
	 * @param i base vertex
	 * @param j neighbor of i
	 * @param k neighbor of k
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
		 * Count y with {x, y} intersecting Gamma(y), s.t. xz is source of 2-simplex
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
	 * Frnally create a sparse linkage graph.
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
		 * The in-sway of an edge is a primitive double. It must be converted to Integer
		 * object.
		 */
		IntFunction<Integer> inswayInt = e -> Integer.valueOf((int) this.linkageGraph.getEdgeWeight(e));
		this.linksGroupedByInsway = this.linkageGraph.edgeSet().stream()
				.collect(Collectors.groupingBy(inswayInt::apply));
		/*
		 * Maximum value of insway over all links
		 */
		int maxInsway = this.linksGroupedByInsway.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
		this.linkCountForInswayValue = new int[1 + maxInsway];
		/*
		 * For each value of insway, count the links where insway takes this value, and
		 * store in array.
		 */
		for (Integer insw : this.linksGroupedByInsway.keySet()) {
			this.linkCountForInswayValue[insw.intValue()] = this.linksGroupedByInsway.get(insw).size();
		}

	}

	/**
	 * TODO connected components under various levels of in-sway
	 */

	/**
	 * @return the mutual friends, i.e. mutual k-NN
	 */
	public List<UnorderedPair<Integer, Integer>> getMutualFriends() {
		return mutualFriends;
	}

	/**
	 * @return the linkage graph, whose edge weights are the in-sway between mutual
	 *         friends
	 */
	public SparseIntUndirectedWeightedGraph getLinkageGraph() {
		return linkageGraph;
	}

	/**
	 * @return the linkCountForInswayValue
	 */
	public int[] getLinkCountForInswayValue() {
		return linkCountForInswayValue;
	}

	/**
	 * @return the linksGroupedByInsway
	 */
	public Map<Integer, List<Integer>> getLinksGroupedByInsway() {
		return linksGroupedByInsway;
	}

}
