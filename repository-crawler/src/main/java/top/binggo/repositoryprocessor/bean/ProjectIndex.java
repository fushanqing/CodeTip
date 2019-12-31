package top.binggo.repositoryprocessor.bean;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * @author binggo
 */
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "project-index", type = "project-index", refreshInterval = "-1")
public class ProjectIndex {
    @Id
    private String id;
    @Field
    private String groupId;
    @Field
    private String artifactId;
    @Field
    private String version;
    @Field
    private int usingCount;
    @Field
    private String description;
    @Field
    private String indexField;

    public ProjectIndex mavenLocation(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.id = groupId + " " + artifactId + " " + version;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public int getUsingCount() {
        return usingCount;
    }

    public void setUsingCount(int usingCount) {
        this.usingCount = usingCount;
    }

    public String getIndexField() {
        return indexField;
    }

    public void setIndexField(String indexField) {
        this.indexField = indexField;
    }

    public void addUsingCount(int usingCount) {
        this.usingCount += usingCount;
    }

    public void addIndexField(String indexField) {
        if (this.indexField == null) {
            this.indexField = indexField;
        } else {
            this.indexField += indexField;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        addIndexField(description);
    }
}
