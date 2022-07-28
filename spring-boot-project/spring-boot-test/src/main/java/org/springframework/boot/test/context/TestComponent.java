/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * {@link Component @Component} that can be used when a bean is intended only for tests,
 * and should be excluded from Spring Boot's component scanning.
 * <p>
 * Note that if you directly use {@link ComponentScan @ComponentScan} rather than relying
 * on {@code @SpringBootApplication} you should ensure that a {@link TypeExcludeFilter} is
 * declared as an {@link ComponentScan#excludeFilters() excludeFilter}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see TypeExcludeFilter
 * @see TestConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TestComponent {
	/**
	 * 1. @Component，当bean只用于测试时可以使用，并且应该从Spring Boot的组件扫描中排除。
	 * 注意，如果你直接使用@ComponentScan而不是依赖于@SpringBootApplication，你应该确保TypeExcludeFilter被声明为一个excludeFilter。
	 *
	 *
	 * 2.
	 * https://docs.spring.io/spring-boot/docs/1.5.1.RELEASE/reference/htmlsingle/#boot-features-testing-spring-boot-applications-excluding-config
	 *
	 * 41.3.2 Excluding test configuration
	 * If your application uses component scanning, for example if you use @SpringBootApplication or @ComponentScan,
	 * you may find components or configurations created only for specific tests accidentally get picked up everywhere.
	 * 如果你的应用程序使用了组件扫描，比如 SpringBoootApplication 或者componentscan，那么你可能 会发现 哪些被创建的对某些特殊的测试才匹配的
	 * 组件或者配置类 会 导出被 picked up。 也就是说有些组件是针对某些testcase 才有效的，但是因为你的组件扫描导致被扫描到 从而被 创建。
	 *
	 *
	 * To help prevent this, Spring Boot provides @TestComponent and @TestConfiguration annotations that can be used
	 * on classes in src/test/java to indicate that they should not be picked up by scanning.
	 *
	 * @TestComponent和@TestConfiguration只在顶级类中需要。如果您将@Configuration或@Component定义为测试中的内部类(任何具有@Test方法或@RunWith的类)，它们将被自动过滤。
	 * 也就是说为了阻止这种情况， spring boot 提供了@TestComponent and @TestConfiguration  ，那么也就是说 如果你的test路径下的类使用了
	 * @TestComponent and @TestConfiguration 注解标注，那么他@ComponentScan 组件扫描功能不会创建这些组件对象。
	 * 而且下文提到 这两个Test注解，只在顶层class中 使用。
	 *
	 *
	 * [Note]
	 * @TestComponent and @TestConfiguration are only needed on top level classes. If you define @Configuration or @Component as inner-classes within a test (any class that has @Test methods or @RunWith), they will be automatically filtered.
	 *
	 * @TestComponent和@TestConfiguration只在顶级类中需要。如果您将@Configuration或@Component定义为测试中的内部类(任何具有@Test方法或@RunWith的类)，它们将被自动过滤。
	 *
	 * [Note]
	 * If you directly use @ComponentScan (i.e. not via @SpringBootApplication) you will need to register the TypeExcludeFilter with it. See the Javadoc for details.
	 *
	 * [注]
	 * 如果你直接使用@ComponentScan(即不是通过@SpringBootApplication)，你将需要向它注册TypeExcludeFilter。详细信息请参见Javadoc。
	 *
	 *
	 *
	 * 3.springboot框架在单元测试时可能需要忽略某些带有@component的实例
	 *
	 * 例如以下代码：
	 *
	 * @Component
	 * public class MyCommandLineRunner implements CommandLineRunner {
	 *        @Override
	 *    public void run(String... var1) throws Exception {
	 *    }
	 * }
	 * 服务启动会执行commandLineRanner实例。那如何忽略commandLineRanner实例这个@component呢？
	 * 其实很简单，在commandLineRanner实例上加注解@TestConfiguration或者@TestComponent就可以忽略。
	 * ————————————————
	 *
	 *
	 * 4.注解	作用	实践中的使用
	 * @TestComponent 该注解另一种@Component，在语义上用来指定某个Bean是专门用于测试的。	该注解适用于测试代码和正式混合在一起时，不加载被该注解描述的Bean，使用不多。
	 * @TestConfiguration 该注解是另一种@TestComponent，它用于补充额外的Bean或覆盖已存在的Bean	在不修改正式代码的前提下，使配置更加灵活
	 *
	 *使用@SpringBootApplication启动测试或者生产代码，被@TestComponent描述的Bean会自动被排除掉。如果不是则需要
	 * 向@SpringBootApplication添加TypeExcludeFilter。
	 *
	 *
	 * 3.1 @TestComment vs @Comment
	 * @TestComponent是另一种@Component，在语义上用来指定某个Bean是专门用于测试的
	 * 使用@SpringBootApplication服务时，@TestComponent会被自动排除
	 * 3.2 @TestConfiguration vs @Configuration
	 * @TestConfiguration是Spring Boot Boot Test提供的，@Configuration是Spring Framework提供的。
	 * @TestConfiguration实际上是也是一种@TestComponent，只是这个@TestComponent专门用来做配置用。
	 * @TestConfiguration和@Configuration不同，它不会阻止@SpringBootTest的查找机制，相当于是对既有配置的补充或覆盖。
	 *
	 * 5.实现机制
	 * 在 TestTypeExcludeFilter#isTestConfiguration(org.springframework.core.type.classreading.MetadataReader)
	 *  中会判断 这个class上是否存在 @TestComponent 注解，如果存在则排除
	 *
	 */

	/**
	 * The value may indicate a suggestion for a logical component name, to be turned into
	 * a Spring bean in case of an auto-detected component.
	 * @return the specified bean name, if any
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
