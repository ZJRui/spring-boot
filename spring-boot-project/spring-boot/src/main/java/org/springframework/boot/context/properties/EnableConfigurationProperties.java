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

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Enable support for {@link ConfigurationProperties @ConfigurationProperties} annotated
 * beans. {@code @ConfigurationProperties} beans can be registered in the standard way
 * (for example using {@link Bean @Bean} methods) or, for convenience, can be specified
 * directly on this annotation.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableConfigurationPropertiesRegistrar.class)
public @interface EnableConfigurationProperties {


	/***
	 *
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
	 * 为了 解决 将Bean注入到容器中 可以使用@EnableCopnfigurationProperties
	 *
	 *
	 *
	 *
	 */


	/**
	 * The bean name of the configuration properties validator.
	 * @since 2.2.0
	 */
	String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	/**
	 * Convenient way to quickly register
	 * {@link ConfigurationProperties @ConfigurationProperties} annotated beans with
	 * Spring. Standard Spring Beans will also be scanned regardless of this value.
	 * @return {@code @ConfigurationProperties} annotated beans to register
	 */
	Class<?>[] value() default {};

}
