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
 * Reads weighted edge records from CSV into {@link StringPairWithWeight}
 * objects.
 * <p>
 * Expected CSV row format is:
 * {@code sourceLabel,targetLabel,weight}, where the third field is parsed as a
 * {@code double}.
 * <p>
 * The data may represent directed or undirected weighted graphs. No per-source
 * degree restriction is imposed at read time; nearest-neighbor extraction occurs
 * in later processing stages.
 *
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public class StringPairCSVReader {
	private List<StringPairWithWeight> weightedEdges;

	/**
	 * Creates a reader instance with an initially empty edge list.
	 */
	public StringPairCSVReader() {
		GlobalLog.log(Level.INFO, "Instance of " + StringPairCSVReader.class.getName());
		this.weightedEdges = new ArrayList<>();
	}

	/**
	 * Reads edge triples from a CSV file and appends them to the internal list.
	 * <p>
	 * Each parsed row contributes one {@link StringPairWithWeight} record. If this
	 * method is called multiple times, previously read edges are retained.
	 *
	 * @param csvFileName path to the CSV input file
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
	 * Returns the accumulated list of weighted edges read so far.
	 *
	 * @return mutable list of {@link StringPairWithWeight} records
	 */
	public List<StringPairWithWeight> getWeightedEdges() {
		return weightedEdges;
	}

}