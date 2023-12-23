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
 * Tests {@link readWrite.StringPairCSVReader} class.
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TestStringPairCSVReader {
	StringPairCSVReader reader;

	/**
	 * Minimal constructor
	 */
	public TestStringPairCSVReader() {
		GlobalLog.log(Level.INFO, "Instance of " + TestStringPairCSVReader.class.getName());
		this.reader = new StringPairCSVReader();
	}

	/**
	 * @param args args[0] is the csv file name
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
