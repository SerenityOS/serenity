/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 *
 * @bug 6608456
 * @author Igor Kushnirskiy
 * @modules java.desktop/com.sun.java.swing
 * @summary tests if delegate RepaintManager gets invoked.
 */

import java.awt.*;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;



public class bug6608456 {
    private static final TestFuture testFuture = new TestFuture();
    public static void main(String[] args) throws Exception {
        final JComponent component = invokeAndWait(
            new Callable<JComponent>() {
                public JComponent call() throws Exception {
                    RepaintManager.setCurrentManager(new TestRepaintManager());
                    JFrame frame = new JFrame("test");
                    frame.setLayout(new FlowLayout());
                    JButton button = new JButton("default");

                    frame.add(button);
                    button = new JButton("delegate");
                    if ( ! registerDelegate(
                             button, new TestRepaintManager())) {
                        return null;
                    }
                    frame.add(button);
                    frame.pack();
                    frame.setVisible(true);
                    return button;
                }
            });
        if (component == null) {
            throw new RuntimeException("failed. can not register delegate");
        }
        blockTillDisplayed(component);
        // trigger repaint for delegate RepaintManager
        invokeAndWait(
            new Callable<Void>() {
                public Void call() {
                    component.repaint();
                    return null;
                }
        });
        try {
            if (testFuture.get(10, TimeUnit.SECONDS)) {
                // passed
            }
        } catch (Exception e) {
            throw new RuntimeException("failed", e);
        } finally {
            JFrame frame = (JFrame) SwingUtilities
                .getAncestorOfClass(JFrame.class, component);
            if (frame != null) {
                frame.dispose();
            }
        }
    }
    static class TestRepaintManager extends RepaintManager {
        @Override
        public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
            if (RepaintManager.currentManager(c) == this) {
                testFuture.defaultCalled();
            } else {
                testFuture.delegateCalled();
            }
            super.addDirtyRegion(c, x, y, w, h);
        }
    }
    static class TestFuture extends FutureTask<Boolean> {
        private volatile boolean defaultCalled = false;
        private volatile boolean delegateCalled = false;
        public TestFuture() {
            super(new Callable<Boolean>() {
                public Boolean call() {
                    return null;
                }
            });
        }
        public void defaultCalled() {
            defaultCalled = true;
            updateState();
        }
        public void delegateCalled() {
            delegateCalled = true;
            updateState();
        }
        private void updateState() {
            if (defaultCalled && delegateCalled) {
                set(Boolean.TRUE);
            }
        }
    }

    private static boolean registerDelegate(JComponent c,
            RepaintManager repaintManager) {
        boolean rv = false;
        try {
            Class<?> clazz = Class.forName("com.sun.java.swing.SwingUtilities3");
            Method method = clazz.getMethod("setDelegateRepaintManager",
                JComponent.class, RepaintManager.class);
            method.invoke(clazz, c, repaintManager);
            rv = true;
        } catch (Exception ignore) {
        }
        return rv;
    }
    static <T> T invokeAndWait(Callable<T> callable) throws Exception {
        FutureTask<T> future = new FutureTask<T>(callable);
        SwingUtilities.invokeLater(future);
        return future.get();
    }

    public static void blockTillDisplayed(Component comp) {
        Point p = null;
        while (p == null) {
            try {
                p = comp.getLocationOnScreen();
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
