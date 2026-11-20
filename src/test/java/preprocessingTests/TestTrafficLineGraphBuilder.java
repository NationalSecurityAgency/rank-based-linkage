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
 * Deterministic test for {@link preprocessing.TrafficLineGraphBuilder}.
 * <p>
 * Uses six hand-checkable traffic records:
 * <p>
 * {@code r1 A->B at 1, r2 B->C at 2, r3 B->D at 5, r4 C->A at 3, r5 B->C at 0,
 * r6 D->B at 6}.
 * <p>
 * Expected unbounded-time edges are
 * {@code {r1,r2,1}, {r1,r3,4}, {r2,r4,1}, {r3,r6,1}, {r5,r4,3}}.
 * Record {@code r5} is not a successor of {@code r1} because it precedes
 * {@code r1} in time.
 * <p>
 * With maximum time difference {@code 2}, only weight-1 edges remain.
 */
public class TestTrafficLineGraphBuilder {

	/**
	 * Fixed traffic-record fixture used by all checks in this class.
	 */
	static final List<TrafficRecord> records = List.of(new TrafficRecord("r1", "A", "B", 1.0),
			new TrafficRecord("r2", "B", "C", 2.0), new TrafficRecord("r3", "B", "D", 5.0),
			new TrafficRecord("r4", "C", "A", 3.0), new TrafficRecord("r5", "B", "C", 0.0),
			new TrafficRecord("r6", "D", "B", 6.0));

	/**
	 * Runs two assertions of the line-graph construction:
	 * <ol>
	 * <li>without a maximum time-difference restriction, and</li>
	 * <li>with maximum time difference equal to {@code 2.0}.</li>
	 * </ol>
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		check(OptionalDouble.empty(), Set.of("r1 r2 1.0", "r1 r3 4.0", "r2 r4 1.0", "r3 r6 1.0", "r5 r4 3.0"));
		check(OptionalDouble.of(2.0), Set.of("r1 r2 1.0", "r2 r4 1.0", "r3 r6 1.0"));
		GlobalLog.log(Level.INFO, "TestTrafficLineGraphBuilder passed.");
	}

	/**
	 * Builds a line graph under a given temporal cutoff and checks exact edge
	 * equality against the expected set.
	 *
	 * @param maxDiff optional maximum allowed time difference between predecessor
	 *                and successor records
	 * @param expected expected edge set encoded via
	 *                 {@link StringPairWithWeight#toString()} values
	 * @throws IllegalStateException if observed edges differ from {@code expected}
	 */
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