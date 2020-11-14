package com.github.rmannibucau.reactive.cdi.scope.internal.flow;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.concurrent.Flow;

public class ReactiveSubscriber<A> implements Flow.Subscriber<A> {
    protected final Flow.Subscriber<A> delegate;
    protected final ReactiveContext.Ctx ctx;

    public ReactiveSubscriber(final Flow.Subscriber<A> delegate, final ReactiveContext.Ctx ctx) {
        this.delegate = delegate;
        this.ctx = ctx;
    }

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        ctx.wrap(() -> delegate.onSubscribe(subscription));
    }

    @Override
    public void onNext(final A item) {
        ctx.wrap(() -> delegate.onNext(item));
    }

    @Override
    public void onError(final Throwable throwable) {
        ctx.wrap(() -> delegate.onError(throwable));
    }

    @Override
    public void onComplete() {
        ctx.wrap(delegate::onComplete);
    }
}
