package top.binggo.repositoryprocessor.crawler.pipeline;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.Map;

/**
 * 用来处理*.xml.md5文件
 *
 * @author <a href=mailto:libing22@meituan.com>binggo</a>
 * @since 2019/3/1
 **/
public class Md5ResultItemPipeline implements Pipeline {

	@Override
	public void process(ResultItems resultItems, Task task) {
		Map<String, Object> all = resultItems.getAll();
//		all.entrySet().stream().
	}
}
