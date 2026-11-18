package preprocessing;

/**
 * Record class for one line of network traffic data: a record ID, a source
 * address, a destination address, and a numerical time stamp (for example,
 * seconds since the epoch).
 *
 */
public record TrafficRecord(String recordID, String source, String destination, double time) {

}
