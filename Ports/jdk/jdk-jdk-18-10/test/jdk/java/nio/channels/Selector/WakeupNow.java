/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4940372
 * @summary Ensure that the wakeup state is cleared by selectNow()
 */

import java.nio.channels.*;

public class WakeupNow {

    public static void main(String[] args) throws Exception {
        test1();
        test2();
    }

    // Test if selectNow clears wakeup with the wakeup fd and
    // another fd in the selector.
    // This fails before the fix on Linux
    private static void test1() throws Exception {
        Selector sel = Selector.open();
        Pipe p = Pipe.open();
        p.source().configureBlocking(false);
        p.source().register(sel, SelectionKey.OP_READ);
        sel.wakeup();
        // ensure wakeup is consumed by selectNow
        Thread.sleep(2000);
        sel.selectNow();
        long startTime = System.currentTimeMillis();
        int n = sel.select(2000);
        long endTime = System.currentTimeMillis();
        p.source().close();
        p.sink().close();
        sel.close();
        if (endTime - startTime < 1000)
            throw new RuntimeException("test failed");
    }

    // Test if selectNow clears wakeup with only the wakeup fd
    // in the selector.
    // This fails before the fix on Solaris
    private static void test2() throws Exception {
        Selector sel = Selector.open();
        Pipe p = Pipe.open();
        p.source().configureBlocking(false);
        sel.wakeup();
        // ensure wakeup is consumed by selectNow
        Thread.sleep(2000);
        sel.selectNow();
        long startTime = System.currentTimeMillis();
        int n = sel.select(2000);
        long endTime = System.currentTimeMillis();
        sel.close();
        if (endTime - startTime < 1000)
            throw new RuntimeException("test failed");
    }

}
