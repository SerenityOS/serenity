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

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Base Selector implementation class.
 */

public abstract class SelectorImpl
    extends AbstractSelector
{
    // The set of keys registered with this Selector
    private final Set<SelectionKey> keys;

    // The set of keys with data ready for an operation
    private final Set<SelectionKey> selectedKeys;

    // Public views of the key sets
    private final Set<SelectionKey> publicKeys;             // Immutable
    private final Set<SelectionKey> publicSelectedKeys;     // Removal allowed, but not addition

    // pending cancelled keys for deregistration
    private final Deque<SelectionKeyImpl> cancelledKeys = new ArrayDeque<>();

    // used to check for reentrancy
    private boolean inSelect;

    protected SelectorImpl(SelectorProvider sp) {
        super(sp);
        keys = ConcurrentHashMap.newKeySet();
        selectedKeys = new HashSet<>();
        publicKeys = Collections.unmodifiableSet(keys);
        publicSelectedKeys = Util.ungrowableSet(selectedKeys);
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    @Override
    public final Set<SelectionKey> keys() {
        ensureOpen();
        return publicKeys;
    }

    @Override
    public final Set<SelectionKey> selectedKeys() {
        ensureOpen();
        return publicSelectedKeys;
    }

    /**
     * Marks the beginning of a select operation that might block
     */
    protected final void begin(boolean blocking) {
        if (blocking) begin();
    }

    /**
     * Marks the end of a select operation that may have blocked
     */
    protected final void end(boolean blocking) {
        if (blocking) end();
    }

    /**
     * Selects the keys for channels that are ready for I/O operations.
     *
     * @param action  the action to perform, can be null
     * @param timeout timeout in milliseconds to wait, 0 to not wait, -1 to
     *                wait indefinitely
     */
    protected abstract int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException;

    private int lockAndDoSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        synchronized (this) {
            ensureOpen();
            if (inSelect)
                throw new IllegalStateException("select in progress");
            inSelect = true;
            try {
                synchronized (publicSelectedKeys) {
                    return doSelect(action, timeout);
                }
            } finally {
                inSelect = false;
            }
        }
    }

    @Override
    public final int select(long timeout) throws IOException {
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout");
        return lockAndDoSelect(null, (timeout == 0) ? -1 : timeout);
    }

    @Override
    public final int select() throws IOException {
        return lockAndDoSelect(null, -1);
    }

    @Override
    public final int selectNow() throws IOException {
        return lockAndDoSelect(null, 0);
    }

    @Override
    public final int select(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        Objects.requireNonNull(action);
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout");
        return lockAndDoSelect(action, (timeout == 0) ? -1 : timeout);
    }

    @Override
    public final int select(Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action);
        return lockAndDoSelect(action, -1);
    }

    @Override
    public final int selectNow(Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action);
        return lockAndDoSelect(action, 0);
    }

    /**
     * Invoked by implCloseSelector to close the selector.
     */
    protected abstract void implClose() throws IOException;

    @Override
    public final void implCloseSelector() throws IOException {
        wakeup();
        synchronized (this) {
            implClose();
            synchronized (publicSelectedKeys) {
                // Deregister channels
                Iterator<SelectionKey> i = keys.iterator();
                while (i.hasNext()) {
                    SelectionKeyImpl ski = (SelectionKeyImpl)i.next();
                    deregister(ski);
                    SelectableChannel selch = ski.channel();
                    if (!selch.isOpen() && !selch.isRegistered())
                        ((SelChImpl)selch).kill();
                    selectedKeys.remove(ski);
                    i.remove();
                }
                assert selectedKeys.isEmpty() && keys.isEmpty();
            }
        }
    }

    @Override
    protected final SelectionKey register(AbstractSelectableChannel ch,
                                          int ops,
                                          Object attachment)
    {
        if (!(ch instanceof SelChImpl))
            throw new IllegalSelectorException();
        SelectionKeyImpl k = new SelectionKeyImpl((SelChImpl)ch, this);
        if (attachment != null)
            k.attach(attachment);

        // register (if needed) before adding to key set
        implRegister(k);

        // add to the selector's key set, removing it immediately if the selector
        // is closed. The key is not in the channel's key set at this point but
        // it may be observed by a thread iterating over the selector's key set.
        keys.add(k);
        try {
            k.interestOps(ops);
        } catch (ClosedSelectorException e) {
            assert ch.keyFor(this) == null;
            keys.remove(k);
            k.cancel();
            throw e;
        }
        return k;
    }

    /**
     * Register the key in the selector.
     *
     * The default implementation checks if the selector is open. It should
     * be overridden by selector implementations as needed.
     */
    protected void implRegister(SelectionKeyImpl ski) {
        ensureOpen();
    }

    /**
     * Removes the key from the selector
     */
    protected abstract void implDereg(SelectionKeyImpl ski) throws IOException;

    /**
     * Queue a cancelled key for the next selection operation
     */
    public void cancel(SelectionKeyImpl ski) {
        synchronized (cancelledKeys) {
            cancelledKeys.addLast(ski);
        }
    }

    /**
     * Invoked by selection operations to process the cancelled keys
     */
    protected final void processDeregisterQueue() throws IOException {
        assert Thread.holdsLock(this);
        assert Thread.holdsLock(publicSelectedKeys);

        synchronized (cancelledKeys) {
            SelectionKeyImpl ski;
            while ((ski = cancelledKeys.pollFirst()) != null) {
                // remove the key from the selector
                implDereg(ski);

                selectedKeys.remove(ski);
                keys.remove(ski);

                // remove from channel's key set
                deregister(ski);

                SelectableChannel ch = ski.channel();
                if (!ch.isOpen() && !ch.isRegistered())
                    ((SelChImpl)ch).kill();
            }
        }
    }

    /**
     * Invoked by selection operations to handle ready events. If an action
     * is specified then it is invoked to handle the key, otherwise the key
     * is added to the selected-key set (or updated when it is already in the
     * set).
     */
    protected final int processReadyEvents(int rOps,
                                           SelectionKeyImpl ski,
                                           Consumer<SelectionKey> action) {
        if (action != null) {
            ski.translateAndSetReadyOps(rOps);
            if ((ski.nioReadyOps() & ski.nioInterestOps()) != 0) {
                action.accept(ski);
                ensureOpen();
                return 1;
            }
        } else {
            assert Thread.holdsLock(publicSelectedKeys);
            if (selectedKeys.contains(ski)) {
                if (ski.translateAndUpdateReadyOps(rOps)) {
                    return 1;
                }
            } else {
                ski.translateAndSetReadyOps(rOps);
                if ((ski.nioReadyOps() & ski.nioInterestOps()) != 0) {
                    selectedKeys.add(ski);
                    return 1;
                }
            }
        }
        return 0;
    }

    /**
     * Invoked by interestOps to ensure the interest ops are updated at the
     * next selection operation.
     */
    protected abstract void setEventOps(SelectionKeyImpl ski);
}
