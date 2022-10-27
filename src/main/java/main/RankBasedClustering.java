/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package main;

import java.util.logging.Level;

import logging.GlobalLog;

/**
 * Takes an orientation of the line graph of an undirected K-nearest neighbor
 * graph on a set S of n objects, in the form of a partial ranking table,
 * and creates the edge-weighted linkage graph (S, L, sigma).
 * Here L consists of pairs of mutual friends, and sigma is the in-sway.
 * The graph components for edges with in-sway at least t is one of the partitions
 * in rank-based linkage hierarchical clustering.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class RankBasedClustering {

	public RankBasedClustering() {
		GlobalLog.log(Level.INFO, "Instance of " + RankBasedClustering.class.getName());
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
