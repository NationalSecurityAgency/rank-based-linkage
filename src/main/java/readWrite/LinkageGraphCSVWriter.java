/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

import java.io.BufferedWriter;
import java.io.File;
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
 * CSV writer for linkageGraph edges. Will typically be invoked to export
 * multiple graphs
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class LinkageGraphCSVWriter {

	List<String> objectStringLookup;
/**
 * Minimal constructor
 */
	public LinkageGraphCSVWriter() {
		GlobalLog.log(Level.INFO, "Instance of " + LinkageGraphCSVWriter.class.getName());
	}

	/**
	 * 
	 * @param directory      where file is to be placed
	 * @param edgeFileName   name of edge file
	 * @param linkageGraph   graph whose edges will be written to file, with Integer
	 *                       vertices
	 * @param whichComponent gives the component number to which source vertex of an
	 *                       edge belongs.
	 */
	public void writeEdgesToFile(String directory, String edgeFileName, SparseIntUndirectedWeightedGraph linkageGraph,
			int[] whichComponent) {
		GlobalLog.log(Level.INFO,
				"Writing linkage graph edges as {component, vertex, vertex, insway} to file: " + edgeFileName);

		try {
			this.createFolder(directory);
			BufferedWriter cgwriterE = Files.newBufferedWriter(Paths.get(directory, edgeFileName));
			CSVPrinter csvprinterE = new CSVPrinter(cgwriterE, CSVFormat.DEFAULT);
			csvprinterE.printRecord("component", "source-vertex", "target-vertex", "weight");
			int w, c;
			String sourceID, targetID;
			for (Integer e : linkageGraph.edgeSet()) {
				c = whichComponent[linkageGraph.getEdgeSource(e).intValue()];
				sourceID = this.objectStringLookup.get(linkageGraph.getEdgeSource(e));
				targetID = this.objectStringLookup.get(linkageGraph.getEdgeTarget(e));
				w = (int) Math.round(linkageGraph.getEdgeWeight(e));
				csvprinterE.printRecord(c, sourceID, targetID, w);
			}
			csvprinterE.close();

		} catch (

		IOException e) {
			System.err.println("Caught IOException when writing edges.");
		}
	}

	/**
	 * 
	 * @param directory            to write file
	 * @param singleVertexFileName name for file of isolated vertices
	 * @param linkageGraph         subgraph on which components are comnputed
	 * @param isIsolated           array: true at v if vertex v is isolated
	 */
	public void writeSingletonVerticesToFile(String directory, String singleVertexFileName,
			SparseIntUndirectedWeightedGraph linkageGraph, boolean[] isIsolated) {
		GlobalLog.log(Level.INFO, "Writing isolated vertices to file, one per line, to file " + singleVertexFileName);
		try {
			this.createFolder(directory);
			BufferedWriter cgwriterV = Files.newBufferedWriter(Paths.get(directory, singleVertexFileName));
			CSVPrinter csvprinterV = new CSVPrinter(cgwriterV, CSVFormat.DEFAULT);
			csvprinterV.printRecord("isolated vertex");
			for (Integer vertex : linkageGraph.vertexSet()) {
				if (isIsolated[vertex.intValue()]) {
					csvprinterV.printRecord(this.objectStringLookup.get(vertex));
				}
			}
			csvprinterV.close();

		} catch (

		IOException e) {
			System.err.println("Caught IOException when writing singletons.");
		}

	}

	private boolean createFolder(String theFilePath) {
		boolean result = false;

		File directory = new File(theFilePath);

		if (directory.exists()) {
			GlobalLog.log(Level.INFO, "Folder already exists");
		} else {
			result = directory.mkdirs();
		}

		return result;
	}

	/**
	 * @param objectStringLookup the objectStringLookup to set
	 */
	public void setObjectStringLookup(List<String> objectStringLookup) {
		this.objectStringLookup = objectStringLookup;
	}

}
