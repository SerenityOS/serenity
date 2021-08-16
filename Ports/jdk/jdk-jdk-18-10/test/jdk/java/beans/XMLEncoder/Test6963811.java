/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6963811
 * @summary Tests deadlock in Encoder
 * @author Sergey Malenkov
 */

import java.beans.Encoder;
import java.beans.DefaultPersistenceDelegate;

public class Test6963811 implements Runnable {
    private static final Encoder ENCODER = new Encoder();
    private final long time;
    private final boolean sync;

    public Test6963811(long time, boolean sync) {
        this.time = time;
        this.sync = sync;
    }

    public void run() {
        try {
            Thread.sleep(this.time); // increase the chance of the deadlock
            if (this.sync) {
                synchronized (Test6963811.class) {
                    ENCODER.getPersistenceDelegate(Super.class);
                }
            }
            else {
                ENCODER.getPersistenceDelegate(Sub.class);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Thread[] threads = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Test6963811(0L, i > 0));
            threads[i].start();
            Thread.sleep(500L); // increase the chance of the deadlock
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    public static class Super {
    }

    public static class Sub extends Super {
    }

    public static class SubPersistenceDelegate extends DefaultPersistenceDelegate {
        public SubPersistenceDelegate() {
            new Test6963811(1000L, true).run();
        }
    }
}
