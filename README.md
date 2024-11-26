# java-bloomfilter
基于 Java 实现了一个开箱即用的简易布隆过滤器
## 使用
1. add dependency to `pom.xml`
```maven
<dependency>
    <groupId>io.github.dalowed</groupId>
    <artifactId>bloomfilter</artifactId>
    <version>0.0.2</version>
</dependency>
```
2.  write a configuration file
```yml
# sample
bloom-filter:
  expected-insertions: 500000
  false-positive-probability: 0.0001
  enable-logging: true

# 恢复是根据生成的二进制文件进行恢复，如果选择恢复则以上不需要配置
#bloom-filter:
#  recovery: true
```

3. add `@EnableBloomFilter` with `@SpringBootApplication`, then
you have `BloomFilter` anywhere!
### todo
- [ ] 完善日志功能
- [ ] 默认配置不起作用
- [ ] ...
