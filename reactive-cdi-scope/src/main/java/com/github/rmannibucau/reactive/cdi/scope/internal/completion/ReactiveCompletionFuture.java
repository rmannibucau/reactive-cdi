package com.github.rmannibucau.reactive.cdi.scope.internal.completion;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReactiveCompletionFuture<T> extends CompletableFuture<T> {
    private final ReactiveContext context;

    public ReactiveCompletionFuture(final ReactiveContext context, final CompletionStage<T> delegate) {
        this.context = context;

        // subscribe
        delegate.whenComplete((r, ex) -> {
            if (ex != null) {
                ReactiveCompletionFuture.this.completeExceptionally(ex);
            } else {
                ReactiveCompletionFuture.this.complete(r);
            }
        });
    }

    @Override
    public <U> CompletableFuture<U> thenApply(final Function<? super T, ? extends U> fn) {
        return super.thenApply(context.wrapFunction(fn));
    }

    @Override
    public CompletableFuture<Void> thenAccept(final Consumer<? super T> action) {
        return super.thenAccept(context.wrapConsumer(action));
    }

    @Override
    public CompletableFuture<Void> thenRun(final Runnable action) {
        return super.thenRun(context.wrapRunnable(action));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(final CompletionStage<? extends U> other,
                                                   final BiFunction<? super T, ? super U, ? extends V> fn) {
        return super.thenCombine(other, context.wrapBiFunction(fn));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(final CompletionStage<? extends U> other,
                                                      final BiConsumer<? super T, ? super U> action) {
        return super.thenAcceptBoth(other, context.wrapBiConsumer(action));
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(final CompletionStage<?> other, final Runnable action) {
        return super.runAfterBoth(other, context.wrapRunnable(action));
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
        return super.applyToEither(other, context.wrapFunction(fn));
    }

    @Override
    public CompletableFuture<Void> acceptEither(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
        return super.acceptEither(other, context.wrapConsumer(action));
    }

    @Override
    public CompletableFuture<Void> runAfterEither(final CompletionStage<?> other, final Runnable action) {
        return super.runAfterEither(other, context.wrapRunnable(action));
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return super.thenCompose(context.wrapFunction(fn));
    }

    @Override
    public CompletableFuture<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
        return super.whenComplete(context.wrapBiConsumer(action));
    }

    @Override
    public <U> CompletableFuture<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return super.handle(context.wrapBiFunction(fn));
    }

    @Override
    public CompletableFuture<T> exceptionally(final Function<Throwable, ? extends T> fn) {
        return super.exceptionally(context.wrapFunction(fn));
    }
}
