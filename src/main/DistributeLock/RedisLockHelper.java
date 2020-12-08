package main.DistributeLock;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁的实现
 */
public class RedisLockHelper {
    private static Jedis jedis;

    static {
        jedis = new Jedis("127.0.0.1", 6379);
    }

    /**
     * 利用setnx做锁，expire保证超时机制
     * 这种方法实际上有一定的问题，因为其并没有保证操作的原子性，
     * 可能存在sernx成功但是expire之前就挂了
     *
     * @param key     redis的key
     * @param request 对应的value可以是类似sql的请求
     * @param timeout 超时时间
     * @return true表示设置成功
     */
    public static boolean tryLock(String key, String request, int timeout) {
        long result = jedis.setnx(key, request);
        if (result == 1) {
            return jedis.expire(key, timeout) == 1;
        } else {
            return false;
        }
    }

    /**
     * 使用lua的方式解决了原子性的问题，
     * redis保证了Lua脚本的原子性，在执行脚本的时候，不会执行其他脚本或者Redis的命令。
     * 从其他客户端来看，lua脚本要不就是仍然不可见，要不就是已经完成
     * 注意：
     * KEYS[i]表示keys集合的第i个数据
     */
    public static boolean tryLockWithLua(String key, String uniqueId, int second) {
        String lua_script = "if redis.call('setnx',KEYS[1],ARGV[1]) == 1 then" +
                "redis.call('expire',KEYS[1],ARGV[2]) == 1 return 1 else return 0 end";
        List<String> keys = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        keys.add(key);
        values.add(uniqueId);
        values.add(String.valueOf(second));
        Object eval = jedis.eval(lua_script, keys, values);
        return eval.equals(1);
    }

    /**
     * 利用set命令的方法
     *
     * @param key
     * @param uniqueId
     * @param second
     * @return
     */
    public static boolean tryLockWithSet(String key, String uniqueId, int second) {
        return "OK".equals(jedis.set(key, uniqueId, "NX", "EX", second));
    }

    private static int sleepTime = 100;

    public static boolean tryLockWithWaitTime(String key, String uniqueId, int timeout, long waitTime) throws InterruptedException{
        while (waitTime > 0) {
            if(tryLockWithSet(key,uniqueId,timeout)){
                return true;
            }
            waitTime -= sleepTime;
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        }
        return false;
    }

    /**
     * 同样的，利用Lua释放锁
     *
     * @param key
     * @param value 需要保证是唯一的
     * @return
     */
    public static boolean releaseLockWithLua(String key, String value) {
        String luaScript = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                "return redis.call('del',KEYS[1]) else return 0 end";
        return jedis.eval(luaScript, Collections.singletonList(key), Collections.singletonList(value)).equals(1);
    }
}
