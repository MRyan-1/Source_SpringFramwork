package com.mryan.bean;

import com.mryan.pojo.User;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @description： UserFactoryBean
 * @Author MRyan
 * @Date 2021/9/11 16:30
 * @Version 1.0
 */
public class UserFactoryBean implements FactoryBean<User>,
		BeanNameAware, BeanFactoryAware, ApplicationContextAware , InitializingBean {

	private String userInfo;

	public void setUserInfo(String userInfo) {
		this.userInfo = userInfo;
	}

	@Override
	public User getObject() throws Exception {
		User user = new User();
		String[] split = userInfo.split(",");
		user.setUserName(split[0]);
		user.setAge(Integer.parseInt(split[1]));
		return user;
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("3. 注册bean时定义的id：" + name);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("4. 管理我的BeanFactory是：" + beanFactory);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		System.out.println("5. 高级容器接口ApplicationContext为：" + applicationContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("7. afterPropertiesSet...");
	}

	public void initMethod(){
		System.out.println("8. initMethod...");
	}
}
