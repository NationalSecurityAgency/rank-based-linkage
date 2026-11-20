/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWriteTests;

import java.util.Random;
import java.util.logging.Level;

import logging.GlobalLog;
import readWrite.StringPairCSVReader;
import readWrite.StringPairWithWeight;

/**
 * Simple test driver for {@link StringPairCSVReader}.
 * <p>
 * The program reads weighted edge records from a CSV file, logs the total
 * number of parsed edges, and logs one randomly selected edge for spot-checking
 * input parsing.
 *
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public class TestStringPairCSVReader {
	StringPairCSVReader reader;

	/**
	 * Creates a test instance with a fresh {@link StringPairCSVReader}.
	 */
	public TestStringPairCSVReader() {
		GlobalLog.log(Level.INFO, "Instance of " + TestStringPairCSVReader.class.getName());
		this.reader = new StringPairCSVReader();
	}

	/**
	 * Runs the CSV reader smoke test.
	 * <p>
	 * Expected argument:
	 * <ol>
	 * <li>{@code args[0]}: path to a CSV file of
	 * {@code source,target,weight} records</li>
	 * </ol>
	 *
	 * @param args command-line arguments, where {@code args[0]} is the CSV file
	 *             name
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		TestStringPairCSVReader test = new TestStringPairCSVReader();
		test.reader.readStringPairsWithWeights(fileName);
		int nEdges = test.reader.getWeightedEdges().size();
		GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
		Random g = new Random();
		int index = g.nextInt(nEdges);
		StringPairWithWeight e = test.reader.getWeightedEdges().get(index);
		GlobalLog.log(Level.INFO, "Randomly selected edge: source = " + e.source() + ", target = " + e.target()
				+ ", weight = " + e.weight());

	}

}