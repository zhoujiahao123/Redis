package main.DistributeLock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
public class LockMethodAspect {
    @Around("main.DistributeLock.RedisLock")
    public Object around(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisLock redisLock = method.getAnnotation(RedisLock.class);
        String key = redisLock.key();
        int expire = redisLock.expire();
        long waitTime = redisLock.waitTime();
        String value = UUID.randomUUID().toString();
        try {
            boolean successLock = RedisLockHelper.tryLockWithWaitTime(key, value, expire, waitTime);
            if (!successLock) {
                System.out.println("获取锁失败");
            }
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            RedisLockHelper.releaseLockWithLua(key, value);
        }
        return null;
    }
}