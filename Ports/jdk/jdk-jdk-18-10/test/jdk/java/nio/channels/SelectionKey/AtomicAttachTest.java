/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6436220
 * @summary SelectionKey.attach should atomically set and return the attachment
 */
import java.nio.channels.*;
import java.util.concurrent.atomic.*;

public class AtomicAttachTest {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        Pipe pipe = Pipe.open();
        SelectableChannel channel = pipe.sink().configureBlocking(false);
        final SelectionKey key = channel.register(selector, 0);
        key.attach(new AtomicBoolean());

        final AtomicInteger errorCount = new AtomicInteger();

        Thread t = new Thread() {
            public void run() {
                AtomicBoolean att = new AtomicBoolean();
                for (int i=0; i<(10*1000*1000); i++) {
                    att = (AtomicBoolean)key.attach(att);
                    // We should have exclusive ownership of att.
                    if (!att.compareAndSet(false, true) ||
                        !att.compareAndSet(true, false))
                    {
                        errorCount.incrementAndGet();
                    }
                }
            }
            {
                start();
                run();
            }
        };

        t.join();

        pipe.sink().close();
        pipe.source().close();
        selector.close();

        int count = errorCount.get();
        if (count > 0) {
            throw new RuntimeException("Error count:" + count);
        }
    }
}
