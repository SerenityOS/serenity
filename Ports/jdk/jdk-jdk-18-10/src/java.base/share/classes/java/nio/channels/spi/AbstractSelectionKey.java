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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import sun.nio.ch.SelectionKeyImpl;
import sun.nio.ch.SelectorImpl;

/**
 * Base implementation class for selection keys.
 *
 * <p> This class tracks the validity of the key and implements cancellation.
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class AbstractSelectionKey
    extends SelectionKey
{
    private static final VarHandle INVALID;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            INVALID = l.findVarHandle(AbstractSelectionKey.class, "invalid", boolean.class);
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }

    /**
     * Initializes a new instance of this class.
     */
    protected AbstractSelectionKey() { }

    private volatile boolean invalid;

    public final boolean isValid() {
        return !invalid;
    }

    void invalidate() {                                 // package-private
        invalid = true;
    }

    /**
     * Cancels this key.
     *
     * <p> If this key has not yet been cancelled then it is added to its
     * selector's cancelled-key set while synchronized on that set.  </p>
     */
    public final void cancel() {
        boolean changed = (boolean) INVALID.compareAndSet(this, false, true);
        if (changed) {
            Selector sel = selector();
            if (sel instanceof SelectorImpl) {
                // queue cancelled key directly
                ((SelectorImpl) sel).cancel((SelectionKeyImpl) this);
            } else {
                ((AbstractSelector) sel).cancel(this);
            }
        }
    }
}
