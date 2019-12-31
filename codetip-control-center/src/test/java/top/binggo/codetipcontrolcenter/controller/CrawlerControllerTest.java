package top.binggo.codetipcontrolcenter.controller;

import codetip.commons.bean.Page;
import codetip.commons.bean.Task;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class CrawlerControllerTest {


    @Autowired
    private CrawlerController crawlerController;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void listTree() {
    }

    @Test
    public void listTask() {
        ResponseEntity<Page.Respond<Task>> respondResponseEntity = crawlerController.listTask(new Page.Request(0, 5), null);
        log.info("{}", respondResponseEntity);
    }

    @Test
    public void newTask() {
    }

    @Test
    public void taskType() {
    }

    @Test
    public void stopTask() {
    }
}