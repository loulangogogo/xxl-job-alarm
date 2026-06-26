package io.github.loulangogogo.xxl.job.admin.alarm.config;

import com.xxl.job.admin.business.scheduler.alarm.impl.EmailJobAlarm;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

/*********************************************************
 ** 排除掉写不想让注入的bean对象
 ** 
 ** @author loulan
 ** @since 17
 *********************************************************/
@Component
public class RemoveBeanProcessor implements BeanDefinitionRegistryPostProcessor {

    /**
     * 处理 Bean 定义注册表
     * <p>
     * 在 Spring 容器启动时调用，用于移除不需要注入的 Bean
     * </p>
     *
     * @param registry Bean 定义注册表
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        removeBean(registry, EmailJobAlarm.class);
    }

    /**
     * 从注册表中移除指定类型的 Bean
     *
     * @param registry Bean 定义注册表
     * @param clazz    需要移除的 Bean 类型
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