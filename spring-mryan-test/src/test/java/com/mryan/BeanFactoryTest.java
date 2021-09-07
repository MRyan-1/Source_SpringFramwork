package com.mryan;

import com.mryan.bean.MyTestBean;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

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

	@Test
	public void TEST_APPLICATION_CONTEXT_SIMPLE_LOAD() {
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"test.xml"});
		MyTestBean myTestBean = (MyTestBean) context.getBean("myTestBean");
		System.out.println(myTestBean.getTestStr());
	}

}
