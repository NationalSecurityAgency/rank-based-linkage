/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package main;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import orientationStructures.UndirectedToNNDigraph;
import readWrite.StringPairCSVReader;
import readWrite.StringPairWithWeight;

/**
 * REVISE! TODO In this version, input can be either
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
	private List<StringPairWithWeight> edgeRecords;
	private final boolean higherWeightMoreSimilar;
	private final boolean inputEdgesAreDirected;
	private final int kNN;
	private OptionalInt maxCluster;
	/*
	 * Allowed values of args[2]
	 */
	static final Set<String> inputGraphOptions = Set.of("Dh", "Dl", "Uh", "Ul");
	private SparseIntDirectedWeightedGraph knnDigraph;
	LinkageGraphBuilder lgb;
	List<Set<String>> subcriticalComponents;
	Map<String, Integer> objectToComponentMap;

	/**
	 * 
	 * @param weightedEdges
	 * @param directed
	 * @param higherIsBetter
	 * @param numNN
	 * @param maxClusterSize
	 */
	public ClusteringByInsway(List<StringPairWithWeight> weightedEdges, boolean directed, boolean higherIsBetter,
			int numNN, OptionalInt maxClusterSize) {
		this.edgeRecords = weightedEdges;
		this.inputEdgesAreDirected = directed;
		this.higherWeightMoreSimilar = higherIsBetter;
		this.kNN = numNN;
		this.maxCluster = maxClusterSize;
		GlobalLog.log(Level.INFO, "Instance of " + ClusteringByInsway.class.getName());
	}

	/**
	 * 
	 * @param args args[0] is input file name; format is .CSV, each line is: source,
	 *             target, weight
	 *             <p>
	 *             args[1] is one of: Dh, Dl, Uh, Ul explained above
	 *             <p>
	 *             args[2] is integer k to count # nearest neighbors (e.g.8)
	 *             <p>
	 *             Also optional run-time property -maxCluster: integer referring to
	 *             user-preferred maximum cluster size (e.g. 100). This will
	 *             determine in-sway threshold for graph components. If omitted, the
	 *             subcritical clustering will be returned.
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		String modifiers = args[1];
		String clusterSizeLimit = System.getProperty("maxCluster");
		if (!inputGraphOptions.contains(modifiers)) {
			throw new IllegalArgumentException("Third argument must be one of: " + " Dh," + " Dl," + " Uh," + " Ul.");
		} else {
			boolean isDirected = modifiers.startsWith(new String("D")) ? true : false;
			boolean higherBetter = modifiers.endsWith(new String("h")) ? true : false;
			int k = Integer.parseInt(args[2]); // # neighbors
			if (args.length != 3) {
				throw new RuntimeException("User must provide three arguments - "
						+ " (1) input file/directory, (2) one of the modifiers Dh, Dl, Uh, Ul,"
						+ "and (3) # nearest neighbors K");
			}
			OptionalInt maxClusterSize = OptionalInt.empty();
			if (clusterSizeLimit != null) {
				maxClusterSize = OptionalInt.of(Integer.parseInt(clusterSizeLimit));
			}
			StringPairCSVReader reader = new StringPairCSVReader();
			reader.readStringPairsWithWeights(fileName);
			int nEdges = reader.getWeightedEdges().size();
			GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
			ClusteringByInsway rbl = new ClusteringByInsway(reader.getWeightedEdges(), isDirected, higherBetter, k,
					maxClusterSize);
			GlobalLog.log(Level.INFO, "Input file " + fileName + ", size: " + nEdges + "; directed edges? " + isDirected
					+ "; higher edge weights mean more similar? " + higherBetter);
			GlobalLog.log(Level.INFO, k + " nearest neighbors will be used; maximum cluster size " + maxClusterSize);
			GlobalLog.log(Level.INFO,"Java runtime version: " + System.getProperty("java.runtime.version" ));
			/*
			 * Prepare the k-NN digraph
			 */
			rbl.prepareNNDigraph();
			rbl.buildLinkageGraph();
			/*
			 * If -maxCluster argument was not set, export edges of the sub-critical graph,
			 * each tagged with a component number.
			 */
		}
	}

	private void prepareNNDigraph() {
		if (this.inputEdgesAreDirected) {
			DirectedToNNDigraph d2nndg = new DirectedToNNDigraph(this.higherWeightMoreSimilar);
			d2nndg.buildRawSparseGraph(this.edgeRecords);
			d2nndg.determineTwoCore();
			d2nndg.buildKNearNbrDigraph(this.kNN);
			this.knnDigraph = d2nndg.getKNNDigraph();
		} else {
			UndirectedToNNDigraph u2nndg = new UndirectedToNNDigraph(this.higherWeightMoreSimilar);
			u2nndg.buildRawSparseGraph(this.edgeRecords);
			u2nndg.determineTwoCore();
			u2nndg.buildKNearNbrDigraph(this.kNN);
			this.knnDigraph = u2nndg.getKNNDigraph();
		}
	}

	private void buildLinkageGraph() {
		this.lgb = new LinkageGraphBuilder(this.higherWeightMoreSimilar);
		this.lgb.setDigraph(this.knnDigraph);
		this.lgb.buildLinkageGraph();
	}



	private void buildClustersNotBiggerThan(int mx) {
		// TODO Union Find

	}

	/**
	 * TODO Change design.
	 * <p>
	 * Need to tune the in-sway threshold to give desired max cluster size. Use
	 * UnionFind in JGraphT.
	 * <p>
	 * Need (1) String to (int) component map, and (2) for each (int) component,
	 * variable length list of members.
	 * 
	 */

}
