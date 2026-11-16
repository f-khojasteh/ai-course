package com.changeflow.integration.support;

import com.changeflow.application.*;
import com.changeflow.application.port.*;
import com.changeflow.domain.*;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Test-only port decorators. All storage/locks/flushes still use the real adapters. */
@TestConfiguration(proxyBeanMethods = false)
public class ConcurrencyTestConfiguration {
    public record Event(String stage, UUID id, long revision, Object context, String thread) { }
    public static class Control {
        private final EntityManagerFactory factory;
        public final List<Event> events = new CopyOnWriteArrayList<>();
        public volatile Consumer<Event> hook = event -> { };
        Control(EntityManagerFactory factory) { this.factory = factory; }
        public void reset() { hook = event -> { }; events.clear(); }
        public void event(String stage, UUID id, long revision) {
            var em = EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
            var event = new Event(stage, id, revision, em, Thread.currentThread().getName());
            if (em == null || !TransactionSynchronizationManager.isActualTransactionActive()) throw new AssertionError("Missing real transaction/context at " + stage);
            events.add(event); hook.accept(event);
        }
        public void event(String stage, UUID id) { event(stage, id, -1); }
    }
    public static class Pause implements AutoCloseable {
        public final CountDownLatch arrived = new CountDownLatch(1), release = new CountDownLatch(1);
        private final Predicate<Event> matches;
        private final int seconds;
        public final List<Event> arrivals = new CopyOnWriteArrayList<>();
        public Pause(String stage, UUID id) { this(stage, id, 15); }
        public Pause(String stage, UUID id, int seconds) { this.matches = event -> event.stage().equals(stage) && event.id().equals(id); this.seconds = seconds; }
        public void accept(Event event) { if (matches.test(event)) { arrivals.add(event); arrived.countDown(); await(release, seconds); } }
        public void awaitArrival() { await(arrived, 8); }
        @Override public void close() { release.countDown(); }
    }
    public static class Overlap {
        private final String stage;
        private final UUID id;
        private final CyclicBarrier barrier = new CyclicBarrier(2);
        public final List<Event> arrivals = new CopyOnWriteArrayList<>();
        public Overlap(String stage, UUID id) { this.stage = stage; this.id = id; }
        public void accept(Event event) {
            if (!event.stage().equals(stage) || !event.id().equals(id)) return;
            arrivals.add(event);
            try { barrier.await(8, TimeUnit.SECONDS); }
            catch (Exception error) { throw new AssertionError("Two real transactions did not reach the overlap barrier.", error); }
        }
    }
    public static void await(CountDownLatch latch, int seconds) {
        try { if (!latch.await(seconds, TimeUnit.SECONDS)) throw new AssertionError("Bounded test coordination expired."); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
    }
    @Bean Control concurrencyControl(EntityManagerFactory factory) { return new Control(factory); }
    @Bean @Primary ChangeRequestStore coordinatedRequests(@Qualifier("jpaChangeRequestStore") ChangeRequestStore delegate, Control control) {
        return new ChangeRequestStore() {
            public Optional<RequestSnapshot> find(UUID id) { var result = delegate.find(id); result.ifPresent(value -> control.event("LOADED", id, value.revision())); return result; }
            public Optional<RequestSnapshot> findVisible(UUID id, UUID user, boolean requester, boolean reviewer) { var result = delegate.findVisible(id, user, requester, reviewer); result.ifPresent(value -> control.event("LOADED", id, value.revision())); return result; }
            public List<RequestSnapshot> listVisible(UUID id, boolean requester, boolean reviewer, int offset, int limit) { return delegate.listVisible(id, requester, reviewer, offset, limit); }
            public RequestSnapshot insert(ChangeRequest value) { var result = delegate.insert(value); control.event("INSERTED", value.id(), result.revision()); return result; }
            public RequestSnapshot update(ChangeRequest value, long revision) { control.event("BEFORE_UPDATE", value.id(), revision); var result = delegate.update(value, revision); control.event("UPDATED", value.id(), result.revision()); return result; }
            public RequestSnapshot verifyUnchanged(UUID id, long revision) { control.event("BEFORE_UNCHANGED", id, revision); var result = delegate.verifyUnchanged(id, revision); control.event("UNCHANGED_CHECKED", id, result.revision()); return result; }
        };
    }
    @Bean @Primary CurrentAccessGuard coordinatedAccess(@Qualifier("jpaCurrentAccessGuard") CurrentAccessGuard delegate, Control control) {
        return new CurrentAccessGuard() {
            public UserAccessStore.Access current(AuthenticatedCaller caller) { return delegate.current(caller); }
            public UserAccessStore.Access lockAndVerify(AuthenticatedCaller caller) {
                control.event("BEFORE_ACCESS", caller.userId()); var result = delegate.lockAndVerify(caller); control.event("AFTER_ACCESS", caller.userId()); return result;
            }
        };
    }
    @Bean @Primary CredentialStore coordinatedCredentials(@Qualifier("jpaCredentialStore") CredentialStore delegate, Control control) {
        return new CredentialStore() {
            public Optional<Credential> find(UUID id) { control.event("CREDENTIAL_FIND", id); return delegate.find(id); }
            public Optional<Credential> lock(UUID id) { control.event("BEFORE_CREDENTIAL_LOCK", id); var result = delegate.lock(id); control.event("CREDENTIAL_LOCKED", id); return result; }
            public void insert(UUID id, UUID user, byte[] hash, Instant at) { delegate.insert(id, user, hash, at); }
            public void revokeLocked(UUID id, Instant at) { delegate.revokeLocked(id, at); control.event("REVOKE_WRITTEN", id); }
        };
    }
    @Bean @Primary UserAccessStore coordinatedUsers(@Qualifier("jpaUserAccessStore") UserAccessStore delegate, Control control) {
        return new UserAccessStore() {
            public Optional<Access> find(UUID id) { return delegate.find(id); }
            public Optional<Access> lock(UUID id) { control.event("BEFORE_USER_LOCK", id); var result = delegate.lock(id); control.event("USER_LOCKED", id); return result; }
            public void insert(UUID id, boolean requester, boolean reviewer, Instant at) { delegate.insert(id, requester, reviewer, at); }
            public void setRolesLocked(UUID id, boolean requester, boolean reviewer, Instant at) { delegate.setRolesLocked(id, requester, reviewer, at); control.event("ROLES_WRITTEN", id); }
        };
    }
}
