/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8012933
 * @summary Tests (although somewhat indirectly) that createNewAppContext()
 * immediately followed by dispose() works correctly
 * @author Leonid Romanov
 * @modules java.desktop/sun.awt
 */

import sun.awt.SunToolkit;
import sun.awt.AppContext;

public class Test8012933 {
    private AppContext appContext = null;
    final ThreadGroup threadGroup = new ThreadGroup("test thread group");
    final Object lock = new Object();
    boolean isCreated = false;

    public static void main(String[] args) throws Exception {
        SunToolkit.createNewAppContext();
        new Test8012933().test();
    }

    private void test() throws Exception {
        createAppContext();
        long startTime = System.currentTimeMillis();
        appContext.dispose();
        long endTime = System.currentTimeMillis();

        // In case of the bug, calling dispose() when there is no EQ
        // dispatch thread running fails to create it, so it takes
        // almost 10 sec to return from dispose(), which is spent
        // waiting on the notificationLock.
        if ((endTime - startTime) > 9000) {
            throw new RuntimeException("Returning from dispose() took too much time, probably a bug");
        }
    }

    private void createAppContext() {
        isCreated = false;
        final Runnable runnable = new Runnable() {
                public void run() {
                    appContext = SunToolkit.createNewAppContext();
                    synchronized (lock) {
                        isCreated = true;
                        lock.notifyAll();
                    }
                }
            };

        final Thread thread = new Thread(threadGroup, runnable, "creates app context");
        synchronized (lock) {
            thread.start();
            while (!isCreated) {
                try {
                    lock.wait();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        if (appContext == null) {
            throw new RuntimeException("failed to create app context.");
        } else {
            System.out.println("app context was created.");
        }
    }

}
