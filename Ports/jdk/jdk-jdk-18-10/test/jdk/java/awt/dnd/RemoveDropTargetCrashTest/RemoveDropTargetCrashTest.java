/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Robot;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @bug 4393148 8136999 8186263 8224632
 * @summary tests that removal of the drop target or disposal of frame during
 *          drop processing doesn't cause crash
 * @run main/timeout=300 RemoveDropTargetCrashTest RUN_PROCESS
 */
public class RemoveDropTargetCrashTest {

    private static final String RUN_PROCESS = "RUN_PROCESS";
    private static final String RUN_TEST = "RUN_TEST";
    private static boolean exception;
    private static volatile CountDownLatch go;

    public static void main(String[] args) throws Exception {
        String command = args.length < 1 ? RUN_TEST : args[0];

        switch (command) {
            case RUN_PROCESS:
                runProcess();
                break;
            case RUN_TEST:
                for (int i = 0; i < 10; ++i) {
                    runTest(i * 10);
                    runTest(-1);
                }
                break;
            default:
                throw new RuntimeException("Unknown command: " + command);
        }
    }

    private static void runTest(int delay) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(10);
        Frame frame = null;
        try {
            DragSourceButton dragSourceButton = new DragSourceButton();
            dragSourceButton.addActionListener(e -> go.countDown());

            DropTargetPanel dropTargetPanel = new DropTargetPanel();

            frame = new Frame();
            frame.setTitle("Test frame");
            frame.setLocation(200, 200);
            frame.setLayout(new GridLayout(2, 1));
            frame.add(dragSourceButton);
            frame.add(dropTargetPanel);

            frame.pack();
            frame.setVisible(true);

            robot.waitForIdle();
            robot.delay(200);

            Point dragPoint = dragSourceButton.getLocationOnScreen();
            Dimension size = dragSourceButton.getSize();
            dragPoint.translate(size.width / 2, size.height / 2);

            pressOnButton(robot, dragPoint);

            Point dropPoint = dropTargetPanel.getLocationOnScreen();
            size = dropTargetPanel.getSize();
            dropPoint.translate(size.width / 2, size.height / 2);

            robot.mouseMove(dragPoint.x, dragPoint.y);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);

            Point tmpPoint = new Point(dragPoint);
            for (; !tmpPoint.equals(dropPoint);
                 tmpPoint.translate(sign(dropPoint.x - tmpPoint.x),
                            sign(dropPoint.y - tmpPoint.y))) {
                robot.mouseMove(tmpPoint.x, tmpPoint.y);
            }
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            // This delay() is important part of the test, because we need to
            // test a different delays before frame.dispose().
            if (delay >= 0) {
                robot.delay(delay);
            } else {
                robot.waitForIdle();
                robot.delay(1000);

                Point clickPoint = dragSourceButton.getLocationOnScreen();
                size = dragSourceButton.getSize();
                clickPoint.translate(size.width / 2, size.height / 2);

                if (clickPoint.equals(dragPoint)) {
                    throw new RuntimeException("Button was not moved");
                }
                pressOnButton(robot, clickPoint);
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }

    private static void pressOnButton(Robot robot, Point clickPoint)
            throws InterruptedException {
        go = new CountDownLatch(1);
        robot.mouseMove(clickPoint.x, clickPoint.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        if (!go.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Button was not pressed");
        }
    }

    public static int sign(int n) {
        return n < 0 ? -1 : n == 0 ? 0 : 1;
    }

    static class DragSourceButton extends Button implements Serializable,
            Transferable,
            DragGestureListener,
            DragSourceListener {

        private static DataFlavor dataflavor;

        static {
            try {
                dataflavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
                dataflavor.setHumanPresentableName("Local Object Flavor");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public DragSourceButton() {
            this("DragSourceButton");
        }

        public DragSourceButton(String str) {
            super(str);

            DragSource ds = DragSource.getDefaultDragSource();
            ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY,
                    this);
        }

        public void dragGestureRecognized(DragGestureEvent dge) {
            dge.startDrag(null, this, this);
        }

        public void dragEnter(DragSourceDragEvent dsde) {
        }

        public void dragExit(DragSourceEvent dse) {
        }

        public void dragOver(DragSourceDragEvent dsde) {
        }

        public void dragDropEnd(DragSourceDropEvent dsde) {
        }

        public void dropActionChanged(DragSourceDragEvent dsde) {
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {

            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }

            return this;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{dataflavor};
        }

        public boolean isDataFlavorSupported(DataFlavor dflavor) {
            return dataflavor.equals(dflavor);
        }
    }

    static class DropTargetPanel extends Panel implements DropTargetListener {

        final Dimension preferredSize = new Dimension(100, 100);

        public DropTargetPanel() {
            setDropTarget(new DropTarget(this, this));
        }

        public Dimension getPreferredSize() {
            return preferredSize;
        }

        public void dragEnter(DropTargetDragEvent dtde) {
        }

        public void dragExit(DropTargetEvent dte) {
        }

        public void dragOver(DropTargetDragEvent dtde) {
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        public void drop(DropTargetDropEvent dtde) {

            setDropTarget(null);

            DropTargetContext dtc = dtde.getDropTargetContext();

            if ((dtde.getSourceActions() & DnDConstants.ACTION_COPY) != 0) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
            } else {
                dtde.rejectDrop();
            }

            DataFlavor[] dfs = dtde.getCurrentDataFlavors();

            if (dfs != null && dfs.length >= 1) {
                Transferable transfer = dtde.getTransferable();
                Component comp;
                try {
                    comp = (Component) transfer.getTransferData(dfs[0]);
                } catch (Throwable e) {
                    dtc.dropComplete(false);
                    throw new RuntimeException(e);
                }
                add(comp);
                validate();
            }
            dtc.dropComplete(true);
        }
    }

    private static void runProcess() throws Exception {
        String javaPath = System.getProperty("java.home", "");
        String command = javaPath + File.separator + "bin" + File.separator + "java"
                + " " + RemoveDropTargetCrashTest.class.getName() + " " + RUN_TEST;

        Process process = Runtime.getRuntime().exec(command);
        boolean processExit = process.waitFor(200, TimeUnit.SECONDS);

        StringBuilder inStream = new StringBuilder();
        StringBuilder errStream = new StringBuilder();
        checkErrors(process.getErrorStream(), errStream);
        checkErrors(process.getInputStream(), inStream);

        System.out.println(inStream);
        System.err.println(errStream);

        if (exception) {
            throw new RuntimeException("Exception in the output!");
        }

        if (!processExit) {
            process.destroy();
            throw new RuntimeException(""
                    + "The sub process has not exited!");
        }
    }

    private static void checkErrors(InputStream in, StringBuilder stream) throws IOException {
        try (BufferedReader bufferedReader
                = new BufferedReader(new InputStreamReader(in))) {

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (!exception) {
                    exception = line.contains("Exception") || line.contains("Error");
                }
                stream.append(line).append("\n");
            }
        }
    }
}

