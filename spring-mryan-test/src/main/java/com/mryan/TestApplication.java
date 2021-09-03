package com.mryan;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author MRyan
 * @version 1.0.0
 * @date 2021/08/30 23:54
 */
public class TestApplication {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		String name = applicationContext.getApplicationName();
		System.out.println("name = " + name);
	}
}
