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

package org.springframework.boot.actuate.autoconfigure.health;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 *
 *
 *
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
@EnableConfigurationProperties(HealthEndpointProperties.class)
@Import({ HealthEndpointConfiguration.class, ReactiveHealthEndpointConfiguration.class,
		HealthEndpointWebExtensionConfiguration.class, HealthEndpointReactiveWebExtensionConfiguration.class })
public class HealthEndpointAutoConfiguration {

	/**
	 * 过程，spring-boot-starter-actuator是官方提供的starter，包含spring-boot-actuator-autoconfigure和actuator本身逻辑两部分，
	 * 其中autoconfigure中spring.factories中配置了诸多自动配置类，这些类在springboot启动时，都会因为starter机制，而被实例化，
	 * 这里也正是actuator被启动的开端，其中就包括HealthEndpointAutoConfiguration，
	 * 该类在被实例化前会因@Import注解先实例化HealthEndpointConfiguration，
	 * 接着即创建HealthEndpoint，并将该实例加入spring容器，源码如下：
	 */
}
