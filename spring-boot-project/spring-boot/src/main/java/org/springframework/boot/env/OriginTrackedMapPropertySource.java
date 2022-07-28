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

package org.springframework.boot.env;

import java.util.Map;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MapPropertySource;

/**
 * {@link OriginLookup} backed by a {@link Map} containing {@link OriginTrackedValue
 * OriginTrackedValues}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see OriginTrackedValue
 */
public final class OriginTrackedMapPropertySource extends MapPropertySource implements OriginLookup<String> {

	private final boolean immutable;

	/**
	 * Create a new {@link OriginTrackedMapPropertySource} instance.
	 * @param name the property source name
	 * @param source the underlying map source
	 */
	@SuppressWarnings("rawtypes")
	public OriginTrackedMapPropertySource(String name, Map source) {
		/**
		 *1.name：Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'
		 *
		 * 2.什么时候 创建  application.properties 对应的PropertySource?
		 *
		 *
		 * <init>:58, OriginTrackedMapPropertySource (org.springframework.boot.env)
		 * load:57, PropertiesPropertySourceLoader (org.springframework.boot.env)
		 * load:54, StandardConfigDataLoader (org.springframework.boot.context.config)
		 * load:36, StandardConfigDataLoader (org.springframework.boot.context.config)
		 * load:107, ConfigDataLoaders (org.springframework.boot.context.config)
		 * load:128, ConfigDataImporter (org.springframework.boot.context.config)
		 * resolveAndLoad:86, ConfigDataImporter (org.springframework.boot.context.config)
		 * withProcessedImports:116, ConfigDataEnvironmentContributors (org.springframework.boot.context.config)
		 * processInitial:240, ConfigDataEnvironment (org.springframework.boot.context.config)
		 * processAndApply:227, ConfigDataEnvironment (org.springframework.boot.context.config)
		 * postProcessEnvironment:102, ConfigDataEnvironmentPostProcessor (org.springframework.boot.context.config)
		 * postProcessEnvironment:94, ConfigDataEnvironmentPostProcessor (org.springframework.boot.context.config)
		 * onApplicationEnvironmentPreparedEvent:102, EnvironmentPostProcessorApplicationListener (org.springframework.boot.env)
		 * onApplicationEvent:87, EnvironmentPostProcessorApplicationListener (org.springframework.boot.env)
		 * doInvokeListener:176, SimpleApplicationEventMulticaster (org.springframework.context.event)
		 * invokeListener:169, SimpleApplicationEventMulticaster (org.springframework.context.event)
		 * multicastEvent:143, SimpleApplicationEventMulticaster (org.springframework.context.event)
		 * multicastEvent:131, SimpleApplicationEventMulticaster (org.springframework.context.event)
		 * environmentPrepared:85, EventPublishingRunListener (org.springframework.boot.context.event)
		 * lambda$environmentPrepared$2:66, SpringApplicationRunListeners (org.springframework.boot)
		 * accept:-1, 1818747191 (org.springframework.boot.SpringApplicationRunListeners$$Lambda$391)
		 * forEach:1259, ArrayList (java.util)
		 * doWithListeners:120, SpringApplicationRunListeners (org.springframework.boot)
		 * doWithListeners:114, SpringApplicationRunListeners (org.springframework.boot)
		 * environmentPrepared:65, SpringApplicationRunListeners (org.springframework.boot)
		 * prepareEnvironment:344, SpringApplication (org.springframework.boot)
		 * run:302, SpringApplication (org.springframework.boot)
		 * loadContext:132, SpringBootContextLoader (org.springframework.boot.test.context)
		 * loadContextInternal:99, DefaultCacheAwareContextLoaderDelegate (org.springframework.test.context.cache)
		 * loadContext:124, DefaultCacheAwareContextLoaderDelegate (org.springframework.test.context.cache)
		 * getApplicationContext:124, DefaultTestContext (org.springframework.test.context.support)
		 * setUpRequestContextIfNecessary:190, ServletTestExecutionListener (org.springframework.test.context.web)
		 * prepareTestInstance:132, ServletTestExecutionListener (org.springframework.test.context.web)
		 * prepareTestInstance:248, TestContextManager (org.springframework.test.context)
		 * postProcessTestInstance:138, SpringExtension (org.springframework.test.context.junit.jupiter)
		 * lambda$invokeTestInstancePostProcessors$8:363, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * execute:-1, 231725600 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$378)
		 * executeAndMaskThrowable:368, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * lambda$invokeTestInstancePostProcessors$9:363, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * accept:-1, 1491860739 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$377)
		 * accept:-1, 2089016471 (java.util.stream.StreamSpliterators$WrappingSpliterator$$Lambda$259)
		 * accept:193, ReferencePipeline$3$1 (java.util.stream)
		 * accept:175, ReferencePipeline$2$1 (java.util.stream)
		 * forEachRemaining:1384, ArrayList$ArrayListSpliterator (java.util)
		 * copyInto:482, AbstractPipeline (java.util.stream)
		 * wrapAndCopyInto:472, AbstractPipeline (java.util.stream)
		 * forEachRemaining:312, StreamSpliterators$WrappingSpliterator (java.util.stream)
		 * forEachRemaining:743, Streams$ConcatSpliterator (java.util.stream)
		 * forEachRemaining:742, Streams$ConcatSpliterator (java.util.stream)
		 * forEach:580, ReferencePipeline$Head (java.util.stream)
		 * invokeTestInstancePostProcessors:362, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * lambda$instantiateAndPostProcessTestInstance$6:283, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * execute:-1, 276869158 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$376)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * instantiateAndPostProcessTestInstance:282, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * lambda$testInstancesProvider$4:272, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * get:-1, 1625090026 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$371)
		 * orElseGet:267, Optional (java.util)
		 * lambda$testInstancesProvider$5:271, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * getTestInstances:-1, 201576232 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$277)
		 * getTestInstances:31, TestInstancesProvider (org.junit.jupiter.engine.execution)
		 * lambda$prepare$0:102, TestMethodTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * execute:-1, 48305285 (org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor$$Lambda$370)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * prepare:101, TestMethodTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * prepare:66, TestMethodTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * lambda$prepare$2:123, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 1052195003 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$219)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * prepare:123, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:90, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * accept:-1, 537066525 (org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService$$Lambda$234)
		 * forEach:1259, ArrayList (java.util)
		 * invokeAll:41, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$6:155, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 951050903 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$230)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$8:141, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * invoke:-1, 1437654187 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$229)
		 * around:137, Node (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$9:139, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 1101184763 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$228)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * executeRecursively:138, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:95, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * accept:-1, 537066525 (org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService$$Lambda$234)
		 * forEach:1259, ArrayList (java.util)
		 * invokeAll:41, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$6:155, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 951050903 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$230)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$8:141, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * invoke:-1, 1437654187 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$229)
		 * around:137, Node (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$9:139, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 1101184763 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$228)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * executeRecursively:138, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:95, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * submit:35, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
		 * execute:57, HierarchicalTestExecutor (org.junit.platform.engine.support.hierarchical)
		 * execute:54, HierarchicalTestEngine (org.junit.platform.engine.support.hierarchical)
		 * execute:107, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * execute:88, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * lambda$execute$0:54, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * accept:-1, 1364913072 (org.junit.platform.launcher.core.EngineExecutionOrchestrator$$Lambda$172)
		 * withInterceptedStreams:67, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * execute:52, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * execute:114, DefaultLauncher (org.junit.platform.launcher.core)
		 * execute:86, DefaultLauncher (org.junit.platform.launcher.core)
		 * execute:86, DefaultLauncherSession$DelegatingLauncher (org.junit.platform.launcher.core)
		 * execute:53, SessionPerRequestLauncher (org.junit.platform.launcher.core)
		 * startRunnerWithArgs:71, JUnit5IdeaTestRunner (com.intellij.junit5)
		 * execute:38, IdeaTestRunner$Repeater$1 (com.intellij.rt.junit)
		 * repeat:11, TestsRepeater (com.intellij.rt.execution.junit)
		 * startRunnerWithArgs:35, IdeaTestRunner$Repeater (com.intellij.rt.junit)
		 * prepareStreamsAndStart:235, JUnitStarter (com.intellij.rt.junit)
		 * main:54, JUnitStarter (com.intellij.rt.junit)
		 *
		 *
		 *
		 */
		this(name, source, false);
	}

	/**
	 * Create a new {@link OriginTrackedMapPropertySource} instance.
	 * @param name the property source name
	 * @param source the underlying map source
	 * @param immutable if the underlying source is immutable and guaranteed not to change
	 * @since 2.2.0
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public OriginTrackedMapPropertySource(String name, Map source, boolean immutable) {
		super(name, source);
		this.immutable = immutable;
	}

	@Override
	public Object getProperty(String name) {
		Object value = super.getProperty(name);
		if (value instanceof OriginTrackedValue) {
			return ((OriginTrackedValue) value).getValue();
		}
		return value;
	}

	@Override
	public Origin getOrigin(String name) {
		Object value = super.getProperty(name);
		if (value instanceof OriginTrackedValue) {
			return ((OriginTrackedValue) value).getOrigin();
		}
		return null;
	}

	@Override
	public boolean isImmutable() {
		return this.immutable;
	}

}
