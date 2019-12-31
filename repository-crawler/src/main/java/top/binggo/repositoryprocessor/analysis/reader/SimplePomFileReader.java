package top.binggo.repositoryprocessor.analysis.reader;

import codetip.commons.bean.MavenCoordinate;
import javafx.util.Pair;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.Map;

/**
 * @author binggo
 */
public class SimplePomFileReader {
    public Pair<Map<MavenCoordinate.Location, String>, Map<String, String>> read(String groupId, String artifactId, String version) throws DocumentException, IOException {
//        InputStream download = downloader.download(groupId, artifactId, version, ".pom");
//        String input = IOUtils.toString(download);
//        Document document = DocumentHelper.parseText(input);
//        Element rootElement = document.getRootElement();
//        Node dependenciesNode = rootElement.selectSingleNode("/pom:project/pom:dependencies");
//        List<MavenCoordinate> parseDependencies = parseDependencies(dependenciesNode);
        return null;
//
    }
}
