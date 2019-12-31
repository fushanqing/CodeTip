package top.binggo.codetip.filesystem.core;

import codetip.commons.bean.MavenCoordinate;
import codetip.commons.bean.MavenMetadataPublicBean;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用来解析下载完成的MavenMetadata-public.xml文件
 *
 * @author followWindD
 */
public class MavenMetadataReader {
    private MavenMetadataReader() {

    }

    /**
     * 这是要解析的xml文件的例子
     * <pre>
     * <?xml version="1.0" encoding="UTF-8"?>
     * <metadata xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *           xsi:noNamespaceSchemaLocation="sample-maven-metadata-public.xsd">
     *     <groupId>HTTPClient</groupId>
     *     <artifactId>HTTPClient</artifactId>
     *     <version>0.3-3</version>
     *     <versioning>
     *         <release>0.3-3</release>
     *         <versions>
     *             <version>0.3-3</version>
     *         </versions>
     *         <lastUpdated>20160210141417</lastUpdated>
     *     </versioning>
     * </metadata>
     * </pre>
     *
     * @param input 输入的xml字符串
     * @return 通过xml解析获得的推荐maven坐标
     */
    public static MavenCoordinate getMavenCoordinate(String input) throws DocumentException {
        Document document = DocumentHelper.parseText(input);
        Element rootElement = document.getRootElement();
        String groupId = rootElement.selectSingleNode("/metadata/groupId").getText();
        String artifactId = rootElement.selectSingleNode("/metadata/artifactId").getText();
        String version = rootElement.selectSingleNode("/metadata/version").getText();

        String lastUpdated = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return MavenCoordinate.builder()
                .location(MavenCoordinate.Location.builder().groupId(groupId).artifactId(artifactId).build())
                .stamp(MavenCoordinate.Stamp.builder().version(version).lastUpdated(lastUpdated).build()).build();
    }


    public static MavenMetadataPublicBean getMavenMetadataPublicBean(String input) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        return xmlMapper.readValue(input, MavenMetadataPublicBean.class);
    }
}
