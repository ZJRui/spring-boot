/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Indexed;

/**
 * Annotation for externalized configuration. Add this to a class definition or a
 * {@code @Bean} method in a {@code @Configuration} class if you want to bind and validate
 * some external Properties (e.g. from a .properties file).
 * <p>
 * Binding is either performed by calling setters on the annotated class or, if
 * {@link ConstructorBinding @ConstructorBinding} is in use, by binding to the constructor
 * parameters.
 * <p>
 * Note that contrary to {@code @Value}, SpEL expressions are not evaluated since property
 * values are externalized.
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see ConfigurationPropertiesScan
 * @see ConstructorBinding
 * @see ConfigurationPropertiesBindingPostProcessor
 * @see EnableConfigurationProperties
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface ConfigurationProperties {
	/**
	 *
	 * 问题1： 如果我使用@ConfigurationProperties属性标记了一个类，并配置prefix 为a.b 但是整个系统并不存在a.b的属性，那么 spring容器中
	 * 会创建该注解标记的类的一个Bean对象吗？
	 *
	 * 注意2： @ConfigurationProperties注解的使用 依赖语 注解处理器，这个注解处理器是在
	 * <artifactId>spring-boot-configuration-processor</artifactId> 中提供的，因此项目中需要引入这个jar
	 *
	 * You can easily generate your own configuration metadata
	 * file from items annotated with @ConfigurationProperties by using
	 * the spring-boot-configuration-processor jar. The jar includes a
	 *Java annotation processor which is invoked as your project is compiled.
	 *
	 *
	 * 您可以使用 spring-boot-configuration-processor jar 从带有 @ConfigurationProperties 注释的项目轻松生成自己的配置元数据文件。
	 * 该 jar 包含一个 Java 注释处理器，它在您的项目编译时被调用。
	 *
	 * 问题3 ，既然上面提到了 @ConfigurationProperties的实现 是通过 注解处理器在编译期间 处理的，那么编译之后的类是什么样子的？
	 *
	 *
	 * 问题4  @EnableConfigurationProperties 和 @ConfigurationProperties的关系
	 * @EnableConfigurationProperties注解的作用是：使使用 @ConfigurationProperties 注解的类生效。
	 * 说明：
	 * 如果一个配置类只配置@ConfigurationProperties注解，而没有使用@Component，那么在IOC容器中是获取不到properties 配置文件转化的bean。说白了 @EnableConfigurationProperties 相当于把使用 @ConfigurationProperties 的类进行了一次注入。
	 * 测试发现 @ConfigurationProperties 与 @EnableConfigurationProperties 关系特别大。
	 *
	 * 测试证明：
	 * @ConfigurationProperties 与 @EnableConfigurationProperties 的关系。
	 *
	 * @EnableConfigurationProperties 文档中解释：
	 * 当@EnableConfigurationProperties注解应用到你的@Configuration时， 任何被@ConfigurationProperties注解的beans将自动被Environment属性配置。 这种风格的配置特别适合与SpringApplication的外部YAML配置进行配合使用。
	 *
	 *
	 * -----------------------------------------
	 * 问题@EnableConfigurationProperties 和 @ConfigurationProperties 的区别
	 *
	 * Enable support for @ConfigurationProperties annotated beans.
	 * @ConfigurationProperties beans can be registered in the standard way
	 * (for example using @Bean methods) or, for convenience, can be specified directly on this annotation.
	 *
	 * @EnableCopnfigurationProperties 用来支持 被@ConfigurationProperties  注解的bean。
	 *
	 * 被@ConfigurationProperties  注解的bean 可以通过 @Bean的方式注入到容器中，这也就是说 如果你不使用@Bean标记，那么默认
	 * 情况下@ConfigurationProperties  不会将这个bean注入到容器中。
	 *
	 * 为了 解决 将Bean注入到容器中 可以使用@EnableCopnfigurationProperties.
	 * 且 EnableConfiguratonProperties 注解中需要指定 配置类 @EnableConfigurationProperties(value = {PropertiesTest.class})
	 *PropertiesTest 使用了@COnfigurationProperties标记
	 *
	 * -----------------
	 *
	 *
	 * 问题： @ConfigurationProperties标记的类  注解处理器会生成一个spring-configuration-metadata.json 文件，文件中有记录你
	 * 使用@ConfigurationProperties注解标记的类。
	 * {
	 *   "groups": [
	 *     {
	 *       "name": "a.b",
	 *       "type": "com.example.healthb.PropertiesTest",
	 *       "sourceType": "com.example.healthb.PropertiesTest"
	 *     }
	 *   ],
	 *   "properties": [],
	 *   "hints": []
	 * }
	 *-------------------
	 * 问题： 关于驼峰属性
	 * @ConfigurationProperties 的 POJO类的命名比较严格,因为它必须和prefix的后缀名要一致, 不然值会绑定不上,
	 * 特殊的后缀名是“driver-class-name”这种带横杠的情况,在POJO里面的命名规则是 下划线转驼峰 就可以绑定成功，所以就是 “driverClassName”
	 *
	 *
	 */


	/**
	 * The prefix of the properties that are valid to bind to this object. Synonym for
	 * {@link #prefix()}. A valid prefix is defined by one or more words separated with
	 * dots (e.g. {@code "acme.system.feature"}).
	 * @return the prefix of the properties to bind
	 */
	@AliasFor("prefix")
	String value() default "";

	/**
	 * The prefix of the properties that are valid to bind to this object. Synonym for
	 * {@link #value()}. A valid prefix is defined by one or more words separated with
	 * dots (e.g. {@code "acme.system.feature"}).
	 * @return the prefix of the properties to bind
	 */
	@AliasFor("value")
	String prefix() default "";

	/**
	 * Flag to indicate that when binding to this object invalid fields should be ignored.
	 * Invalid means invalid according to the binder that is used, and usually this means
	 * fields of the wrong type (or that cannot be coerced into the correct type).
	 * @return the flag value (default false)
	 */
	boolean ignoreInvalidFields() default false;

	/**
	 * Flag to indicate that when binding to this object unknown fields should be ignored.
	 * An unknown field could be a sign of a mistake in the Properties.
	 * @return the flag value (default true)
	 */
	boolean ignoreUnknownFields() default true;

}
