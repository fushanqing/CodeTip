package top.binggo.codetipcontrolcenter.bean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import top.binggo.codetipcontrolcenter.config.AppConfig;

import java.util.List;

/**
 * @author binggo
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class RedisMultiTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void multiTest() {
        Object execute = stringRedisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().set("test", "test");
                List exec = operations.exec();
                System.out.println(exec);
                return null;
            }
        });
        System.out.println(execute);
    }
}
