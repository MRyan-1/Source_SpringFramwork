package com.mryan.pojo;

import java.io.Serializable;

/**
 * @description： user
 * @Author MRyan
 * @Date 2021/9/11 16:31
 * @Version 1.0
 */
public class User {

	private String userName;

	private int age;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getAge() {
		return age;
	}

	@Override
	public String toString() {
		return "User{" +
				"userName='" + userName + '\'' +
				", age=" + age +
				'}';
	}

	public void setAge(int age) {
		this.age = age;
	}
}
