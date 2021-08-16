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
 * @summary Checks that dragExit is generated when the new DropTarget is created under the drag
 * @library ../../regtesthelpers
 * @build Util
 * @compile MissedDragExitTest.java
 * @run main/othervm MissedDragExitTest
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
import java.awt.dnd.DropTargetEvent;
import java.awt.event.InputEvent;

public class MissedDragExitTest {

    private static final int FRAME_SIZE = 100;
    private static final int FRAME_LOCATION = 100;

    private static volatile boolean dragExitCalled = false;

    private static volatile Frame f;

    private static void initAndShowUI() {
        f = new Frame("Test frame");
        f.setBounds(FRAME_LOCATION,FRAME_LOCATION,FRAME_SIZE,FRAME_SIZE);

        final DraggablePanel dragSource = new DraggablePanel();
        dragSource.setBackground(Color.yellow);
        DropTarget dt = new DropTarget(dragSource, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent dtde) { }

            @Override
            public void dragExit(DropTargetEvent dte) {
                dragExitCalled = true;
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                Panel newDropTarget = new Panel();
                newDropTarget.setDropTarget(new DropTarget());
                newDropTarget.setBackground(Color.red);
                newDropTarget.setBounds(0, 0, FRAME_SIZE, FRAME_SIZE);
                dragSource.add(newDropTarget);
            }
        });
        dragSource.setDropTarget(dt);
        f.add(dragSource);

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
            Util.drag(r,
                    new Point(FRAME_LOCATION + FRAME_SIZE / 3, FRAME_LOCATION + FRAME_SIZE / 3),
                    new Point(FRAME_LOCATION + FRAME_SIZE / 3 * 2, FRAME_LOCATION + FRAME_SIZE / 3 * 2),
                    InputEvent.BUTTON1_MASK);
            Util.waitForIdle(r);

            if (!dragExitCalled) {
                throw new RuntimeException("Failed. Drag exit was not called" );
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
