package org.ohmage.domain;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.ohmage.domain.Observer.Stream;
import org.ohmage.exception.DomainException;

/**
 * This class represents a set of data generated by a stream. It retains the
 * reference to the stream that was used to decode the data.
 *
 * @author John Jenkins
 */
public class DataStream {
	public static class InvalidDataException extends DomainException {
		/**
		 * Randomly generated serial version UID used for serialization.
		 */
		private static final long serialVersionUID = -5437115762710099279L;
		
		public InvalidDataException(
			final String reason) {
			
			this(reason, null);
		}

		public InvalidDataException(
			final String reason,
			final Throwable cause) {
			
			super(reason, cause);
		}
	}
	
	/**
	 * This class represents the meta-data for a data stream. All fields are 
	 * optional. This class is immutable and, therefore, thread-safe.
	 *
	 * @author John Jenkins
	 */
	public static class MetaData {
		/**
		 * This class is responsible for building new MetaData objects. This
		 * class is mutable and, therefore, not thread-safe.
		 *
		 * @author John Jenkins
		 */
		public static class Builder {
			// ID
			public static final String JSON_KEY_ID = "id";
			
			// Time
			public static final String JSON_KEY_TIME = "time"; 
			public static final String JSON_KEY_TIMEZONE = "timezone";
			public static final String JSON_KEY_TIMESTAMP = "timestamp";
			
			// Location
			public static final String JSON_KEY_LOCATION = "location";
			
			private String id = null;
			private DateTime timestamp = null;
			private Location location = null;
			
			/**
			 * Creates an empty builder.
			 */
			public Builder() {};
			
			/**
			 * Returns true if an ID has been set; false, otherwise.
			 * 
			 * @return True if an ID has been set; false, otherwise.
			 */
			public boolean hasId() {
				return id != null;
			}
			
			/**
			 * Sets the ID.
			 * 
			 * @param id The ID.
			 */
			public void setId(final String id) {
				this.id = id;
			}
			
			/**
			 * Decomposes a JsonNode from Jackson into an ID and stores it.
			 * 
			 * @param metaDataNode The meta-data node from an upload.
			 * 
			 * @throws DomainException Never thrown.
			 */
			public void setId(
					final JsonNode metaDataNode)
					throws DomainException {
				
				if(metaDataNode == null) {
					return;
				}
				
				if(metaDataNode.has(JSON_KEY_ID)) {
					JsonNode idNode = metaDataNode.get(JSON_KEY_ID);
					
					if(idNode.isValueNode()) {
						this.id = idNode.asText();
					}
					else {
						throw new DomainException(
							"The ID JSON is not a value.");
					}
				}
			}
			
			/**
			 * Returns true if a time stamp has been set; false, otherwise.
			 * 
			 * @return True if a time stamp has been set; false, otherwise.
			 */
			public boolean hasTimestamp() {
				return timestamp != null;
			}
			
			/**
			 * Sets the timestamp.
			 * 
			 * @param timetamp The timestamp.
			 */
			public void setTimestamp(final DateTime timestamp) {
				this.timestamp = timestamp;
			}
			
			/**
			 * Decomposes a JsonNode from Jackson into a timestamp and stores
			 * it.
			 * 
			 * @param metaDataNode The meta-data node from an upload.
			 * 
			 * @throws DomainException The timestamp node was invalid.
			 */
			public void setTimestamp(
					final JsonNode metaDataNode) 
					throws DomainException {
				
				if(metaDataNode == null) {
					return;
				}
				
				List<DateTime> timestampRepresentations =
					new LinkedList<DateTime>();

				// Get the timestamp if the time and timezone fields were
				// specified.
				if(metaDataNode.has(JSON_KEY_TIME)) {
					JsonNode timeNode = metaDataNode.get(JSON_KEY_TIME);
					
					if(! timeNode.isNumber()) {
						throw new DomainException("The time isn't a number.");
					}
					long time = timeNode.getNumberValue().longValue();
					
					DateTimeZone timeZone = DateTimeZone.UTC;
					if(metaDataNode.has(JSON_KEY_TIMEZONE)) {
						JsonNode timeZoneNode =
							metaDataNode.get(JSON_KEY_TIMEZONE);
						
						if(! timeZoneNode.isTextual()) {
							throw new DomainException(
								"The time zone is not a string.");
						}
						
						try {
							timeZone = 
								DateTimeZone.forID(
									timeZoneNode.getTextValue());
						}
						catch(IllegalArgumentException e) {
							throw new DomainException(
								"The time zone is unknown: " +
									timeZoneNode.getTextValue());
						}
					}
					
					timestampRepresentations.add(new DateTime(time, timeZone));
				}
				
				// Get the timestamp if the timestamp field was specified.
				if(metaDataNode.has(JSON_KEY_TIMESTAMP)) {
					JsonNode timestampNode =
						metaDataNode.get(JSON_KEY_TIMESTAMP);
					
					if(! timestampNode.isTextual()) {
						throw new DomainException(
							"The timestamp value was not a string.");
					}
					
					try {
						timestampRepresentations
							.add( 
								ISOW3CDateTimeFormat
									.any()
										.parseDateTime(
											timestampNode.getTextValue()));
					}
					catch(IllegalArgumentException e) {
						throw new DomainException(
							"The timestamp was not a valid ISO 8601 timestamp.",
							e);
					}
				}
				
				// Ensure that all representations of time are equal.
				if(timestampRepresentations.size() > 0) {
					// Create an iterator to cycle through the representations.
					Iterator<DateTime> timestampRepresentationsIter =
						timestampRepresentations.iterator();
					
					// The first timestamp will be set as the result. 
					DateTime timestamp = timestampRepresentationsIter.next();
					
					// Check against all subsequent timestamps to ensure that
					// they represent the same point in time.
					while(timestampRepresentationsIter.hasNext()) {
						if(timestamp.getMillis() != 
							timestampRepresentationsIter.next().getMillis()) {
							
							throw
								new DomainException(
									"Multiple representations of the timestamp were given, and they are not equal.");
						}
					}
					
					// If we checked out all of the timestamps and they are
					// equal, then save this timestamp.
					this.timestamp = timestamp;
				}
			}
			
			/**
			 * Returns true if a location has been set; false, otherwise.
			 * 
			 * @return True if a location has been set; false, otherwise.
			 */
			public boolean hasLocation() {
				return location != null;
			}
			
			/**
			 * Sets the location.
			 * 
			 * @param location The Location object.
			 */
			public void setLocation(final Location location) {
				this.location = location;
			}
			
			/**
			 * Decomposes a JsonNode from Jackson into a location and stores
			 * it.
			 * 
			 * @param metaDataNode The meta-data node from an upload.
			 * 
			 * @throws DomainException The location node was invalid.
			 */
			public void setLocation(
					final JsonNode metaDataNode) 
					throws DomainException {
				
				if(metaDataNode == null) {
					return;
				}
				
				if(metaDataNode.has(JSON_KEY_LOCATION)) {
					location =
						new Location(metaDataNode.get(JSON_KEY_LOCATION));
				}
			}
			
			/**
			 * Builds the MetaData object.
			 * 
			 * @return The MetaData object.
			 */
			public MetaData build() throws DomainException {
				return new MetaData(id, timestamp, location);
			}
		}
		
		private final String id;
		private final DateTime timestamp;
		private final Location location;
		
		/**
		 * Creates a new MetaData object.
		 * 
		 * @param timestamp The time stamp for this meta-data.
		 * 
		 * @param location The location for this meta-data.
		 */
		public MetaData(
				final String id,
				final DateTime timestamp, 
				final Location location)
				throws DomainException{
			
			// Validate the ID and then save it.
			this.id = id;
			
			// Validate the timestamp and then save it.
			if((timestamp != null) && timestamp.isAfterNow()) {
				long now = (new DateTime()).getMillis();
				throw
					new DomainException(
						"The timestamp cannot be in the future: " +
							"Now: " + now + " " +
							"Given: " + timestamp.getMillis() + " " +
							"Difference: " + (timestamp.getMillis() - now));
			}
			this.timestamp = timestamp;
			
			// Validate the location and then save it.
			this.location = location;
		}
		
		/**
		 * Returns the ID.
		 * 
		 * @return The ID.
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns timestamp.
		 *
		 * @return The timestamp.
		 */
		public DateTime getTimestamp() {
			return timestamp;
		}

		/**
		 * Returns location.
		 *
		 * @return The location.
		 */
		public Location getLocation() {
			return location;
		}
	}
	private final MetaData metaData;
	
	/**
	 * The stream that defines how this data is represented.
	 */
	private final Stream stream;
	
	/**
	 * The data in its Jackson object representation.
	 */
	private final JsonNode data;

	/**
	 * Creates a new DataStream from JSON data encoded as a JsonNode.
	 * 
	 * @param stream The stream that contains the definition on how to decode
	 *				 the data.
	 *
	 * @param metaData The meta-data.
	 * 
	 * @param data The data.
	 * 
	 * @throws DomainException One of the parameters is invalid or null.
	 */
	public DataStream(
			final Stream stream,
			final MetaData metaData,
			final JsonNode data) 
			throws DomainException {

		if(stream == null) {
			throw new DomainException("The stream is null.");
		}
		else if(data == null) {
			throw new DomainException("The data is null.");
		}
		
		// Save the reference to the stream.
		this.stream = stream;
		
		// Save the meta-data.
		this.metaData = metaData;
		
		// Decode the data from the stream.
		this.data = data;
	}

	/**
	 * Returns the stream.
	 *
	 * @return The stream.
	 */
	public Stream getStream() {
		return stream;
	}

	/**
	 * Returns the meta-data.
	 *
	 * @return The meta-data.
	 */
	public MetaData getMetaData() {
		return metaData;
	}
	
	/**
	 * Returns a JsonNode for the data.
	 * 
	 * @return A JsonNode for the data.
	 */
	public JsonNode getData() {
		return data;
	}
}