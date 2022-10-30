/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package main;

import java.util.logging.Level;

import algorithms.LinkageGraphBuilder;
import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import readWrite.StringPairCSVReader;

/**
 * In this preview version, input can be either
 * <p>
 * &bull; Directed graph D with edge weights, supplied as multiple lines of form
 * (String source, String target, Double weight), or
 * <p>
 * &bull; Undirected edge-weighted graph U in this format.
 * <p>
 * Step I: extract the 2-core of U, or of the undirected form of D, as
 * appropriate. After that, every object has total degree at least two in either
 * case, and therefore participates in at least one "voter triangle" {xy, yz,
 * yz}.
 * <p>
 * Step II: given in integer parameter K &ge; 2 (supplied at run time), scan the
 * neighbors (case U) or out-neighbors (case D) of each object x, and retain
 * edges only for the K nearest according to x's ranking (or d nearest, if x has
 * d &\lt; K neighbors in case U or out-neighbors in case D). Call this set
 * Gamma(x). Even in the case of input U, we save results as a DIRECTED weighted
 * graph (S, A, w), because y in Gamma(x) does not imply x in Gamma(y). The
 * weight function w is the same as the one we started with. As explained in the
 * paper
 * <p>
 * &bull; <i>Rank-based linkage I: triplet comparisons and oriented simplicial
 * complexes</i>, Darling, Grilliette and Logan, 2022
 * <p>
 * This data supplies an orientation of the line graph of an undirected
 * K-nearest neighbor graph on a set S of n objects.
 * <p>
 * The present JAVA API creates the edge-weighted linkage graph (S, B, sigma).
 * Here B consists of pairs of mutual friends, and sigma is the in-sway. As t varies, the
 * graph components for edges with in-sway at least t supplies a partition of S.
 * This is rank-based linkage hierarchical clustering.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class ClusteringByInsway {
	DirectedToNNDigraph rsb;
	StringPairCSVReader reader;
	LinkageGraphBuilder lgb;

	public ClusteringByInsway() {
		GlobalLog.log(Level.INFO, "Instance of " + ClusteringByInsway.class.getName());
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
