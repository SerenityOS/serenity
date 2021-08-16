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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import sun.nio.ch.Interruptible;
import sun.nio.ch.SelectorImpl;


/**
 * Base implementation class for selectors.
 *
 * <p> This class encapsulates the low-level machinery required to implement
 * the interruption of selection operations.  A concrete selector class must
 * invoke the {@link #begin begin} and {@link #end end} methods before and
 * after, respectively, invoking an I/O operation that might block
 * indefinitely.  In order to ensure that the {@link #end end} method is always
 * invoked, these methods should be used within a
 * {@code try}&nbsp;...&nbsp;{@code finally} block:
 *
 * <blockquote><pre id="be">
 * try {
 *     begin();
 *     // Perform blocking I/O operation here
 *     ...
 * } finally {
 *     end();
 * }</pre></blockquote>
 *
 * <p> This class also defines methods for maintaining a selector's
 * cancelled-key set and for removing a key from its channel's key set, and
 * declares the abstract {@link #register register} method that is invoked by a
 * selectable channel's {@link AbstractSelectableChannel#register register}
 * method in order to perform the actual work of registering a channel.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class AbstractSelector
    extends Selector
{
    private static final VarHandle CLOSED;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            CLOSED = l.findVarHandle(AbstractSelector.class, "closed", boolean.class);
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
    private volatile boolean closed;

    // The provider that created this selector
    private final SelectorProvider provider;

    // cancelled-key, not used by the JDK Selector implementations
    private final Set<SelectionKey> cancelledKeys;

    /**
     * Initializes a new instance of this class.
     *
     * @param  provider
     *         The provider that created this selector
     */
    protected AbstractSelector(SelectorProvider provider) {
        this.provider = provider;
        if (this instanceof SelectorImpl) {
            // not used in JDK Selector implementations
            this.cancelledKeys = Set.of();
        } else {
            this.cancelledKeys = new HashSet<>();
        }
    }

    void cancel(SelectionKey k) {                       // package-private
        synchronized (cancelledKeys) {
            cancelledKeys.add(k);
        }
    }

    /**
     * Closes this selector.
     *
     * <p> If the selector has already been closed then this method returns
     * immediately.  Otherwise it marks the selector as closed and then invokes
     * the {@link #implCloseSelector implCloseSelector} method in order to
     * complete the close operation.  </p>
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public final void close() throws IOException {
        boolean changed = (boolean) CLOSED.compareAndSet(this, false, true);
        if (changed) {
            implCloseSelector();
        }
    }

    /**
     * Closes this selector.
     *
     * <p> This method is invoked by the {@link #close close} method in order
     * to perform the actual work of closing the selector.  This method is only
     * invoked if the selector has not yet been closed, and it is never invoked
     * more than once.
     *
     * <p> An implementation of this method must arrange for any other thread
     * that is blocked in a selection operation upon this selector to return
     * immediately as if by invoking the {@link
     * java.nio.channels.Selector#wakeup wakeup} method. </p>
     *
     * @throws  IOException
     *          If an I/O error occurs while closing the selector
     */
    protected abstract void implCloseSelector() throws IOException;

    public final boolean isOpen() {
        return !closed;
    }

    /**
     * Returns the provider that created this channel.
     *
     * @return  The provider that created this channel
     */
    public final SelectorProvider provider() {
        return provider;
    }

    /**
     * Retrieves this selector's cancelled-key set.
     *
     * <p> This set should only be used while synchronized upon it.  </p>
     *
     * @return  The cancelled-key set
     */
    protected final Set<SelectionKey> cancelledKeys() {
        return cancelledKeys;
    }

    /**
     * Registers the given channel with this selector.
     *
     * <p> This method is invoked by a channel's {@link
     * AbstractSelectableChannel#register register} method in order to perform
     * the actual work of registering the channel with this selector.  </p>
     *
     * @param  ch
     *         The channel to be registered
     *
     * @param  ops
     *         The initial interest set, which must be valid
     *
     * @param  att
     *         The initial attachment for the resulting key
     *
     * @return  A new key representing the registration of the given channel
     *          with this selector
     */
    protected abstract SelectionKey register(AbstractSelectableChannel ch,
                                             int ops, Object att);

    /**
     * Removes the given key from its channel's key set.
     *
     * <p> This method must be invoked by the selector for each channel that it
     * deregisters.  </p>
     *
     * @param  key
     *         The selection key to be removed
     */
    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel)key.channel()).removeKey(key);
    }


    // -- Interruption machinery --

    private Interruptible interruptor = null;

    /**
     * Marks the beginning of an I/O operation that might block indefinitely.
     *
     * <p> This method should be invoked in tandem with the {@link #end end}
     * method, using a {@code try}&nbsp;...&nbsp;{@code finally} block as
     * shown <a href="#be">above</a>, in order to implement interruption for
     * this selector.
     *
     * <p> Invoking this method arranges for the selector's {@link
     * Selector#wakeup wakeup} method to be invoked if a thread's {@link
     * Thread#interrupt interrupt} method is invoked while the thread is
     * blocked in an I/O operation upon the selector.  </p>
     */
    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                    public void interrupt(Thread ignore) {
                        AbstractSelector.this.wakeup();
                    }};
        }
        AbstractInterruptibleChannel.blockedOn(interruptor);
        Thread me = Thread.currentThread();
        if (me.isInterrupted())
            interruptor.interrupt(me);
    }

    /**
     * Marks the end of an I/O operation that might block indefinitely.
     *
     * <p> This method should be invoked in tandem with the {@link #begin begin}
     * method, using a {@code try}&nbsp;...&nbsp;{@code finally} block as
     * shown <a href="#be">above</a>, in order to implement interruption for
     * this selector.  </p>
     */
    protected final void end() {
        AbstractInterruptibleChannel.blockedOn(null);
    }

}
