package com.github.rmannibucau.reactive.cdi.scope.internal.executor;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.toList;

public class ReactiveExecutorService implements ExecutorService {
    private final ReactiveContext context;
    private final ExecutorService delegate;

    public ReactiveExecutorService(final ReactiveContext context, final ExecutorService executor) {
        this.context = context;
        this.delegate = executor;
    }

    @Override
    public void execute(final Runnable command) {
        final var current = context.current();
        try {
            delegate.execute(current.wrap(command));
        } finally {
            current.release();
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        final var current = context.current();
        try {
            return delegate.submit(wrapCallable(task, current));
        } finally {
            current.release();
        }
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        final var current = context.current();
        try {
            return delegate.submit(current.wrap(task), result);
        } finally {
            current.release();
        }
    }

    @Override
    public Future<?> submit(final Runnable task) {
        final var current = context.current();
        try {
            return delegate.submit(current.wrap(task));
        } finally {
            current.release();
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        final var current = context.current();
        try {
            return delegate.invokeAll(tasks.stream().map(it -> wrapCallable(it, current)).collect(toList()));
        } finally {
            current.release();
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        final var current = context.current();
        try {
            return delegate.invokeAll(tasks.stream().map(it -> wrapCallable(it, current)).collect(toList()), timeout, unit);
        } finally {
            current.release();
        }
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        final var current = context.current();
        try {
            return delegate.invokeAny(tasks.stream().map(it -> wrapCallable(it, current)).collect(toList()));
        } finally {
            current.release();
        }
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final var current = context.current();
        try {
            return delegate.invokeAny(tasks.stream().map(it -> wrapCallable(it, current)).collect(toList()), timeout, unit);
        } finally {
            current.release();
        }
    }

    private <T> Callable<T> wrapCallable(final Callable<T> task, final ReactiveContext.Ctx current) {
        return () -> {
            final var previous = context.push(current);
            try {
                return task.call();
            } finally {
                context.reset(previous);
            }
        };
    }
}
