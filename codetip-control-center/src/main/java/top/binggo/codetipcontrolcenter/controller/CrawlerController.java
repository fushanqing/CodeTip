package top.binggo.codetipcontrolcenter.controller;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.Page;
import codetip.commons.bean.Task;
import codetip.commons.bean.TaskType;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.binggo.codetipcontrolcenter.bean.TreeNode;
import top.binggo.codetipcontrolcenter.service.CrawlerTaskService;
import top.binggo.codetipcontrolcenter.service.FileTreeService;
import top.binggo.codetipcontrolcenter.service.NewTaskProcessors;

import java.util.List;
import java.util.Set;

/**
 * @author binggo
 */
@RestController
@RequestMapping("/crawler")
@Slf4j
public class CrawlerController {


    private final FileTreeService fileTreeService;
    private final CrawlerTaskService crawlerTaskService;
    private final NewTaskProcessors newTaskProcessors;

    @Autowired
    public CrawlerController(FileTreeService fileTreeService, CrawlerTaskService crawlerTaskService, NewTaskProcessors newTaskProcessors) {
        this.fileTreeService = fileTreeService;
        this.crawlerTaskService = crawlerTaskService;
        this.newTaskProcessors = newTaskProcessors;
    }


    @GetMapping("/tree/list")
    public ResponseEntity<List<TreeNode>> listTree(
            @RequestParam(value = "nodeName", defaultValue = "#root") String nodeName,
            @RequestParam(value = "withFolderStatus", defaultValue = "false", required = false) boolean withFolderStatus) {
        log.info("/crawler/tree/list?nodeName={}&withFolderStatus={}", nodeName, withFolderStatus);
        TreeNode treeNode = fileTreeService.listFolder(nodeName, withFolderStatus);
        if (treeNode == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(Lists.newArrayList(treeNode));
    }

    @GetMapping("/task/list")
    public ResponseEntity<Page.Respond<Task>> listTask(Page.Request page, @RequestParam(name = "status", required = false) String status) {
        log.info("/crawler/task/list?from={}&size={}&status={}", page.getFrom(), page.getSize(), status);
        CrawlerTaskStatus crawlerTaskStatus = null;
        if (status != null) {
            crawlerTaskStatus = CrawlerTaskStatus.valueOf(status);
        }
        List<Task> tasks = crawlerTaskService.listTask(crawlerTaskStatus);
        List<Task> subTask = tasks.subList(page.getFrom(), Math.min(page.getSize() + page.getFrom(), tasks.size()));
        for (Task task : subTask) {
            StringBuilder builder = new StringBuilder("targets:\n");
            String prefix = task.getTaskType() == TaskType.UPDATE_DIRECTORY ? "https://maven.aliyun.com/browse/tree?_input_charset=utf-8&repoId=central&path=" : "#root/";
            for (String s : task.getContent()) {
                builder.append(s.substring(prefix.length())).append('\n');
            }
            task.setResultMsg(builder.append("result:\n").append(task.getResultMsg()).toString());
        }
        Page.Respond<Task> build = Page.Respond.<Task>builder().data(subTask).from(page.getFrom()).size(subTask.size()).totalSize(tasks.size()).build();
        return ResponseEntity.ok(build);
    }

    @PostMapping("/task/new")
    public ResponseEntity<Task> newTask(@RequestBody Set<String> targetFolder, @RequestParam String taskType) {
        log.info("/crawler/task/new?taskType={},body:{}", taskType, targetFolder);
        return ResponseEntity.ok(newTaskProcessors.apply(taskType, targetFolder));
    }

    @PostMapping("/task/start")
    public ResponseEntity<String> startTask(@RequestParam String taskId) {
        log.info("/crawler/task/start?taskId={}", taskId);
        String s = crawlerTaskService.startATask(taskId);
        return ResponseEntity.ok(s);
    }


    @GetMapping("/task/types")
    public ResponseEntity<TaskType[]> taskType() {
        log.info("/crawler/task/types");
        return ResponseEntity.ok(TaskType.values());
    }

    @PutMapping("/task/stop")
    public ResponseEntity<String> stopTask(@RequestParam String taskId) {
        log.info("/crawler/task/stop?taskId={}", taskId);
        crawlerTaskService.stopTask(taskId);
        return ResponseEntity.ok("success");
    }


}
