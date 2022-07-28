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

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

/**
 * {@link Configuration @Configuration} that can be used to define additional beans or
 * customizations for a test. Unlike regular {@code @Configuration} classes the use of
 * {@code @TestConfiguration} does not prevent auto-detection of
 * {@link SpringBootConfiguration @SpringBootConfiguration}.
 * <P>
 *     @Configuration，可用于为测试定义额外的bean或自定义。
 *     与常规的@Configuration类不同，
 *     @TestConfiguration的使用不会阻止@SpringBootConfiguration的自动检测。
 * </P>
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see SpringBootTestContextBootstrapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@TestComponent
@SuppressWarnings("all")
public @interface TestConfiguration {

	/**
	 * 1，这个类被使用是在
	 * SpringBootTestContextBootstrapper#containsNonTestComponent(java.lang.Class[])
	 *
	 * 2
	 */

	/**
	 * Explicitly specify the name of the Spring bean definition associated with this
	 * Configuration class. See {@link Configuration#value()} for details.
	 * @return the specified bean name, if any
	 * <P>
	 *     显式指定与这个Configuration类关联的Spring bean定义的名称。详细信息请参见Configuration.value()。
	 * </P>
	 */
	@AliasFor(annotation = Configuration.class)
	String value() default "";

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
	 * @return whether to proxy {@code @Bean} methods
	 * @since 2.2.1
	 *
	 * <P>
	 * 指定@Bean方法是否应该被代理以执行bean生命周期行为，例如，即使在用户代码中直接调用@Bean方法，也要返回共享的单例bean实例。该特性需要方法拦截，通过运行时生成的CGLIB子类实现，该子类有一些限制，比如配置类及其方法不允许声明final。
	 * 默认为true，允许配置类内部的“bean间引用”，也允许外部调用该配置的@Bean方法，例如从另一个配置类。如果不需要这样做，因为此特定配置的每个@Bean方法都是自包含的，并设计为供容器使用的普通工厂方法，则将此标志切换为false，以避免CGLIB子类处理。
	 * 关闭bean方法拦截可以有效地单独处理@Bean方法，就像在non-@Configuration类上声明时一样。“@Bean精简模式”(参见@Bean的javadoc)。因此，它在行为上等同于删除@Configuration原型。
	 * 返回:
	 * 是否代理@Bean方法
	 * </P>
	 */
	@AliasFor(annotation = Configuration.class)
	boolean proxyBeanMethods() default true;

}
