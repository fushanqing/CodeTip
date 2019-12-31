package codetip.commons.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author followWindD
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MavenCoordinate {

	private Location location;
	private Stamp stamp;

	@Data
	@NoArgsConstructor
    @AllArgsConstructor
    @Builder
	public static class Location {
		private String groupId;
		private String artifactId;
	}

	@Data
	@NoArgsConstructor
    @AllArgsConstructor
    @Builder
	public static class Stamp {
		private String version;
		private String lastUpdated;
        private boolean downloaded = false;
    }
	
}
