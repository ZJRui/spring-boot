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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 *
 * {@link TestContextBootstrapper} for Spring Boot. Provides support for
 * {@link SpringBootTest @SpringBootTest} and may also be used directly or subclassed.
 * Provides the following features over and above {@link DefaultTestContextBootstrapper}:
 * <ul>
 * <li>Uses {@link SpringBootContextLoader} as the
 * {@link #getDefaultContextLoaderClass(Class) default context loader}.</li>
 * <li>Automatically searches for a
 * {@link SpringBootConfiguration @SpringBootConfiguration} when required.</li>
 * <li>Allows custom {@link Environment} {@link #getProperties(Class)} to be defined.</li>
 * <li>Provides support for different {@link WebEnvironment webEnvironment} modes.</li>
 * </ul>
 *
 *
 TestContextBootstrapper用于Spring Boot。提供对@SpringBootTest的支持，也可以直接使用或子类化。
 在DefaultTestContextBootstrapper之上提供了以下特性:
 使用SpringBootContextLoader作为默认的上下文加载器。
 需要时自动搜索@SpringBootConfiguration。
 允许自定义环境getProperties(类)被定义。
 提供对不同webEnvironment模式的支持。
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Lorenzo Dee
 * @since 1.4.0
 * @see SpringBootTest
 * @see TestConfiguration
 */
public class SpringBootTestContextBootstrapper extends DefaultTestContextBootstrapper {

	private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private static final String REACTIVE_WEB_ENVIRONMENT_CLASS = "org.springframework."
			+ "web.reactive.DispatcherHandler";

	private static final String MVC_WEB_ENVIRONMENT_CLASS = "org.springframework.web.servlet.DispatcherServlet";

	private static final String JERSEY_WEB_ENVIRONMENT_CLASS = "org.glassfish.jersey.server.ResourceConfig";

	private static final String ACTIVATE_SERVLET_LISTENER = "org.springframework.test."
			+ "context.web.ServletTestExecutionListener.activateListener";

	private static final Log logger = LogFactory.getLog(SpringBootTestContextBootstrapper.class);

	@Override
	public TestContext buildTestContext() {
		/**
		 *
		 * 如果不考虑SpringBoot， 在Spring中 测试类的运行是 ： Junit会根据
		 * @Runwith注解指定的Runner是SpringJunit4ClassRunner来 创建这个Runner对象，
		 * 在SpringJunit4ClassRunner的构造器中会创建TestManager
		 * TestManager的构造器中 会 调用  contextBOotStrap的buildTestContext方法构建TestContext
		 * this.testContext = testContextBootstrapper.buildTestContext();
		 * 在buildContext之前会先执行 buildMergedContextConfiguration  来解析测试类上的注解确定 Configuration。
		 * 然后将这个Configuration信息 叫个buildContext 方法中构建的 TestContext对象。
		 *
		 * AbstractTestContextBootstrapper的buildTestContext实现
		 * public TestContext buildTestContext() {
		 * 		return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
		 * 				getCacheAwareContextLoaderDelegate());
		 * }
		 *
		 * 因为SpringBoot 对配置的解析不同于Spring，SpringBoot需要解析@SpringBootTest中指定的classes参数指定ID配置类，或者添加
		 * @SpringBootApplicatiion注解标注的主类作为 测试的配置。所以SpringBoot的 SpringBootTestContextBootstrapper
		 * 重写了buildTestContext方法，但是这个build方法的第一步还是调用了 super.buildTestContext 来构建一个TestContext
		 *
		 * 在 spring的 buildMergedContextConfiguration 为TestContext构建配置信息的时候，会执行resolveContextLoader
		 *
		 * 而SpringBootTestContextBootstrapper重写了resolveContextLoader ，在这个resolveContextLoader 方法中 会解析
		 * @SpringBootTest注解中 配置的classes【org.springframework.boot.test.context.SpringBootTestContextBootstrapper#getClasses(java.lang.Class)】
		 *
		 *
		 * ------------------------------------
		 *
		 * 问题： SpringBootTestContextBootstrapper是什么时候被创建的？
		 * 是在SpringJunit4ClassRunner 创建TestManager的的时候， TestManager构造器中会创建ContextBootstrap对象
		 * BootstrapUtils.createBootstrapContext(testClass)
		 *
		 *
		 * 这里的super中会 buildMergedContextConfiguration  构建 configuration
		 *
		 * 注意当前类：SpringBootTestContextBootstrapper是SpringBoot的类，而父类
		 * DefaultTestContextBootstrapper 是Spring的，super.buildTestContext(); 会执行spring的内容
		 *
		 *
		 *
		 *
		 */
		TestContext context = super.buildTestContext();
		verifyConfiguration(context.getTestClass());
		WebEnvironment webEnvironment = getWebEnvironment(context.getTestClass());
		if (webEnvironment == WebEnvironment.MOCK && deduceWebApplicationType() == WebApplicationType.SERVLET) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, true);
		}
		else if (webEnvironment != null && webEnvironment.isEmbedded()) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, false);
		}
		return context;
	}

	@Override
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> listeners = super.getDefaultTestExecutionListenerClasses();
		List<DefaultTestExecutionListenersPostProcessor> postProcessors = SpringFactoriesLoader
				.loadFactories(DefaultTestExecutionListenersPostProcessor.class, getClass().getClassLoader());
		for (DefaultTestExecutionListenersPostProcessor postProcessor : postProcessors) {
			listeners = postProcessor.postProcessDefaultTestExecutionListeners(listeners);
		}
		return listeners;
	}

	@Override
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {

		/**
		 *
		 * classes:-1, $Proxy7 (com.sun.proxy)---------》提取@Bootstrap注解中的value参数
		 * invoke0:-1, NativeMethodAccessorImpl (sun.reflect)
		 * invoke:62, NativeMethodAccessorImpl (sun.reflect)
		 * invoke:43, DelegatingMethodAccessorImpl (sun.reflect)
		 * invoke:498, Method (java.lang.reflect)
		 * isValid:112, AttributeMethods (org.springframework.core.annotation)
		 * getDeclaredAnnotations:460, AnnotationsScanner (org.springframework.core.annotation)------------》寻找@BootstrapWith注解
		 * isKnownEmpty:492, AnnotationsScanner (org.springframework.core.annotation)
		 * from:251, TypeMappedAnnotations (org.springframework.core.annotation)
		 * from:351, MergedAnnotations (org.springframework.core.annotation)
		 * from:330, MergedAnnotations (org.springframework.core.annotation)
		 * from:313, MergedAnnotations (org.springframework.core.annotation)
		 * from:300, MergedAnnotations (org.springframework.core.annotation)
		 * isAnnotationDeclaredLocally:675, AnnotationUtils (org.springframework.core.annotation)
		 * findAnnotationDescriptor:240, TestContextAnnotationUtils (org.springframework.test.context)
		 * findAnnotationDescriptor:214, TestContextAnnotationUtils (org.springframework.test.context)
		 * resolveExplicitTestContextBootstrapper:165, BootstrapUtils (org.springframework.test.context)
		 * resolveTestContextBootstrapper:138, BootstrapUtils (org.springframework.test.context)
		 * <init>:122, TestContextManager (org.springframework.test.context)------------->TestContextManager创建
		 *
		 *
		 * TestContextManager创建的时候户i执行如下内容， resolveTestContextBootstrapper 会触发 解析测试类上的@BootstrapWith注解 从而提取这个注解中的value参数
		 * 		this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
		 *
		 * 	对于SpringBoot测试类而言，他使用@SpringBootTest注解标注，这个@SpringBootTest 默认聚合了@BootstrapWith(SpringBootTestContextBootstrapper.class)
		 *
		 * 	因此他会创建SpringbootTestContextbootstrapper对象，然后 这个对象会负责解析SpringBootTest注解本身的内容。
		 *
		 * 	也就是说@SpringBootTest注解本身有两部分内容（1）聚合了Spring的注解（SpringBootTestContextBootstrapper），通过这个Sprig的注解告诉 spring 你可以
		 * 	我这个注解指定的springboot的类来完成某项工作 （2）SpringBoot框架自己的@SpringBootTest注解自身的 内容，比如@SpringBootTest注解内可以配置classes 、exclude等
		 * 	信息，Spring本身不会解析@SpringBootTest，因为这个是SpringBoot的注解， 但是SpringBoot可以通过Spring的注解告诉spring 使用springboot的SpringbootTestContextbootstrapper
		 * 	来解析@SPringBootTest
		 *
		 *
		 *
		 *
		 *
		 */

		Class<?>[] classes = getClasses(testClass);//解析测试中的@SpringBootTest注解中的classes参数
		if (!ObjectUtils.isEmpty(classes)) {
			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
				/**
				 * 注意这里，一旦发现@SpringBootTest 注解中 通过classes指定的了配置类，那么就 将其添加到 confgiAttributes中。
				 *
				 *List<ContextConfigurationAttributes> defaultConfigAttributesList =
				 * 				Collections.singletonList(new ContextConfigurationAttributes(testClass));
				 *也就是说 有一个configAttributes 持有testClass作为declaringClass属性，同时会将配置信息
				 * 放置到自身的classes数组对象中
				 *
				 * private final Class<?> declaringClass;---->测试类
				 *
				 * 	private Class<?>[] classes = new Class<?>[0];---》存放配置类信息
				 *
				 *
				 * 	一旦ContextConfigurationAttributes 对象中的 classes数组中的配置类信息不为空，那么在 getOrFindConfigurationClasses
				 * 	org.springframework.boot.test.context.SpringBootTestContextBootstrapper#getOrFindConfigurationClasses
				 * 	方法中就会不再 查找 项目中 @SpringBootConfiguration注解标注的类作为配置类了（因为@SpringBootApplication注解聚合了
				 * 	@SpringBootApplicationConfiguration，实际上也就是查找@SpringBootApplication配置的类作为 测试的配置类）
				 */
				addConfigAttributesClasses(configAttributes, classes);
			}
		}
		/**
		 * 最终会返回
		 * org.springframework.boot.test.context.SpringBootContextLoader
		 *
		 * 这是通过 ：
		 * org.springframework.boot.test.context.SpringBootTestContextBootstrapper#getDefacultContextLoaderClass
		 *
		 */
		return super.resolveContextLoader(testClass, configAttributesList);
	}

	private void addConfigAttributesClasses(ContextConfigurationAttributes configAttributes, Class<?>[] classes) {
		Set<Class<?>> combined = new LinkedHashSet<>(Arrays.asList(classes));
		if (configAttributes.getClasses() != null) {
			combined.addAll(Arrays.asList(configAttributes.getClasses()));
		}
		configAttributes.setClasses(ClassUtils.toClassArray(combined));
	}

	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		return SpringBootContextLoader.class;
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
		List<String> propertySourceProperties = getAndProcessPropertySourceProperties(mergedConfig);
		mergedConfig = createModifiedConfig(mergedConfig, classes, StringUtils.toStringArray(propertySourceProperties));
		WebEnvironment webEnvironment = getWebEnvironment(mergedConfig.getTestClass());
		if (webEnvironment != null && isWebEnvironmentSupported(mergedConfig)) {
			WebApplicationType webApplicationType = getWebApplicationType(mergedConfig);
			if (webApplicationType == WebApplicationType.SERVLET
					&& (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
				mergedConfig = new WebMergedContextConfiguration(mergedConfig, determineResourceBasePath(mergedConfig));
			}
			else if (webApplicationType == WebApplicationType.REACTIVE
					&& (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
				return new ReactiveWebMergedContextConfiguration(mergedConfig);
			}
		}
		return mergedConfig;
	}

	private WebApplicationType getWebApplicationType(MergedContextConfiguration configuration) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(
				TestPropertySourceUtils.convertInlinedPropertiesToMap(configuration.getPropertySourceProperties()));
		Binder binder = new Binder(source);
		return binder.bind("spring.main.web-application-type", Bindable.of(WebApplicationType.class))
				.orElseGet(this::deduceWebApplicationType);
	}

	private WebApplicationType deduceWebApplicationType() {
		if (ClassUtils.isPresent(REACTIVE_WEB_ENVIRONMENT_CLASS, null)
				&& !ClassUtils.isPresent(MVC_WEB_ENVIRONMENT_CLASS, null)
				&& !ClassUtils.isPresent(JERSEY_WEB_ENVIRONMENT_CLASS, null)) {
			return WebApplicationType.REACTIVE;
		}
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return WebApplicationType.NONE;
			}
		}
		return WebApplicationType.SERVLET;
	}

	/**
	 * Determines the resource base path for web applications using the value of
	 * {@link WebAppConfiguration @WebAppConfiguration}, if any, on the test class of the
	 * given {@code configuration}. Defaults to {@code src/main/webapp} in its absence.
	 * @param configuration the configuration to examine
	 * @return the resource base path
	 * @since 2.1.6
	 */
	protected String determineResourceBasePath(MergedContextConfiguration configuration) {
		return MergedAnnotations.from(configuration.getTestClass(), SearchStrategy.TYPE_HIERARCHY)
				.get(WebAppConfiguration.class).getValue(MergedAnnotation.VALUE, String.class)
				.orElse("src/main/webapp");
	}

	private boolean isWebEnvironmentSupported(MergedContextConfiguration mergedConfig) {
		Class<?> testClass = mergedConfig.getTestClass();
		ContextHierarchy hierarchy = AnnotationUtils.getAnnotation(testClass, ContextHierarchy.class);
		if (hierarchy == null || hierarchy.value().length == 0) {
			return true;
		}
		ContextConfiguration[] configurations = hierarchy.value();
		return isFromConfiguration(mergedConfig, configurations[configurations.length - 1]);
	}

	private boolean isFromConfiguration(MergedContextConfiguration candidateConfig,
			ContextConfiguration configuration) {
		ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(candidateConfig.getTestClass(),
				configuration);
		Set<Class<?>> configurationClasses = new HashSet<>(Arrays.asList(attributes.getClasses()));
		for (Class<?> candidate : candidateConfig.getClasses()) {
			if (configurationClasses.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = mergedConfig.getClasses();
		/**
		 * ---》 注意这里的containsNonTestComponent 会娇艳 classes上是否存在@TestConfiguration注解，如果没有@TestConfiguration注解则表明这个
		 * 类可以替代@SpringBootApplication配置了。如果有@TestConfiguration配置类则说明是对@SpringBootApplication配置类的补充。
		 * 也就是说： 一旦对测试类进行解析，发现已经配置了配置类， 这个配置的方式可以是
		 * （1）@ContextConfiguraon注解配置  （2）SpringBootTest的classes属性指定配置类
		 * （3）通过import等其他方式倒入配置和类 （4）测试类内部有定义了 静态内部配置类且这个静态内部配置类没有使用@TestConfiguration注解配置
		 * 那么这个时候我们就认为 已经为测试类配置了 有效的配置类。 不在寻找@SpringBootConfiguration配置类
		 */
		if (containsNonTestComponent(classes) || mergedConfig.hasLocations()) {
			return classes;
		}
		/**
		 * SpringBootApplication就聚合了 @SpringBootConfiguration ，因此这里会找到  主配置类。
		 */
		Class<?> found = new AnnotatedClassFinder(SpringBootConfiguration.class)
				.findFromClass(mergedConfig.getTestClass());
		//found==null的时候 assert跑出异常
		Assert.state(found != null, "Unable to find a @SpringBootConfiguration, you need to use "
				+ "@ContextConfiguration or @SpringBootTest(classes=...) with your test");
		logger.info("Found @SpringBootConfiguration " + found.getName() + " for test " + mergedConfig.getTestClass());
		return merge(found, classes);
	}

	private boolean containsNonTestComponent(Class<?>[] classes) {
		for (Class<?> candidate : classes) {
			if (!MergedAnnotations.from(candidate, SearchStrategy.INHERITED_ANNOTATIONS)
					.isPresent(TestConfiguration.class)) {
				return true;
			}
		}
		return false;
	}

	private Class<?>[] merge(Class<?> head, Class<?>[] existing) {
		Class<?>[] result = new Class<?>[existing.length + 1];
		result[0] = head;
		System.arraycopy(existing, 0, result, 1, existing.length);
		return result;
	}

	private List<String> getAndProcessPropertySourceProperties(MergedContextConfiguration mergedConfig) {
		List<String> propertySourceProperties = new ArrayList<>(
				Arrays.asList(mergedConfig.getPropertySourceProperties()));
		String differentiator = getDifferentiatorPropertySourceProperty();
		if (differentiator != null) {
			propertySourceProperties.add(differentiator);
		}
		processPropertySourceProperties(mergedConfig, propertySourceProperties);
		return propertySourceProperties;
	}

	/**
	 * Return a "differentiator" property to ensure that there is something to
	 * differentiate regular tests and bootstrapped tests. Without this property a cached
	 * context could be returned that wasn't created by this bootstrapper. By default uses
	 * the bootstrapper class as a property.
	 * @return the differentiator or {@code null}
	 */
	protected String getDifferentiatorPropertySourceProperty() {
		return getClass().getName() + "=true";
	}

	/**
	 * Post process the property source properties, adding or removing elements as
	 * required.
	 * @param mergedConfig the merged context configuration
	 * @param propertySourceProperties the property source properties to process
	 */
	protected void processPropertySourceProperties(MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		Class<?> testClass = mergedConfig.getTestClass();
		String[] properties = getProperties(testClass);
		if (!ObjectUtils.isEmpty(properties)) {
			// Added first so that inlined properties from @TestPropertySource take
			// precedence
			propertySourceProperties.addAll(0, Arrays.asList(properties));
		}
		if (getWebEnvironment(testClass) == WebEnvironment.RANDOM_PORT) {
			propertySourceProperties.add("server.port=0");
		}
	}

	/**
	 * Return the {@link WebEnvironment} type for this test or null if undefined.
	 * @param testClass the source test class
	 * @return the {@link WebEnvironment} or {@code null}
	 */
	protected WebEnvironment getWebEnvironment(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation != null) ? annotation.webEnvironment() : null;
	}

	protected Class<?>[] getClasses(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation != null) ? annotation.classes() : null;
	}

	protected String[] getProperties(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation != null) ? annotation.properties() : null;
	}

	protected SpringBootTest getAnnotation(Class<?> testClass) {
		return TestContextAnnotationUtils.findMergedAnnotation(testClass, SpringBootTest.class);
	}

	protected void verifyConfiguration(Class<?> testClass) {
		SpringBootTest springBootTest = getAnnotation(testClass);
		if (springBootTest != null && isListeningOnPort(springBootTest.webEnvironment()) && MergedAnnotations
				.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS).isPresent(WebAppConfiguration.class)) {
			throw new IllegalStateException("@WebAppConfiguration should only be used "
					+ "with @SpringBootTest when @SpringBootTest is configured with a "
					+ "mock web environment. Please remove @WebAppConfiguration or reconfigure @SpringBootTest.");
		}
	}

	private boolean isListeningOnPort(WebEnvironment webEnvironment) {
		return webEnvironment == WebEnvironment.DEFINED_PORT || webEnvironment == WebEnvironment.RANDOM_PORT;
	}

	/**
	 * Create a new {@link MergedContextConfiguration} with different classes.
	 * @param mergedConfig the source config
	 * @param classes the replacement classes
	 * @return a new {@link MergedContextConfiguration}
	 */
	protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
			Class<?>[] classes) {
		return createModifiedConfig(mergedConfig, classes, mergedConfig.getPropertySourceProperties());
	}

	/**
	 * Create a new {@link MergedContextConfiguration} with different classes and
	 * properties.
	 * @param mergedConfig the source config
	 * @param classes the replacement classes
	 * @param propertySourceProperties the replacement properties
	 * @return a new {@link MergedContextConfiguration}
	 */
	protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
			Class<?>[] classes, String[] propertySourceProperties) {
		Set<ContextCustomizer> contextCustomizers = new LinkedHashSet<>(mergedConfig.getContextCustomizers());
		contextCustomizers.add(new SpringBootTestArgs(mergedConfig.getTestClass()));
		contextCustomizers.add(new SpringBootTestWebEnvironment(mergedConfig.getTestClass()));
		return new MergedContextConfiguration(mergedConfig.getTestClass(), mergedConfig.getLocations(), classes,
				mergedConfig.getContextInitializerClasses(), mergedConfig.getActiveProfiles(),
				mergedConfig.getPropertySourceLocations(), propertySourceProperties, contextCustomizers,
				mergedConfig.getContextLoader(), getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
	}

}
