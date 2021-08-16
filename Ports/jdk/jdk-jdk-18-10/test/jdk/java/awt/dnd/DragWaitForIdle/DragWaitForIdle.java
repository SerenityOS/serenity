/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.InputEvent;

import test.java.awt.regtesthelpers.Util;

/**
 * @test
 * @key headful
 * @bug 7185258
 * @summary Robot.waitForIdle() should not hang forever if dnd is in progress
 * @library ../../regtesthelpers
 * @build Util
 * @run main/othervm DragWaitForIdle
 */
public final class DragWaitForIdle {

    public static void main(final String[] args) throws Exception {
        Frame frame = new Frame();
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true); // key point of the test

        DragGestureListener dragGestureListener = dge -> {
            dge.startDrag(null, new StringSelection("OK"), new DragSourceAdapter(){});
        };

        new DragSource().createDefaultDragGestureRecognizer(frame,
                DnDConstants.ACTION_MOVE, dragGestureListener);

        new DropTarget(frame, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                dtde.dropComplete(true);
            }
        });

        try {
            frame.setUndecorated(true);
            frame.setBounds(100, 100, 200, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            robot.waitForIdle();
            frame.toFront();

            Point startPoint = frame.getLocationOnScreen();
            Point endPoint = new Point(startPoint);
            startPoint.translate(50, 50);
            endPoint.translate(150, 150);

            Util.drag(robot, startPoint, endPoint, InputEvent.BUTTON2_MASK);

            robot.delay(500);
        } finally {
            frame.dispose();
        }
    }
}
