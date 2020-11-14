package com.github.rmannibucau.reactive.cdi.scope.internal.flow;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.concurrent.Flow;

public class ReactiveProcessor<A, B> extends ReactiveSubscriber<A> implements Flow.Processor<A, B> {
    private final Flow.Processor<A, B> delegate;

    public ReactiveProcessor(final Flow.Processor<A, B> delegate, final ReactiveContext.Ctx ctx) {
        super(delegate, ctx);
        this.delegate = delegate;
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super B> subscriber) {
        ctx.wrap(() -> delegate.subscribe(subscriber));
    }
}
