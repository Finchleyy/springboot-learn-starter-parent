# springboot-learn-starter-parent
starter集成入应用有两种方式。我们从应用视角来看有两种：

## 主动生效，在starter组件集成入Spring Boot应用时需要你主动声明启用该starter才生效，即使你配置完全。这里会用到@Import注解，将该注解标记到你自定义的@Enable注解上：


## 被动生效，在starter组件集成入Spring Boot应用时就已经被应用捕捉到。这里会用到类似java的SPI机制。在autoconfigure资源包下新建META-INF/spring.factories写入SmsAutoConfiguration全限定名。
