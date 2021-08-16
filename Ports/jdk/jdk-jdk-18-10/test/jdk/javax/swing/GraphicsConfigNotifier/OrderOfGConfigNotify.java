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

import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @test
 * @key headful
 * @bug 8201552
 * @summary Container should get "graphicsConfiguration" notification when all
 *          its children are updated
 */
public final class OrderOfGConfigNotify {

    private static String name = "graphicsConfiguration";

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            AtomicBoolean parentCalled = new AtomicBoolean(false);
            AtomicBoolean childCalled = new AtomicBoolean(false);

            JFrame frame = new JFrame();

            JPanel parent = new JPanel();
            parent.addPropertyChangeListener(evt -> {
                if (!evt.getPropertyName().equals(name)) {
                    return;
                }
                if (!childCalled.get()) {
                    throw new RuntimeException("Parent is called/child is not");
                }
                parentCalled.set(true);
                if (parent.getGraphicsConfiguration() == null) {
                    throw new RuntimeException("GraphicsConfiguration is null");
                }
            });
            JPanel child = new JPanel();
            child.addPropertyChangeListener(evt -> {
                if (!evt.getPropertyName().equals(name)) {
                    return;
                }
                childCalled.set(true);
                if (child.getGraphicsConfiguration() == null) {
                    throw new RuntimeException("GraphicsConfiguration is null");
                }
            });
            parent.add(child);

            // Frame.add() should update graphicsConfiguration for all hierarchy
            frame.add(parent);
            if (!parentCalled.get() || !childCalled.get()) {
                throw new RuntimeException("Property listener was not called");
            }
        });
    }
}
