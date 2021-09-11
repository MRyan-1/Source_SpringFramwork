package com.mryan.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @description： MyBeanPostProcessor
 * @Author MRyan
 * @Date 2021/9/11 17:59
 * @Version 1.0
 */
public class MyBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("userFactoryBean".equalsIgnoreCase(beanName)) {
			System.out.println("6. MyBeanPostProcessor before方法拦截处理：userBeanFactory");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("userFactoryBean".equalsIgnoreCase(beanName)) {
			System.out.println("6. MyBeanPostProcessor after方法拦截处理：userBeanFactory");
		}
		return bean;
	}
}
