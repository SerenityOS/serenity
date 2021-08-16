/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;


/**
 * An implementation of SelectionKey.
 */

public final class SelectionKeyImpl
    extends AbstractSelectionKey
{
    private static final VarHandle INTERESTOPS =
            ConstantBootstraps.fieldVarHandle(
                    MethodHandles.lookup(),
                    "interestOps",
                    VarHandle.class,
                    SelectionKeyImpl.class, int.class);

    private final SelChImpl channel;
    private final SelectorImpl selector;

    private volatile int interestOps;
    private volatile int readyOps;

    // registered events in kernel, used by some Selector implementations
    private int registeredEvents;

    // registered events need to be reset, used by some Selector implementations
    private volatile boolean reset;

    // index of key in pollfd array, used by some Selector implementations
    private int index;

    SelectionKeyImpl(SelChImpl ch, SelectorImpl sel) {
        channel = ch;
        selector = sel;
    }

    private void ensureValid() {
        if (!isValid())
            throw new CancelledKeyException();
    }

    FileDescriptor getFD() {
        return channel.getFD();
    }

    int getFDVal() {
        return channel.getFDVal();
    }

    @Override
    public SelectableChannel channel() {
        return (SelectableChannel)channel;
    }

    @Override
    public Selector selector() {
        return selector;
    }

    @Override
    public int interestOps() {
        ensureValid();
        return interestOps;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        ensureValid();
        if ((ops & ~channel().validOps()) != 0)
            throw new IllegalArgumentException();
        int oldOps = (int) INTERESTOPS.getAndSet(this, ops);
        if (ops != oldOps) {
            selector.setEventOps(this);
        }
        return this;
    }

    @Override
    public int interestOpsOr(int ops) {
        ensureValid();
        if ((ops & ~channel().validOps()) != 0)
            throw new IllegalArgumentException();
        int oldVal = (int) INTERESTOPS.getAndBitwiseOr(this, ops);
        if (oldVal != (oldVal | ops)) {
            selector.setEventOps(this);
        }
        return oldVal;
    }

    @Override
    public int interestOpsAnd(int ops) {
        ensureValid();
        int oldVal = (int) INTERESTOPS.getAndBitwiseAnd(this, ops);
        if (oldVal != (oldVal & ops)) {
            selector.setEventOps(this);
        }
        return oldVal;
    }

    @Override
    public int readyOps() {
        ensureValid();
        return readyOps;
    }

    // The nio versions of these operations do not care if a key
    // has been invalidated. They are for internal use by nio code.

    public void nioReadyOps(int ops) {
        readyOps = ops;
    }

    public int nioReadyOps() {
        return readyOps;
    }

    public SelectionKey nioInterestOps(int ops) {
        if ((ops & ~channel().validOps()) != 0)
            throw new IllegalArgumentException();
        interestOps = ops;
        selector.setEventOps(this);
        return this;
    }

    public int nioInterestOps() {
        return interestOps;
    }

    int translateInterestOps() {
        return channel.translateInterestOps(interestOps);
    }

    boolean translateAndSetReadyOps(int ops) {
        return channel.translateAndSetReadyOps(ops, this);
    }

    boolean translateAndUpdateReadyOps(int ops) {
        return channel.translateAndUpdateReadyOps(ops, this);
    }

    void registeredEvents(int events) {
        // assert Thread.holdsLock(selector);
        this.registeredEvents = events;
    }

    int registeredEvents() {
        // assert Thread.holdsLock(selector);
        return registeredEvents;
    }

    int getIndex() {
        return index;
    }

    void setIndex(int i) {
        index = i;
    }

    /**
     * Sets the reset flag, re-queues the key, and wakeups up the Selector
     */
    void reset() {
        reset = true;
        selector.setEventOps(this);
        selector.wakeup();
    }

    /**
     * Clears the reset flag, returning the previous value of the flag
     */
    boolean getAndClearReset() {
        assert Thread.holdsLock(selector);
        boolean r = reset;
        if (r)
            reset = false;
        return r;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("channel=")
          .append(channel)
          .append(", selector=")
          .append(selector);
        if (isValid()) {
            sb.append(", interestOps=")
              .append(interestOps)
              .append(", readyOps=")
              .append(readyOps);
        } else {
            sb.append(", invalid");
        }
        return sb.toString();
    }

    // used by Selector implementations to record when the key was selected
    int lastPolled;
}
