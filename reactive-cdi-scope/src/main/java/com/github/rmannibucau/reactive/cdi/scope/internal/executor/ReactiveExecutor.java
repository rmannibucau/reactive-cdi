package com.github.rmannibucau.reactive.cdi.scope.internal.executor;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.concurrent.Executor;

public class ReactiveExecutor implements Executor {
    private final ReactiveContext context;
    private final Executor delegate;

    public ReactiveExecutor(final ReactiveContext context, final Executor executor) {
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
}
