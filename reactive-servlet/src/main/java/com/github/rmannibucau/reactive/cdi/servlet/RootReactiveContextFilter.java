package com.github.rmannibucau.reactive.cdi.servlet;

import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class RootReactiveContextFilter implements Filter {
    @Inject
    private ReactiveContext context;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {
        final var rootCtx = context.start();
        request.setAttribute(ReactiveContext.Ctx.class.getName(), rootCtx);
        try {
            chain.doFilter(new HttpServletRequestWrapper(HttpServletRequest.class.cast(request)) {
                @Override
                public AsyncContext startAsync() throws IllegalStateException {
                    return cleanOnEnd(rootCtx, super.startAsync());
                }

                @Override
                public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
                    return cleanOnEnd(rootCtx, super.startAsync(servletRequest, servletResponse));
                }

            }, response);
        } finally {
            if (!request.isAsyncStarted()) {
                context.finish(rootCtx);
            }
            context.reset(rootCtx); // does not destroy instances but clean up the thread local to avoid to leak
        }
    }

    private AsyncContext cleanOnEnd(final ReactiveContext.Ctx rootCtx, final AsyncContext ctx) {
        ctx.addListener(new ReactiveAsyncListener(context, rootCtx));
        return new ReactiveAsyncContext(ctx, rootCtx);
    }

    private static class ReactiveAsyncListener implements AsyncListener {
        private final ReactiveContext scope;
        private final ReactiveContext.Ctx root;

        private ReactiveAsyncListener(final ReactiveContext context, final ReactiveContext.Ctx rootCtx) {
            this.scope = context;
            this.root = rootCtx;
        }

        private void onEnd() { // end of the request we can destroy the context we created
            scope.finish(root);
        }

        @Override
        public void onComplete(final AsyncEvent event) {
            onEnd();
        }

        @Override
        public void onTimeout(final AsyncEvent event) {
            onEnd();
        }

        @Override
        public void onError(final AsyncEvent event) {
            onEnd();
        }

        @Override
        public void onStartAsync(final AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }
    }

    private static class ReactiveAsyncContext implements AsyncContext {
        private final AsyncContext delegate;
        private final ReactiveContext.Ctx current;

        private ReactiveAsyncContext(final AsyncContext ctx, final ReactiveContext.Ctx current) {
            this.delegate = ctx;
            this.current = current;
        }

        @Override
        public void start(final Runnable run) {
            delegate.start(current.wrap(run));
        }

        @Override
        public ServletRequest getRequest() {
            return delegate.getRequest();
        }

        @Override
        public ServletResponse getResponse() {
            return delegate.getResponse();
        }

        @Override
        public boolean hasOriginalRequestAndResponse() {
            return delegate.hasOriginalRequestAndResponse();
        }

        @Override
        public void dispatch() {
            delegate.dispatch();
        }

        @Override
        public void dispatch(String path) {
            delegate.dispatch(path);
        }

        @Override
        public void dispatch(final ServletContext context, final String path) {
            delegate.dispatch(context, path);
        }

        @Override
        public void complete() {
            delegate.complete();
        }

        @Override
        public void addListener(final AsyncListener listener) {
            delegate.addListener(listener);
        }

        @Override
        public void addListener(final AsyncListener listener, final ServletRequest request, final ServletResponse response) {
            delegate.addListener(listener, request, response);
        }

        @Override
        public <T extends AsyncListener> T createListener(final Class<T> clazz) throws ServletException {
            return delegate.createListener(clazz);
        }

        @Override
        public void setTimeout(final long timeout) {
            delegate.setTimeout(timeout);
        }

        @Override
        public long getTimeout() {
            return delegate.getTimeout();
        }
    }
}
