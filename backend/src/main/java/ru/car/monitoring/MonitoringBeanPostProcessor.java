package ru.car.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
//@Component
@RequiredArgsConstructor
public class MonitoringBeanPostProcessor implements BeanPostProcessor {

    private final MonitoringService monitoringService;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        Class<?> c = bean.getClass();

        Map<Method, Events[]> methods = Arrays.stream(c.getMethods())
                .filter(m -> Objects.nonNull(m.getAnnotation(Monitoring.class)))
                .collect(Collectors.toMap(Function.identity(), m -> m.getAnnotation(Monitoring.class).value()));

        if (methods.isEmpty()) {
            return bean;
        }

        log.info("Создаем прокси для бина {}", beanName);

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(c);
        enhancer.setCallback((InvocationHandler) (obj, method, args) -> invoke(obj, method, args, bean, beanName, methods));
        if (c.getConstructors().length == 0) {
            return enhancer.create();
        }
        Constructor constructor = c.getConstructors()[0];
        return enhancer.create(constructor.getParameterTypes(), new Object[constructor.getParameterTypes().length]);
    }

    public Object invoke(Object o, Method method, Object[] args, Object bean, String beanName, Map<Method, Events[]> methods) throws Throwable {
        if (!method.canAccess(bean)) {
            method.setAccessible(true);
        }
        if (!methods.containsKey(method)) {
            try {
                return method.invoke(bean, args);
            } catch (InvocationTargetException t) {
                throw t.getCause();
            }
        }

        try {
            return monitoringService.around(methods.get(method), () -> method.invoke(bean, args));
        } catch (InvocationTargetException t) {
            log.error("что-то пошло не так с MonitoringBeanPostProcessor у {}", beanName);
            throw t.getCause();
        }
    }

}

