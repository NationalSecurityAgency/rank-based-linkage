/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.DoubleSummaryStatistics;
import java.util.logging.Level;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import readWrite.StringPairCSVReader;

/**
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TestLinkageGraphBuilder {
	DirectedToNNDigraph rsb;
	StringPairCSVReader reader;
	LinkageGraphBuilder lgb;
	final boolean higherWeightMoreSimilar = true;

	/**
	 * 
	 */
	public TestLinkageGraphBuilder() {
		GlobalLog.log(Level.INFO, "Instance of " + TestLinkageGraphBuilder.class.getName());
		this.reader = new StringPairCSVReader();
		this.rsb = new DirectedToNNDigraph();
		this.lgb = new LinkageGraphBuilder(higherWeightMoreSimilar);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		TestLinkageGraphBuilder test = new TestLinkageGraphBuilder();
		test.reader.readStringPairsWithWeights(fileName);
		int nEdges = test.reader.getWeightedEdges().size();
		GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
		test.rsb.setSourceTargetWeightList(test.reader.getWeightedEdges());
		GlobalLog.log(Level.INFO, "Edge list has been passed to OldDirectedToNNDigraph.");
		double start = (double) System.currentTimeMillis();
		test.rsb.buildSparseDirectedGraph();
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "Sparse directed graph built in " + duration + " seconds.");
		/*
		 * Test of LinkageGraphBuilder starts here.
		 */
		test.lgb.setDigraph(test.rsb.getKNNDigraph());
		GlobalLog.log(Level.INFO, "Sparse directed graph passed to LinkageGraphBuilder.");
		test.lgb.buildLinkageGraph();
		DoubleSummaryStatistics inSwayStatistics = test.lgb.getLinkageGraph().edgeSet().stream()
				.mapToDouble(e->test.lgb.getLinkageGraph().getEdgeWeight(e)).summaryStatistics();
		GlobalLog.log(Level.INFO,"Insway summary statistics:");
		GlobalLog.log(Level.INFO, inSwayStatistics.toString());
					
	}

}
