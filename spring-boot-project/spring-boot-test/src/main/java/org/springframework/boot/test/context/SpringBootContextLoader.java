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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.web.SpringBootMockServletContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.SpringVersion;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * A {@link ContextLoader} that can be used to test Spring Boot applications (those that
 * normally startup using {@link SpringApplication}). Although this loader can be used
 * directly, most test will instead want to use it with
 * {@link SpringBootTest @SpringBootTest}.
 * <p>
 * The loader supports both standard {@link MergedContextConfiguration} as well as
 * {@link WebMergedContextConfiguration}. If {@link WebMergedContextConfiguration} is used
 * the context will either use a mock servlet environment, or start the full embedded web
 * server.
 * <p>
 * If {@code @ActiveProfiles} are provided in the test class they will be used to create
 * the application context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 1.4.0
 * @see SpringBootTest
 */
public class SpringBootContextLoader extends AbstractContextLoader {

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration config) throws Exception {
		Class<?>[] configClasses = config.getClasses();
		String[] configLocations = config.getLocations();
		Assert.state(!ObjectUtils.isEmpty(configClasses) || !ObjectUtils.isEmpty(configLocations),
				() -> "No configuration classes or locations found in @SpringApplicationConfiguration. "
						+ "For default configuration detection to work you need Spring 4.0.3 or better (found "
						+ SpringVersion.getVersion() + ").");
		SpringApplication application = getSpringApplication();
		application.setMainApplicationClass(config.getTestClass());
		application.addPrimarySources(Arrays.asList(configClasses));
		application.getSources().addAll(Arrays.asList(configLocations));
		ConfigurableEnvironment environment = getEnvironment();
		if (!ObjectUtils.isEmpty(config.getActiveProfiles())) {
			setActiveProfiles(environment, config.getActiveProfiles());
		}
		ResourceLoader resourceLoader = (application.getResourceLoader() != null) ? application.getResourceLoader()
				: new DefaultResourceLoader(null);
		TestPropertySourceUtils.addPropertiesFilesToEnvironment(environment, resourceLoader,
				config.getPropertySourceLocations());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, getInlinedProperties(config));
		application.setEnvironment(environment);
		List<ApplicationContextInitializer<?>> initializers = getInitializers(config, application);
		if (config instanceof WebMergedContextConfiguration) {
			application.setWebApplicationType(WebApplicationType.SERVLET);
			if (!isEmbeddedWebEnvironment(config)) {
				new WebConfigurer().configure(config, application, initializers);
			}
		}
		else if (config instanceof ReactiveWebMergedContextConfiguration) {
			application.setWebApplicationType(WebApplicationType.REACTIVE);
			if (!isEmbeddedWebEnvironment(config)) {
				application.setApplicationContextFactory(
						ApplicationContextFactory.of(GenericReactiveWebApplicationContext::new));
			}
		}
		else {
			application.setWebApplicationType(WebApplicationType.NONE);
		}
		application.setInitializers(initializers);
		String[] args = SpringBootTestArgs.get(config.getContextCustomizers());
		return application.run(args);
	}

	/**
	 * Builds new {@link org.springframework.boot.SpringApplication} instance. You can
	 * override this method to add custom behavior
	 * @return {@link org.springframework.boot.SpringApplication} instance
	 */
	protected SpringApplication getSpringApplication() {
		return new SpringApplication();
	}

	/**
	 * Builds a new {@link ConfigurableEnvironment} instance. You can override this method
	 * to return something other than {@link StandardEnvironment} if necessary.
	 * @return a {@link ConfigurableEnvironment} instance
	 */
	protected ConfigurableEnvironment getEnvironment() {
		return new StandardEnvironment();
	}

	private void setActiveProfiles(ConfigurableEnvironment environment, String[] profiles) {
		environment.setActiveProfiles(profiles);
		// Also add as properties to override any application.properties
		String[] pairs = new String[profiles.length];
		for (int i = 0; i < profiles.length; i++) {
			pairs[i] = "spring.profiles.active[" + i + "]=" + profiles[i];
		}
		TestPropertyValues.of(pairs).applyTo(environment);
	}

	protected String[] getInlinedProperties(MergedContextConfiguration config) {
		ArrayList<String> properties = new ArrayList<>();
		// JMX bean names will clash if the same bean is used in multiple contexts
		disableJmx(properties);
		properties.addAll(Arrays.asList(config.getPropertySourceProperties()));
		return StringUtils.toStringArray(properties);
	}

	private void disableJmx(List<String> properties) {
		properties.add("spring.jmx.enabled=false");
	}

	/**
	 * Return the {@link ApplicationContextInitializer initializers} that will be applied
	 * to the context. By default this method will adapt {@link ContextCustomizer context
	 * customizers}, add {@link SpringApplication#getInitializers() application
	 * initializers} and add
	 * {@link MergedContextConfiguration#getContextInitializerClasses() initializers
	 * specified on the test}.
	 * @param config the source context configuration
	 * @param application the application instance
	 * @return the initializers to apply
	 * @since 2.0.0
	 */
	protected List<ApplicationContextInitializer<?>> getInitializers(MergedContextConfiguration config,
			SpringApplication application) {
		List<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
		for (ContextCustomizer contextCustomizer : config.getContextCustomizers()) {
			initializers.add(new ContextCustomizerAdapter(contextCustomizer, config));
		}
		initializers.addAll(application.getInitializers());
		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : config
				.getContextInitializerClasses()) {
			initializers.add(BeanUtils.instantiateClass(initializerClass));
		}
		if (config.getParent() != null) {
			initializers.add(new ParentContextApplicationContextInitializer(config.getParentApplicationContext()));
		}
		return initializers;
	}

	private boolean isEmbeddedWebEnvironment(MergedContextConfiguration config) {
		return MergedAnnotations.from(config.getTestClass(), SearchStrategy.TYPE_HIERARCHY).get(SpringBootTest.class)
				.getValue("webEnvironment", WebEnvironment.class).orElse(WebEnvironment.NONE).isEmbedded();
	}

	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		super.processContextConfiguration(configAttributes);
		if (!configAttributes.hasResources()) {
			/**
			 *
			 * 这里的configAttributes
			 *
			 * 在org.springframework.test.context.support.AbstractTestContextBootstrapper#buildDefaultMergedContextConfiguration(java.lang.Class, org.springframework.test.context.CacheAwareContextLoaderDelegate)
			 * 方法中会构建contextConfigurationAttributes
			 * 其中有一个 就是包含了测试类的ContextConfigurationAttributes
			 *
			 * List<ContextConfigurationAttributes> defaultConfigAttributesList =
			 * 				Collections.singletonList(new ContextConfigurationAttributes(testClass));
			 *
			 */

			Class<?>[] defaultConfigClasses = detectDefaultConfigurationClasses(configAttributes.getDeclaringClass());
			configAttributes.setClasses(defaultConfigClasses);
		}
	}

	/**
	 * Detect the default configuration classes for the supplied test class. By default
	 * simply delegates to
	 * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses}.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but never
	 * {@code null}
	 * @see AnnotationConfigContextLoaderUtils
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {

		/**
		 *
		 * 讨论： 什么情况下会 使用@SpringBootApplication注解标注的类作为配置类  ，@TestConfiguration的工作原理
		 *
		 * 首先我们可以嘉定这里的declaringClasses就是测试类 class
		 * 其次要注意 下面的AnnotationConfigContextLoaderUtils 类是spring-test的类，detectDefaultConfigurationClasses 方法
		 * 会使用getDeclaredClasses得到该类所有的内部类，除去父类的。
		 *
		 * 其次： 我们要考虑一个配置类  如果内部有 静态配置类和 非静态内部配置类。spring会如何处理
		 *
		 *
		 * 实际测试发现: 非测试路径下的配置类， 如果这个配置类能被扫描到加载那么这个配置类中的静态和非静态的内部配置类都能 使用@Bean注入Bean。但是一般我们都是使用静态内部类。
		 *
		 * 在运行测试用例的时候： 项目正式路径下的配置类不会 被detectDefaultConfigurationClasses 方法执行。也就是说 如果你的测试类能够 扫描到了 正式路径下的配置类，
		 * 那么这个正式配置类中的静态和非静态内部配置类中的@Bean都会生效。
		 *
		 *
		 * 在运行测试用例的时候 我们会 分析测试类上的@ContextConfigruation注解  或者通过这里的declaringClass等于测试类 分析测试类内部是否有静态内部配置类 来作为配置类。
		 * 如果发现了有静态内部类作为配置了，那么这个时候就不会再 寻找@SpringBootConfiguration 注解标注的类 作为配置类了。
		 *
		 * 以上对于 静态内部类生效的前提条件是这个静态内部类没有使用 @TestConfiguration注解标注，在没有使用@@TestConfiguration注解标注的情况下，我们说这个配置类可以替代@SpringBoootApplication
		 *
		 * 如果 静态内部配置类使用了@TestConfiguration注解标注，那么这个静态内部类只是对@SpringBootappliction配置类的补充，因此还会继续寻找 @SpringBootApplication注解标注的类
		 *
		 *
		 *
		 *
		 *
		 *
		 *
		 */


		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
	}

	@Override
	public ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"SpringApplicationContextLoader does not support the loadContext(String...) method");
	}

	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { "-context.xml", "Context.groovy" };
	}

	@Override
	protected String getResourceSuffix() {
		throw new IllegalStateException();
	}

	/**
	 * Inner class to configure {@link WebMergedContextConfiguration}.
	 */
	private static class WebConfigurer {

		void configure(MergedContextConfiguration configuration, SpringApplication application,
				List<ApplicationContextInitializer<?>> initializers) {
			WebMergedContextConfiguration webConfiguration = (WebMergedContextConfiguration) configuration;
			addMockServletContext(initializers, webConfiguration);
			application.setApplicationContextFactory((webApplicationType) -> new GenericWebApplicationContext());
		}

		private void addMockServletContext(List<ApplicationContextInitializer<?>> initializers,
				WebMergedContextConfiguration webConfiguration) {
			SpringBootMockServletContext servletContext = new SpringBootMockServletContext(
					webConfiguration.getResourceBasePath());
			initializers.add(0, new ServletContextApplicationContextInitializer(servletContext, true));
		}

	}

	/**
	 * Adapts a {@link ContextCustomizer} to a {@link ApplicationContextInitializer} so
	 * that it can be triggered via {@link SpringApplication}.
	 */
	private static class ContextCustomizerAdapter
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		private final ContextCustomizer contextCustomizer;

		private final MergedContextConfiguration config;

		ContextCustomizerAdapter(ContextCustomizer contextCustomizer, MergedContextConfiguration config) {
			this.contextCustomizer = contextCustomizer;
			this.config = config;
		}

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			this.contextCustomizer.customizeContext(applicationContext, this.config);
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private static class ParentContextApplicationContextInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		private final ApplicationContext parent;

		ParentContextApplicationContextInitializer(ApplicationContext parent) {
			this.parent = parent;
		}

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.setParent(this.parent);
		}

	}

}
