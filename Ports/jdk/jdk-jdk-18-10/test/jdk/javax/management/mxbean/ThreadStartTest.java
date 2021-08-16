/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231666
 * @summary Test checks that new threads could be successfully started when the thread
 * table introduced in 8185005 is growing. The test enables the thread table by calling
 * ThreadMXBean.getThreadInfo() and then creates a number of threads to force the thread
 * table to grow.
 *
 * @run main ThreadStartTest
 */

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class ThreadStartTest {
    public static void main(String[] args) {

        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        // Enable thread table
        mbean.getThreadInfo(Thread.currentThread().getId());

        // Create a large number of threads to make the thread table grow
        for (int i = 0; i < 1000; i++) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            });
            t.start();
        }
    }
}
