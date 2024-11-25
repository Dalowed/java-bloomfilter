# java-bloomfilter
基于 Java 实现了一个开箱即用的简易布隆过滤器
## 使用
1. 在 `pom.xml` 中添加依赖
```maven
<dependency>
    <groupId>io.github.dalowed</groupId>
    <artifactId>bloomfilter</artifactId>
    <version>0.0.1</version>
</dependency>
```
2.  在配置文件中编写配置文件, 以下是配置样例
```yml
bloom-filter:
  expected-insertions: 500000
  false-positive-probability: 0.0001
  enable-logging: true

# 恢复是根据生成的二进制文件进行恢复，如果选择恢复则以上不需要配置
#bloom-filter:
#  recovery: true
```

3. 在启动类上添加注解 `@EnableBloomFilter`, 即可开启

### todo
- [ ] 完善日志功能
- [ ] 默认配置不起作用
- [ ] ...
