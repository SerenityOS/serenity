/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.windows;

import java.util.Map;
import java.util.WeakHashMap;

abstract class WObjectPeer {

    static {
        initIDs();
    }

    // The Windows handle for the native widget.
    volatile long pData;
    // if the native peer has been destroyed
    private volatile boolean destroyed;
    // The associated AWT object.
    volatile Object target;

    private volatile boolean disposed;

    // set from JNI if any errors in creating the peer occur
    volatile Error createError = null;

    // used to synchronize the state of this peer
    private final Object stateLock = new Object();

    private volatile Map<WObjectPeer, WObjectPeer> childPeers;

    public static WObjectPeer getPeerForTarget(Object t) {
        WObjectPeer peer = (WObjectPeer) WToolkit.targetToPeer(t);
        return peer;
    }

    public long getData() {
        return pData;
    }

    public Object getTarget() {
        return target;
    }

    public final Object getStateLock() {
        return stateLock;
    }

    /*
     * Subclasses should override disposeImpl() instead of dispose(). Client
     * code should always invoke dispose(), never disposeImpl().
     */
    protected abstract void disposeImpl();
    public final void dispose() {
        boolean call_disposeImpl = false;

        synchronized (this) {
            if (!disposed) {
                disposed = call_disposeImpl = true;
            }
        }

        if (call_disposeImpl) {
            if (childPeers != null) {
                disposeChildPeers();
            }
            disposeImpl();
        }
    }
    protected final boolean isDisposed() {
        return disposed;
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    // if a child peer existence depends on this peer, add it to this collection
    final void addChildPeer(WObjectPeer child) {
        synchronized (getStateLock()) {
            if (childPeers == null) {
                childPeers = new WeakHashMap<>();
            }
            if (isDisposed()) {
                throw new IllegalStateException("Parent peer is disposed");
            }
            childPeers.put(child, this);
        }
    }

    // called to dispose dependent child peers
    private void disposeChildPeers() {
        synchronized (getStateLock()) {
            for (WObjectPeer child : childPeers.keySet()) {
                if (child != null) {
                    try {
                        child.dispose();
                    }
                    catch (Exception e) {
                        // ignored
                    }
                }
            }
        }
    }
}
