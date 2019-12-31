package top.binggo.codetip.filesystem.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

@Slf4j
public class MavenRedisFileSystemNodeMetadataTest {
    @Parameterized.Parameters
    public static MavenRedisFileSystemNodeMetadata data() {
        MavenRedisFileSystemNodeMetadata xml = MavenRedisFileSystemNodeMetadata.xml("some.xml");
        xml.setNodeType(MavenRedisFileSystemNodeMetadata.NodeType.FOLDER);
        return xml;
    }

    @Test
    public void fromObjectToJson() throws JsonProcessingException {
        MavenRedisFileSystemNodeMetadata xml = data();
        log.info("{}", xml);
        ObjectMapper objectMapper = new ObjectMapper();
        String s = objectMapper.writeValueAsString(xml);
        assertNotNull(s);
        log.info("{}", s);
    }

    @Test
    public void jsonToObject() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        MavenRedisFileSystemNodeMetadata xml = data();
        String xmlString = objectMapper.writeValueAsString(xml);
        MavenRedisFileSystemNodeMetadata mavenRedisFileSystemNodeMetadata = objectMapper.readValue(xmlString, MavenRedisFileSystemNodeMetadata.class);
        log.info("{}", objectMapper.writeValueAsString(mavenRedisFileSystemNodeMetadata));
    }
}