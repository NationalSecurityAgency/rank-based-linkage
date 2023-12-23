/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.IntStream;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import orientationStructures.UndirectedToNNDigraph;
import readWrite.LinkageGraphCSVWriter;
import readWrite.StringPairCSVReader;

/**
 * Tests for {@link algorithms.LinkageGraphBuilder}
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TestLinkageGraphBuilder {
	DirectedToNNDigraph d2nndg;
	UndirectedToNNDigraph u2nndg;
	StringPairCSVReader reader;
	LinkageGraphBuilder lgb;
	LinkageGraphCSVWriter lgWriter;
	final boolean higherWeightMoreSimilar = true;

	/**
	 * Constructor initializes field.
	 */
	public TestLinkageGraphBuilder() {
		GlobalLog.log(Level.INFO, "Instance of " + TestLinkageGraphBuilder.class.getName());
		this.reader = new StringPairCSVReader();
		this.d2nndg = new DirectedToNNDigraph(true);
		this.u2nndg = new UndirectedToNNDigraph(true);
		this.lgb = new LinkageGraphBuilder(higherWeightMoreSimilar);
		this.lgWriter = new LinkageGraphCSVWriter();
	}

	/**
	 * Current version works for directed weighted linkageGraph input. Need to
	 * prepare undirected input, and distinguish two cases. 
	 * 
	 * @param args filename, K (# neighbors)
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		int k = Integer.parseInt(args[1]); // # neighbors
		GlobalLog.log(Level.INFO, "Input file: " + fileName);
		TestLinkageGraphBuilder test = new TestLinkageGraphBuilder();
		test.reader.readStringPairsWithWeights(fileName);
		int nEdges = test.reader.getWeightedEdges().size();
		GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
		/**
		 * (1) raw digraph
		 */
		GlobalLog.log(Level.INFO, "Edge list has been passed to DirectedToNNDigraph.");
		double start = (double) System.currentTimeMillis();
		test.d2nndg.buildRawSparseGraph(test.reader.getWeightedEdges());
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "Sparse directed linkageGraph built in " + duration + " seconds.");
		/**
		 * (2) Determine two core. Needed to exclude non-two core vertices and edges
		 * from K-NN digraph
		 */
		start = (double) System.currentTimeMillis();
		test.d2nndg.determineTwoCore();
		test.d2nndg.buildKNearNbrDigraph(k);
		duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "K-NN digraph of two core built in " + duration + " seconds.");
		/*
		 * Test of LinkageGraphBuilder starts here.
		 */
		test.lgb.setDigraph(test.d2nndg.getKNNDigraph());
		GlobalLog.log(Level.INFO, "Sparse directed linkageGraph passed to LinkageGraphBuilder.");
		test.lgb.buildLinkageGraph();
		DoubleSummaryStatistics inSwayStatistics = test.lgb.getLinkageGraph().edgeSet().stream()
				.mapToDouble(e -> test.lgb.getLinkageGraph().getEdgeWeight(e)).summaryStatistics();
		GlobalLog.log(Level.INFO, "In-sway summary statistics:");
		GlobalLog.log(Level.INFO, inSwayStatistics.toString());
		/*
		 * Count links according to insway
		 */
		GlobalLog.log(Level.INFO, "Count links with given in-sway: ");
		for (int j = test.lgb.getMaxInsway(); j >= 0; j--) {
			GlobalLog.log(Level.INFO,
					"# links with in-sway exactly " + j + ": " + test.lgb.getLinkCountByInswayValue()[j]
							+ "; # links with in-sway GREATER than " + j + ": "
							+ test.lgb.getLinkCountAboveInswayValue()[j]);
		}
		GlobalLog.log(Level.INFO, "Critical in-sway: " + test.lgb.getCriticalInsway());
		/*
		 * Subcritical graph
		 */
		test.lgb.buildSubCriticalGraph();
		int nSubcEdges = test.lgb.getSubCriticalGraph().edgeSet().size();
		GlobalLog.log(Level.INFO, "Subcritical graph has " + nSubcEdges + " edges.");
		/*
		 * Subcritical components
		 */
		int maxSubCritComponentSize = test.lgb.detectComponentsOf(test.lgb.getSubCriticalGraph());
		/*
		 * Check that component array makes sense
		 */
		long numberCompsGT1 = Arrays.stream(test.lgb.getWhichComponent()).distinct().count();
		GlobalLog.log(Level.INFO, "In sub-critical graph, max Component Size is " + maxSubCritComponentSize);
		GlobalLog.log(Level.INFO, "Number of components of size greater than one: " + numberCompsGT1);
		long numberIsol = IntStream.range(0, test.lgb.getNumberObjects()).filter(i -> test.lgb.getIsIsolatedObject()[i])
				.count();
		GlobalLog.log(Level.INFO, "Number of vertex objects marked as isolated: " + numberIsol);
		/*
		 * Set a component size limit, and export modest graph
		 */
		int componentSizeMax = 30;
		int inSwayLB = test.lgb.unionFindUpToClusterThreshold(componentSizeMax);
		GlobalLog.log(Level.INFO, "How max component size varies with lower bound on in-sway:");
		for (Map.Entry<Integer, Integer> kv : test.lgb.getMaxComponentSizeByInsway().entrySet()) {
			GlobalLog.log(Level.INFO, "Insway at least " + kv.getKey() + ": max component size " + kv.getValue());
		}
		/*
		 * Build modest graph for this component size bound, and find components
		 */
		test.lgb.buildGraphInswayNotLessThan(inSwayLB);
		int modestEdgeCount = test.lgb.getLinkageSubgraph().edgeSet().size();
		GlobalLog.log(Level.INFO,
				"Modest graph with lower bound " + inSwayLB + " on in-sway has " + modestEdgeCount + " edges.");
		int maxModestComponentSize = test.lgb.detectComponentsOf(test.lgb.getLinkageSubgraph());
		GlobalLog.log(Level.INFO, "In modest graph, max Component Size is " + maxModestComponentSize);
		/*
		 * Export (1) modest linkage graph and (2) singleton vertices to a directory
		 */
		test.lgWriter.setObjectStringLookup(test.d2nndg.getObjectStringLookup());
		String prefix = "RBL-" + test.lgb.getNumberObjects() + "-objects-component-size-bound-"
				+ maxModestComponentSize + "-";
		String dirName = prefix + modestEdgeCount + "-links-" + k + "-NN-" + System.currentTimeMillis();
		String fileNameEdges = modestEdgeCount + "-modest-edges.csv";
		test.lgWriter.writeEdgesToFile(dirName, fileNameEdges, test.lgb.getLinkageSubgraph(),
				test.lgb.getWhichComponent());
		GlobalLog.log(Level.INFO, "Modest graph notated with components has been written to file " + fileNameEdges);
		String fileNameVert = test.lgb.getNumSingletons() + "-singletons-among-" + test.lgb.getNumberObjects()
				+ "-objects.csv";
		test.lgWriter.writeSingletonVerticesToFile(dirName, fileNameVert, test.lgb.getLinkageGraph(),
				test.lgb.getIsIsolatedObject());
		GlobalLog.log(Level.INFO, "Singleton objects in modest graph have been written to file " + fileNameVert);
	}

}
