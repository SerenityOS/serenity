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
 * @bug 8029979
 * @summary Checks if acceptDrop() can be called several times
 * @library ../../regtesthelpers
 * @build Util
 * @compile AcceptDropMultipleTimes.java
 * @run main/othervm AcceptDropMultipleTimes
 * @author anthony.petrov@oracle.com
 */

import test.java.awt.regtesthelpers.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.InputEvent;

public class AcceptDropMultipleTimes {

    private static final int FRAME_SIZE = 100;
    private static final int FRAME_LOCATION = 100;

    private static volatile Frame f;

    private static void initAndShowUI() {
        f = new Frame("Test frame");
        f.setBounds(FRAME_LOCATION, FRAME_LOCATION, FRAME_SIZE, FRAME_SIZE);

        final DraggablePanel dragSource = new DraggablePanel();
        dragSource.setBackground(Color.yellow);
        DropTarget dt = new DropTarget(dragSource, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent dtde) {
                // The first call always succeeds
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                // The second call should succeed if the fix works
                dtde.acceptDrop(DnDConstants.ACTION_MOVE);

                dtde.dropComplete(true);
            }
        });
        dragSource.setDropTarget(dt);
        f.add(dragSource);

        f.setVisible(true);
    }

    public static void main(String[] args) throws Throwable {
        try {

            SwingUtilities.invokeAndWait(() -> initAndShowUI());

            Robot r = new Robot();
            Util.waitForIdle(r);
            Util.drag(r,
                    new Point(FRAME_LOCATION + FRAME_SIZE / 3, FRAME_LOCATION + FRAME_SIZE / 3),
                    new Point(FRAME_LOCATION + FRAME_SIZE / 3 * 2, FRAME_LOCATION + FRAME_SIZE / 3 * 2),
                    InputEvent.BUTTON1_MASK);
            Util.waitForIdle(r);
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
