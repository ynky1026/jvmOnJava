package com.wangzhen.myspring.bean.factory.impl;

import com.wangzhen.myspring.bean.beandefinition.BeanDefinition;
import com.wangzhen.myspring.bean.beandefinition.BeanDefinitionRegistry;
import com.wangzhen.myspring.bean.factory.BeanFactory;
import com.wangzhen.myspring.bean.postprocessor.AopPostProcessor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: 默认Bean 工厂
 *              实现Closeable接口用于销毁对象
 * Datetime:    2020/11/1   8:16 下午
 * Author:   王震
 */
public class DefaultBeanFactory implements BeanFactory , BeanDefinitionRegistry , Closeable {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // 使用ConcurrentHashMap 应对 并发情况
    private Map<String, BeanDefinition> bdMap = new ConcurrentHashMap<>();

    private Map<String,Object> beanMap = new ConcurrentHashMap<>();

    // 记录正在创建的 bean
    private ThreadLocal<Set<String>> initialedBeans = new ThreadLocal<>();

    // 记录观察者
    private List<AopPostProcessor> aopPostProcessors = new ArrayList<>();

    @Override
    public Object getBean(String beanName) throws Exception {
        return doGetBean(beanName);

    }


    // 真正实例化bean的过程
    public Object doGetBean(String beanName) throws Exception {
        if(!bdMap.containsKey(beanName)){
            logger.info(beanName+"不存在");
            return null;
        }
        //得到当前线程正在创建的bean
        Set<String> initingBeans = this.initialedBeans.get();
        if(initingBeans==null){
            initingBeans = new HashSet<>();
            this.initialedBeans.set(initingBeans);
        }
        // 这样就能检测循环依赖嘛
        if(initingBeans.contains(beanName)){
            throw new Exception("检测到"+beanName+"存在循环依赖:"+initingBeans);
        }
        // 表明正在创建该bean
        initingBeans.add(beanName);


        // 如果该实例已经被创建过了 直接返回
        Object instance = beanMap.get(beanName);
        if(instance!=null){
            return instance;
        }
        //todo  不存在beanMap 定义 则进行创建
        if(!this.bdMap.containsKey(beanName)){
            logger.info("不存在名为：[" + beanName + "]的bean定义,即将进行创建");
        }

        BeanDefinition bd = bdMap.get(beanName);
        Class<?> beanClass = bd.getBeanClass();

        if(beanClass!=null){
            instance = createBeanByConstruct(bd);
            if(instance==null){
                instance = beanClass.newInstance();
            }
        }else if(instance == null && StringUtils.isNotBlank(bd.getStaticCreateBeanMethod())){
            // 通过工厂类创建bean
            instance = createBeanByFactoryMethod(bd);
        }
        // 初始化 instance
        doInit(bd,instance);
        //添加属性依赖
        this.parsePropertyValues(bd,instance);
        // 创建完成 移除记录
        initingBeans.remove(beanName);
        // 添加 aop 处理
        instance = applyAopBeanPostProcessor(instance,beanName);

        // 如果是单例的话 那么直接 存放到 map 中后直接获取
        if(instance!=null&&bd.isSingleton()){
            beanMap.put(beanName,instance);
        }
        return instance;
    }

    // 对该bean 进行 aop 织入
    private Object applyAopBeanPostProcessor(Object instance, String beanName) throws Exception {
        for (AopPostProcessor aopPostProcessor : aopPostProcessors) {
            instance = aopPostProcessor.postProcessWeaving(instance, beanName);
        }
        return instance;
    }

        // todo 解析 bean 的属性依赖
    private void parsePropertyValues(BeanDefinition bd, Object instance) throws Exception {

    }


    // 初始化对象
    private void doInit(BeanDefinition bd, Object instance) {
        Class<?> beanClass = instance.getClass();
        if(!StringUtils.isNoneBlank(bd.getBeanInitMethodName())){
            try {
                Method method = beanClass.getMethod(bd.getBeanInitMethodName(), null);
                method.invoke(instance,null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过工厂方法创建类
     */
    private Object createBeanByFactoryMethod(BeanDefinition bd) {
        Object o = null;
        try{
            // 根据工厂类名 获取工厂对象
            Object factory = doGetBean(bd.getBeanFactory());
            Method method = factory.getClass().getMethod(bd.getCreateBeanMethod());
            o = method.invoke(factory,null);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return o;
    }

    /**
     * todo 通过构造方法创建对象
     * 这里直接通过 class 创建出对象
     * @param bd
     * @return
     */
    private Object createBeanByConstruct(BeanDefinition bd) {
        Object o = null;
        try {
             o = bd.getBeanClass().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return o;
    }

    private Object createBeanByStaticFactoryMethod(BeanDefinition bd) {
        Object instance = null;
        Class<?> beanClass = bd.getBeanClass();
        try {
            Method method = beanClass.getMethod(bd.getStaticCreateBeanMethod(), null);
           // method.invoke(beanClass,null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return instance;
    }

    @Override
    public void registerBeanPostProcessor(AopPostProcessor processor) {
        this.aopPostProcessors.add(processor);
    }

    @Override
    public String[] getBeanNameForType(Class<?> tClass) {
        // todo
        return new String[0];
    }

    @Override
    public Map<String, Object> getBeansForType(Class<?> clazz) {

        //todo
        return null;
    }

    @Override
    public Class getType(String beanName) {
        BeanDefinition o = (BeanDefinition) beanMap.get(beanName);
        return o.getBeanClass();
    }

    @Override
    public void register(BeanDefinition bd, String beanName) {
        Assert.assertNotNull("bd 不能为空",bd);
        Assert.assertNotNull("beanName 不能为空",beanName);
        if(bdMap.containsKey(beanName)){
            logger.info("["+beanName+"]已经存在，将覆盖这个beanName");
        }

        if(!bd.validate()){
            logger.error("BeanDefinition不合法");
            return;
        }
        bdMap.put(beanName,bd);


    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return bdMap.containsKey(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        if(!bdMap.containsKey(beanName)){
            logger.info("[" + beanName + "]不存在");
            return null;
        }
        return bdMap.get(beanName);
    }

    @Override
    public void close() throws IOException {

    }
}
