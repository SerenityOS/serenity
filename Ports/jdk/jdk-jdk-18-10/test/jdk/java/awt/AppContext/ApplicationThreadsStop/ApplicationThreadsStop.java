/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Robot;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

/**
 * @test
 * @key headful
 * @bug 8136858
 * @modules java.desktop/sun.awt
 * @run main/othervm/java.security.policy=java.policy -Djava.security.manager ApplicationThreadsStop
 */
public final class ApplicationThreadsStop implements Runnable {

    private static AppContext contextToDispose;
    private static Thread thread;

    public static void main(final String[] args) throws Exception {
        ThreadGroup tg = new ThreadGroup("TestThreadGroup");
        Thread t = new Thread(tg, new ApplicationThreadsStop());
        t.start();
        t.join();
        contextToDispose.dispose();
        // wait for appcontext to be destroyed
        Thread.sleep(10000);
        if(thread.isAlive()){
            throw new RuntimeException("Thread is alive");
        }
    }

    @Override
    public void run() {
        contextToDispose = SunToolkit.createNewAppContext();
        Frame f = new Frame();
        f.setSize(300, 300);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        thread = new Thread(() -> {
            while(true);
        });
        thread.start();
        sync();
    }

    private static void sync() {
        try {
            new Robot().waitForIdle();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
}
