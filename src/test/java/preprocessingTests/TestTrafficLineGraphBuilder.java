package preprocessingTests;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import logging.GlobalLog;
import preprocessing.TrafficLineGraphBuilder;
import preprocessing.TrafficRecord;
import readWrite.StringPairWithWeight;

/**
 * Test the {@link preprocessing.TrafficLineGraphBuilder} class on six traffic
 * records whose line graph can be checked by hand:
 * <p>
 * r1 A&rarr;B at 1, r2 B&rarr;C at 2, r3 B&rarr;D at 5, r4 C&rarr;A at 3, r5
 * B&rarr;C at 0, r6 D&rarr;B at 6.
 * <p>
 * Expected edges: {r1, r2, 1}, {r1, r3, 4}, {r2, r4, 1}, {r3, r6, 1}, {r5, r4,
 * 3}. Record r5 is not a successor of r1, because it precedes r1. With maximum
 * time difference 2, only the three edges of weight 1 remain.
 *
 */
public class TestTrafficLineGraphBuilder {

	static final List<TrafficRecord> records = List.of(new TrafficRecord("r1", "A", "B", 1.0),
			new TrafficRecord("r2", "B", "C", 2.0), new TrafficRecord("r3", "B", "D", 5.0),
			new TrafficRecord("r4", "C", "A", 3.0), new TrafficRecord("r5", "B", "C", 0.0),
			new TrafficRecord("r6", "D", "B", 6.0));

	/**
	 * @param args none
	 */
	public static void main(String[] args) {
		check(OptionalDouble.empty(), Set.of("r1 r2 1.0", "r1 r3 4.0", "r2 r4 1.0", "r3 r6 1.0", "r5 r4 3.0"));
		check(OptionalDouble.of(2.0), Set.of("r1 r2 1.0", "r2 r4 1.0", "r3 r6 1.0"));
		GlobalLog.log(Level.INFO, "TestTrafficLineGraphBuilder passed.");
	}

	static void check(OptionalDouble maxDiff, Set<String> expected) {
		TrafficLineGraphBuilder tlgb = new TrafficLineGraphBuilder(records, maxDiff);
		tlgb.buildLineGraph();
		Set<String> found = tlgb.getLineGraphEdges().stream().map(StringPairWithWeight::toString)
				.collect(Collectors.toSet());
		GlobalLog.log(Level.INFO, "Maximum time difference " + maxDiff + ": line graph edges " + found);
		if (!found.equals(expected) || tlgb.getLineGraphEdges().size() != expected.size()) {
			throw new IllegalStateException("Expected " + expected + " but found " + found);
		}
	}

}
