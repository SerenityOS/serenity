/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Timeout event notified by selector thread. Executes the given handler if
 * the timer not canceled first.
 *
 * Register with {@link HttpClientImpl#registerTimer(TimeoutEvent)}.
 *
 * Cancel with {@link HttpClientImpl#cancelTimer(TimeoutEvent)}.
 */
abstract class TimeoutEvent implements Comparable<TimeoutEvent> {

    private static final AtomicLong COUNTER = new AtomicLong();
    // we use id in compareTo to make compareTo consistent with equals
    // see TimeoutEvent::compareTo below;
    private final long id = COUNTER.incrementAndGet();
    private final Duration duration;
    private final Instant deadline;

    TimeoutEvent(Duration duration) {
        this.duration = duration;
        deadline = Instant.now().plus(duration);
    }

    public abstract void handle();

    public Instant deadline() {
        return deadline;
    }

    @Override
    public int compareTo(TimeoutEvent other) {
        if (other == this) return 0;
        // if two events have the same deadline, but are not equals, then the
        // smaller is the one that was created before (has the smaller id).
        // This is arbitrary and we don't really care which is smaller or
        // greater, but we need a total order, so two events with the
        // same deadline cannot compare == 0 if they are not equals.
        final int compareDeadline = this.deadline.compareTo(other.deadline);
        if (compareDeadline == 0 && !this.equals(other)) {
            long diff = this.id - other.id; // should take care of wrap around
            if (diff < 0) return -1;
            else if (diff > 0) return 1;
            else assert false : "Different events with same id and deadline";
        }
        return compareDeadline;
    }

    @Override
    public String toString() {
        return "TimeoutEvent[id=" + id + ", duration=" + duration
                + ", deadline=" + deadline + "]";
    }
}
