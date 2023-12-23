/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import logging.GlobalLog;

/**
 * Given an n by n rank table write n*(n-1) edges to file, of the form:<p>
 * Object-i, Object-j, -k<p>
 * where k is the rank which i affords to j.
 */
public class OrderedNeighborsToEdgesCSVWriter {
	int[][] orderedNeighbors;
/**
 * Minimal constructor
 */
	public OrderedNeighborsToEdgesCSVWriter() {
		GlobalLog.log(Level.INFO, "Instance of " + OrderedNeighborsToEdgesCSVWriter.class.getName());
	}

	/**
	 * 
	 * @param orderedNeighbors the ordered Neighbors from the rank Table
	 *            <p>
	 *            orderedNeighbors[x][k] = y means y is the k-th nearest neighbor of
	 *            x, according to x's ranking. See
	 *            {@link orientationStructures.ConcordanceExplorer}.
	 */
	public void setOrderedNeighbors(int[][] orderedNeighbors) {
		this.orderedNeighbors = orderedNeighbors;
	}

	/**
	 * 
	 * @param numberNeighbors how many ordered neighbors of x (other than x) to take
	 * @param edgeFileName    file to contain "object, object, weight" triples.
	 */
	public void writeFirstKNeighborsToEdgeFile(int numberNeighbors, String edgeFileName) {
		System.out.println("Writing off-diagonal rank table entries {vertex, vertex, -rank} to file: " + edgeFileName);

		try {
			BufferedWriter cgwriterE = Files.newBufferedWriter(Paths.get(edgeFileName));
			CSVPrinter csvprinterE = new CSVPrinter(cgwriterE, CSVFormat.DEFAULT);
			/*
			 * No header row
			 */
			// csvprinterE.printRecord("source-vertex", "target-vertex", "weight");
			int w;
			String sourceID, targetID;
			for (int i = 0; i < this.orderedNeighbors.length; i++) {
				sourceID = "Object-" + i;
				for (int k = 1; k < numberNeighbors + 1; k++) {
					targetID = "Object-" + this.orderedNeighbors[i][k];
					w = -k;
					csvprinterE.printRecord(sourceID, targetID, w);
				}

			}
			csvprinterE.close();
		} catch (IOException e) {
			System.err.println("Caught IOException when writing edges.");
		}
	}
}
