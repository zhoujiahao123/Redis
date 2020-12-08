package main;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Pipeline;

import java.nio.channels.Pipe;

public class Main {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("127.0.0.1", 6379);
        //正常使用
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            jedis.hset("hashkey:" + i, "field" + i, "value" + i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        for (int i = 0; i < 10000; i++) {
            jedis.del("hashkey:" + i);
        }
        //pipeLine方式
        long startTime1 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Pipeline pipeline = jedis.pipelined();
            for (int j = i * 100; j < (i + 1) * 100; j++) {
                pipeline.hset("hashkey:"+j,"field"+j,"value"+j);
            }
            pipeline.syncAndReturnAll();
        }
        long endTime1 = System.currentTimeMillis();
        System.out.println(endTime1 - startTime1);
        //发布订阅,发布一个消息后所有订阅者都可以收到
        //消息队列，发布一个消息后只有一个订阅者可以收到，例如抢红包
        System.out.println(jedis.publish("channel","hi"));
        Jedis jedis1  = new Jedis("127.0.0.1",6379);

    }
}
