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

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.Utils;

/**
 * A Send Window Flow-Controller that is used to control outgoing Connection
 * and Stream flows, per HTTP/2 connection.
 *
 * A Http2Connection has its own unique single instance of a WindowController
 * that it shares with its Streams. Each stream must acquire the appropriate
 * amount of Send Window from the controller before sending data.
 *
 * WINDOW_UPDATE frames, both connection and stream specific, must notify the
 * controller of their increments. SETTINGS frame's INITIAL_WINDOW_SIZE must
 * notify the controller so that it can adjust the active stream's window size.
 */
final class WindowController {

    static final Logger debug =
            Utils.getDebugLogger("WindowController"::toString, Utils.DEBUG);

    /**
     * Default initial connection Flow-Control Send Window size, as per HTTP/2.
     */
    private static final int DEFAULT_INITIAL_WINDOW_SIZE = 64 * 1024 - 1;

    /** The connection Send Window size. */
    private int connectionWindowSize;
    /** A Map of the active streams, where the key is the stream id, and the
     *  value is the stream's Send Window size, which may be negative. */
    private final Map<Integer,Integer> streams = new HashMap<>();
    /** A Map of streams awaiting Send Window. The key is the stream id. The
     * value is a pair of the Stream ( representing the key's stream id ) and
     * the requested amount of send Window. */
    private final Map<Integer, Map.Entry<Stream<?>, Integer>> pending
            = new LinkedHashMap<>();

    private final ReentrantLock controllerLock = new ReentrantLock();

    /** A Controller with the default initial window size. */
    WindowController() {
        connectionWindowSize = DEFAULT_INITIAL_WINDOW_SIZE;
    }

//    /** A Controller with the given initial window size. */
//    WindowController(int initialConnectionWindowSize) {
//        connectionWindowSize = initialConnectionWindowSize;
//    }

    /** Registers the given stream with this controller. */
    void registerStream(int streamid, int initialStreamWindowSize) {
        controllerLock.lock();
        try {
            Integer old = streams.put(streamid, initialStreamWindowSize);
            if (old != null)
                throw new InternalError("Unexpected entry ["
                        + old + "] for streamid: " + streamid);
        } finally {
            controllerLock.unlock();
        }
    }

    /** Removes/De-registers the given stream with this controller. */
    void removeStream(int streamid) {
        controllerLock.lock();
        try {
            Integer old = streams.remove(streamid);
            // Odd stream numbers (client streams) should have been registered.
            // Even stream numbers (server streams - aka Push Streams) should
            // not be registered
            final boolean isClientStream = (streamid & 0x1) == 1;
            if (old == null && isClientStream) {
                throw new InternalError("Expected entry for streamid: " + streamid);
            } else if (old != null && !isClientStream) {
                throw new InternalError("Unexpected entry for streamid: " + streamid);
            }
        } finally {
            controllerLock.unlock();
        }
    }

    /**
     * Attempts to acquire the requested amount of Send Window for the given
     * stream.
     *
     * The actual amount of Send Window available may differ from the requested
     * amount. The actual amount, returned by this method, is the minimum of,
     * 1) the requested amount, 2) the stream's Send Window, and 3) the
     * connection's Send Window.
     *
     * A negative or zero value is returned if there's no window available.
     * When the result is negative or zero, this method arranges for the
     * given stream's {@link Stream#signalWindowUpdate()} method to be invoke at
     * a later time when the connection and/or stream window's have been
     * increased. The {@code tryAcquire} method should then be invoked again to
     * attempt to acquire the available window.
     */
    int tryAcquire(int requestAmount, int streamid, Stream<?> stream) {
        controllerLock.lock();
        try {
            Integer streamSize = streams.get(streamid);
            if (streamSize == null)
                throw new InternalError("Expected entry for streamid: "
                                        + streamid);
            int x = Math.min(requestAmount,
                             Math.min(streamSize, connectionWindowSize));

            if (x <= 0)  { // stream window size may be negative
                if (debug.on())
                    debug.log("Stream %d requesting %d but only %d available (stream: %d, connection: %d)",
                              streamid, requestAmount, Math.min(streamSize, connectionWindowSize),
                              streamSize, connectionWindowSize);
                // If there's not enough window size available, put the
                // caller in a pending list.
                pending.put(streamid, Map.entry(stream, requestAmount));
                return x;
            }

            // Remove the caller from the pending list ( if was waiting ).
            pending.remove(streamid);

            // Update window sizes and return the allocated amount to the caller.
            streamSize -= x;
            streams.put(streamid, streamSize);
            connectionWindowSize -= x;
            if (debug.on())
                debug.log("Stream %d amount allocated %d, now %d available (stream: %d, connection: %d)",
                          streamid, x, Math.min(streamSize, connectionWindowSize),
                          streamSize, connectionWindowSize);
            return x;
        } finally {
            controllerLock.unlock();
        }
    }

    /**
     * Increases the Send Window size for the connection.
     *
     * A number of awaiting requesters, from unfulfilled tryAcquire requests,
     * may have their stream's {@link Stream#signalWindowUpdate()} method
     * scheduled to run ( i.e. awake awaiters ).
     *
     * @return false if, and only if, the addition of the given amount would
     *         cause the Send Window to exceed 2^31-1 (overflow), otherwise true
     */
    boolean increaseConnectionWindow(int amount) {
        List<Stream<?>> candidates = null;
        controllerLock.lock();
        try {
            int size = connectionWindowSize;
            size += amount;
            if (size < 0)
                return false;
            connectionWindowSize = size;
            if (debug.on())
                debug.log("Connection window size is now %d (amount added %d)",
                          size, amount);

            // Notify waiting streams, until the new increased window size is
            // effectively exhausted.
            Iterator<Map.Entry<Integer,Map.Entry<Stream<?>,Integer>>> iter =
                    pending.entrySet().iterator();

            while (iter.hasNext() && size > 0) {
                Map.Entry<Integer,Map.Entry<Stream<?>,Integer>> item = iter.next();
                Integer streamSize = streams.get(item.getKey());
                if (streamSize == null) {
                    iter.remove();
                } else {
                    Map.Entry<Stream<?>,Integer> e = item.getValue();
                    int requestedAmount = e.getValue();
                    // only wakes up the pending streams for which there is
                    // at least 1 byte of space in both windows
                    int minAmount = 1;
                    if (size >= minAmount && streamSize >= minAmount) {
                        size -= Math.min(streamSize, requestedAmount);
                        iter.remove();
                        if (candidates == null)
                            candidates = new ArrayList<>();
                        candidates.add(e.getKey());
                    }
                }
            }
        } finally {
            controllerLock.unlock();
        }
        if (candidates != null) {
            candidates.forEach(Stream::signalWindowUpdate);
        }
        return true;
    }

    /**
     * Increases the Send Window size for the given stream.
     *
     * If the given stream is awaiting window size, from an unfulfilled
     * tryAcquire request, it will have its stream's {@link
     * Stream#signalWindowUpdate()} method scheduled to run ( i.e. awoken ).
     *
     * @return false if, and only if, the addition of the given amount would
     *         cause the Send Window to exceed 2^31-1 (overflow), otherwise true
     */
    boolean increaseStreamWindow(int amount, int streamid) {
        Stream<?> s = null;
        controllerLock.lock();
        try {
            Integer size = streams.get(streamid);
            if (size == null) {
                // The stream may have been cancelled.
                if (debug.on())
                    debug.log("WARNING: No entry found for streamid: %s. May be cancelled?",
                              streamid);
            } else {
                int prev = size;
                size = prev + amount;
                if (size < prev)
                    return false;
                streams.put(streamid, size);
                if (debug.on())
                    debug.log("Stream %s window size is now %s (amount added %d)",
                              streamid, size, amount);

                Map.Entry<Stream<?>, Integer> p = pending.get(streamid);
                if (p != null) {
                    int minAmount = 1;
                    // only wakes up the pending stream if there is at least
                    // 1 byte of space in both windows
                    if (size >= minAmount
                            && connectionWindowSize >= minAmount) {
                        pending.remove(streamid);
                        s = p.getKey();
                    }
                }
            }
        } finally {
            controllerLock.unlock();
        }

        if (s != null)
            s.signalWindowUpdate();

        return true;
    }

    /**
     * Adjusts, either increases or decreases, the active streams registered
     * with this controller.  May result in a stream's Send Window size becoming
     * negative.
     */
    void adjustActiveStreams(int adjustAmount) {
        assert adjustAmount != 0;

        controllerLock.lock();
        try {
            for (Map.Entry<Integer,Integer> entry : streams.entrySet()) {
                int streamid = entry.getKey();
                // the API only supports sending on Streams initialed by
                // the client, i.e. odd stream numbers
                if (streamid != 0 && (streamid % 2) != 0) {
                    Integer size = entry.getValue();
                    size += adjustAmount;
                    streams.put(streamid, size);
                    if (debug.on())
                        debug.log("Stream %s window size is now %s (adjusting amount %d)",
                                  streamid, size, adjustAmount);
                }
            }
        } finally {
            controllerLock.unlock();
        }
    }

    /** Returns the Send Window size for the connection. */
    int connectionWindowSize() {
        controllerLock.lock();
        try {
            return connectionWindowSize;
        } finally {
            controllerLock.unlock();
        }
    }

    /** Returns the Send Window size for the given stream. */
    int streamWindowSize(int streamid) {
        controllerLock.lock();
        try {
            Integer size = streams.get(streamid);
            if (size == null)
                throw new InternalError("Expected entry for streamid: " + streamid);
            return size;
        } finally {
            controllerLock.unlock();
        }
    }

}
