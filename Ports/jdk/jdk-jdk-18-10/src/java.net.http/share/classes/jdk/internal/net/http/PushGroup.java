/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.net.http;

import java.security.AccessControlContext;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.concurrent.Executor;

import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Log;

/**
 * One PushGroup object is associated with the parent Stream of the pushed
 * Streams. This keeps track of all common state associated with the pushes.
 */
class PushGroup<T> {
    private final HttpRequest initiatingRequest;

    final CompletableFuture<Void> noMorePushesCF;

    volatile Throwable error; // any exception that occurred during pushes

    // user's subscriber object
    final PushPromiseHandler<T> pushPromiseHandler;

    private final Executor executor;

    int numberOfPushes;
    int remainingPushes;
    boolean noMorePushes = false;

    PushGroup(PushPromiseHandler<T> pushPromiseHandler,
              HttpRequestImpl initiatingRequest,
              Executor executor) {
        this(pushPromiseHandler, initiatingRequest, new MinimalFuture<>(), executor);
    }

    // Check mainBodyHandler before calling nested constructor.
    private PushGroup(HttpResponse.PushPromiseHandler<T> pushPromiseHandler,
                      HttpRequestImpl initiatingRequest,
                      CompletableFuture<HttpResponse<T>> mainResponse,
                      Executor executor) {
        this.noMorePushesCF = new MinimalFuture<>();
        this.pushPromiseHandler = pushPromiseHandler;
        this.initiatingRequest = initiatingRequest;
        this.executor = executor;
    }

    interface Acceptor<T> {
        BodyHandler<T> bodyHandler();
        CompletableFuture<HttpResponse<T>> cf();
        boolean accepted();
    }

    private static class AcceptorImpl<T> implements Acceptor<T> {
        private final Executor executor;
        private volatile HttpResponse.BodyHandler<T> bodyHandler;
        private volatile CompletableFuture<HttpResponse<T>> cf;

        AcceptorImpl(Executor executor) {
            this.executor = executor;
        }

        CompletableFuture<HttpResponse<T>> accept(BodyHandler<T> bodyHandler) {
            Objects.requireNonNull(bodyHandler);
            if (this.bodyHandler != null)
                throw new IllegalStateException("non-null bodyHandler");
            this.bodyHandler = bodyHandler;
            cf = new MinimalFuture<>();
            return cf.whenCompleteAsync((r,t) -> {}, executor);
        }

        @Override public BodyHandler<T> bodyHandler() { return bodyHandler; }

        @Override public CompletableFuture<HttpResponse<T>> cf() { return cf; }

        @Override public boolean accepted() { return cf != null; }
    }

    Acceptor<T> acceptPushRequest(HttpRequest pushRequest) {
        AcceptorImpl<T> acceptor = new AcceptorImpl<>(executor);
        try {
            pushPromiseHandler.applyPushPromise(initiatingRequest, pushRequest, acceptor::accept);
        } catch (Throwable t) {
            if (acceptor.accepted()) {
                CompletableFuture<?> cf = acceptor.cf();
                cf.completeExceptionally(t);
            }
            throw t;
        }

        synchronized (this) {
            if (acceptor.accepted()) {
                numberOfPushes++;
                remainingPushes++;
            }
            return acceptor;
        }
    }

    // This is called when the main body response completes because it means
    // no more PUSH_PROMISEs are possible

    synchronized void noMorePushes(boolean noMore) {
        noMorePushes = noMore;
        checkIfCompleted();
        noMorePushesCF.complete(null);
    }

    synchronized CompletableFuture<Void> pushesCF() {
        return noMorePushesCF;
    }

    synchronized boolean noMorePushes() {
        return noMorePushes;
    }

    synchronized void pushCompleted() {
        remainingPushes--;
        checkIfCompleted();
    }

    synchronized void checkIfCompleted() {
        if (Log.trace()) {
            Log.logTrace("PushGroup remainingPushes={0} error={1} noMorePushes={2}",
                         remainingPushes,
                         (error==null)?error:error.getClass().getSimpleName(),
                         noMorePushes);
        }
        if (remainingPushes == 0 && error == null && noMorePushes) {
            if (Log.trace()) {
                Log.logTrace("push completed");
            }
        }
    }

    synchronized void pushError(Throwable t) {
        if (t == null) {
            return;
        }
        this.error = t;
    }
}
