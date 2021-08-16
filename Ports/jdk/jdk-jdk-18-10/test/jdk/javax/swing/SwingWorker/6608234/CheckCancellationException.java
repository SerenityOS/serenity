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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingWorker;

/**
 * @test
 * @bug 6608234
 */
public final class CheckCancellationException {

    private static final CountDownLatch go = new CountDownLatch(1);

    public static void main(final String[] args) throws Exception {
        SwingWorker<?, ?> worker = new SwingWorker() {
            protected Void doInBackground() {
                go.countDown();
                while (!Thread.interrupted()) ;
                return null;
            }
        };
        worker.execute();
        go.await();
        worker.cancel(true);
        try {
            worker.get();
        } catch (final CancellationException expected) {
            // expected exception
            return;
        }
        throw new RuntimeException("CancellationException was not thrown");
    }
}
