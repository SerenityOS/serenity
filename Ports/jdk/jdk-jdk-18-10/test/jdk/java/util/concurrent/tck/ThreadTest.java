/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import junit.framework.Test;
import junit.framework.TestSuite;

public class ThreadTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(ThreadTest.class);
    }

    static class MyHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * getUncaughtExceptionHandler returns ThreadGroup unless set,
     * otherwise returning value of last setUncaughtExceptionHandler.
     */
    public void testGetAndSetUncaughtExceptionHandler() {
        // these must be done all at once to avoid state
        // dependencies across tests
        Thread current = Thread.currentThread();
        ThreadGroup tg = current.getThreadGroup();
        MyHandler eh = new MyHandler();
        assertSame(tg, current.getUncaughtExceptionHandler());
        current.setUncaughtExceptionHandler(eh);
        try {
            assertSame(eh, current.getUncaughtExceptionHandler());
        } finally {
            current.setUncaughtExceptionHandler(null);
        }
        assertSame(tg, current.getUncaughtExceptionHandler());
    }

    /**
     * getDefaultUncaughtExceptionHandler returns value of last
     * setDefaultUncaughtExceptionHandler.
     */
    public void testGetAndSetDefaultUncaughtExceptionHandler() {
        assertNull(Thread.getDefaultUncaughtExceptionHandler());
        // failure due to SecurityException is OK.
        // Would be nice to explicitly test both ways, but cannot yet.
        Thread.UncaughtExceptionHandler defaultHandler
            = Thread.getDefaultUncaughtExceptionHandler();
        MyHandler eh = new MyHandler();
        try {
            Thread.setDefaultUncaughtExceptionHandler(eh);
            try {
                assertSame(eh, Thread.getDefaultUncaughtExceptionHandler());
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
            }
        } catch (SecurityException ok) {
            assertNotNull(System.getSecurityManager());
        }
        assertSame(defaultHandler, Thread.getDefaultUncaughtExceptionHandler());
    }

    // How to test actually using UEH within junit?

}
