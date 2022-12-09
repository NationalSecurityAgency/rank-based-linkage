
/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import logging.GlobalLog;

/**
 * Reads triples of form (String, String, double), and places them in a list of
 * records of type {@link StringPairWithWeight}.
 * <p>
 * The triples can represent either a directed or undirected weighted linkageGraph.
 * There is no restriction of the number of targets associated with each source.
 * The K-NN extraction occurs later. 
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class StringPairCSVReader {
	private List<StringPairWithWeight> weightedEdges;
/**
 * Minimal constructor
 */
	public StringPairCSVReader() {
		GlobalLog.log(Level.INFO, "Instance of " + StringPairCSVReader.class.getName());
		this.weightedEdges = new ArrayList<>();
	}

	/**
	 * Read triples from CSV file
	 * 
	 * @param csvFileName name of CSV file
	 */
	public void readStringPairsWithWeights(String csvFileName) {
		try {
			Reader in = new FileReader(csvFileName);
			Iterable<CSVRecord> lines = CSVFormat.RFC4180.parse(in);
			int counter = 0;
			for (CSVRecord ssw : lines) {
				this.weightedEdges
						.add(new StringPairWithWeight(ssw.get(0), ssw.get(1), Double.parseDouble(ssw.get(2))));
				counter++;
			}
			GlobalLog.log(Level.INFO, counter + " edges were read from file.");

		} catch (Exception e) {
			GlobalLog.log(Level.INFO, e.toString());
			e.printStackTrace();
		}

	}

	/**
	 * @return the weightedEdges
	 */
	public List<StringPairWithWeight> getWeightedEdges() {
		return weightedEdges;
	}

}
