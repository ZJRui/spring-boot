package org.springframework.boot.loader.sachin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.jar.JarFile;

import java.net.URL;


/**
 * @Author Sachin
 * @Date 2022/4/10
 **/
public class ZjrLaunchedURLClassLoaderTest {


	@Test
	public void test() throws Exception{
		//注册org.springframework.boot.loader.jar.Handler URL协议处理器
		JarFile.registerUrlProtocolHandler();

		//构造LaunchedURLClassloader类加载器，这里使用了2个url
		//分别对应jar包中依赖包spring-boot-loader和spring-boot，使用 "!/" 分开，需要org.springframework.boot.loader.jar.Handler处理器处理
		LaunchedURLClassLoader classLoader = new LaunchedURLClassLoader(new URL[]{
				new URL("jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-loader-1.3.5.RELEASE.jar!/"),
				new URL("jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-1.3.5.RELEASE.jar!/")

		}, LaunchedURLClassLoader.class.getClassLoader());

		//加载类 这2个类都会在第二步本地查找中被找出(URLClassLoader的findClass方法)
		classLoader.loadClass("org.springframework.boot.loader.JarLauncher");
		classLoader.loadClass("org.springframework.boot.SpringApplication");

		// 在第三步使用默认的加载顺序在ApplicationClassLoader中被找出
		classLoader.loadClass("org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration");

	}
}
