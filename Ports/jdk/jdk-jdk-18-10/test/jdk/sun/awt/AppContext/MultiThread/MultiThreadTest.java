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
 * @bug 8019623
 * @summary Tests that AppContext.getAppContext() works correctly in multi-threads scenario.
 * @author Leonid Romanov
 * @modules java.desktop/sun.awt
 */

import sun.awt.AppContext;

public class MultiThreadTest {
    private static final int NUM_THREADS = 2;

    private static AppContextGetter[] getters = new AppContextGetter[NUM_THREADS];

    public static void main(String[] args) {
        createAndStartThreads();
        compareAppContexts();
    }

    private static void createAndStartThreads() {
        ThreadGroup systemGroup = getSystemThreadGroup();
        for (int i = 0; i < NUM_THREADS; ++i) {
            ThreadGroup tg = new ThreadGroup(systemGroup, "AppContextGetter" + i);
            getters[i] = new AppContextGetter(tg);
        }

        for (int i = 0; i < NUM_THREADS; ++i) {
            getters[i].start();
        }

        for (int i = 0; i < NUM_THREADS; ++i) {
            try {
                getters[i].join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private static ThreadGroup getSystemThreadGroup() {
        ThreadGroup currentThreadGroup =
                Thread.currentThread().getThreadGroup();
        ThreadGroup parentThreadGroup = currentThreadGroup.getParent();
        while (parentThreadGroup != null) {
            currentThreadGroup = parentThreadGroup;
            parentThreadGroup = currentThreadGroup.getParent();
        }

        return currentThreadGroup;
    }

    private static void compareAppContexts() {
        AppContext ctx = getters[0].getAppContext();
        for (int i = 1; i < NUM_THREADS; ++i) {
            if (!ctx.equals(getters[i].getAppContext())) {
                throw new RuntimeException("Unexpected AppContexts difference, could be a race condition");
            }
        }
    }

    private static class AppContextGetter extends Thread {
        private AppContext appContext;

        public AppContextGetter(ThreadGroup tg) {
            super(tg, tg.getName());
        }

        AppContext getAppContext() {
            return appContext;
        }

        @Override
        public void run() {
            appContext = AppContext.getAppContext();
        }
    }
}
