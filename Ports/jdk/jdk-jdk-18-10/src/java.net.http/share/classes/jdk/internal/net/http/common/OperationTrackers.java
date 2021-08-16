/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.common;

import java.net.http.HttpClient;

/**
 * A small class allowing to track how many operations are
 * left outstanding on an instance of HttpClient.
 */
public final class OperationTrackers {
    private OperationTrackers() {
        throw new InternalError("not instantiable");
    }

    /**
     * A tracker can return the current value of
     * operation counters maintained by an instance
     * of {@link Trackable}, such as an HttpClientImpl.
     */
    public interface Tracker {
        // The total number of outstanding operations
        long getOutstandingOperations();
        // The number of outstanding HTTP/1.1 operations.
        // A single HTTP/1.1 request may increment this counter
        // multiple times, so the value returned will be >= to
        // the number of active HTTP/1.1 connections, but will
        // still be 0 if there are no active connections.
        long getOutstandingHttpOperations();
        // The number of active HTTP/2 streams
        long getOutstandingHttp2Streams();
        // The number of active WebSockets
        long getOutstandingWebSocketOperations();
        // Whether the facade returned to the
        // user is still referenced
        boolean isFacadeReferenced();
        // The name of the object being tracked.
        String getName();
    }

    /**
     * Implemented by objects that maintain operation counters.
     */
    public interface Trackable {
        Tracker getOperationsTracker();
    }

    /**
     * Returns a tracker to track pending operations started on
     * an HttpClient instance. May return null if this isn't
     * an HttpClientImpl or HttpClientFacade.
     * @param client the HttpClient instance to track.
     * @return A tracker or null.
     */
    public static Tracker getTracker(HttpClient client) {
        if (client instanceof Trackable) {
            return ((Trackable)client).getOperationsTracker();
        } else {
            return null;
        }
    }

}
