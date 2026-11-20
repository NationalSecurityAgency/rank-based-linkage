package preprocessing;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import logging.GlobalLog;
import readWrite.StringPairWithWeight;

/**
 * Preprocessor for network traffic data, as proposed by R. W. R. Darling, and implemented by
 * Dukotah Hutcheon (2026).
 * 
 * <p>
 * Input is a CSV file where each line is: record ID, source address,
 * destination address, time stamp. The time stamp must be numerical (for
 * example, seconds since the epoch). A header line is allowed, and is detected
 * by a time stamp field which is not a number.
 * <p>
 * Build the undirected line graph in which records e and f are adjacent if the
 * destination of e is the source of f, and e precedes f in time (strictly).
 * The weight of the edge {e, f} is the time difference between e and f, so
 * that each record ranks its neighbors by absolute difference between their
 * time stamps and its own time stamp.
 * <p>
 * Output is a CSV file without header, whose lines are of the form
 * <p>
 * (String e, String f, double time difference)
 * <p>
 * which becomes an input for rbl.jar with Ul as input parameter.
 * <p>
 * Example:
 * <p>
 * "java -cp rbl.jar preprocessing.TrafficLineGraphBuilder traffic.csv
 * lineGraph.csv 3600"
 * <p>
 * "java -jar rbl.jar lineGraph.csv Ul 8"
 * <p>
 * The optional third argument is a maximum time difference: pairs (e, f) with
 * time(f) - time(e) exceeding it are not linked. Without it, the line graph may
 * have a number of edges quadratic in the number of records.
 *
 *
 */
public class TrafficLineGraphBuilder {

	private List<TrafficRecord> records;
	private OptionalDouble maxTimeDifference;
	private List<StringPairWithWeight> lineGraphEdges;

	/**
	 * Creates a line-graph builder for a fixed set of traffic records and an
	 * optional maximum time-difference constraint.
	 *
	 *
	 * @param trafficRecords    input list of records
	 * @param maxTimeDifference optional upper bound on time difference of linked
	 *                          records
	 */
	public TrafficLineGraphBuilder(List<TrafficRecord> trafficRecords, OptionalDouble maxTimeDifference) {
		GlobalLog.log(Level.INFO, "Instance of " + TrafficLineGraphBuilder.class.getName());
		this.records = trafficRecords;
		this.maxTimeDifference = maxTimeDifference;
		this.lineGraphEdges = new ArrayList<>();
	}

	/**
	 * Command-line entry point.
	 * <p>
	 * Expected arguments:
	 * <ol>
	 * <li>{@code args[0]} input CSV file of traffic records</li>
	 * <li>{@code args[1]} output CSV file for line-graph edges</li>
	 * <li>(optional) {@code args[2]} maximum time difference between linked
	 * records</li>
	 * </ol>
	 *
	 * @param args command-line arguments as described above
	 * @throws RuntimeException if the number of arguments is not 2 or 3
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			throw new RuntimeException("Two or three arguments are needed: \n filename for traffic records "
					+ "\n filename for line graph edges, and \n (optional) maximum time difference");
		}
		OptionalDouble maxDiff = OptionalDouble.empty();
		if (args.length > 2) {
			maxDiff = OptionalDouble.of(Double.parseDouble(args[2]));
		}
		List<TrafficRecord> trafficRecords = readTrafficRecords(args[0]);
		GlobalLog.log(Level.INFO, trafficRecords.size() + " traffic records were read from file " + args[0]);
		TrafficLineGraphBuilder tlgb = new TrafficLineGraphBuilder(trafficRecords, maxDiff);
		tlgb.buildLineGraph();
		tlgb.writeLineGraphToFile(args[1]);
	}

	/**
	 * Read records of form (record ID, source, destination, time stamp) from CSV
	 * file. A first line whose time stamp is not a number is treated as a header.
	 * <p>
	 * Whitespace around fields is stripped before record creation.
	 *
	 * @param csvFileName name of CSV file
	 * @return list of traffic records
	 */
	public static List<TrafficRecord> readTrafficRecords(String csvFileName) {
		List<TrafficRecord> trafficRecords = new ArrayList<>();
		try {
			Reader in = new FileReader(csvFileName);
			Iterable<CSVRecord> lines = CSVFormat.RFC4180.parse(in);
			boolean firstLine = true;
			for (CSVRecord r : lines) {
				double t;
				try {
					t = Double.parseDouble(r.get(3).strip());
				} catch (NumberFormatException e) {
					if (firstLine) {
						GlobalLog.log(Level.INFO, "Header line skipped: " + r.toList());
						firstLine = false;
						continue;
					}
					throw e;
				}
				firstLine = false;
				trafficRecords.add(new TrafficRecord(r.get(0).strip(), r.get(1).strip(), r.get(2).strip(), t));
			}
			in.close();
		} catch (IOException e) {
			GlobalLog.log(Level.INFO, e.toString());
			e.printStackTrace();
		}
		return trafficRecords;
	}

	/**
	 * For each record e, find the records f whose source is the destination of
	 * e, and whose time stamp is later than that of e (and within the maximum time
	 * difference, if one was given). Records are grouped by source address and
	 * sorted by time, so each search is a binary search followed by a scan.
	 */
	public void buildLineGraph() {
		Map<String, List<TrafficRecord>> bySource = new HashMap<>();
		for (TrafficRecord r : this.records) {
			bySource.computeIfAbsent(r.source(), s -> new ArrayList<>()).add(r);
		}
		for (List<TrafficRecord> list : bySource.values()) {
			list.sort(Comparator.comparingDouble(TrafficRecord::time));
		}
		this.lineGraphEdges.clear();
		for (TrafficRecord e : this.records) {
			List<TrafficRecord> successors = bySource.get(e.destination());
			if (successors == null) {
				continue;
			}
			for (int i = firstIndexLaterThan(successors, e.time()); i < successors.size(); i++) {
				TrafficRecord f = successors.get(i);
				double diff = f.time() - e.time();
				if (this.maxTimeDifference.isPresent() && diff > this.maxTimeDifference.getAsDouble()) {
					break;
				}
				this.lineGraphEdges.add(new StringPairWithWeight(e.recordID(), f.recordID(), diff));
			}
		}
		GlobalLog.log(Level.INFO, "Line graph built, with " + this.lineGraphEdges.size() + " edges on "
				+ this.records.size() + " records.");
	}

	/**
	 * Returns the index of the first record in a time-sorted list with time strictly
	 * greater than {@code t}.
	 *
	 * @param sorted list sorted by nondecreasing {@link TrafficRecord#time()}
	 * @param t reference time
	 * @return smallest index {@code i} such that {@code sorted.get(i).time() > t},
	 *         or {@code sorted.size()} if no such index exists
	 */
	private static int firstIndexLaterThan(List<TrafficRecord> sorted, double t) {
		int lo = 0;
		int hi = sorted.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (sorted.get(mid).time() > t) {
				hi = mid;
			} else {
				lo = mid + 1;
			}
		}
		return lo;
	}

	/**
	 * Write line graph edges as (record ID, record ID, time difference), without
	 * header, for use as rbl.jar input with option Ul.
	 *
	 * @param fileName output CSV file
	 */
	public void writeLineGraphToFile(String fileName) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName));
			CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.RFC4180);
			for (StringPairWithWeight e : this.lineGraphEdges) {
				csvprinter.printRecord(e.source(), e.target(), e.weight());
			}
			csvprinter.close();
			GlobalLog.log(Level.INFO,
					"Line graph with " + this.lineGraphEdges.size() + " edges has been written to file " + fileName);
		} catch (IOException e) {
			System.err.println("Caught IOException when writing line graph edges.");
		}
	}

	/**
	 * Returns the currently built line-graph edges.
	 *
	 * @return list of {@link StringPairWithWeight} records of the form
	 *         {@code (recordID_e, recordID_f, time(f)-time(e))}
	 */
	public List<StringPairWithWeight> getLineGraphEdges() {
		return lineGraphEdges;
	}

}
