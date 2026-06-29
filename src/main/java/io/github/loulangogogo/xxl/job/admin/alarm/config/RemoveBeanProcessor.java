package io.github.loulangogogo.xxl.job.admin.alarm.config;

import com.xxl.job.admin.business.scheduler.alarm.impl.EmailJobAlarm;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

/*********************************************************
 ** Bean定义移除处理器
 ** <p>
 ** 解决自定义告警实现与XXL-Job原生【EmailJobAlarm】的注入冲突。
 ** 原生EmailJobAlarm不支持开关控制，本模块用NewEmailJobAlarm替代它，
 ** 但两者实现了同一接口，必须在容器启动时移除原生Bean，否则Spring会因多个同类型Bean报错。
 ** </p>
 **
 ** @author loulan
 ** @since 17
 *********************************************************/
@Component
public class RemoveBeanProcessor implements BeanDefinitionRegistryPostProcessor {

    /**
     * 处理Bean定义注册表，移除与自定义实现冲突的原生Bean
     *
     * <p>在Spring容器完成Bean定义注册后、实例化之前调用。
     * 此处只移除【EmailJobAlarm】，为NewEmailJobAlarm让出注入位置。
     *
     * @param registry Bean定义注册表，包含所有已注册的Bean定义信息
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        removeBean(registry, EmailJobAlarm.class);
    }

    /**
     * 从注册表中按类名精确移除Bean定义
     *
     * <p>使用类的全限定名而非Class对象进行匹配，因为在BeanDefinitionRegistry处理阶段，
     * 部分Bean的Class尚未被加载，直接比较Class对象会导致匹配失败。
     *
     * @param registry Bean定义注册表
     * @param clazz    需要移除的Bean对应的类
     */
    private void removeBean(BeanDefinitionRegistry registry, Class<?> clazz) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            if (clazz.getName().equals(className)) {
                registry.removeBeanDefinition(beanName);
            }
        }
    }
}