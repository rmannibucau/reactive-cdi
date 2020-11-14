package com.github.rmannibucau.reactive.cdi.scope.internal.flow;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.concurrent.Flow;

public class ReactivePublisher<A> implements Flow.Publisher<A> {
    private final Flow.Publisher<A> delegate;
    private final ReactiveContext.Ctx ctx;

    public ReactivePublisher(final Flow.Publisher<A> delegate, final ReactiveContext.Ctx ctx) {
        this.delegate = delegate;
        this.ctx = ctx;
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super A> subscriber) {
        ctx.wrap(() -> delegate.subscribe(subscriber));
    }
}
