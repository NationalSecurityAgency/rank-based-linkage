/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructuresTest;

import java.util.Random;
import java.util.logging.Level;

import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import readWrite.StringPairCSVReader;

/**
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class DirectedToNNDigraphTest {
	DirectedToNNDigraph rsb;
	StringPairCSVReader reader;

	/**
	 * 
	 */
	public DirectedToNNDigraphTest() {
		GlobalLog.log(Level.INFO, "Instance of " + DirectedToNNDigraphTest.class.getName());
		this.reader = new StringPairCSVReader();
		this.rsb = new DirectedToNNDigraph();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		DirectedToNNDigraphTest test = new DirectedToNNDigraphTest();
		test.reader.readStringPairsWithWeights(fileName);
		int nEdges = test.reader.getWeightedEdges().size();
		GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
		test.rsb.setSourceTargetWeightList(test.reader.getWeightedEdges());
		GlobalLog.log(Level.INFO, "Edge list has been passed to DirectedToNNDigraph.");
		double start = (double) System.currentTimeMillis();
		test.rsb.buildSparseDirectedGraph();
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "Sparse directed graph built in " + duration + " seconds.");
		/*
		 * Inspect vertex index map
		 */
		Random g = new Random();
		int index = g.nextInt(test.rsb.getnV());
		String image = test.rsb.getSourceStrings().get(index);
		GlobalLog.log(Level.INFO, "Random source vertex: index = " + index + "; string = " + image);
		GlobalLog.log(Level.INFO, "Reverse lookup for image: " + test.rsb.getVertexIndex().get(image));
		/*
		 * Two core
		 */
		test.rsb.extractTwoCore();
	}

}
