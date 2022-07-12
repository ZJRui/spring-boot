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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

	/**
	 * The bean name that this post-processor is registered with.
	 *
	 * Spring 初始化Bean 的时候会调用 bean后置处理 ConfigurationPropertiesBindingPostProcessor
	 * 的postProcessBeforeInitialization方法
	 * 这里其实就是我们上面猜想的注解处理器，它会处理ConfigurationProperties注解
	 * 获取配置文件中的prefix，和注解对象的类成员变量
	 * ————————————————
	 *
	 *
	 *   可以看到该处理器实现了比较重要的 BeanProcessor、InitializingBean、ApplicationContextAware，
	 *   利用ApplicationContextAware给字段applicationContext赋值，利用InitializingBean给registry和binder赋值、利用BeanPostProcessor来处理属性对象属性值绑定。
	 *
	 *    整个绑定过程简单说就是ConfigurationPropertiesBindingPostProcessor将属性绑定委托给ConfigurationPropertiesBinder，
	 *    ConfigurationPropertiesBinder利用应用上下文（ApplicationContext）的环境（Environment）的（PropertySources->MutablePropertySources），
	 *    协调其它组件进行属性绑定，{Environment提供属性值集合，针对有@ConfigurationProperties的bean进行属性值绑定}，
	 *    当然详细来说还有ConfigurationPropertiesBinder是如何进行属性绑定的？其实ConfigurationPropertiesBinder就是个协调者，
	 *    真正进行属性绑定的也不是它，它创建BindHandler、Binder，让Binder根据不同的属性值类型（Value、bean）让
	 *    DataObjectBinder（ValueObjectBinder、JavaBeanBinder）去进行绑定，最终交给BindHandler来进行绑定，
	 *    其实看到ConfigurationPropertiesBinder的PropertySources字段的时候，也就大概知道绑定核心原理了，就是到环境中找属性资源给类赋值。
	 *
	 *     正常的pojo属性赋值spring提供了依赖注入的方式，@ConfigurationProperties标注的属性类spring提供了可配置方式。
	 *     那接下来可能还要看看application.yml这样的配置文件是如何映射成spring的Enviroment的？我们知道Environment是
	 *     profile和propertySource的抽象，这样当我们知道了环境也就知道了，当前**的哪个配置文件、哪些属性资源是可获得的。
	 *
	 *    springboot应用构建时会准备环境，准备环境时会用多播器发布一个事件ApplicationEnvironmentPreparedEvent，
	 *    boot的应用监听器ApplicationListener会监听应用事件，该事件的监听器ConfigFileApplicationListener监听
	 *    到该事件会让EnvironmentPostProcessor进行相应处理，EnvironmentPostProcessor.Loader去load，具体点就是PropertySourceLoader
	 *    到默认的DEFAULT_SEARCH_LOCATIONS（classpath:/,classpath:/config/,file:./,file:./config/ /,file:./config/）
	 *    和指定的CONFIG_LOCATION_PROPERTY位置去加载PropertySource到Environment中。
	 *
	 *
	 *思考 :我们知道springcloud的配置管理，核心点是客户端从服务端取得配置信息，服务端对配置信息进行持久化
	 * （以某种方式落磁盘如数据库等等），那么取得的配置客户端作何处理呢，不难猜测应该是要映射到环境中，
	 * 也就用到了MutablePropertySources，到底猜测的是否正确，可以后续分析一下。
	 *
	 *
	 *
	 *
	 *
	 */
	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

	private ApplicationContext applicationContext;

	private BeanDefinitionRegistry registry;

	private ConfigurationPropertiesBinder binder;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// We can't use constructor injection of the application context because
		// it causes eager factory bean initialization
		this.registry = (BeanDefinitionRegistry) this.applicationContext.getAutowireCapableBeanFactory();
		// 这里使用了 ConfigurationPropertiesBinder
		/**
		 * afterPropertiesSet方法在Bean创建时被调用，保证内部变量configurationPropertiesBinder被初始化，
		 * 这个binder类就是使prefix与propertyBean进行值绑定的关键工具类
		 *
		 */
		this.binder = ConfigurationPropertiesBinder.get(this.applicationContext);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		/**
		 *
		 * 这里使用了 ConfigurationPropertiesBean
		 *
		 * 注意：这里的binder方法最终 还是会调用 ConfigurationPropertiesBinder对象的bind方法。
		 * 但是他的bind方法接收的参数是ConfigurationPropertiesBean。 而这里的postProcessBeforeInitialization
		 * 方法接收的参数是 Bean对象。 这里的 get方法会创建一个 ConfigurationPropertiesBean对象。
		 *
		 */
		bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
		return bean;
	}

	private void bind(ConfigurationPropertiesBean bean) {
		if (bean == null || hasBoundValueObject(bean.getName())) {
			return;
		}
		Assert.state(bean.getBindMethod() == BindMethod.JAVA_BEAN, "Cannot bind @ConfigurationProperties for bean '"
				+ bean.getName() + "'. Ensure that @ConstructorBinding has not been applied to regular bean");
		try {
			/**
			 *
			 */
			this.binder.bind(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

	private boolean hasBoundValueObject(String beanName) {
		return this.registry.containsBeanDefinition(beanName) && this.registry
				.getBeanDefinition(beanName) instanceof ConfigurationPropertiesValueObjectBeanDefinition;
	}

	/**
	 * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
	 * already registered.
	 * @param registry the bean definition registry
	 * @since 2.2.0
	 */
	public static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		/**
		 * 注册 ConfigurationPropertiesBindingPostProcessor 后置处理器
		 */
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class,
							ConfigurationPropertiesBindingPostProcessor::new)
					.getBeanDefinition();
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		/**
		 *
		 * 1.注册一个ConfigurationPropertiesBinder.Factory
		 * 2.注册 一个ConfigurationPropertiesBinder
		 */
		ConfigurationPropertiesBinder.register(registry);
	}

}
