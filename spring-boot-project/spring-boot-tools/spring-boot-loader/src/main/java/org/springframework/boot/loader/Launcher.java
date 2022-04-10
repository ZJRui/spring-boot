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

package org.springframework.boot.loader;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath backed by one or more {@link Archive}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class Launcher {

	private static final String JAR_MODE_LAUNCHER = "org.springframework.boot.loader.jarmode.JarModeLauncher";

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 *
	 * 启动应用程序。 此方法是应由子类 public static void main(String[] args) 方法调用的初始入口点。
	 * @param args the incoming arguments
	 * @throws Exception if the application fails to launch
	 */
	protected void launch(String[] args) throws Exception {
		/**
		 *注册协议，URLStreamHandler机制协议。
		 * 我们可以通过JVM启动参数-D java.protocol.handler.pkgs来设置URLStreamHandler实现类的包路径，
		 * 这里的代码也是通过这个系统参数将URLStreamHandler实现类的包路径设置为loader包下的。
		 * 在Java中会根据 包路径前缀 +协议名称+.Handler 寻找URLStreamHandler实现类。
		 *   String clsName = packagePrefix + "." + protocol +
		 *                           ".Handler";
		 * 在springboot中前缀是	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";
		 * 协议是jar。 因此最终定位到org.springframework.boot.loader.jar.Handler
		 *
		 */
		if (!isExploded()) {
			JarFile.registerUrlProtocolHandler();
		}
		/**
		 *创建LaunchedURLClassLoader
		 * 问题：这里需要思考下为何要拷贝本来应该放入到lib里面的spring-boot-loader.jar里面的class到结构，也就是
		 * spring-boot插件打包之后 为什么 需要提取一些spring-boot-loader的class文件放置到jar包的顶层结构中。而不是以spring-boot-loader.jar的形式
		 *，如流程图首先使用Appclassloader加载了JarLauncher类并创建了LaunchedURLClassLoader类，
		 * 而LaunchedURLClassLoader是属于spring-boot-loader.jar包里面的，而Appclassloader是普通的加载器不能加载嵌套的jar里面的文件，
		 * 所以如果把spring-boot-loader.jar放到lib 目录下，Appclassloader将找不到LaunchedURLClassLoader。所以在打包时候
		 * 拷贝本来应该放入到lib里面的spring-boot-loader.jar里面的class到结构（2）。
		 *
		 *     // getClassPathArchives方法在会去找lib目录下对应的第三方依赖JarFileArchive，同时也会项目自身的JarFileArchive
		 *     // 根据getClassPathArchives得到的JarFileArchive集合去创建类加载器ClassLoader。这里会构造一个LaunchedURLClassLoader类加载器，这个类加载器继承URLClassLoader，并使用这些JarFileArchive集合的URL构造成URLClassPath
		 *     // LaunchedURLClassLoader类加载器的父类加载器是当前执行类JarLauncher的类加载器
		 *
		 *
		 *    LaunchedURLClassLoader重写了loadClass方法，也就是说它修改了默认的类加载方式(先看该类是否已加载这部分不变，
		 *    后面真正去加载类的规则改变了，不再是直接从父类加载器中去加载)。LaunchedURLClassLoader定义了自己的类加载规则：
		 *    （1）如果根类加载器存在，则调用它的加载方法，这里根类加载器是ExtClassLoader
		 *    （2）调用LaunchedUrlClassLoader自身的findClass方法，也就是URLClassLoader的findClass方法
		 *    （3）调用父类的loadClass方法，也就是执行默认的类加载顺序（从BootstrapClassLoader开始从上往下寻找）
		 *
		 * =================================
		 * JarLauncher 继承自Launcher， Launcher的父类 ExecutableArchiveLauncher中有一个属性Archive
		 * private final Archive archive;
		 * 这个属性表示 jar文件。Archive的getNestedArchives方法 返回的目录就是类加载器加载的目录，
		 * 在Launcher的launch方法中，通过以上archive的getNestedArchives方法找到/BOOT-INF/lib下所有jar及/BOOT-INF/classes目录
		 * 所对应的archive，通过这些archives的url生成LaunchedURLClassLoader，并将其设置为线程上下文类加载器，启动应用
		 *
		 * ================
		 * 使用 IDEA 运行 springboot 程序与 java -jar 运行 springboot 程序时 ClassLoader 不同，导致 classloader.getResource() 拿不到资源
		 *
		 * 使用 this.getClass().getClassLoader() 获取 classloader 时，运行方式不同，结果不一样
		 *
		 * 使用 IDEA 运行 springboot 程序时，sun.misc.Launcher$AppClassLoader@18b4aac2
		 * 使用 java -jar 运行打包后的 jar 包时，org.springframework.boot.loader.LaunchedURLClassLoader@71dac704
		 * 使用下面的代码，可以拿到类的代码的路径：
		 * 如：jar:file:/data/spring-boot-theory.jar!/BOOT-INF/lib/spring-aop-5.0.4.RELEASE.jar!/org/springframework/aop/SpringProxy.class
		 *
		 * ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		 * CodeSource codeSource = protectionDomain.getCodeSource();
		 * URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
		 * 对于原始的JarFile URL，只支持一个'!/'，SpringBoot 扩展了此协议，使其支持多个'!/'，以实现 jar in jar 的加载资源方式。
		 *
		 * 但是，取到了资源路径，原生的 new File() 还是处理不了这种资源路径的。
		 *
		 * 我们通过传统的方式取不到资源了，即使取到了，也没法直接使用，那在 SpringBoot 中我们应该怎么获取资源呢？
		 * 可以参考 MyBatisPlus 中对 xml 文件的处理：com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties#mapperLocations  （有待研究）
		 *
		 *
		 */
		ClassLoader classLoader = createClassLoader(getClassPathArchivesIterator());
		String jarMode = System.getProperty("jarmode");
		String launchClass = (jarMode != null && !jarMode.isEmpty()) ? JAR_MODE_LAUNCHER : getMainClass();
		launch(args, launchClass, classLoader);
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of
	 * {@link #createClassLoader(Iterator)}
	 */
	@Deprecated
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		return createClassLoader(archives.iterator());
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 * @since 2.3.0
	 */
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<>(50);
		while (archives.hasNext()) {
			urls.add(archives.next().getUrl());
		}
		return createClassLoader(urls.toArray(new URL[0]));
	}

	/**
	 * Create a classloader for the specified URLs.
	 * @param urls the URLs
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(isExploded(), getArchive(), urls, getClass().getClassLoader());
	}

	/**
	 * Launch the application given the archive file and a fully configured classloader.
	 * @param args the incoming arguments
	 * @param launchClass the launch class to run
	 * @param classLoader the classloader
	 * @throws Exception if the launch fails
	 */
	protected void launch(String[] args, String launchClass, ClassLoader classLoader) throws Exception {
		//将LaunchedURLClassLoader设置到当前线程上下文类加载你中
		Thread.currentThread().setContextClassLoader(classLoader);
		createMainMethodRunner(launchClass, args, classLoader).run();
	}

	/**
	 * Create the {@code MainMethodRunner} used to launch the application.
	 * @param mainClass the main class
	 * @param args the incoming arguments
	 * @param classLoader the classloader
	 * @return the main method runner
	 */
	protected MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
		return new MainMethodRunner(mainClass, args);
	}

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 * @since 2.3.0
	 */
	protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
		return getClassPathArchives().iterator();
	}

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of implementing
	 * {@link #getClassPathArchivesIterator()}.
	 */
	@Deprecated
	protected List<Archive> getClassPathArchives() throws Exception {
		throw new IllegalStateException("Unexpected call to getClassPathArchives()");
	}

	protected final Archive createArchive() throws Exception {
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
		String path = (location != null) ? location.getSchemeSpecificPart() : null;
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException("Unable to determine code source archive from " + root);
		}
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

	/**
	 * Returns if the launcher is running in an exploded mode. If this method returns
	 * {@code true} then only regular JARs are supported and the additional URL and
	 * ClassLoader support infrastructure can be optimized.
	 * 如果启动器以爆炸模式运行，则返回。 如果此方法返回 true，则仅支持常规 JAR，
	 * 并且可以优化额外的 URL 和 ClassLoader 支持基础结构。
	 * @return if the jar is exploded.
	 * @since 2.3.0
	 */
	protected boolean isExploded() {
		return false;
	}

	/**
	 * Return the root archive.
	 * @return the root archive
	 * @since 2.3.1
	 */
	protected Archive getArchive() {
		return null;
	}

}
