/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6432565
 * @summary Tests if no ArrayStoreException is thrown
 * @author Igor Kushnirskiy
 */
import java.awt.*;
import javax.swing.SwingWorker;
import java.util.concurrent.atomic.*;


public class bug6432565 {
    private final static AtomicReference<Throwable> throwable =
        new AtomicReference<Throwable>(null);
    private final static AtomicBoolean isDone = new AtomicBoolean(false);
    public static void main(String[] args) throws Exception {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventProcessor());

        SwingWorker<Void, CharSequence> swingWorker =
            new SwingWorker<Void,CharSequence>() {
                @Override
                protected Void doInBackground() {
                    publish(new String[] {"hello"});
                    publish(new StringBuilder("world"));
                    return null;
                }
                @Override
                protected void done() {
                    isDone.set(true);
                }
            };
        swingWorker.execute();

        while (! isDone.get()) {
            Thread.sleep(100);
        }
        if (throwable.get() instanceof ArrayStoreException) {
            throw new RuntimeException("Test failed");
        }
    }
    private final static class EventProcessor extends EventQueue {
        @Override
        protected void dispatchEvent(AWTEvent event) {
            try {
                super.dispatchEvent(event);
            } catch (Throwable e) {
                e.printStackTrace();
                throwable.set(e);
            }
        }
    }
}
