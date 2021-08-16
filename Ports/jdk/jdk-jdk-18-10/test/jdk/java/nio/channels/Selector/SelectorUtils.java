/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.nio.channels.Selector;

public class SelectorUtils {

    /**
     * tell if the monitor of an Object is held by a Thread.
     * @param t    the Thread to hold the monitor of the selected-key set
     * @param lock the Object
     * @return
     */
    public static boolean mightHoldLock(Thread t, Object lock) {
        long tid = t.getId();
        int hash = System.identityHashCode(lock);
        ThreadInfo ti = ManagementFactory.getThreadMXBean().
                getThreadInfo(new long[]{ tid} , true, false, 100)[0];
        if (ti != null) {
            for (MonitorInfo mi : ti.getLockedMonitors()) {
                if (mi.getIdentityHashCode() == hash)
                    return true;
            }
        }
        return false;
    }

    /**
     * Spin until the monitor of the selected-key set is likely held
     * as selected operations are specified to synchronize on the
     * selected-key set.
     * @param t   the Thread to hold the monitor of the selected-key set
     * @param sel the Selector
     * @throws Exception
     */
    public static void spinUntilLocked(Thread t, Selector sel) throws Exception {
        while (!mightHoldLock(t, sel.selectedKeys())) {
            Thread.sleep(50);
        }
    }
}
