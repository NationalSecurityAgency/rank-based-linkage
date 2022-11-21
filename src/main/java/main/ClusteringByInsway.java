/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package main;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AbstractGraph;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import readWrite.StringPairCSVReader;

/**
 * In this version, input can be either
 * <p>
 * &bull; [Command line option -Dh or -Dl] Directed graph D with edge weights,
 * supplied as multiple lines of form (String source, String target, Double
 * weight). Dh (RESP. Dl) means higher (RESP. lower) weight means more similar;
 * or
 * <p>
 * &bull; [Command line option -Uh or -Ul] Undirected edge-weighted graph U in
 * this format. Uh (RESP. Ul) means higher (RESP. lower) weight means more
 * similar.
 * <p>
 * Step I: extract the 2-core of U, or of the undirected form of D, as
 * appropriate. After that, every object has total degree at least two in either
 * case, and therefore participates in at least one "voter triangle" {xy, yz,
 * yz}.
 * <p>
 * Step II: given an integer parameter K &ge; 2 (supplied at run time), scan the
 * neighbors (case U) or out-neighbors (case D) of each object x, and retain
 * edges only for the K nearest according to x's ranking (or d nearest, if x has
 * d &lt; K neighbors in case U or out-neighbors in case D). Call this set
 * Gamma(x). Even in the case of input U, we save results as a DIRECTED weighted
 * graph (S, A, w), because y in Gamma(x) does not imply x in Gamma(y). The
 * weight function w is the same as the one we started with. As explained in the
 * paper
 * <p>
 * &bull; <i>Rank-based linkage I: triplet comparisons and oriented simplicial
 * complexes</i>, Darling, Grilliette and Logan, 2022
 * <p>
 * this data supplies an orientation of the line graph of an undirected
 * K-nearest neighbor graph on a set S of n objects.
 * <p>
 * The present JAVA API creates the edge-weighted linkage graph (S, B, sigma).
 * Here B consists of pairs of mutual friends, and sigma is the in-sway. As t
 * varies, the graph components for edges with in-sway at least t supplies a
 * partition of S. This is rank-based linkage hierarchical clustering. The
 * critical value of t is the smallest t such that the number of links whose
 * in-sway exceeds t is less than the number of objects. The subcritical
 * clustering uses links whose in-sway exceeds the critical value.
 * <p>
 * Three output files:
 * <p>
 * &bull; CSV file for the linkage graph, whose lines are of form (String
 * source, String target, int in-sway)
 * <p>
 * &bull; CSV file indicating the component of the subcritical linkage graph to
 * which each object belongs. Lines are of form (String object, int index),
 * where the index points to a component of the subcritical linkage graph. Lines
 * are lexicographically ordered by object.
 * <p>
 * &bull; CSV file listing the objects in each numbered component of the
 * subcritical linkage graph. Lines are of form (int index, {String object1,
 * String object2, ...}) where {...} contains the objects in the component
 * associated with the index. Lines are ordered by index. Hence an object in
 * component j appears in the line indexed by j, and in no other line.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class ClusteringByInsway {
	private final boolean higherWeightMoreSimilar;
	private final boolean inputEdgesAreDirected;
	/*
	 * Allowed values of args[2]
	 */
	static final Set<String> inputGraphOptions = Set.of("Dh", "Dl", "Uh", "Ul");
	DirectedToNNDigraph d2nndg;
	StringPairCSVReader reader;
	LinkageGraphBuilder lgb;
	List<Set<String>> subcriticalComponents;
	Map<String, Integer> objectToComponentMap;

	public ClusteringByInsway(boolean directed, boolean higherIsBetter) {
		// this.inputGraphProperties = new Properties()
		this.inputEdgesAreDirected = directed;
		this.higherWeightMoreSimilar = higherIsBetter;
		GlobalLog.log(Level.INFO, "Instance of " + ClusteringByInsway.class.getName());

	}

	/**
	 * 
	 * @param args args[0] is input file name; format is .CSV, each line is: source,
	 *             target, weight
	 *             <p>
	 *             args[1] is integer k to count # nearest neighbors
	 *             <p>
	 *             args[2] is one of: Dh, Dl, Uh, Ul explained above
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		int k = Integer.parseInt(args[1]); // # neighbors
		GlobalLog.log(Level.INFO, "Input file: " + fileName + "; # neighbors: " + k);
		String modifiers = args[2];
		if (!inputGraphOptions.contains(modifiers)) {
			throw new IllegalArgumentException("Third argument must be one of: " + " Dh," + " Dl," + " Uh," + " Ul.");
		} else {
			boolean isDirected = modifiers.startsWith(new String("D"));
			boolean higherBetter = modifiers.endsWith(new String("h"));
			ClusteringByInsway rbl = new ClusteringByInsway(isDirected, higherBetter);
			// Copy stuff from other tests. then done!
		}
	}

	/**
	 * TODO Change design. Need (1) String to (int) component map, and (2) for each
	 * (int) component, variable length list  of members.
	 * 
	 * @param seed is one of the objects among the input data
	 * @return objects in the same component of the subcritical linkage graph
	 */
	public Set<String> objectsInSameSubcriticalComponentAs(String seed) throws IllegalArgumentException {
		if (!d2nndg.getVertexIndex().containsKey(seed)) {
			throw new IllegalArgumentException("Cannot find this String in lookup table.");
		} else {
			return this.objectsInSameComponentAs(seed, lgb.getSubCriticalGraph());
		}
	}

	/**
	 * 
	 * @param seed is one of the objects among the input data
	 * @return objects in the same component of the critical linkage graph
	 */
	public Set<String> objectsInSameCriticalComponentAs(String seed) throws IllegalArgumentException {
		if (!d2nndg.getVertexIndex().containsKey(seed)) {
			throw new IllegalArgumentException("Cannot find this String in lookup table.");
		} else {
			return this.objectsInSameComponentAs(seed, lgb.getCriticalGraph());
		}
	}

	/**
	 * Utility method
	 * <p>
	 * TODO: wasteful to compute connected components for one single seed. Best to
	 * deliver ALL the components.
	 */
	private Set<String> objectsInSameComponentAs(String seed, AbstractGraph<Integer, Integer> graph) {
		ConnectivityInspector<Integer, Integer> inspector = new ConnectivityInspector<>(graph);
		Integer v = d2nndg.getVertexIndex().get(seed);
		return inspector.connectedSetOf(v).stream().map(u -> d2nndg.getObjectStringLookup().get(u))
				.collect(Collectors.toSet());

	}

}
