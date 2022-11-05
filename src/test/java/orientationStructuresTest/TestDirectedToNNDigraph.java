/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructuresTest;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.jgrapht.alg.util.Triple;

import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import readWrite.StringPairCSVReader;

/**
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TestDirectedToNNDigraph {
	DirectedToNNDigraph d2nndg;
	StringPairCSVReader reader;

	/**
	 * 
	 */
	public TestDirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + TestDirectedToNNDigraph.class.getName());
		this.reader = new StringPairCSVReader();
		this.d2nndg = new DirectedToNNDigraph();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		int k = Integer.parseInt(args[1]); // # neighbors
		TestDirectedToNNDigraph test = new TestDirectedToNNDigraph();
		test.reader.readStringPairsWithWeights(fileName);
		int nEdges = test.reader.getWeightedEdges().size();
		GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
		double start = (double) System.currentTimeMillis();
		// test.rsb.setSourceTargetWeightList(test.reader.getWeightedEdges());
		GlobalLog.log(Level.INFO, "Edge list has been passed to DirectedToNNDigraph.");
		test.d2nndg.buildRawSparseGraph(test.reader.getWeightedEdges());
		;
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "Sparse directed graph built in " + duration + " seconds.");
		/*
		 * Inspect vertex index map
		 */
		Random g = new Random();
		int index = g.nextInt(test.d2nndg.getnV());
		String image = test.d2nndg.getSourceTargetStrings().get(index);
		GlobalLog.log(Level.INFO, "Random source vertex: index = " + index + "; string = " + image);
		GlobalLog.log(Level.INFO, "Reverse lookup for image: " + test.d2nndg.getVertexIndex().get(image));
		/*
		 * Two core and K-NN digraph. WARNING! Must test kNearNbrOfVertex() method first. FAILED
		 */
		List<Triple<Integer, Integer, Double>> nbrTest = test.d2nndg.kNearNbrOfVertex(index, k).toList();
		int index0 = nbrTest.get(0).getSecond();
		String image0 = test.d2nndg.getSourceTargetStrings().get(index0);
		double weight = nbrTest.get(0).getThird();
		GlobalLog.log(Level.INFO,"Source: " + image + " Target: " + image0 + " Weight: " + weight);
		
		
		/*
		 * start = (double) System.currentTimeMillis();
		 * test.d2nndg.buildKNearNbrDigraph(k); duration = (double)
		 * (System.currentTimeMillis() - start) / 1000.0; GlobalLog.log(Level.INFO,
		 * "K-NN digraph built in " + duration + " seconds."); int nEKNN =
		 * test.d2nndg.getKNNDigraph().edgeSet().size(); GlobalLog.log(Level.INFO,
		 * "K-NN digraph has " + nEKNN + " edges.");
		 */
	}

}
