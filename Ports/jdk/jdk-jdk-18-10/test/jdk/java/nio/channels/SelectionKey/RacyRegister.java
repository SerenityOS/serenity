/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7132889
 * @summary Test that register does not return a valid SelectionKey when
 *    invoked at around the time that the channel is closed
 */

import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.Random;
import java.io.IOException;

public class RacyRegister {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try (Selector sel = Selector.open()) {
            int count = 100;
            while (count-- > 0) {
                final SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);

                // close channel asynchronously
                Future<Void> result = pool.submit(new Callable<Void>() {
                    public Void call() throws IOException {
                        sc.close();
                        return null;
                    }
                });

                // attempt to register channel with Selector
                SelectionKey key = null;
                try {
                    key = sc.register(sel, SelectionKey.OP_READ);
                } catch (ClosedChannelException ignore) {
                }

                // ensure close is done
                result.get();

                // if we have a key then it should be invalid
                if (key != null && key.isValid())
                    throw new RuntimeException("Key is valid");
            }
        } finally {
            pool.shutdown();
        }
    }
}
