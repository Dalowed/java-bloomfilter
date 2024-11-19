package dalowed.bloom.annotation;

import dalowed.bloom.configuration.BloomConfiguration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;


public class BloomImportSelector implements ImportSelector {

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
