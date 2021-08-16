/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.nio.channels.Channel;
import java.io.FileDescriptor;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * An interface that allows translation (and more!).
 *
 * @since 1.4
 */

public interface SelChImpl extends Channel {

    FileDescriptor getFD();

    int getFDVal();

    /**
     * Adds the specified ops if present in interestOps. The specified
     * ops are turned on without affecting the other ops.
     *
     * @return  true iff the new value of sk.readyOps() set by this method
     *          contains at least one bit that the previous value did not
     *          contain
     */
    boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski);

    /**
     * Sets the specified ops if present in interestOps. The specified
     * ops are turned on, and all other ops are turned off.
     *
     * @return  true iff the new value of sk.readyOps() set by this method
     *          contains at least one bit that the previous value did not
     *          contain
     */
    boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski);

    /**
     * Translates an interest operation set into a native event set
     */
    int translateInterestOps(int ops);

    void kill() throws IOException;

    /**
     * Disables the current thread for scheduling purposes until this
     * channel is ready for I/O, or asynchronously closed, for up to the
     * specified waiting time.
     *
     * <p> This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park.
     *
     * @param event the event to poll
     * @param nanos the timeout to wait; {@code <= 0} to wait indefinitely
     */
    default void park(int event, long nanos) throws IOException {
        long millis;
        if (nanos <= 0) {
            millis = -1;
        } else {
            millis = NANOSECONDS.toMillis(nanos);
        }
        Net.poll(getFD(), event, millis);
    }

    /**
     * Disables the current thread for scheduling purposes until this
     * channel is ready for I/O, or asynchronously closed.
     *
     * <p> This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park.
     *
     * @param event the event to poll
     */
    default void park(int event) throws IOException {
        park(event, 0L);
    }

}
