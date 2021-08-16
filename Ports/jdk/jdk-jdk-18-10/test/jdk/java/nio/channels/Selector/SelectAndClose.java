/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5004077 8203765
 * @build SelectorUtils
 * @run main SelectAndClose
 * @summary Check blocking of select and close
 */

import java.io.IOException;
import java.nio.channels.Selector;

public class SelectAndClose {
    static Selector selector;
    static volatile boolean awakened = false;

    public static void main(String[] args) throws Exception {
        selector = Selector.open();

        // Create and start a selector in a separate thread.
        Thread selectThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        selector.select();
                        awakened = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        selectThread.start();

        // spin until make sure select is invoked
        SelectorUtils.spinUntilLocked(selectThread, selector);

        // Close the selector.
        selector.close();

        if (!awakened)
            selector.wakeup();

        // Wait for select() thread to finish.
        selectThread.join();

        if (!awakened) {
            throw new RuntimeException("Select did not awaken!");
        }
    }
}
