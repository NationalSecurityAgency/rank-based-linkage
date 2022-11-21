/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.DoubleSummaryStatistics;
import java.util.logging.Level;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import orientationStructures.UndirectedToNNDigraph;
import readWrite.LinkageGraphCSVWriter;
import readWrite.StringPairCSVReader;

/**
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
	 * 
	 */
	public TestLinkageGraphBuilder() {
		GlobalLog.log(Level.INFO, "Instance of " + TestLinkageGraphBuilder.class.getName());
		this.reader = new StringPairCSVReader();
		this.d2nndg = new DirectedToNNDigraph();
		this.u2nndg = new UndirectedToNNDigraph();
		this.lgb = new LinkageGraphBuilder(higherWeightMoreSimilar);
		this.lgWriter = new LinkageGraphCSVWriter();
	}

	/**
	 * Current version works for directed weighted linkageGraph input. Need to
	 * prepare undirected input, and distinguish two cases. Pass a third argument?
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
					"# links with in-sway exactly " + j + ": " + test.lgb.getLinkCounByInswayValue()[j]
							+ "; # links with in-sway GREATER than " + j + ": "
							+ test.lgb.getLinkCountAboveInswayValue()[j]);
		}
		GlobalLog.log(Level.INFO, "Critical in-sway: " + test.lgb.getCriticalInsway());
		test.lgb.buildTwoCriticalGraphs();
		int nCritEdges = test.lgb.getCriticalGraph().edgeSet().size();
		GlobalLog.log(Level.INFO, "Critical graph has " + nCritEdges + " edges.");
		int nSubcEdges = test.lgb.getSubCriticalGraph().edgeSet().size();
		GlobalLog.log(Level.INFO, "Subcritical graph has " + nSubcEdges + " edges.");

		/*
		 * Export both critical and subcritical linkage graphs to a single directory
		 */
		test.lgWriter.setObjectStringLookup(test.d2nndg.getObjectStringLookup());
		String prefix = "RBL-" + test.lgb.getNumberObjects() + "-objects-";
		String dirName = prefix + inSwayStatistics.getCount() + "-links-"
				+ k + "-NN-" + System.currentTimeMillis();
		String fileNameCrit = prefix + k + "-NN-critical-" + nCritEdges + ".csv";
		test.lgWriter.writeEdgesToFile(dirName, fileNameCrit, test.lgb.getCriticalGraph());
		GlobalLog.log(Level.INFO, "Critical graph has been written to file " + fileNameCrit);
		String fileNameSubC = prefix + k + "-NN-subcritical" + nSubcEdges + ".csv";
		test.lgWriter.writeEdgesToFile(dirName, fileNameSubC, test.lgb.getSubCriticalGraph());
		GlobalLog.log(Level.INFO, "Subcritical graph has been written to file " + fileNameSubC);
	}

}
