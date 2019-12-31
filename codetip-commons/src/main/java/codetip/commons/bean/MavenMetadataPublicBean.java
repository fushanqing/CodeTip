package codetip.commons.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author followWindD
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JacksonXmlRootElement(localName = "metadata")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MavenMetadataPublicBean {
    /**
     * <?xml version="1.0" encoding="UTF-8"?>
     * <metadata xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     * xsi:noNamespaceSchemaLocation="sample-maven-metadata-public.xsd">
     * <groupId>HTTPClient</groupId>
     * <artifactId>HTTPClient</artifactId>
     * <version>0.3-3</version>
     * <versioning>
     * <release>0.3-3</release>
     * <versions>
     * <version>0.3-3</version>
     * </versions>
     * <lastUpdated>20160210141417</lastUpdated>
     * </versioning>
     * </metadata>
     */
    private String groupId;
    private String artifactId;
    private String version;
    private Versioning versioning;

    public String getRecommendVersion() {
        return Optional.ofNullable(version).orElse(versioning == null ? null : versioning.getVersion());
    }

    public boolean isUseless() {
        return groupId == null || artifactId == null || getRecommendVersion() == null;

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class Versioning {

        private String latest;
        private String release;
        private List<String> versions;
        private String lastUpdated;

        private String getVersion() {
            if (release != null) {
                return release;
            }
            if (latest != null) {
                return latest;
            }
            if (CollectionUtils.isEmpty(versions)) {
                return null;
            }
            return versions.get(versions.size() - 1);
        }
    }

}
