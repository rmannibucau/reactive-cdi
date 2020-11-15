package com.github.rmannibucau.reactive.cdi.scope.internal;

import com.github.rmannibucau.reactive.cdi.scope.api.ReactiveScoped;
import com.github.rmannibucau.reactive.cdi.scope.internal.completion.ReactiveCompletionFuture;
import com.github.rmannibucau.reactive.cdi.scope.internal.executor.ReactiveExecutor;
import com.github.rmannibucau.reactive.cdi.scope.internal.executor.ReactiveExecutorService;
import com.github.rmannibucau.reactive.cdi.scope.internal.flow.ReactiveProcessor;
import com.github.rmannibucau.reactive.cdi.scope.internal.flow.ReactivePublisher;
import com.github.rmannibucau.reactive.cdi.scope.internal.flow.ReactiveSubscriber;
import com.github.rmannibucau.reactive.cdi.scope.internal.flow.ReactiveSubscription;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Vetoed;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

@Vetoed
public class ReactiveContext implements AlterableContext {
    private final ThreadLocal<Map<Contextual<?>, BeanInstanceBag<?>>> instances = new ThreadLocal<>();

    public Flow.Subscription wrapSubscription(final Flow.Subscription delegate) {
        final var current = current();
        try {
            return new ReactiveSubscription(delegate, current);
        } finally {
            current.release();
        }
    }

    public <A, B> Flow.Processor<A, B> wrapProcessor(final Flow.Processor<A, B> delegate) {
        final var current = current();
        try {
            return new ReactiveProcessor<>(delegate, current);
        } finally {
            current.release();
        }
    }

    public <A> Flow.Subscriber<A> wrapSubscriber(final Flow.Subscriber<A> delegate) {
        final var current = current();
        try {
            return new ReactiveSubscriber<>(delegate, current);
        } finally {
            current.release();
        }
    }

    public <A> Flow.Publisher<A> wrapPublisher(final Flow.Publisher<A> delegate) {
        final var current = current();
        try {
            return new ReactivePublisher<>(delegate, current);
        } finally {
            current.release();
        }
    }

    public Runnable wrapRunnable(final Runnable delegate) {
        final var current = current();
        try {
            return () -> current.wrap(delegate);
        } finally {
            current.release();
        }
    }

    public <A, B> BiConsumer<A, B> wrapBiConsumer(final BiConsumer<A, B> delegate) {
        final var current = current();
        try {
            return (a, b) -> current.wrap(() -> delegate.accept(a, b));
        } finally {
            current.release();
        }
    }

    public <A, B, C> BiFunction<A, B, C> wrapBiFunction(final BiFunction<A, B, C> delegate) {
        final var current = current();
        try {
            return (a, b) -> current.wrap(() -> delegate.apply(a, b)).get();
        } finally {
            current.release();
        }
    }

    public <A, B> Function<A, B> wrapFunction(final Function<A, B> delegate) {
        final var current = current();
        try {
            return a -> current.wrap(() -> delegate.apply(a)).get();
        } finally {
            current.release();
        }
    }

    public <A> Consumer<A> wrapConsumer(final Consumer<A> delegate) {
        return this.<A, Void>wrapFunction(a -> {
            delegate.accept(a);
            return null;
        })::apply;
    }

    public <T> CompletableFuture<T> wrapCompletableFuture(final CompletableFuture<T> promise) {
        return new ReactiveCompletionFuture<T>(this, promise);
    }

    public <T> CompletionStage<T> wrapCompletionStage(final CompletionStage<T> promise) {
        return new ReactiveCompletionFuture<T>(this, promise);
    }

    public Executor wrapExecutor(final Executor executor) {
        return new ReactiveExecutor(this, executor);
    }

    public ExecutorService wrapExecutorService(final ExecutorService executor) {
        return new ReactiveExecutorService(this, executor);
    }

    public Ctx start() {
        final var bags = new ConcurrentHashMap<Contextual<?>, BeanInstanceBag<?>>();
        instances.set(bags);
        return new Ctx(this, true, Thread.currentThread(), bags);
    }

    public void finish(final Ctx ctx) {
        final var bags = ctx.bags;
        if (bags == null) {
            return;
        }
        bags.forEach((k, v) -> doDestroy(Contextual.class.cast(k), v));
        bags.clear();
    }

    public Ctx current() {
        final var bags = instances.get();
        final var thread = Thread.currentThread();
        if (bags == null) {
            instances.remove();
            return new Ctx(this, true, thread, emptyMap());
        }
        return new Ctx(this, false, thread, bags);
    }

    public Ctx push(final Ctx ctx) {
        return setState(ctx, Thread.currentThread());
    }

    public Ctx reset(final Ctx ctx) {
        final var thread = Thread.currentThread();
        if (thread != ctx.originalThread) {
            throw new IllegalStateException("Restoring a context on a different thread");
        }
        if (ctx.removeOnReset) {
            instances.remove();
            return new Ctx(this, true, thread, emptyMap());
        }
        return setState(ctx, thread);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ReactiveScoped.class;
    }

    @Override
    public <T> T get(final Contextual<T> component, final CreationalContext<T> creationalContext) {
        final var bags = requireBags();
        BeanInstanceBag<T> bag = (BeanInstanceBag<T>) bags.get(component);
        if (bag == null) {
            bag = new BeanInstanceBag<>(creationalContext);
            final BeanInstanceBag<?> existing = bags.putIfAbsent(component, bag);
            if (existing != null) {
                bag = (BeanInstanceBag<T>) existing;
            }
        }
        if (bag.instance != null) {
            return bag.instance;
        }
        if (creationalContext == null) {
            return null;
        }
        synchronized (bag) {
            if (bag.instance != null) {
                return bag.instance;
            }
            return bag.instance = component.create(creationalContext);
        }
    }

    @Override
    public <T> T get(final Contextual<T> component) {
        final var wrapper = requireBags().get(component);
        return wrapper == null ? null : (T) wrapper.instance;
    }

    @Override
    public boolean isActive() {
        final var bags = instances.get();
        if (bags == null) {
            instances.remove();
        }
        return bags != null;
    }

    @Override
    public void destroy(final Contextual<?> contextual) {
        final var bags = instances.get();
        if (bags == null) {
            instances.remove();
            return;
        }
        final BeanInstanceBag<Object> instance = (BeanInstanceBag<Object>) bags.get(contextual);
        if (instance == null) {
            return;
        }
        if (instance.instance != null) {
            doDestroy(Contextual.class.cast(contextual), instance);
            bags.remove(contextual);
        }
    }

    private Map<Contextual<?>, BeanInstanceBag<?>> requireBags() {
        final var bags = instances.get();
        if (bags == null) {
            instances.remove();
            throw new ContextNotActiveException("@" + getScope().getName() + " is not active");
        }
        return bags;
    }

    private <T> void doDestroy(final Contextual<T> contextual, final BeanInstanceBag<T> instance) {
        synchronized (instance) {
            if (instance.destroyed) {
                return;
            }
            instance.destroyed = true;
            contextual.destroy(instance.instance, instance.creationalContext);
        }
    }

    private Ctx setState(final Ctx ctx, final Thread thread) {
        final var bags = instances.get();
        final var previous = new Ctx(this, false, thread, bags);
        if (ctx.bags == null) {
            instances.remove();
        } else {
            instances.set(ctx.bags);
        }
        return previous;
    }

    public static class Ctx {
        private final ReactiveContext ctx;
        private final boolean removeOnReset;
        private final Thread originalThread;
        private final Map<Contextual<?>, BeanInstanceBag<?>> bags;

        private Ctx(final ReactiveContext root,
                    final boolean removeOnReset, final Thread originalThread,
                    final Map<Contextual<?>, BeanInstanceBag<?>> bags) {
            this.ctx = root;
            this.removeOnReset = removeOnReset;
            this.originalThread = originalThread;
            this.bags = bags;
        }

        public Runnable wrap(final Runnable task) {
            return wrap(() -> {
                task.run();
                return null;
            })::get;
        }

        public <T> Supplier<T> wrap(final Supplier<T> task) {
            final var current = ctx.current();
            try {
                return () -> {
                    final var previous = ctx.push(Ctx.this);
                    try {
                        return task.get();
                    } finally {
                        previous.release();
                    }
                };
            } finally {
                current.release();
            }
        }

        public void release() {
            ctx.reset(this);
        }
    }

    private static class BeanInstanceBag<T> implements Serializable {
        private final CreationalContext<T> creationalContext;
        private volatile boolean destroyed = false;
        private volatile T instance;

        private BeanInstanceBag(final CreationalContext<T> creationalContext) {
            this.creationalContext = creationalContext;
        }
    }
}
