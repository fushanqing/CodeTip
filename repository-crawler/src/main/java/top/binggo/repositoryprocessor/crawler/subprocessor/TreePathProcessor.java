package top.binggo.repositoryprocessor.crawler.subprocessor;


import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.crawl.RedisTaskAccessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Sets;
import top.binggo.repositoryprocessor.crawler.CrawlerApplication;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 1.2.3-RC
 * 用来处理类似:
 * https://maven.aliyun.com/browse/tree?_input_charset=utf
 * -8&repoId=central&path=
 * 的请求
 * @author binggo
 */
@Slf4j
public class TreePathProcessor extends AbstractSubProcessor {
    public static final String URL_PREFIX = "https://maven.aliyun.com/browse/tree?_input_charset=utf-8&repoId=central&path=";
    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+).*");

    private RedisTaskAccessor redisTaskAccessor;
    private CrawlerApplication crawlerApplication;
    private String taskId;

    public TreePathProcessor(RedisTaskAccessor redisTaskAccessor,
                             CrawlerApplication crawlerApplication, String taskId) {
        super(URL_PREFIX, "(.*)");
        this.redisTaskAccessor = redisTaskAccessor;
        this.crawlerApplication = crawlerApplication;
        this.taskId = taskId;
    }

    private static boolean isNodePathToSkip(String nodePath) {
        int indexOf = nodePath.lastIndexOf("/", nodePath.length() - 2);
        if (indexOf == -1) {
            return false;
        }
        String lastItem = nodePath.substring(indexOf, nodePath.length() - 1);
		return lastItem.contains(".");
	}

	@Override
	public void process(Page page) {

		//page对象是否符合预期
		if (page.getStatusCode() == HttpConstant.StatusCode.CODE_200
				&& page.getUrl().regex(this.urlRegex).match()) {
            Spider spider = this.crawlerApplication.getRunningSpider().get(taskId);
            if (spider != null) {
                redisTaskAccessor.setTaskResultMsgInStatus(CrawlerTaskStatus.PROCESSING, taskId, "processed " + spider.getPageCount() + " pages");
            }
			ResponseEntity responseEntity = page.getJson().toObject(ResponseEntity.class);
			if (responseEntity.successful) {
                log.info("process url:{}", page.getUrl());
				boolean haveTargetFile = false;
				Set<String> treePathTargetRequests = new HashSet<>();
                boolean hasOneVersionFile = false;
				//处理结果的主要信息实体
                for (int i = responseEntity.getObject().size() - 1; i >= 0; i--) {
                    ResponseEntity.RepoPathNode repoPathNode = responseEntity.getObject().get(i);
                    String nodePath = repoPathNode.getNodePath();
					switch (repoPathNode.getNodeType()) {
						//如果是目录,接着请求目录中的内容
						case ResponseEntity.RepoPathNode.NODE_TYPE_FOLDER:
                            boolean canBeSkip = isNodePathToSkip(nodePath);
                            if (canBeSkip && !hasOneVersionFile) {
                                hasOneVersionFile = true;
                            }
							if (!canBeSkip) {

								treePathTargetRequests.add(URL_PREFIX
                                        + nodePath);
							}
							break;
						//如果是文件
						case ResponseEntity.RepoPathNode.NODE_TYPE_FILE:
							//如果是要处理的文件
							if (ResponseEntity.RepoPathNode.TARGET_FILE_NAMES.contains(
									repoPathNode.getNodeName())) {

                                page.addTargetRequest(FileInfoProcessor.URL_PREFIX + nodePath);
							}
							break;
						default:
							break;
					}

				}
				//如果没有找到目标文件,继续顺着TreePathTargetRequest寻找
				if (!hasOneVersionFile) {
                    log.info("don't get any metadata.xml file,but get {} target url", treePathTargetRequests.size());
					page.addTargetRequests(new ArrayList<>(treePathTargetRequests));
				}
			}
		}else {
			page.setSkip(true);
		}
	}

	/**
	 * 请求返回结果的 bean 封装
	 */
	@Data
	public static class ResponseEntity {
		/**
         * object : [{"nodeKey":"abbot/","name":"abbot/","nodePath":"abbot/abbot/","nodeType":"FOLDER","repoId":"central"},{"nodeKey":"costello/","name":"costello/","nodePath":"abbot/costello/","nodeType":"FOLDER","repoId":"central"}]
		 * successful : true
		 */

		private boolean successful;
		private List<RepoPathNode> object;

		@Data
		public static class RepoPathNode {

			public static final String NODE_TYPE_FOLDER = "FOLDER";
			public static final String NODE_TYPE_FILE = "FILE";
			public static final Set<String> TARGET_FILE_NAMES =
                    Sets.newLinkedHashSet("maven-metadata-public.xml", "maven-metadata.xml");

			/**
			 * nodeKey : abbot/
             * name : abbot/
			 * nodePath : abbot/abbot/
			 * nodeType : FOLDER
			 * repoId : central
			 */

			private String nodeKey;
			private String nodeName;
			private String nodePath;
			private String nodeType;
			private String repoId;

		}
	}
}
