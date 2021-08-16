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

/* @test
 * @bug 8201315
 * @build SelectorUtils
 * @run main RegisterDuringSelect
 * @summary Test that channels can be registered, interest ops can changed,
 *          and keys cancelled while a selection operation is in progress.
 */

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class RegisterDuringSelect {
    interface TestOperation {
        void accept(Thread t, Selector sel, Pipe.SourceChannel sc) throws Exception;
    }
    static class Test {
        final Selector sel;
        final Pipe p;
        final Pipe.SourceChannel sc;

        Test() throws Exception {
            sel = Selector.open();
            p = Pipe.open();
            sc = p.source();
            sc.configureBlocking(false);
        }
        void test(TestOperation op) throws Exception {
            try {
                Thread t = new Thread(() -> {
                    try {
                        sel.select();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                t.start();
                op.accept(t, sel, sc);
            } finally {
                sel.close();
                p.source().close();
                p.sink().close();
            }
        }
    }
    /**
     * Invoke register, interestOps, and cancel concurrently with a thread
     * doing a selection operation
     */
    public static void main(String args[]) throws Exception {
        new Test().test((t, sel, sc) -> {
            System.out.println("register ...");
            // spin until make sure select is invoked
            SelectorUtils.spinUntilLocked(t, sel);
            SelectionKey key = sc.register(sel, SelectionKey.OP_READ);
            try {
                if (!sel.keys().contains(key))
                    throw new RuntimeException("key not in key set");
            } finally {
                sel.wakeup();
                t.join();
            }
        });
        new Test().test((t, sel, sc) -> {
            System.out.println("interestOps ...");
            SelectionKey key = sc.register(sel, SelectionKey.OP_READ);
            // spin until make sure select is invoked
            SelectorUtils.spinUntilLocked(t, sel);
            key.interestOps(0);
            try {
                if (key.interestOps() != 0)
                    throw new RuntimeException("interested ops not cleared");
            } finally {
                sel.wakeup();
                t.join();
            }
        });
        new Test().test((t, sel, sc) -> {
            System.out.println("cancel ...");
            SelectionKey key = sc.register(sel, SelectionKey.OP_READ);
            // spin until make sure select is invoked
            SelectorUtils.spinUntilLocked(t, sel);
            key.cancel();
            sel.wakeup();
            t.join();
            if (sel.keys().contains(key))
                throw new RuntimeException("key not removed from key set");
        });
    }
}
