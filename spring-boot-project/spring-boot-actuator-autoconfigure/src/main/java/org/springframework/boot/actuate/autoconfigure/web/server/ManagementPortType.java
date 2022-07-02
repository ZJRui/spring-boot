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

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.springframework.core.env.Environment;

/**
 * Port types that can be used to control how the management server is started.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public enum ManagementPortType {

	/**
	 * The management port has been disabled.
	 *
	 * @ConditionalOnManagementPort
	 * 仅当 management.server.port 条件与 server.port 具体某种关系时加载bean。
	 *
	 * 有三种关系类型：
	 *
	 * ManagementPortType.DISABLED：禁用或没定义管理端口
	 * ManagementPortType.SAME：服务端口与管理端口相同
	 * ManagementPortType.DIFFERENT：服务与管理端口不同
	 *
	 */
	DISABLED,

	/**
	 * The management port is the same as the server port.
	 */
	SAME,

	/**
	 * The management port and server port are different.
	 *
	 *  springboot actuator用于springboot项目健康监控， 默认端口和应用程序相同。 这时他们使用同一个应用程序上下文以及tomcat容器。
	 *  当managment.server.port 端口和应用程序不同时，actuator的应用程序时系统的子上下文，使用独立的tomcat容器
	 *
	 */
	DIFFERENT;

	/**
	 * Look at the given environment to determine if the {@link ManagementPortType} is
	 * {@link #DISABLED}, {@link #SAME} or {@link #DIFFERENT}.
	 * @param environment the Spring environment
	 * @return {@link #DISABLED} if {@code management.server.port} is set to a negative
	 * value, {@link #SAME} if {@code management.server.port} is not specified or equal to
	 * {@code server.port} and {@link #DIFFERENT} otherwise.
	 * @since 2.1.4
	 */
	public static ManagementPortType get(Environment environment) {
		Integer managementPort = getPortProperty(environment, "management.server.");
		/**
		 * 将management 端口设置为-1
		 * management.server.port=-1
		 */
		if (managementPort != null && managementPort < 0) {
			return DISABLED;
		}
		Integer serverPort = getPortProperty(environment, "server.");
		return ((managementPort == null || (serverPort == null && managementPort.equals(8080))
				|| (managementPort != 0 && managementPort.equals(serverPort))) ? SAME : DIFFERENT);
	}

	private static Integer getPortProperty(Environment environment, String prefix) {
		return environment.getProperty(prefix + "port", Integer.class);
	}

}
