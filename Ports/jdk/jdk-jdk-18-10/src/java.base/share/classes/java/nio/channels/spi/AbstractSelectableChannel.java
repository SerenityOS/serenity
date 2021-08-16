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

package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.function.Consumer;


/**
 * Base implementation class for selectable channels.
 *
 * <p> This class defines methods that handle the mechanics of channel
 * registration, deregistration, and closing.  It maintains the current
 * blocking mode of this channel as well as its current set of selection keys.
 * It performs all of the synchronization required to implement the {@link
 * java.nio.channels.SelectableChannel} specification.  Implementations of the
 * protected abstract methods defined in this class need not synchronize
 * against other threads that might be engaged in the same operations.  </p>
 *
 *
 * @author Mark Reinhold
 * @author Mike McCloskey
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class AbstractSelectableChannel
    extends SelectableChannel
{

    // The provider that created this channel
    private final SelectorProvider provider;

    // Keys that have been created by registering this channel with selectors.
    // They are saved because if this channel is closed the keys must be
    // deregistered.  Protected by keyLock.
    //
    private SelectionKey[] keys = null;
    private int keyCount = 0;

    // Lock for key set and count
    private final Object keyLock = new Object();

    // Lock for registration and configureBlocking operations
    private final Object regLock = new Object();

    // True when non-blocking, need regLock to change;
    private volatile boolean nonBlocking;

    /**
     * Initializes a new instance of this class.
     *
     * @param  provider
     *         The provider that created this channel
     */
    protected AbstractSelectableChannel(SelectorProvider provider) {
        this.provider = provider;
    }

    /**
     * Returns the provider that created this channel.
     *
     * @return  The provider that created this channel
     */
    public final SelectorProvider provider() {
        return provider;
    }


    // -- Utility methods for the key set --

    private void addKey(SelectionKey k) {
        assert Thread.holdsLock(keyLock);
        int i = 0;
        if ((keys != null) && (keyCount < keys.length)) {
            // Find empty element of key array
            for (i = 0; i < keys.length; i++)
                if (keys[i] == null)
                    break;
        } else if (keys == null) {
            keys = new SelectionKey[2];
        } else {
            // Grow key array
            int n = keys.length * 2;
            SelectionKey[] ks =  new SelectionKey[n];
            for (i = 0; i < keys.length; i++)
                ks[i] = keys[i];
            keys = ks;
            i = keyCount;
        }
        keys[i] = k;
        keyCount++;
    }

    private SelectionKey findKey(Selector sel) {
        assert Thread.holdsLock(keyLock);
        if (keys == null)
            return null;
        for (int i = 0; i < keys.length; i++)
            if ((keys[i] != null) && (keys[i].selector() == sel))
                return keys[i];
        return null;

    }

    void removeKey(SelectionKey k) {                    // package-private
        synchronized (keyLock) {
            for (int i = 0; i < keys.length; i++)
                if (keys[i] == k) {
                    keys[i] = null;
                    keyCount--;
                }
            ((AbstractSelectionKey)k).invalidate();
        }
    }

    private boolean haveValidKeys() {
        synchronized (keyLock) {
            if (keyCount == 0)
                return false;
            for (int i = 0; i < keys.length; i++) {
                if ((keys[i] != null) && keys[i].isValid())
                    return true;
            }
            return false;
        }
    }


    // -- Registration --

    public final boolean isRegistered() {
        synchronized (keyLock) {
            return keyCount != 0;
        }
    }

    public final SelectionKey keyFor(Selector sel) {
        synchronized (keyLock) {
            return findKey(sel);
        }
    }

    /**
     * Invokes an action for each key.
     *
     * This method is invoked by DatagramChannelImpl::disconnect.
     */
    private void forEach(Consumer<SelectionKey> action) {
        synchronized (keyLock) {
            SelectionKey[] keys = this.keys;
            if (keys != null) {
                Arrays.stream(keys).filter(k -> k != null).forEach(action::accept);
            }
        }
    }

    /**
     * Registers this channel with the given selector, returning a selection key.
     *
     * <p>  This method first verifies that this channel is open and that the
     * given initial interest set is valid.
     *
     * <p> If this channel is already registered with the given selector then
     * the selection key representing that registration is returned after
     * setting its interest set to the given value.
     *
     * <p> Otherwise this channel has not yet been registered with the given
     * selector, so the {@link AbstractSelector#register register} method of
     * the selector is invoked while holding the appropriate locks.  The
     * resulting key is added to this channel's key set before being returned.
     * </p>
     *
     * @throws  ClosedSelectorException {@inheritDoc}
     *
     * @throws  IllegalBlockingModeException {@inheritDoc}
     *
     * @throws  IllegalSelectorException {@inheritDoc}
     *
     * @throws  CancelledKeyException {@inheritDoc}
     *
     * @throws  IllegalArgumentException {@inheritDoc}
     */
    public final SelectionKey register(Selector sel, int ops, Object att)
        throws ClosedChannelException
    {
        if ((ops & ~validOps()) != 0)
            throw new IllegalArgumentException();
        if (!isOpen())
            throw new ClosedChannelException();
        synchronized (regLock) {
            if (isBlocking())
                throw new IllegalBlockingModeException();
            synchronized (keyLock) {
                // re-check if channel has been closed
                if (!isOpen())
                    throw new ClosedChannelException();
                SelectionKey k = findKey(sel);
                if (k != null) {
                    k.attach(att);
                    k.interestOps(ops);
                } else {
                    // New registration
                    k = ((AbstractSelector)sel).register(this, ops, att);
                    addKey(k);
                }
                return k;
            }
        }
    }


    // -- Closing --

    /**
     * Closes this channel.
     *
     * <p> This method, which is specified in the {@link
     * AbstractInterruptibleChannel} class and is invoked by the {@link
     * java.nio.channels.Channel#close close} method, in turn invokes the
     * {@link #implCloseSelectableChannel implCloseSelectableChannel} method in
     * order to perform the actual work of closing this channel.  It then
     * cancels all of this channel's keys.  </p>
     */
    protected final void implCloseChannel() throws IOException {
        implCloseSelectableChannel();

        // clone keys to avoid calling cancel when holding keyLock
        SelectionKey[] copyOfKeys = null;
        synchronized (keyLock) {
            if (keys != null) {
                copyOfKeys = keys.clone();
            }
        }

        if (copyOfKeys != null) {
            for (SelectionKey k : copyOfKeys) {
                if (k != null) {
                    k.cancel();   // invalidate and adds key to cancelledKey set
                }
            }
        }
    }

    /**
     * Closes this selectable channel.
     *
     * <p> This method is invoked by the {@link java.nio.channels.Channel#close
     * close} method in order to perform the actual work of closing the
     * channel.  This method is only invoked if the channel has not yet been
     * closed, and it is never invoked more than once.
     *
     * <p> An implementation of this method must arrange for any other thread
     * that is blocked in an I/O operation upon this channel to return
     * immediately, either by throwing an exception or by returning normally.
     * </p>
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    protected abstract void implCloseSelectableChannel() throws IOException;


    // -- Blocking --

    public final boolean isBlocking() {
        return !nonBlocking;
    }

    public final Object blockingLock() {
        return regLock;
    }

    /**
     * Adjusts this channel's blocking mode.
     *
     * <p> If the given blocking mode is different from the current blocking
     * mode then this method invokes the {@link #implConfigureBlocking
     * implConfigureBlocking} method, while holding the appropriate locks, in
     * order to change the mode.  </p>
     */
    public final SelectableChannel configureBlocking(boolean block)
        throws IOException
    {
        synchronized (regLock) {
            if (!isOpen())
                throw new ClosedChannelException();
            boolean blocking = !nonBlocking;
            if (block != blocking) {
                if (block && haveValidKeys())
                    throw new IllegalBlockingModeException();
                implConfigureBlocking(block);
                nonBlocking = !block;
            }
        }
        return this;
    }

    /**
     * Adjusts this channel's blocking mode.
     *
     * <p> This method is invoked by the {@link #configureBlocking
     * configureBlocking} method in order to perform the actual work of
     * changing the blocking mode.  This method is only invoked if the new mode
     * is different from the current mode.  </p>
     *
     * @param  block  If {@code true} then this channel will be placed in
     *                blocking mode; if {@code false} then it will be placed
     *                non-blocking mode
     *
     * @throws IOException
     *         If an I/O error occurs
     */
    protected abstract void implConfigureBlocking(boolean block)
        throws IOException;

}
