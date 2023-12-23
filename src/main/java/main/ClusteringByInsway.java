/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package main;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;

import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import orientationStructures.UndirectedToNNDigraph;
import readWrite.LinkageGraphCSVWriter;
import readWrite.StringPairCSVReader;
import readWrite.StringPairWithWeight;

/**
 * TODO Exit gracefully if mutual friend set is empty.
 * <p>
 * See main method for input argument syntax.
 * <p>
 * Input can be either
 * <p>
 * &bull; [Command line option Dh or Dl] Directed graph D with edge weights,
 * supplied as multiple lines of form (String source, String target, Double
 * weight). Dh (RESP. Dl) means higher (RESP. lower) weight means more similar;
 * or
 * <p>
 * &bull; [Command line option Uh or Ul] Undirected edge-weighted graph U in
 * this format. Uh (RESP. Ul) means higher (RESP. lower) weight means more
 * similar.
 * <p>
 * &bull; [Compulsory command line argument K &ge; 2] (such as 8) for number of
 * near neighbors of each object to include.
 * <p>
 * &bull; [Optional argument m] (such as 30) produces a linkage subgraph where
 * the in-sway threshold is as low as possible such that no component of the
 * subgraph contains more than m objects. If this argument is omitted, the
 * sub-critical linkage graph is produced.
 * <p>
 * Step I: extract the 2-core of U, or of the undirected form of D, as
 * appropriate. After that, every object has total degree at least two in either
 * case, and therefore is eligible to participate in at least one "voter
 * triangle" {xy, yz, yz}.
 * <p>
 * Step II: given an integer parameter K &ge; 2 (supplied at run time), scan the
 * neighbors (case U) or out-neighbors (case D) of each object x, and retain
 * edges only for the K nearest according to x's ranking (or d nearest, if x has
 * d &lt; K neighbors in case U or out-neighbors in case D). Call this set
 * &Gamma;(x). Even in the case of input U, we save results as a DIRECTED
 * weighted graph (S, A, w), because y in &Gamma;(x) does not imply x in
 * &Gamma;(y). The weight function w is the same as the one we started with. As
 * explained in the paper
 * <p>
 * &bull; <i>Rank-based linkage I: triplet comparisons and oriented simplicial
 * complexes</i>, Darling, Grilliette and Logan, 2022
 * <p>
 * this data supplies an orientation of the line graph of an undirected
 * K-nearest neighbor graph on a set S of n objects.
 * <p>
 * The present JAVA API creates the edge-weighted linkage graph (S, L, &sigma;).
 * Here L consists of pairs of mutual friends, and &sigma; is the in-sway. As t
 * varies, the graph components for edges with in-sway at least t supplies a
 * partition of S. This is rank-based linkage hierarchical clustering. The
 * critical value of t is the smallest t such that the number of links whose
 * in-sway exceeds t is less than the number of objects. The sub-critical
 * clustering uses links whose in-sway exceeds the critical value.
 * <p>
 * Two output files:
 * <p>
 * (1) CSV file where each line is an edge of the linkage subgraph, whose lines
 * are of form
 * <p>
 * (int component #, String source, String target, int in-sway)
 * <p>
 * Here components are computed with respect to edges in the subgraph, and
 * numbers are 0, 1, 2, &hellip;
 * <p>
 * (2) CSV file listing the isolated objects, i.e. those of degree zero in the
 * linkage subgraph. One String per line.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class ClusteringByInsway {

	private List<StringPairWithWeight> edgeRecords;
	private List<String> objectStringLookup;
	private final boolean higherWeightMoreSimilar;
	private final boolean inputEdgesAreDirected;
	private final int kNN;
	private OptionalInt maxCluster;
	/*
	 * Allowed values of args[2]
	 */
	static final Set<String> inputGraphOptions = Set.of("Dh", "Dl", "Uh", "Ul");
	private SparseIntDirectedWeightedGraph knnDigraph; // rank-based linkage is applied to this
	private LinkageGraphBuilder lgb;// all the graph analytics happen here
	private LinkageGraphCSVWriter lgWriter;
	private int subgraphEdgeCount, subgraphMaxComponentSize;

	/**
	 * 
	 * @param weightedEdges  input list of records
	 * @param directed       true if edges are directed
	 * @param higherIsBetter true if higher weight means more similar
	 * @param numNN          number of nearest neighbors
	 * @param maxClusterSize optional parameter in selecting in-sway threshold
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
	 * Examples: "java -jar rbl.jar edgefileD.csv Dh 8 50"; "java -jar rbl.jar
	 * edgefileU.csv Ul 16"
	 * 
	 * @param args args[0] is input file name; format is .CSV, each line is: source,
	 *             target, weight
	 *             <p>
	 *             args[1] is one of: Dh, Dl, Uh, Ul explained above
	 *             <p>
	 *             args[2] is integer k to count # nearest neighbors (e.g.8)
	 *             <p>
	 *             Optional argument maxCluster: integer referring to user-preferred
	 *             maximum cluster size (e.g. 100). This will determine in-sway
	 *             threshold for graph components. If omitted, the sub-critical
	 *             linkage graph and its components will be returned.
	 */
	public static void main(String[] args) {
		GlobalLog.log(Level.INFO, "Java runtime version: " + System.getProperty("java.runtime.version"));
		if (args.length < 3 || args.length > 4) {
			throw new RuntimeException("Three or four arguments are needed: \n filename for graph edges "
					+ "\n 2-chararacter instruction Dh, Dl, Uh or Ul for reading graph "
					+ "\n number K of neighbors, and \n (optional) maximum cluster size");
		} else {
			String fileName = args[0];
			String modifiers = args[1];
			int k = Integer.parseInt(args[2]); // # neighbors
			OptionalInt maxClusterSize = OptionalInt.empty();
			if (args.length > 3) {
				maxClusterSize = OptionalInt.of(Integer.parseInt(args[3]));
			}
			if (!inputGraphOptions.contains(modifiers)) {
				throw new IllegalArgumentException(
						"Third argument must be one of: " + " Dh," + " Dl," + " Uh," + " Ul.");
			} else {
				boolean isDirected = modifiers.startsWith(new String("D")) ? true : false;
				boolean higherBetter = modifiers.endsWith(new String("h")) ? true : false;
				StringPairCSVReader reader = new StringPairCSVReader();
				reader.readStringPairsWithWeights(fileName);
				int nEdges = reader.getWeightedEdges().size();
				GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
				ClusteringByInsway rbl = new ClusteringByInsway(reader.getWeightedEdges(), isDirected, higherBetter, k,
						maxClusterSize);
				GlobalLog.log(Level.INFO,
						"Input file " + fileName + ", size: " + nEdges + ". Directed edges? " + isDirected);
				GlobalLog.log(Level.INFO,
						k + " nearest neighbors will be used; maximum cluster size " + maxClusterSize);

				/*
				 * All the work happens here
				 */
				double t0 = (double) System.currentTimeMillis();
				rbl.prepareNNDigraph();// prepare the k-NN digraph
				double t1 = (double) System.currentTimeMillis();
				GlobalLog.log(Level.INFO, "Digraph preparation: " + (t1 - t0) / 1000.0 + " secs.");
				rbl.buildLinkageGraph();// rank-based linkage algorithm
				double t2 = (double) System.currentTimeMillis();
				GlobalLog.log(Level.INFO, "Rank-based linkage computation: " + (t2 - t1) / 1000.0 + " secs.");
				rbl.extractLinkageSubgraph(); // choose a subgraph, find its components
				double t3 = (double) System.currentTimeMillis();
				GlobalLog.log(Level.INFO, "Subgraph extraction and components: " + (t3 - t2) / 1000.0 + " secs.");
				rbl.exportLinkageSubgraph(); // write subgraph to two files as above
			}
		}
	}

	private void prepareNNDigraph() {
		if (this.inputEdgesAreDirected) {
			DirectedToNNDigraph d2nndg = new DirectedToNNDigraph(this.higherWeightMoreSimilar);
			d2nndg.buildRawSparseGraph(this.edgeRecords);
			d2nndg.determineTwoCore();
			d2nndg.buildKNearNbrDigraph(this.kNN);
			this.knnDigraph = d2nndg.getKNNDigraph();
			this.objectStringLookup = d2nndg.getObjectStringLookup();
		} else {
			UndirectedToNNDigraph u2nndg = new UndirectedToNNDigraph(this.higherWeightMoreSimilar);
			u2nndg.buildRawSparseGraph(this.edgeRecords);
			u2nndg.determineTwoCore();
			u2nndg.buildKNearNbrDigraph(this.kNN);
			this.knnDigraph = u2nndg.getKNNDigraph();
			this.objectStringLookup = u2nndg.getObjectStringLookup();
		}
	}

	private void buildLinkageGraph() {
		this.lgb = new LinkageGraphBuilder(this.higherWeightMoreSimilar);
		this.lgb.setDigraph(this.knnDigraph);
		this.lgb.buildLinkageGraph();
	}

	private void extractLinkageSubgraph() {
		int inSwayLB;
		/*
		 * Case 1: a maximum cluster size has been set.
		 */
		if (this.maxCluster.isPresent()) {
			/*
			 * Determine in-sway lower bound for desired subgraph
			 */
			inSwayLB = this.lgb.unionFindUpToClusterThreshold(this.maxCluster.getAsInt());
			GlobalLog.log(Level.INFO, "Insway lower bound: " + inSwayLB + " under cluster constraint.");
		}
		/*
		 * Case 2: default to lower bound which is critical in-sway plus 1
		 */
		else {
			inSwayLB = this.lgb.getCriticalInsway() + 1;
			GlobalLog.log(Level.INFO, "Insway lower bound: " + inSwayLB + " without cluster constraint.");
		}
		/*
		 * Build subgraph for this component size bound.
		 */
		this.lgb.buildGraphInswayNotLessThan(inSwayLB);
		this.subgraphEdgeCount = this.lgb.getLinkageSubgraph().edgeSet().size();
		GlobalLog.log(Level.INFO,
				"Subgraph with insway lower bound " + inSwayLB + " has " + subgraphEdgeCount + " edges.");
		/*
		 * Component structure of subgraph
		 */
		this.subgraphMaxComponentSize = this.lgb.detectComponentsOf(this.lgb.getLinkageSubgraph());
		GlobalLog.log(Level.INFO, "In subgraph, max component size is " + subgraphMaxComponentSize);
	}

	private void exportLinkageSubgraph() {
		this.lgWriter = new LinkageGraphCSVWriter();
		this.lgWriter.setObjectStringLookup(this.objectStringLookup);
		String prefix = "RBL-" + this.lgb.getNumberObjects() + "-objects-component-size-bound-"
				+ this.subgraphMaxComponentSize + "-";
		String dirName = prefix + this.subgraphEdgeCount + "-links-" + this.kNN + "-NN-" + System.currentTimeMillis();
		String fileNameEdges = this.subgraphEdgeCount + "-modest-edges.csv";
		this.lgWriter.writeEdgesToFile(dirName, fileNameEdges, this.lgb.getLinkageSubgraph(),
				this.lgb.getWhichComponent());
		GlobalLog.log(Level.INFO, "Modest graph notated with components has been written to file " + fileNameEdges);
		String fileNameVert = this.lgb.getNumSingletons() + "-singletons-among-" + this.lgb.getNumberObjects()
				+ "-objects.csv";
		this.lgWriter.writeSingletonVerticesToFile(dirName, fileNameVert, this.lgb.getLinkageGraph(),
				this.lgb.getIsIsolatedObject());
		GlobalLog.log(Level.INFO, "Singleton objects in modest graph have been written to file " + fileNameVert);
	}
}
