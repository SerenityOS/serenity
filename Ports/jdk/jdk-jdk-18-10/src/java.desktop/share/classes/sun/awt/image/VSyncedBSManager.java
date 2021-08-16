/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.image;

import java.awt.image.BufferStrategy;
import java.lang.ref.WeakReference;

/**
 * Manages v-synced buffer strategies.
 */
public abstract class VSyncedBSManager {

    private static VSyncedBSManager theInstance;

    @SuppressWarnings("removal")
    private static final boolean vSyncLimit =
        Boolean.valueOf(java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction(
                    "sun.java2d.vsynclimit", "true")));

    private static VSyncedBSManager getInstance(boolean create) {
        if (theInstance == null && create) {
            theInstance =
                vSyncLimit ? new SingleVSyncedBSMgr() : new NoLimitVSyncBSMgr();
        }
        return theInstance;
    }

    abstract boolean checkAllowed(BufferStrategy bs);
    abstract void relinquishVsync(BufferStrategy bs);

    /**
     * Returns true if the buffer strategy is allowed to be created
     * v-synced.
     *
     * @return true if the bs is allowed to be v-synced, false otherwise
     */
    public static boolean vsyncAllowed(BufferStrategy bs) {
        VSyncedBSManager bsm = getInstance(true);
        return bsm.checkAllowed(bs);
    }

    /**
     * Lets the manager know that this buffer strategy is no longer interested
     * in being v-synced.
     */
    public static synchronized void releaseVsync(BufferStrategy bs) {
        VSyncedBSManager bsm = getInstance(false);
        if (bsm != null) {
            bsm.relinquishVsync(bs);
        }
    }

    /**
     * An instance of the manager which allows any buffer strategy to be
     * v-synced.
     */
    private static final class NoLimitVSyncBSMgr extends VSyncedBSManager {
        @Override
        boolean checkAllowed(BufferStrategy bs) {
            return true;
        }

        @Override
        void relinquishVsync(BufferStrategy bs) {
        }
    }

    /**
     * An instance of the manager which allows only one buffer strategy to
     * be v-synced at any give moment in the vm.
     */
    private static final class SingleVSyncedBSMgr extends VSyncedBSManager {
        private WeakReference<BufferStrategy> strategy;

        @Override
        public synchronized boolean checkAllowed(BufferStrategy bs) {
            if (strategy != null) {
                BufferStrategy current = strategy.get();
                if (current != null) {
                    return (current == bs);
                }
            }
            strategy = new WeakReference<BufferStrategy>(bs);
            return true;
        }

        @Override
        public synchronized void relinquishVsync(BufferStrategy bs) {
            if (strategy != null) {
                BufferStrategy b = strategy.get();
                if (b == bs) {
                    strategy.clear();
                    strategy = null;
                }
            }
        }
    }
}
