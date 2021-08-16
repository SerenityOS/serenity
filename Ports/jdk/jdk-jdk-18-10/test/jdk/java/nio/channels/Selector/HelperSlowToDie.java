/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6823609
 * @summary Selector.select can hangs on Windows for cases where a helper thread
 *   becomes redudant but a new helper is immediately needed.
 */

import java.nio.channels.*;
import java.io.IOException;

public class HelperSlowToDie {
    private static final int CHANNELS_PER_THREAD = 1023;
    private static final int TEST_ITERATIONS = 200;
    private static volatile boolean done;

    public static void main(String[] args) throws IOException {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("Test skipped as it verifies a Windows specific bug");
            return;
        }

        Selector sel = Selector.open();

        // register channels
        SocketChannel[] channels = new SocketChannel[CHANNELS_PER_THREAD];
        for (int i=0; i<CHANNELS_PER_THREAD; i++) {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.register(sel, SelectionKey.OP_CONNECT);
            channels[i] = sc;
        }
        sel.selectNow();

        // Start threads to swamp all cores but one. This improves the chances
        // of duplicating the bug.
        Runnable busy = new Runnable() {
            public void run() {
                while (!done) ;  // no nothing
            }
        };
        int ncores = Runtime.getRuntime().availableProcessors();
        for (int i=0; i<ncores-1; i++)
            new Thread(busy).start();

        // Loop changing the number of channels from 1023 to 1024 and back.
        for (int i=0; i<TEST_ITERATIONS; i++) {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.register(sel, SelectionKey.OP_CONNECT);
            sel.selectNow();   // cause helper to spin up
            sc.close();
            sel.selectNow();  // cause helper to retire
        }

        // terminate busy threads
        done = true;

        // clean-up
        for (int i=0; i<CHANNELS_PER_THREAD; i++) {
            channels[i].close();
        }
        sel.close();
    }
}
