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

import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * Event handling interface from HttpClientImpl's selector.
 *
 * If REPEATING is set then the event is not cancelled after being posted.
 */
abstract class AsyncEvent {

    public static final int REPEATING = 0x2; // one off event if not set

    protected final int flags;

    AsyncEvent() {
        this(0);
    }

    AsyncEvent(int flags) {
        this.flags = flags;
    }

    /** Returns the channel */
    public abstract SelectableChannel channel();

    /** Returns the selector interest op flags OR'd */
    public abstract int interestOps();

    /** Called when event occurs */
    public abstract void handle();

    /**
     * Called when an error occurs during registration, or when the selector has
     * been shut down. Aborts all exchanges.
     *
     * @param ioe  the IOException, or null
     */
    public abstract void abort(IOException ioe);

    public boolean repeating() {
        return (flags & REPEATING) != 0;
    }
}
