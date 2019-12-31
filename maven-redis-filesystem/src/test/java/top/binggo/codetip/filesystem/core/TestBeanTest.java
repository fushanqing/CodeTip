package top.binggo.codetip.filesystem.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * @author binggo
 */
public class TestBeanTest {
    @Test
    public void name() throws JsonProcessingException {
        TestBean testBean = new TestBean();
        testBean.setS("123");
        testBean.setTestEnum(TestEnum.A);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(testBean));
    }
}
