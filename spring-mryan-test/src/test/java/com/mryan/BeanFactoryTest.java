package com.mryan;

import com.mryan.bean.MyTestBean;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.spel.spr10210.infra.C;

/**
 * @description： BeanFactoryTest
 * @Author MRyan
 * @Date 2021/9/3 17:49
 */
@SuppressWarnings("deprecation")
public class BeanFactoryTest {

	@Test
	public void TEST_BEAN_FACTORY_SIMPLE_LOAD() {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("test.xml"));
		MyTestBean myTestBean = beanFactory.getBean("myTestBean", MyTestBean.class);
		System.out.println(myTestBean.getTestStr());
	}

	/**
	 * 使用DefaultListableBeanFactory容器
	 */
	@Test
	public void TEST_DEFAULT_LISTABLE_BEAN_FACTORY_LOAD() {
		//1. 创建IOC配置文件的抽象资源，这个抽象资源包括了BeanDefinition的定义信息
		ClassPathResource resource = new ClassPathResource("test.xml");
		//2.创建BeanFactory，这里使用DefaultListableBeanFactory
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		//3. 创建一个载入BeanDefinition的读取器，这里使用XMLBeanDefinitionReader来载入XML文件形式的BeanDefinition，通过一个回调配置给BeanFactory
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//4. 从定义好的资源位置读入配置信息，具体解析过程通过XMLBeanDefinitionReader来完成
		//完成整个载入和注册Bean定义之后，需要的IOC容器就建立起来了
		reader.loadBeanDefinitions(resource);
		MyTestBean myTestBean = factory.getBean("myTestBean", MyTestBean.class);
		System.out.println(myTestBean.getTestStr());
	}

	@Test
	public void TEST_APPLICATION_CONTEXT_SIMPLE_LOAD() {
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"test.xml"});
		MyTestBean myTestBean = (MyTestBean) context.getBean("myTestBean");
		System.out.println(myTestBean.getTestStr());
	}

}
