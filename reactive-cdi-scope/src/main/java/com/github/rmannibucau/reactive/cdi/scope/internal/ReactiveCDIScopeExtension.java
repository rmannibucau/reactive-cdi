package com.github.rmannibucau.reactive.cdi.scope.internal;

import com.github.rmannibucau.reactive.cdi.scope.api.ReactiveScoped;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class ReactiveCDIScopeExtension implements Extension {
    private final ReactiveContext context = new ReactiveContext();

    public void addRouteScope(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addScope(ReactiveScoped.class, true, false);
    }

    public void addRouteContext(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addContext(context);
        afterBeanDiscovery.addBean() // let the context be injectable
                .id(getClass().getName() + "#" + context.getClass().getName())
                .scope(Dependent.class)
                .types(ReactiveContext.class, Object.class)
                .qualifiers(Default.Literal.INSTANCE, Any.Literal.INSTANCE)
                .createWith(c -> context);
    }
}
