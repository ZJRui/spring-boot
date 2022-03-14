/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.repository.Repository;

/**
 * Indicates a {@link Configuration configuration} class that declares one or more
 * {@link Bean @Bean} methods and also triggers {@link EnableAutoConfiguration
 * auto-configuration} and {@link ComponentScan component scanning}. This is a convenience
 * annotation that is equivalent to declaring {@code @Configuration},
 * {@code @EnableAutoConfiguration} and {@code @ComponentScan}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {

	/**
	 *
	 * 之前有一个疑问 @SpringBootApplication这个注解是如何生效的？
	 * SpringBootApplication注解 聚合了@EnableAutoConfiguration主机，这个EnableAutoConfiguration注解通过@import注解导入了一个ImportSelector实现类
	 *
	 * 在这个ImportSelector的实现类的方法中通过SpringFactoriesLoader 加载了Spring.factories配置文件中key为EnableAutoConfiguration的配置项。
	 *
	 * 那么问题究竟 是什么时候 处理了@SpringBootApplication这个注解呢？
	 *
	 * 实际上是在BeanFactoryPostProcessor 容器后置处理器
	 * ConfigurationClassPostProcessor这个处理器中 会解析 判断这个类上是否存在@Configuration注解，以及是否存在@Import注解。
	 * 如果存在则就 对@Import注解进行解析。 从而把spring.factories配置文件中的配置类纳入到spring的ioc容器中。
	 *
	 *
	 *
	 * ConfigurationClassParser.getImports(SourceClass)  (org.springframework.context.annotation)
	 *     ConfigurationClassParser.doProcessConfigurationClass(ConfigurationClass, SourceClass)  (org.springframework.context.annotation)
	 *         ConfigurationClassParser.processConfigurationClass(ConfigurationClass)  (org.springframework.context.annotation)
	 *             ConfigurationClassParser.parse(String, String)  (org.springframework.context.annotation)
	 *                 ConfigurationClassParser.doProcessConfigurationClass(ConfigurationClass, SourceClass)  (org.springframework.context.annotation)
	 *                 ConfigurationClassParser.parse(Set<BeanDefinitionHolder>)  (org.springframework.context.annotation)
	 *                     ConfigurationClassPostProcessor.processConfigBeanDefinitions(BeanDefinitionRegistry)  (org.springframework.context.annotation)
	 *                     //这里是  容器后置处理器 对class进行处理
	 *                         ConfigurationClassPostProcessor.postProcessBeanFactory(ConfigurableListableBeanFactory)  (org.springframework.context.annotation)
	 *
	 *
	 * 值得注意的是  ConfigurationClassPostProcessor 这个容器后置处理器 在调用 doProcessConfigurationClass 方法的时候 不仅仅 会处理@Configuration配置类上的
	 * @Import注解，也会处理器上面的 @ComponentScan注解
	 *
	 *   但是有一点需要思考： @SpringBootApplication标记的类 本质上是使用了@Configuration，这个类被转为一个BeanDefinition，在容器的后置处理器中对BeanDefinition 进行处理的时候
	 *   会判断BeanDefinition 这个类的class上是否存在@Configuration注解 @Import注解等等。
	 *
	 *   那么@SpringBootApplication标记的类 又是什么时候 被 被转化 成了BeanDefinition 放置到了容器中的呢？ ComponentScan需要依赖于一个已经在容器中的BeanDefinition,
	 *   也就是说@ComponentScan和@Configuration注解想要生效，必须先将其所在类转化成一个BeanDefinition并注册到IOC容器中。 如果不先转为BeanDefinition，容器是感知不到的。
	 *   那么问题就来了： 谁、什么时候、 在IOC容器中注入了一个带有@Configuration的BeanDefinition。
	 *
	 *    ApplicationContext支持在创建的时候指定一个类 作为配置类， 也就是如下方式，这个时候指定的Class会被转化成一个BeanDefinition。 在SpringBoot中，SpringApplication.run指定的类 也会被转为一个BeanDefinition。
	 *        // @Configuration注解的spring容器加载方式，用AnnotationConfigApplicationContext替换ClassPathXmlApplicationContext
	 *         ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
	 *
	 *
	 *   SpringApplication。run方法接收一个类参数。 这个参数 会被 注册为一个BeanDefinition，这个就是入口点，下面的调用栈
	 *   setBeanClass:409, AbstractBeanDefinition (org.springframework.beans.factory.support)
	 * <init>:57, AnnotatedGenericBeanDefinition (org.springframework.beans.factory.annotation)
	 * doRegisterBean:253, AnnotatedBeanDefinitionReader (org.springframework.context.annotation)
	 * registerBean:147, AnnotatedBeanDefinitionReader (org.springframework.context.annotation)
	 * register:137, AnnotatedBeanDefinitionReader (org.springframework.context.annotation)
	 * load:157, BeanDefinitionLoader (org.springframework.boot)
	 * load:136, BeanDefinitionLoader (org.springframework.boot)
	 * load:128, BeanDefinitionLoader (org.springframework.boot)
	 * load:691, SpringApplication (org.springframework.boot)
	 * prepareContext:392, SpringApplication (org.springframework.boot)
	 * run:314, SpringApplication (org.springframework.boot)
	 * run:1237, SpringApplication (org.springframework.boot)
	 * run:1226, SpringApplication (org.springframework.boot)
	 * main:12, DemoApplication (com.example.demo)
	 *
	 * 从上的内容我们看到 在SpringAPplication的 prepareContext方法中 调用了BeanDefinitionLoader的load方法。在load的时候 会将
	 * SpringApplication.run 方法接收到的参数 转化成一个BeanDefinition注册到Ioc容器中。
	 * 然后在容器后置处理器中 对这个BeanDefinition 进行解析处理 ，判断beanClass上是否存在@Configuration注解
	 *
	 *
	 */

	/**
	 * Exclude specific auto-configuration classes such that they will never be applied.
	 * @return the classes to exclude
	 */
	@AliasFor(annotation = EnableAutoConfiguration.class)
	Class<?>[] exclude() default {};

	/**
	 * Exclude specific auto-configuration class names such that they will never be
	 * applied.
	 * @return the class names to exclude
	 * @since 1.3.0
	 */
	@AliasFor(annotation = EnableAutoConfiguration.class)
	String[] excludeName() default {};

	/**
	 * Base packages to scan for annotated components. Use {@link #scanBasePackageClasses}
	 * for a type-safe alternative to String-based package names.
	 * <p>
	 * <strong>Note:</strong> this setting is an alias for
	 * {@link ComponentScan @ComponentScan} only. It has no effect on {@code @Entity}
	 * scanning or Spring Data {@link Repository} scanning. For those you should add
	 * {@link org.springframework.boot.autoconfigure.domain.EntityScan @EntityScan} and
	 * {@code @Enable...Repositories} annotations.
	 * @return base packages to scan
	 * @since 1.3.0
	 */
	@AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
	String[] scanBasePackages() default {};

	/**
	 * Type-safe alternative to {@link #scanBasePackages} for specifying the packages to
	 * scan for annotated components. The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * <p>
	 * <strong>Note:</strong> this setting is an alias for
	 * {@link ComponentScan @ComponentScan} only. It has no effect on {@code @Entity}
	 * scanning or Spring Data {@link Repository} scanning. For those you should add
	 * {@link org.springframework.boot.autoconfigure.domain.EntityScan @EntityScan} and
	 * {@code @Enable...Repositories} annotations.
	 * @return base packages to scan
	 * @since 1.3.0
	 */
	@AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
	Class<?>[] scanBasePackageClasses() default {};

	/**
	 * The {@link BeanNameGenerator} class to be used for naming detected components
	 * within the Spring container.
	 * <p>
	 * The default value of the {@link BeanNameGenerator} interface itself indicates that
	 * the scanner used to process this {@code @SpringBootApplication} annotation should
	 * use its inherited bean name generator, e.g. the default
	 * {@link AnnotationBeanNameGenerator} or any custom instance supplied to the
	 * application context at bootstrap time.
	 * @return {@link BeanNameGenerator} to use
	 * @see SpringApplication#setBeanNameGenerator(BeanNameGenerator)
	 * @since 2.3.0
	 */
	@AliasFor(annotation = ComponentScan.class, attribute = "nameGenerator")
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * Specify whether {@link Bean @Bean} methods should get proxied in order to enforce
	 * bean lifecycle behavior, e.g. to return shared singleton bean instances even in
	 * case of direct {@code @Bean} method calls in user code. This feature requires
	 * method interception, implemented through a runtime-generated CGLIB subclass which
	 * comes with limitations such as the configuration class and its methods not being
	 * allowed to declare {@code final}.
	 * <p>
	 * The default is {@code true}, allowing for 'inter-bean references' within the
	 * configuration class as well as for external calls to this configuration's
	 * {@code @Bean} methods, e.g. from another configuration class. If this is not needed
	 * since each of this particular configuration's {@code @Bean} methods is
	 * self-contained and designed as a plain factory method for container use, switch
	 * this flag to {@code false} in order to avoid CGLIB subclass processing.
	 * <p>
	 * Turning off bean method interception effectively processes {@code @Bean} methods
	 * individually like when declared on non-{@code @Configuration} classes, a.k.a.
	 * "@Bean Lite Mode" (see {@link Bean @Bean's javadoc}). It is therefore behaviorally
	 * equivalent to removing the {@code @Configuration} stereotype.
	 * @since 2.2
	 * @return whether to proxy {@code @Bean} methods
	 */
	@AliasFor(annotation = Configuration.class)
	boolean proxyBeanMethods() default true;

}
