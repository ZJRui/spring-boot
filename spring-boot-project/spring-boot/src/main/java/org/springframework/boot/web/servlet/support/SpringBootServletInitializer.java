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

package org.springframework.boot.web.servlet.support;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * An opinionated {@link WebApplicationInitializer} to run a {@link SpringApplication}
 * from a traditional WAR deployment. Binds {@link Servlet}, {@link Filter} and
 * {@link ServletContextInitializer} beans from the application context to the server.
 * <p>
 * To configure the application either override the
 * {@link #configure(SpringApplicationBuilder)} method (calling
 * {@link SpringApplicationBuilder#sources(Class...)}) or make the initializer itself a
 * {@code @Configuration}. If you are using {@link SpringBootServletInitializer} in
 * combination with other {@link WebApplicationInitializer WebApplicationInitializers} you
 * might also want to add an {@code @Ordered} annotation to configure a specific startup
 * order.
 * <p>
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded web server then you won't need this at
 * all.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see #configure(SpringApplicationBuilder)
 */

/**
 *
 * SpringBoot 的两种启动方式  一个是 war 部署，一个是jar部署
 *
 *
 * 首先我们说 SpringBoot是 如何 支持嵌入式Servlet的，也就是Jar 直接启动的方式。 所谓支持 的含义就是 Servlet容器 （ServletContext）中如何注册Servlet、Filter、Listener等组件
 * 以及如何启动Tomcat。
 *  *
 * 		 * 	 * Tomcat启动 ，Tomcat创建
 * 		 * 	 * ServletWebServerApplicationContext 作为ApplicationContext，本身重写了onRefresh方法 ，用来创建Tomcat
 * 		 * 	 * ServletWebServerApplicationContext.onRefresh()  (org.springframework.boot.web.servlet.context)
 * 		 * 	 *     AbstractApplicationContext.refresh()  (org.springframework.context.support)
 * 		 *
 * 		 * 	 问题 Tomcat 和TomcatStarter 有什么区别？
 * 		 * 	 TomcatStarter 本质上是一个ServletContainerInitializer.
 * 		 * 	 ServletContainerInitializer 是servlet 3.0的规范。  传统的tomcat启动的时候 会通过spi机制 获取
 * 		 * 	 meta-info/services目录下配置的ServletContainerInitializer 实现类，然后在StandardContext的start方法中
 * 		 * 	 调用每一个ServletContainerInitializer实现类的onStartUp方法。
 * 		 *
 * 		 *
 * 		 * 	 在这里 首先我们创建了Tomcat，然后在下面的prepareContext 中创建了 TomcatEmbeddedContext （这是StandardContext的实现类）
 * 		 *
 * 		 * 	 然后在prepareContext中调用了configureContext ，在这个config方法中创建了TomcatStarter（这是ServletContainerInitializer实现类）
 * 		 * 	 并将这个TomcatStarter放置到StandardContext中。
 * 		 *
 * 		 * 	 那么TomcatStarter的作用是什么呢？
 * 		 * 	 TomcatStarter作用是获取到Tomcat的StandardContext的启动时机。 在TomcatStarter的实现中 会调用 每个 ServletContextInitializer 的onStartUp方法
 * 		 *
 * 		 * 	 那么ServletContextInitializer 又是什么呢？  ServletContextInitializer 是SpringBoot提供的API
 * 		 * 	 他的子类是RegistrationBean 、 ServletListenerRegistrationBean  ServletRegistrationBean FilterRegistrationBean
 * 		 *
 * 		 * 	 在onStartUp方法中会将 这些Bean 注入到ServletContext中 ：servletContext.addFilter(getOrDeduceName(filter), filter);
 * 		 * 	 servletContext.addListener(this.listener);  servletContext.addServlet(name, this.servlet);
 * 		 *
 * 		 * 	 因此 ServletContextInitializer 接口的主要作用就是动态 将Servlet、Filter、Listener添加到ServletContext中。
 * 		 *
 * 		 *
 * 		 * 	 因此在 web 自动装配的过程中 WebMvcAutoConfiguration 导入了 DispatcherServletAutoConfiguration ，在DispatcherServletAutoConfiguration中 我们 既 创建了
 * 		 * 	 DispatcherServlet对象， 将这个servlet注入到IOC容器中， 又创建了 DispatcherServletRegistrationBean ，使用这个RegistrationBean 将 DispatcherServlet 注册到ServletContext中。
 * 		 *
 * 		 * 	 如果我们想自己注册Servlet或者Filter，只需要使用RegistrationBean 将Servlet、filter注册到ServletContext。 并不需要把Servlet、Filter对象注册到IOC容器中。因此一般如下：
 * 		 *            @Bean
 * 		 *     public DispatcherServletRegistrationBean dispatcherServletRegistrationBeanJsp() {
 * 		 *         DispatcherServlet dispatcherServlet = new DispatcherServlet(new AnnotationConfigServletWebApplicationContext("com.michael.springsecurityentitlement.jsp"));
 * 		 *         DispatcherServletRegistrationBean dispatcher = new DispatcherServletRegistrationBean(dispatcherServlet , "/jsp/*");
 * 		 *         dispatcher.setName("jspDispatcher");//注册另外一个servlet
 * 		 *         dispatcher.setLoadOnStartup(1);
 * 		 *         dispatcher.setOrder(Ordered.HIGHEST_PRECEDENCE);
 * 		 *         return dispatcher;
 * 		 *     }
 * 		 *
 *
 *
 * 		 ===========================
 *
 * 		 然后我们来说  SpringBoot是如何支持war 部署的
 * 		 在Servlet3.0 API中提供了一个javax.servlet.ServletContainerInitializer接口，
 * 		 Tomcat启动的时候会通过ServiceLoader机制加载meta-info/services目录配置的实现类。
 * 		 Spring在Spring-web项目的services目录下配置了SpringServletContainerInitializer
 * 		 作为实现类。内部实现会调用所有WebApplicationInitializer的方法。
 * 		 而SpringBoot针对WebApplicationInitializer 提供了SpringBootServletInitializer 作为实现类，在这个实现类中会创建springIOC容器。
 *
 *
 * 		 在SpringBootServletInitializer的createRootApplicationContext 方法中 创建IOC容器，在在创建ApplicationContext之前，给这个ApplicationContextBuilder
 * 		 添加了一个 ApplicationContextInitializer  ServletContextApplicationContextInitializer ，这个实现的方法中 将ServletContext 设置给了ApplicationContext。
 * 		 最终被创建出来的 ApplicationContext就是AnnotationConfigServletWebServerApplicationContext ，这个context重写了onRefresh方法。
 * 		 在onRefresh方法中调用了createWebServer ,然后根据 判断当前context中是否持有 ServletContext来 决定是否 创建Tomcat。 war部署的时候并不需要创建tomcat，
 * 		 但是在createWebServer方法中 会 获取所有ServletContextInitializer的实现类（一般都是RegistrationBean，用来注册Filter、Listener、 Servlet）
 * 				 * 然后调用其onStartUp方法， 将servlet、listener、Filter注册到ServletContext中。
 *
 * 				最终war部署模式下 也将 servlet、filter、listener组件注册到了ServletContext中。
 *
 *
 *
 *
 *
 *
 */
public abstract class SpringBootServletInitializer implements WebApplicationInitializer {

	protected Log logger; // Don't initialize early

	private boolean registerErrorPageFilter = true;

	/**
	 * Set if the {@link ErrorPageFilter} should be registered. Set to {@code false} if
	 * error page mappings should be handled via the server and not Spring Boot.
	 * @param registerErrorPageFilter if the {@link ErrorPageFilter} should be registered.
	 */
	protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter) {
		this.registerErrorPageFilter = registerErrorPageFilter;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		servletContext.setAttribute(LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY, false);
		// Logger initialization is deferred in case an ordered
		// LogServletContextInitializer is being used
		this.logger = LogFactory.getLog(getClass());
		/**
		 * 注意这里创建  ApplicationContext。 非web部署方式下   Tomcat会将ServletContext传递给ServletContainerInitializer ->webApplicationInitializer
		 *
		 * 而 通过jar包直接启动的时候 没有ServletContext
		 *
		 */
		WebApplicationContext rootApplicationContext = createRootApplicationContext(servletContext);
		if (rootApplicationContext != null) {
			servletContext.addListener(new SpringBootContextLoaderListener(rootApplicationContext, servletContext));
		}
		else {
			this.logger.debug("No ContextLoaderListener registered, as createRootApplicationContext() did not "
					+ "return an application context");
		}
	}

	/**
	 * Deregisters the JDBC drivers that were registered by the application represented by
	 * the given {@code servletContext}. The default implementation
	 * {@link DriverManager#deregisterDriver(Driver) deregisters} every {@link Driver}
	 * that was loaded by the {@link ServletContext#getClassLoader web application's class
	 * loader}.
	 * @param servletContext the web application's servlet context
	 * @since 2.3.0
	 */
	protected void deregisterJdbcDrivers(ServletContext servletContext) {
		for (Driver driver : Collections.list(DriverManager.getDrivers())) {
			if (driver.getClass().getClassLoader() == servletContext.getClassLoader()) {
				try {
					DriverManager.deregisterDriver(driver);
				}
				catch (SQLException ex) {
					// Continue
				}
			}
		}
	}

	protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
		SpringApplicationBuilder builder = createSpringApplicationBuilder();
		builder.main(getClass());
		ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
		if (parent != null) {
			this.logger.info("Root context already created (using as parent).");
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
			builder.initializers(new ParentContextApplicationContextInitializer(parent));
		}
		/**
		 *
		 * 这里 给 将要创建的 ApplicationContext添加了一个  ServletContextApplicationContextInitializer（ApplicationContextInitializer）
		 * 在这个ServletContextApplicationContextInitializer 接口实现的方法中， 它将servletContext 设置给了 ApplicationContext
		 *
		 * 因此通过war模式部署 产生的ApplicationContext 中会持有  servletContext
		 *
		 *
		 */
		builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
		/**
		 * 最终创建出来的 context 就是  AnnotationConfigServletWebServerApplicationContext
		 */
		builder.contextFactory((webApplicationType) -> new AnnotationConfigServletWebServerApplicationContext());
		builder = configure(builder);
		builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
		SpringApplication application = builder.build();
		if (application.getAllSources().isEmpty()
				&& MergedAnnotations.from(getClass(), SearchStrategy.TYPE_HIERARCHY).isPresent(Configuration.class)) {
			application.addPrimarySources(Collections.singleton(getClass()));
		}
		Assert.state(!application.getAllSources().isEmpty(),
				"No SpringApplication sources have been defined. Either override the "
						+ "configure method or add an @Configuration annotation");
		// Ensure error pages are registered
		if (this.registerErrorPageFilter) {
			application.addPrimarySources(Collections.singleton(ErrorPageFilterConfiguration.class));
		}
		application.setRegisterShutdownHook(false);
		return run(application);
	}

	/**
	 * Returns the {@code SpringApplicationBuilder} that is used to configure and create
	 * the {@link SpringApplication}. The default implementation returns a new
	 * {@code SpringApplicationBuilder} in its default state.
	 * @return the {@code SpringApplicationBuilder}.
	 * @since 1.3.0
	 */
	protected SpringApplicationBuilder createSpringApplicationBuilder() {
		return new SpringApplicationBuilder();
	}

	/**
	 * Called to run a fully configured {@link SpringApplication}.
	 * @param application the application to run
	 * @return the {@link WebApplicationContext}
	 */
	protected WebApplicationContext run(SpringApplication application) {
		return (WebApplicationContext) application.run();
	}

	private ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
		Object context = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (context instanceof ApplicationContext) {
			return (ApplicationContext) context;
		}
		return null;
	}

	/**
	 * Configure the application. Normally all you would need to do is to add sources
	 * (e.g. config classes) because other settings have sensible defaults. You might
	 * choose (for instance) to add default command line arguments, or set an active
	 * Spring profile.
	 * @param builder a builder for the application context
	 * @return the application builder
	 * @see SpringApplicationBuilder
	 */
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder;
	}

	/**
	 * {@link ApplicationListener} to trigger
	 * {@link ConfigurableWebEnvironment#initPropertySources(ServletContext, javax.servlet.ServletConfig)}.
	 */
	private static final class WebEnvironmentPropertySourceInitializer
			implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

		private final ServletContext servletContext;

		private WebEnvironmentPropertySourceInitializer(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
			ConfigurableEnvironment environment = event.getEnvironment();
			if (environment instanceof ConfigurableWebEnvironment) {
				((ConfigurableWebEnvironment) environment).initPropertySources(this.servletContext, null);
			}
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

	/**
	 * {@link ContextLoaderListener} for the initialized context.
	 */
	private class SpringBootContextLoaderListener extends ContextLoaderListener {

		private final ServletContext servletContext;

		SpringBootContextLoaderListener(WebApplicationContext applicationContext, ServletContext servletContext) {
			super(applicationContext);
			this.servletContext = servletContext;
		}

		@Override
		public void contextInitialized(ServletContextEvent event) {
			// no-op because the application context is already initialized
		}

		@Override
		public void contextDestroyed(ServletContextEvent event) {
			try {
				super.contextDestroyed(event);
			}
			finally {
				// Use original context so that the classloader can be accessed
				deregisterJdbcDrivers(this.servletContext);
			}
		}

	}

}
