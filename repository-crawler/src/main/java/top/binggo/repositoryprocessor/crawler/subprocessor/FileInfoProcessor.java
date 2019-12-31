package top.binggo.repositoryprocessor.crawler.subprocessor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.utils.HttpConstant;

/**
 * 用来处理类似:
 * https://maven.aliyun.com/browse/fileInfo?_input_charset
 * =utf-8&repoId=central&path=
 * 的请求
 * @author binggo
 */
@Slf4j
public class FileInfoProcessor extends AbstractSubProcessor {
    public static final String URL_PREFIX = "https://maven.aliyun.com/" +
            "browse/fileInfo?_input_charset=utf-8&repoId=central&path=";

	public FileInfoProcessor() {
        super(URL_PREFIX, "(.*)");
	}

	@Override
	public void process(Page page) {
		if (page.getStatusCode() == HttpConstant.StatusCode.CODE_200
				&& page.getUrl().regex(getUrlRegex()).match()) {
            log.info("get fileInfo by url:{}", page.getUrl());
			ResponseEntity responseEntity = page.getJson().toObject(ResponseEntity.class);
			if (responseEntity.getObject().exist) {
				page.addTargetRequest(responseEntity.getObject().getDownloadUrl());
			}
		}else {
			page.setSkip(true);
		}
	}

	@Data
	public static class ResponseEntity {

		/**
		 * object : {"contentLength":"0.37KB","downloadUrl":"http://archiva-maven-storage-prod.oss-cn-beijing.aliyuncs.com/repository/central/abbot/abbot/maven-metadata-public.xml?Expires=1551421456&OSSAccessKeyId=LTAIfU51SusnnfCC&Signature=y%2Bwlk8%2Bc0763IOFSdjqECxLy71s%3D","exist":true,"lastModified":"2018-08-24 22:04:50","path":"abbot/abbot/maven-metadata-public.xml"}
		 * successful : true
		 */

		private FileInfoBean object;
		private boolean successful;

		@Data
		public static class FileInfoBean {
			/**
			 * contentLength : 0.37KB
			 * downloadUrl : http://archiva-maven-storage-prod.oss-cn-beijing.aliyuncs.com/repository/central/abbot/abbot/maven-metadata-public.xml?Expires=1551421456&OSSAccessKeyId=LTAIfU51SusnnfCC&Signature=y%2Bwlk8%2Bc0763IOFSdjqECxLy71s%3D
			 * exist : true
			 * lastModified : 2018-08-24 22:04:50
			 * path : abbot/abbot/maven-metadata-public.xml
			 */

			private String contentLength;
			private String downloadUrl;
			private boolean exist;
			private String lastModified;
			private String path;

		}
	}

}
