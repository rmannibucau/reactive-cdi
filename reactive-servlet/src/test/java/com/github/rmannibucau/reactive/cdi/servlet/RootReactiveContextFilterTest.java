package com.github.rmannibucau.reactive.cdi.servlet;

import com.github.rmannibucau.reactive.cdi.scope.api.ReactiveScoped;
import com.github.rmannibucau.reactive.cdi.scope.internal.ReactiveContext;
import org.apache.meecrowave.configuration.Configuration;
import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.annotation.WebFilter;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedStage;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MeecrowaveConfig(scanningPackageIncludes = "com.github.rmannibucau.reactive.cdi.servlet.RootReactiveContextFilterTest")
class RootReactiveContextFilterTest {
    @Inject
    private SpyState spyState;

    @Inject
    private Configuration configuration;

    @BeforeEach
    void reset() {
        spyState.setCreated(0);
        spyState.setDestroyed(0);
        spyState.setInstances(new ArrayList<>());
        spyState.setEvent(new ArrayList<>());
    }

    @Test
    void propagateWithAutoStartAndClean() {
        final var client = ClientBuilder.newClient();
        try {
            assertEquals("called",
                    client.target("http://localhost:" + configuration.getHttpPort() + "/RootReactiveContextFilterTest")
                            .request(TEXT_PLAIN_TYPE)
                            .get(String.class));
        } finally {
            client.close();
        }
        assertEquals(1, spyState.getCreated());
        assertEquals(1, spyState.getDestroyed());
        assertEquals(2, spyState.getInstances().size());
        assertEquals(1, spyState.getInstances().stream().distinct().count());
        assertEquals(List.of("create", "get", "async", "destroy"), spyState.getEvent());
    }

    @ReactiveScoped
    @Path("RootReactiveContextFilterTest")
    public static class Endpoint {
        @Inject
        private SpyState spyState;

        @Inject
        private ReactiveContext context;

        @GET
        @Produces(TEXT_PLAIN)
        public CompletionStage<String> get() {
            spyState.getEvent().add("get");
            spyState.getInstances().add(this);
            return completedStage(true).thenApplyAsync(it -> { // change of thread
                        spyState.getEvent().add("async");
                        spyState.getInstances().add(Endpoint.this);
                        return "called";
                    },
                    // not required for this simple app to wrap (since we use a single instance we can access by ref)
                    // but the general pattern is right
                    context.wrapExecutor(command -> new Thread(command).start()));
        }

        @PostConstruct
        private void create() {
            spyState.getEvent().add("create");
            spyState.setCreated(spyState.getCreated() + 1);
        }

        @PreDestroy
        private void destroy() {
            spyState.getEvent().add("destroy");
            spyState.setDestroyed(spyState.getDestroyed() + 1);
        }
    }

    @Dependent
    @WebFilter(urlPatterns = "/*", asyncSupported = true)
    public static class Register extends RootReactiveContextFilter {
    }

    @ApplicationScoped
    public static class SpyState {
        private int created;
        private int destroyed;
        private List<Object> instances;
        private List<String> event;

        public List<String> getEvent() {
            return event;
        }

        public void setEvent(final List<String> event) {
            this.event = event;
        }

        public List<Object> getInstances() {
            return instances;
        }

        public void setInstances(final List<Object> instances) {
            this.instances = instances;
        }

        public int getCreated() {
            return created;
        }

        public void setCreated(final int created) {
            this.created = created;
        }

        public int getDestroyed() {
            return destroyed;
        }

        public void setDestroyed(final int destroyed) {
            this.destroyed = destroyed;
        }
    }
}
