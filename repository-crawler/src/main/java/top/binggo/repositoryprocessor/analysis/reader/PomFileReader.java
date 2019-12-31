package top.binggo.repositoryprocessor.analysis.reader;

import codetip.commons.bean.MavenCoordinate;
import javafx.util.Pair;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.dom4j.*;
import top.binggo.repositoryprocessor.analysis.AliMavenRepoCentralJavaSourceDownloader;
import top.binggo.repositoryprocessor.analysis.JavaSourceDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author binggo
 */
@UtilityClass
public class PomFileReader {

    private static final JavaSourceDownloader downloader = new AliMavenRepoCentralJavaSourceDownloader();

    public Pair<Map<MavenCoordinate.Location, String>, Map<String, String>> read(String groupId, String artifactId, String version) throws DocumentException, IOException {
        InputStream download = downloader.download(groupId, artifactId, version, ".pom");
        String input = IOUtils.toString(download);
        Document document = DocumentHelper.parseText(input);
        Element rootElement = document.getRootElement();
        //解析dependencies
        Node dependenciesNode = rootElement.selectSingleNode("/pom:project/pom:dependencies");
        Pair<HashMap<MavenCoordinate.Location, String>, HashMap<String, String>> ret = new Pair<>(new HashMap<>(), new HashMap<>());
        List<MavenCoordinate> parseDependencies = parseDependencies(dependenciesNode);
        //解析dependencyManagement
        Node dependencyManagement = rootElement.selectSingleNode("/pom:project/pom:dependencyManagement/pom:dependencies");
        List<MavenCoordinate> manaagement = parseDependencies(dependencyManagement);

        Node parent = rootElement.selectSingleNode("/pom:project/pom:parent");
        if (parent != null) {
            MavenCoordinate mavenCoordinate = parseDependency(parent);
            MavenCoordinate.Location location = mavenCoordinate.getLocation();
            String groupId1 = location.getGroupId();
            String artifactId1 = location.getArtifactId();
            String version1 = mavenCoordinate.getStamp().getVersion();
            Pair<Map<MavenCoordinate.Location, String>, Map<String, String>> read = read(groupId1, artifactId1, version1);

        }

        Node properties = rootElement.selectSingleNode("/pom:project/pom:properties");
        List list = properties.selectNodes("/");
        if (list != null) {
            for (Object o : list) {
                Node tNode = (Node) o;
                String name = tNode.getName();
                String text = tNode.getText();
                ret.getValue().put(name, text);
            }
        }
        return null;
    }


    public static MavenCoordinate parseDependency(Node node) {
        String groupId = contentInNode(node, "groupId");
        String artifactId = contentInNode(node, "artifactId");
        String version = contentInNode(node, "version");
        return MavenCoordinate.builder().location(MavenCoordinate.Location.builder().groupId(groupId).artifactId(artifactId).build()).stamp(MavenCoordinate.Stamp.builder().version(version).build()).build();
    }


    public static List<MavenCoordinate> parseDependencies(Node node) {
        List dependency = node.selectNodes("dependency");
        List<MavenCoordinate> ret = new ArrayList<>();
        if (dependency != null) {
            for (Object o : dependency) {
                MavenCoordinate mavenCoordinate = parseDependency(((Node) o));
                ret.add(mavenCoordinate);
            }
        }
        return ret;
    }

    private String contentInNode(Node node, String loc) {
        Node selectSingleNode = node.selectSingleNode(loc);
        if (selectSingleNode != null && selectSingleNode.hasContent()) {
            return selectSingleNode.getText();
        }
        return null;
    }
}
