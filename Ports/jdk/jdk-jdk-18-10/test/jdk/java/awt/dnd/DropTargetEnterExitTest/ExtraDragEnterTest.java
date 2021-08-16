/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8024163
 * @summary Checks the dragEnter event is correctly generated
 * @library ../../regtesthelpers
 * @build Util
 * @compile ExtraDragEnterTest.java
 * @run main/othervm ExtraDragEnterTest
 * @author Petr Pchelko
 */

import test.java.awt.regtesthelpers.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.InputEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtraDragEnterTest {

    private static final int FRAME_SIZE = 100;
    private static final int FRAME_LOCATION = 100;

    private static AtomicInteger dragEnterCalled = new AtomicInteger(0);

    private static volatile Panel mainPanel;
    private static volatile Frame f;

    private static void initAndShowUI() {
        f = new Frame("Test frame");
        f.setBounds(FRAME_LOCATION,FRAME_LOCATION,FRAME_SIZE,FRAME_SIZE);
        mainPanel = new Panel();
        mainPanel.setBounds(0, 0, FRAME_SIZE, FRAME_SIZE);
        mainPanel.setBackground(Color.black);
        mainPanel.setLayout(new GridLayout(2, 1));

        final DraggablePanel dragSource = new DraggablePanel();
        dragSource.setBackground(Color.yellow);
        dragSource.setDropTarget(null);
        mainPanel.add(dragSource);

        Panel dropTarget = new Panel();
        dropTarget.setBackground(Color.red);
        DropTarget dt = new DropTarget(dropTarget, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent dtde) { }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                dragEnterCalled.incrementAndGet();
            }
        });
        dropTarget.setDropTarget(dt);
        mainPanel.add(dropTarget);

        f.add(mainPanel);
        f.setVisible(true);
    }

    public static void main(String[] args) throws Throwable {
        try {

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    initAndShowUI();
                }
            });

            Robot r = new Robot();
            Util.waitForIdle(r);
            Point leftCorner = new Point(mainPanel.getLocationOnScreen());
            leftCorner.translate(5, 5);
            Point rightCorner = new Point(mainPanel.getLocationOnScreen());
            rightCorner.translate(mainPanel.getWidth(), mainPanel.getHeight());
            rightCorner.translate(-5, -5);
            Util.drag(r, leftCorner, rightCorner, InputEvent.BUTTON1_MASK);
            Util.waitForIdle(r);

            int called = dragEnterCalled.get();
            if (called != 1) {
                throw new RuntimeException("Failed. Drag enter called " + called + " times. Expected 1" );
            }
        } finally {
            if (f != null) {
                f.dispose();
            }
        }
    }

    private static class DraggablePanel extends Panel implements DragGestureListener {

        public DraggablePanel() {
            (new DragSource()).createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);
        }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            dge.startDrag(Cursor.getDefaultCursor(), new StringSelection("test"));
        }
    }
}
