package main.DistributeLock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RedisLock {
    String key();
    int expire() default 5;
    long waitTime() default Integer.MIN_VALUE;
}
