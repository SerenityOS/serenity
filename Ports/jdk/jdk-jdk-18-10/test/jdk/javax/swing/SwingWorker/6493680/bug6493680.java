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
 * @bug 6493680
 * @summary Tests if notifications are received in expected order
 * @author Igor Kushnirskiy
 */

import javax.swing.*;
import java.beans.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;


public class bug6493680 {
    /*
     * because timing is involved in this bug we will run the test
     * NUMBER_OF_TRIES times.
     * the test passes if it does not fail once.
     */
    private final static int NUMBER_OF_TRIES = 50;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < NUMBER_OF_TRIES; i++) {
            if (! (new Test()).test()) {
                throw new RuntimeException("failed");
            }
        }
    }

    private static class Test {
        private final AtomicInteger lastProgressValue = new AtomicInteger(-1);
        private final Exchanger<Boolean> exchanger = new Exchanger<Boolean>();

        boolean test() throws Exception {
            TestSwingWorker swingWorker = new TestSwingWorker();
            swingWorker.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("progress" == evt.getPropertyName()) {
                            lastProgressValue.set((Integer) evt.getNewValue());
                        }
                    }
                });

            swingWorker.execute();
            return exchanger.exchange(true);
        }

        private class TestSwingWorker extends SwingWorker<Void, Void> {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(1);
                    setProgress(i);
                }
                return null;
            }
            @Override
            protected void done() {
                boolean isPassed = (lastProgressValue.get() == 100);
                try {
                    exchanger.exchange(isPassed);
                } catch (Exception ingore) {
                }
            }
        }
    }

}
