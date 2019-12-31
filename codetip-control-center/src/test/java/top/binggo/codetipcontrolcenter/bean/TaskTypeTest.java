package top.binggo.codetipcontrolcenter.bean;

import codetip.commons.bean.TaskType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

public class TaskTypeTest {


    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Test
    public void toStringTest() {
        System.out.println(TaskType.UPDATE_DIRECTORY.toString());
    }

    @Test
    public void setJson() throws JsonProcessingException {
        ImmutableSet<String> of = ImmutableSet.of("123", "234");
        ObjectMapper objectMapper = new ObjectMapper();
        String s = objectMapper.writeValueAsString(of);
        System.out.println(s);
    }
}