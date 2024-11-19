package dalowed.bloom.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description // 注解启动
 * @Author dalowed
 * @Date 2024-11-18 16:31
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(BloomImportSelector.class)
public @interface EnableBloomFilter {
    boolean enabled() default true;
}
