package com.github.rmannibucau.reactive.cdi.scope.api;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;
import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MeecrowaveConfig(scanningPackageIncludes = "com.github.rmannibucau.reactive.cdi.scope.api.ReactiveScopedTest")
class ReactiveScopedTest {
    @Inject
    private ReactiveContext context;

    @Inject
    private BeanManager beanManager;

    @Inject
    private ReactiveContextBean bean;

    @Test
    void control() {
        assertFalse(isActive());
        final var previous = context.init();
        assertTrue(isActive());
        assertEquals(1, bean.getConstructed());
        assertEquals(0, bean.getDestroyed());
        final var self = bean.self();
        context.clean(previous);
        context.reset(previous);
        assertEquals(1, self.getDestroyed());
        assertFalse(isActive());
    }

    @Test
    void propagate() {
        assertFalse(isActive());
        final var previous = context.init();
        assertTrue(isActive());
        assertEquals(1, bean.getConstructed());
        assertEquals(0, bean.getDestroyed());
        bean.setContext("prop");

        final var ctx = context.current();
        final var latch = new CountDownLatch(1);
        new Thread(ctx.wrap(() -> {
            try {
                latch.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertEquals(1, bean.getConstructed());
            assertEquals(0, bean.getDestroyed());
            assertEquals("prop", bean.getContext());
        }), getClass().getName() + "-propagate").start();

        final ReactiveContextBean self = bean.self();
        context.reset(previous);
        latch.countDown();

        assertEquals(0, self.getDestroyed());
        assertFalse(isActive());
        context.clean(previous); // finally destroy beans
        assertEquals(1, self.getDestroyed());
    }

    private boolean isActive() {
        try {
            return beanManager.getContext(ReactiveScoped.class).isActive();
        } catch (final ContextNotActiveException cnae) {
            return false;
        }
    }

    @ReactiveScoped
    public static class ReactiveContextBean {
        private int constructed;
        private int destroyed;
        private String context;

        @PostConstruct
        private void init() {
            constructed++;
        }

        @PreDestroy
        private void destroy() {
            destroyed++;
        }

        public String getContext() {
            return context;
        }

        public void setContext(final String context) {
            this.context = context;
        }

        public ReactiveContextBean self() {
            return this;
        }

        public int getConstructed() {
            return constructed;
        }

        public int getDestroyed() {
            return destroyed;
        }
    }
}
