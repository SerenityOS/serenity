/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.management;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * A few utility methods to use ThreadMXBean.
 */
public final class ThreadMXBeanTool {

    /**
     * Waits until {@link Thread} is in the certain {@link Thread.State}
     * and blocking on {@code object}.
     *
     * @param state The thread state
     * @param object The object to block on
     */
    public static void waitUntilBlockingOnObject(Thread thread, Thread.State state, Object object)
        throws InterruptedException {
        String want = object == null ? null : object.getClass().getName() + '@'
                + Integer.toHexString(System.identityHashCode(object));
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        while (thread.isAlive()) {
            ThreadInfo ti = tmx.getThreadInfo(thread.getId());
            if (ti.getThreadState() == state
                    && (want == null || want.equals(ti.getLockName()))) {
                return;
            }
            Thread.sleep(1);
        }
    }

    /**
     * Waits until {@link Thread} is in native.
     */
    public static void waitUntilInNative(Thread thread) throws InterruptedException {
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        while (thread.isAlive()) {
            ThreadInfo ti = tmx.getThreadInfo(thread.getId());
            if (ti.isInNative()) {
                return;
            }
            Thread.sleep(1);
        }
    }

}
