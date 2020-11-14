package com.github.rmannibucau.reactive.cdi.scope.internal.flow;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.concurrent.Flow;

public class ReactiveSubscription implements Flow.Subscription {
    private final Flow.Subscription delegate;
    private final ReactiveContext.Ctx ctx;

    public ReactiveSubscription(final Flow.Subscription delegate, final ReactiveContext.Ctx ctx) {
        this.delegate = delegate;
        this.ctx = ctx;
    }

    @Override
    public void request(final long n) {
        ctx.wrap(() -> delegate.request(n));
    }

    @Override
    public void cancel() {
        ctx.wrap(delegate::cancel);
    }
}
