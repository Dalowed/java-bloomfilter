package io.github.dalowed.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * enable bloomfilter annotation
 * @author dalowed
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(BloomImportSelector.class)
public @interface EnableBloomFilter {
    /**
     * whether to enable Bloom filter
     * @return {@link Boolean}
     */
    boolean enabled() default true;
}
