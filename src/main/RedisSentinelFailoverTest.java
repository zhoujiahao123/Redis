package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisSentinelFailoverTest {
    private static Logger logger = LoggerFactory.getLogger(RedisSentinelFailoverTest.class);

    public static void main(String[] args) {
        String masterName = "mymaster";
        Set<String> sentinels = new HashSet<String>();
        sentinels.add("127.0.0.1:26379");
        sentinels.add("127.0.0.1:26479");
        sentinels.add("127.0.0.1:26579");
        JedisSentinelPool jedisSentinelPool = new JedisSentinelPool(masterName, sentinels);
        int counter = 0;
        while (true) {
            Jedis jedis = null;
            counter++;
            try {
                jedis = jedisSentinelPool.getResource();
                int index = new Random(47).nextInt(100000);
                String key = "key-" + index;
                String value = "value-" + index;
                jedis.set(key, value);
                if (counter % 100 == 0) {
                    logger.info("{} value is {}", key, jedis.get(key));
                }
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
    }
}
