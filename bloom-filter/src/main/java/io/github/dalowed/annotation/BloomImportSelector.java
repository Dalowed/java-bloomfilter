package io.github.dalowed.annotation;

import io.github.dalowed.configuration.BloomConfiguration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;


/**
 * Annotation effective class
 * @author dalowed
 * @since 0.0.1
 */
public class BloomImportSelector implements ImportSelector {

    /**
     * introduce BloomConfiguration
     * @param metadata meta annotation
     * @return {@link String[] }
     */
    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableBloomFilter.class.getName());
        boolean enabled = (boolean) attributes.get("enabled");
        if (enabled) {
            return new String[]{BloomConfiguration.class.getName()};
        } else {
            return new String[]{};
        }
    }
}
