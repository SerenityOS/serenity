/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/*
 * @test
 * @key headful
 * @bug 8134917 8139050
 * @summary [macosx] JOptionPane doesn't receive mouse events when opened from a drop event
 * @run main MissingEventsOnModalDialogTest RUN_PROCESS
 */
public class MissingEventsOnModalDialogTest {

    private static final String RUN_PROCESS = "RUN_PROCESS";
    private static final String RUN_TEST = "RUN_TEST";
    private static boolean exception = false;
    private static volatile boolean passed = false;

    public static void main(String[] args) throws Exception {
        String command = args.length < 1 ? RUN_TEST : args[0];
        switch (command) {
            case RUN_PROCESS:
                runProcess();
                break;
            case RUN_TEST:
                runTest();
                break;
            default:
                throw new RuntimeException("Unknown command: " + command);
        }
    }

    private static void runTest() throws Exception {
        Frame sourceFrame = createFrame("Source Frame", 100, 100);
        Frame targetFrame = createFrame("Target Frame", 350, 350);

        DragSource defaultDragSource
                = DragSource.getDefaultDragSource();
        defaultDragSource.createDefaultDragGestureRecognizer(sourceFrame,
                DnDConstants.ACTION_COPY_OR_MOVE,
                new TestDragGestureListener());
        new DropTarget(targetFrame, DnDConstants.ACTION_COPY_OR_MOVE,
                new TestDropTargetListener(targetFrame));

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        sourceFrame.toFront();
        robot.waitForIdle();

        Point point = getCenterPoint(sourceFrame);
        robot.mouseMove(point.x, point.y);
        robot.waitForIdle();

        mouseDragAndDrop(robot, point, getCenterPoint(targetFrame));

        long time = System.currentTimeMillis() + 1000;

        while (!passed) {
            if (time < System.currentTimeMillis()) {
                sourceFrame.dispose();
                targetFrame.dispose();
                throw new RuntimeException("Mouse clicked event is lost!");
            }
            Thread.sleep(10);
        }
        sourceFrame.dispose();
        targetFrame.dispose();
    }

    private static Frame createFrame(String title, int x, int y) {
        Frame frame = new Frame();
        frame.setSize(200, 200);
        frame.setLocation(x, y);
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

    private static Point getCenterPoint(Window window) {
        Point centerPoint = window.getLocationOnScreen();
        centerPoint.translate(window.getWidth() / 2, window.getHeight() / 2);
        return centerPoint;
    }

    public static void mouseDragAndDrop(Robot robot, Point from, Point to) {
        mouseDND(robot, from.x, from.y, to.x, to.y);
    }

    public static void mouseDND(Robot robot, int x1, int y1, int x2, int y2) {

        int N = 20;
        int x = x1;
        int y = y1;
        int dx = (x2 - x1) / N;
        int dy = (y2 - y1) / N;

        robot.mousePress(InputEvent.BUTTON1_MASK);

        for (int i = 0; i < N; i++) {
            robot.mouseMove(x += dx, y += dy);
        }

        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static class TestDragGestureListener implements DragGestureListener {

        public void dragGestureRecognized(DragGestureEvent dge) {
            dge.startDrag(null, new StringTransferable());
        }
    }

    static class StringTransferable implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.stringFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            return "Hello World!";
        }
    }

    private static class TestDropTargetListener implements DropTargetListener {

        private final Frame targetFrame;

        public TestDropTargetListener(Frame targetFrame) {
            this.targetFrame = targetFrame;
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            dtde.acceptDrag(dtde.getDropAction());
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            dtde.acceptDrag(dtde.getDropAction());
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
            dtde.acceptDrag(dtde.getDropAction());
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            dtde.acceptDrop(dtde.getDropAction());
            showModalDialog(targetFrame);
            dtde.dropComplete(true);
        }
    }

    private static void showModalDialog(Frame targetFrame) {

        Dialog dialog = new Dialog(targetFrame, true);

        dialog.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                passed = true;
                dialog.dispose();
            }
        });

        dialog.setSize(400, 300);
        dialog.setTitle("Modal Dialog!");

        clickOnModalDialog(dialog);
        dialog.setVisible(true);
    }

    private static void clickOnModalDialog(Dialog dialog) {
        new Thread(() -> {
            clickOnDialog(dialog);
        }).start();
    }

    private static void clickOnDialog(Dialog dialog) {
        try {
            long time = System.currentTimeMillis() + 200;

            while (!dialog.isVisible()) {
                if (time < System.currentTimeMillis()) {
                    throw new RuntimeException("Dialog is not visible!");
                }
                Thread.sleep(10);
            }
            Robot robot = new Robot();
            robot.setAutoDelay(50);
            robot.waitForIdle();
            robot.delay(200);

            Point point = getCenterPoint(dialog);

            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runProcess() throws Exception {
        String javaPath = System.getProperty("java.home", "");
        String command = javaPath + File.separator + "bin" + File.separator + "java"
                + " " + MissingEventsOnModalDialogTest.class.getName() + " " + RUN_TEST;

        Process process = Runtime.getRuntime().exec(command);
        boolean processExit = process.waitFor(20, TimeUnit.SECONDS);

        StringBuilder inStream = new StringBuilder();
        StringBuilder errStream = new StringBuilder();
        checkErrors(process.getErrorStream(), errStream);
        checkErrors(process.getInputStream(), inStream);

        if (exception) {
            System.out.println(inStream);
            System.err.println(errStream);
            throw new RuntimeException("Exception in the output!");
        }

        if (!processExit) {
            process.destroy();
            throw new RuntimeException(""
                    + "The sub process has not exited!");
        }
    }

    private static boolean containsError(String line) {
        line = line.toLowerCase();
        return line.contains("exception") || line.contains("error")
                || line.contains("selector");
    }

    private static void checkErrors(InputStream in, StringBuilder stream) throws IOException {
        try (BufferedReader bufferedReader
                = new BufferedReader(new InputStreamReader(in))) {

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (!exception) {
                    exception = containsError(line);
                }
                stream.append(line).append("\n");
            }
        }
    }
}
