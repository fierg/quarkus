package org.jboss.resteasy.reactive.common.deployment;

import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.resteasy.reactive.common.runtime.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.rest.common.QuarkusRestCommonRecorder;

public class FactoryUtils {
    public static <T> BeanFactory<T> factory(ClassInfo providerClass, Set<String> singletons,
            QuarkusRestCommonRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem) {
        return factory(providerClass.name().toString(), singletons, recorder, beanContainerBuildItem);
    }

    public static <T> BeanFactory<T> factory(String providerClass, Set<String> singletons, QuarkusRestCommonRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem) {
        if (singletons.contains(providerClass)) {
            return new SingletonBeanFactory<>(providerClass);
        } else {
            return recorder.factory(providerClass,
                    beanContainerBuildItem.getValue());
        }
    }
}
