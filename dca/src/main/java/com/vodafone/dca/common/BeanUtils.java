package com.vodafone.dca.common;

import java.util.UUID;

import org.springframework.context.support.GenericApplicationContext;

public abstract class BeanUtils {
	
	@SuppressWarnings("unchecked")
	public static <T> T register(GenericApplicationContext applicationContext, String name, T bean, Class<T> clazz) {
		String beanName = beanName(name, bean);
		applicationContext.registerBean(beanName, clazz, () -> bean);
		return (T)applicationContext.getBean(beanName);
	}
	
	public static <T> String beanName(String name, T bean) {
		return name + "-" + bean.getClass().getSimpleName() + "-" + UUID.randomUUID().toString();
	}
	
	public static <T> String beanName(String name, Class<T> clazz) {
		return name + "-" + clazz.getSimpleName() + "-" + UUID.randomUUID().toString();
	}

}
