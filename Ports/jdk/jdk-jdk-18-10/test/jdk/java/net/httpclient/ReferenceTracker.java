/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import jdk.internal.net.http.common.OperationTrackers;
import jdk.internal.net.http.common.OperationTrackers.Tracker;

import java.net.http.HttpClient;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * A small helper class to help track clients which still
 * have pending operations at the end of a test.
 */
public class ReferenceTracker {
    private final ConcurrentLinkedQueue<Tracker> TRACKERS
            = new ConcurrentLinkedQueue<Tracker>();

    public static final ReferenceTracker INSTANCE
            = new ReferenceTracker();

    public HttpClient track(HttpClient client) {
        Tracker tracker = OperationTrackers.getTracker(client);
        assert tracker != null;
        TRACKERS.add(tracker);
        return client;
    }

    public long getTrackedClientCount() {
        return TRACKERS.size();
    }

    public StringBuilder diagnose(StringBuilder warnings) {
        for (Tracker tracker : TRACKERS) {
            checkOutstandingOperations(warnings, tracker);
        }
        return warnings;
    }

    public boolean hasOutstandingOperations() {
        return TRACKERS.stream().anyMatch(t -> t.getOutstandingOperations() > 0);
    }

    public long getOutstandingOperationsCount() {
        return TRACKERS.stream()
                .map(Tracker::getOutstandingOperations)
                .filter(n -> n > 0)
                .collect(Collectors.summingLong(n -> n));
    }

    public long getOutstandingClientCount() {
        return TRACKERS.stream()
                .map(Tracker::getOutstandingOperations)
                .filter(n -> n > 0)
                .count();
    }

    public AssertionError check(long graceDelayMs) {
        AssertionError fail = null;
        if (hasOutstandingOperations()) {
            try {
                Thread.sleep(graceDelayMs);
            } catch (InterruptedException x) {
                // OK
            }
            StringBuilder warnings = diagnose(new StringBuilder());
            addSummary(warnings);
            if (hasOutstandingOperations()) {
                fail = new AssertionError(warnings.toString());
            }
        } else {
            System.out.println("PASSED: No outstanding operations found in "
                    + getTrackedClientCount() + " clients");
        }
        return fail;
    }

    private void addSummary(StringBuilder warning) {
        long activeClients = getOutstandingClientCount();
        long operations = getOutstandingOperationsCount();
        long tracked = getTrackedClientCount();
        if (warning.length() > 0) warning.append("\n");
        int pos = warning.length();
        warning.append("Found ")
                .append(activeClients)
                .append(" client still active, with ")
                .append(operations)
                .append(" operations still pending out of ")
                .append(tracked)
                .append(" tracked clients.");
        System.out.println(warning.toString().substring(pos));
        System.err.println(warning.toString().substring(pos));
    }

    private static void checkOutstandingOperations(StringBuilder warning, Tracker tracker) {
        if (tracker.getOutstandingOperations() > 0) {
            if (warning.length() > 0) warning.append("\n");
            int pos = warning.length();
            warning.append("WARNING: tracker for " + tracker.getName() + " has outstanding operations:");
            warning.append("\n\tPending HTTP/1.1 operations: " + tracker.getOutstandingHttpOperations());
            warning.append("\n\tPending HTTP/2 streams: " + tracker.getOutstandingHttp2Streams());
            warning.append("\n\tPending WebSocket operations: " + tracker.getOutstandingWebSocketOperations());
            warning.append("\n\tTotal pending operations: " + tracker.getOutstandingOperations());
            warning.append("\n\tFacade referenced: " + tracker.isFacadeReferenced());
            System.out.println(warning.toString().substring(pos));
            System.err.println(warning.toString().substring(pos));
        }
    }

}
