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

package org.springframework.boot.docs.features.testing.utilities.testresttemplate;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("enable-allow-bean-definition-overriding")
@RunWith(SpringRunner.class)
@SuppressWarnings("all")
class MySpringBootTests {


	/**
	 * 问题： 这个地方为什么可以直接注入 TestRestTemplate
	 *
	 * createContextCustomizer:38, TestRestTemplateContextCustomizerFactory (org.springframework.boot.test.web.client)
	 * getContextCustomizers:401, AbstractTestContextBootstrapper (org.springframework.test.context.support)------> 获取contextCustomizers
	 * buildMergedContextConfiguration:373, AbstractTestContextBootstrapper (org.springframework.test.context.support)
	 * buildDefaultMergedContextConfiguration:309, AbstractTestContextBootstrapper (org.springframework.test.context.support)
	 * buildMergedContextConfiguration:262, AbstractTestContextBootstrapper (org.springframework.test.context.support)
	 * buildTestContext:107, AbstractTestContextBootstrapper (org.springframework.test.context.support)
	 * buildTestContext:153, SpringBootTestContextBootstrapper (org.springframework.boot.test.context)--------——>构建context
	 * <init>:137, TestContextManager (org.springframework.test.context)
	 * <init>:122, TestContextManager (org.springframework.test.context)
	 * apply:-1, 116734858 (org.springframework.test.context.junit.jupiter.SpringExtension$$Lambda$261)
	 * lambda$getOrComputeIfAbsent$4:86, ExtensionValuesStore (org.junit.jupiter.engine.execution)
	 * get:-1, 93740343 (org.junit.jupiter.engine.execution.ExtensionValuesStore$$Lambda$262)
	 * computeValue:223, ExtensionValuesStore$MemoizingSupplier (org.junit.jupiter.engine.execution)
	 * get:211, ExtensionValuesStore$MemoizingSupplier (org.junit.jupiter.engine.execution)
	 * evaluate:191, ExtensionValuesStore$StoredValue (org.junit.jupiter.engine.execution)
	 * access$100:171, ExtensionValuesStore$StoredValue (org.junit.jupiter.engine.execution)
	 * getOrComputeIfAbsent:89, ExtensionValuesStore (org.junit.jupiter.engine.execution)
	 * getOrComputeIfAbsent:93, ExtensionValuesStore (org.junit.jupiter.engine.execution)
	 * getOrComputeIfAbsent:61, NamespaceAwareStore (org.junit.jupiter.engine.execution)
	 * getTestContextManager:294, SpringExtension (org.springframework.test.context.junit.jupiter)
	 * beforeAll:113, SpringExtension (org.springframework.test.context.junit.jupiter)
	 * lambda$invokeBeforeAllCallbacks$8:368, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
	 * execute:-1, 1171802656 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$256)
	 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
	 * invokeBeforeAllCallbacks:368, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
	 * before:192, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
	 * before:78, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
	 * lambda$executeRecursively$5:136, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * execute:-1, 1961501712 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$207)
	 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
	 * lambda$executeRecursively$7:129, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * invoke:-1, 634297796 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$206)
	 * around:137, Node (org.junit.platform.engine.support.hierarchical)
	 * lambda$executeRecursively$8:127, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * execute:-1, 754177595 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$205)
	 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
	 * executeRecursively:126, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * execute:84, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * accept:-1, 65586123 (org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService$$Lambda$211)
	 * forEach:1259, ArrayList (java.util)
	 * invokeAll:38, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
	 * lambda$executeRecursively$5:143, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * execute:-1, 1961501712 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$207)
	 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
	 * lambda$executeRecursively$7:129, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * invoke:-1, 634297796 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$206)
	 * around:137, Node (org.junit.platform.engine.support.hierarchical)
	 * lambda$executeRecursively$8:127, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * execute:-1, 754177595 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$205)
	 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
	 * executeRecursively:126, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * execute:84, NodeTestTask (org.junit.platform.engine.support.hierarchical)
	 * submit:32, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
	 * execute:57, HierarchicalTestExecutor (org.junit.platform.engine.support.hierarchical)
	 * execute:51, HierarchicalTestEngine (org.junit.platform.engine.support.hierarchical)
	 * execute:108, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
	 * execute:88, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
	 * lambda$execute$0:54, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
	 * accept:-1, 298430307 (org.junit.platform.launcher.core.EngineExecutionOrchestrator$$Lambda$156)
	 * withInterceptedStreams:67, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
	 * execute:52, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
	 * execute:96, DefaultLauncher (org.junit.platform.launcher.core)
	 * execute:75, DefaultLauncher (org.junit.platform.launcher.core)
	 * startRunnerWithArgs:71, JUnit5IdeaTestRunner (com.intellij.junit5)
	 * execute:38, IdeaTestRunner$Repeater$1 (com.intellij.rt.junit)
	 * repeat:11, TestsRepeater (com.intellij.rt.execution.junit)
	 * startRunnerWithArgs:35, IdeaTestRunner$Repeater (com.intellij.rt.junit)
	 * prepareStreamsAndStart:235, JUnitStarter (com.intellij.rt.junit)
	 * main:54, JUnitStarter (com.intellij.rt.junit)
	 *
	 *
	 *
	 * getContextCustomizers方法的主要作用 就是通过springfactoriesloader 读取spring.factories文件中的 ContextCustomizerFactory 配置类，
	 * 然后调用每一个 ContextCustomizerFactoery的createContextCustomizer
	 *
	 * <code>
	 * # Spring Test ContextCustomizerFactories
	 * org.springframework.test.context.ContextCustomizerFactory=\
	 * org.springframework.boot.test.context.ImportsContextCustomizerFactory,\
	 * org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizerFactory,\
	 * org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory,\
	 * org.springframework.boot.test.mock.mockito.MockitoContextCustomizerFactory,\
	 * org.springframework.boot.test.web.client.TestRestTemplateContextCustomizerFactory,\
	 * org.springframework.boot.test.web.reactive.server.WebTestClientContextCustomizerFactory
	 * </code>
	 * 在这个配置类中有一个  TestRestTemplateContextCustomizerFactory，的createContextCustomizer方法 会创建一个TestRestTemplateContextCustomizer对象
	 * TestRestTemplateContextCustomizer对象内部的customizeContext 方法 判断如果 当前是web环境 则注册 registerTestRestTemplate(context);
	 *
	 * 因此我们的测试类中 可以直接 @Autowired TestResetTemplate.
	 *
	 * 需要注意的是  创建一个TestRestTemplateContextCustomizer对象, 这个对象本质上是ContextCustomizer 对象， 最终 ContextCustomizer 对象
	 * 又被ContextCustomizerAdapter 包装， 而 ContextCustomizerAdapter 本质上是实现了 ApplicationContextInitializer 接口。
	 * 因此最终 ContextCustomizer的执行是通过 ApplicationContextInitializer接口的调度。
	 * <code>
	 *     //对每一个ContextCustomizer 转为ContextCustomizerAdapter 本质上是ApplicationContextInitializer
	 *     	for (ContextCustomizer contextCustomizer : config.getContextCustomizers()) {
	 * 			initializers.add(new ContextCustomizerAdapter(contextCustomizer, config));
	 *       }
	 * </code>
	 *
	 *
	 *
	 */
	@Autowired
	private TestRestTemplate template;

	@Test
	void testRequest() {
		HttpHeaders headers = this.template.getForEntity("/example", String.class).getHeaders();
		assertThat(headers.getLocation()).hasHost("other.example.com");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class RestTemplateBuilderConfiguration {

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(1))
					.setReadTimeout(Duration.ofSeconds(1)).rootUri("abc");
		}

	}

}
