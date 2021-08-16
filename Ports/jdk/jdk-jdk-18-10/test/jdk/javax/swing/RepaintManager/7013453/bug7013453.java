/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7013453
   @summary BufferStrategyPaintManager.dispose will cause IllegalMonitorStateException in event thread
   @modules java.desktop/javax.swing:open
   @author Pavel Porvatov
*/

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class bug7013453 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    Method getPaintManagerMethod = RepaintManager.class.getDeclaredMethod("getPaintManager");

                    getPaintManagerMethod.setAccessible(true);

                    final Object paintManager = getPaintManagerMethod.invoke(RepaintManager.currentManager(new JLabel()));

                    String paintManagerName = paintManager.getClass().getCanonicalName();

                    if (!paintManagerName.equals("javax.swing.BufferStrategyPaintManager")) {
                        System.out.println("The test is not suitable for the " + paintManagerName +
                                " paint manager. The test skipped.");

                        return;
                    }

                    final Field showingField = paintManager.getClass().getDeclaredField("showing");

                    showingField.setAccessible(true);

                    synchronized (paintManager) {
                        showingField.setBoolean(paintManager, true);
                    }

                    // Postpone reset the showing field
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

                            synchronized (paintManager) {
                                try {
                                    showingField.setBoolean(paintManager, false);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });

                    thread.start();

                    Method disposeMethod = paintManager.getClass().getDeclaredMethod("dispose");

                    disposeMethod.setAccessible(true);

                    disposeMethod.invoke(paintManager);

                    System.out.println("The test passed.");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
