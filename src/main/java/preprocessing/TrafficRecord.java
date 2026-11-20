package preprocessing;

/**
 * Immutable representation of one network-traffic event.
 * <p>
 * Each instance corresponds to one input row and stores a unique record
 * identifier, source and destination addresses, and a numeric event time
 * stamp (for example, seconds since the Unix epoch).
 *
 * @param recordID unique identifier for the traffic record row
 * @param source source address (host, endpoint, or node label)
 * @param destination destination address (host, endpoint, or node label)
 * @param time event time stamp represented as a numeric value
 */
public record TrafficRecord(String recordID, String source, String destination, double time) {

}