/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedWeightedGraph;

import logging.GlobalLog;

/**
 * CSV writer for linkageGraph edges. Make more abstract.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class LinkageGraphCSVWriter {
	SparseIntUndirectedWeightedGraph linkageGraph;
	List<String> objectStringLookup;

	public LinkageGraphCSVWriter(SparseIntUndirectedWeightedGraph graph, List<String> vertexLookup) {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageGraphCSVWriter.class.getName());
		this.linkageGraph = graph;
		this.objectStringLookup = vertexLookup;
	}

	/**
	 * 
	 * @param directory    - where file is to be placed
	 * @param edgeFileName - name of edge file
	 */
	public void writeEdgesToFile(String directory, String edgeFileName) {
		System.out.println("Writing linkage graph edges as {vertex, vertex, insway} to file: " + edgeFileName);

		try {
			BufferedWriter cgwriterE = Files.newBufferedWriter(Paths.get(directory, edgeFileName));
			CSVPrinter csvprinterE = new CSVPrinter(cgwriterE, CSVFormat.DEFAULT);
			csvprinterE.printRecord("source-vertex", "target-vertex", "weight");
			int w;
			String sourceID, targetID;
			for (Integer e : linkageGraph.edgeSet()) {
				sourceID = this.objectStringLookup.get(linkageGraph.getEdgeSource(e));
				targetID = this.objectStringLookup.get(linkageGraph.getEdgeTarget(e));
				w = (int) Math.round(linkageGraph.getEdgeWeight(e));
				csvprinterE.printRecord(sourceID, targetID, w);
			}
			csvprinterE.close();

		} catch (

		IOException e) {
			System.err.println("Caught IOException when writing edges.");
		}
	}

}
