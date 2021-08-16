/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 4658741
  @summary verifies that getDropSuccess() returns correct value for inter-JVM DnD
  @run main InterJVMGetDropSuccessTest
*/

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;

public class InterJVMGetDropSuccessTest {

    private int returnCode = Util.CODE_NOT_RETURNED;
    private boolean successCodes[] = { true, false };
    private int dropCount = 0;

    final Frame frame = new Frame("Target Frame");

    final DropTargetListener dropTargetListener = new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                dtde.dropComplete(successCodes[dropCount]);
                dropCount++;
            }
        };
    final DropTarget dropTarget = new DropTarget(frame, dropTargetListener);

    public static void main(final String[] args) {
        InterJVMGetDropSuccessTest app = new InterJVMGetDropSuccessTest();
        app.init();
        app.start();
    }

    public void init() {
        frame.setTitle("Test frame");
        frame.setBounds(100, 100, 150, 150);
    } // init()

    public void start() {

        frame.setVisible(true);

        try {
            Thread.sleep(Util.FRAME_ACTIVATION_TIMEOUT);

            Point p = frame.getLocationOnScreen();
            Dimension d = frame.getSize();

            String javaPath = System.getProperty("java.home", "");
            String command = javaPath + File.separator + "bin" +
                File.separator + "java -cp " + System.getProperty("test.classes", ".") +
                " Child " +
                p.x + " " + p.y + " " + d.width + " " + d.height;

            Process process = Runtime.getRuntime().exec(command);
            returnCode = process.waitFor();

            InputStream errorStream = process.getErrorStream();
            int count = errorStream.available();
            if (count > 0) {
                byte[] b = new byte[count];
                errorStream.read(b);
                System.err.println("========= Child VM System.err ========");
                System.err.print(new String(b));
                System.err.println("======================================");
            }

            InputStream outputStream = process.getInputStream();
            count = outputStream.available();
            if (count > 0) {
                byte[] b = new byte[count];
                outputStream.read(b);
                System.err.println("========= Child VM System.out ========");
                System.err.print(new String(b));
                System.err.println("======================================");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        switch (returnCode) {
        case Util.CODE_NOT_RETURNED:
            throw new RuntimeException("Child VM: failed to start");
        case Util.CODE_FAILURE:
            throw new RuntimeException("Child VM: abnormal termination");
        default:
            if (dropCount == 2) {
                int expectedRetCode = 0;
                if (successCodes[0]) {
                    expectedRetCode |= Util.CODE_FIRST_SUCCESS;
                }
                if (successCodes[1]) {
                    expectedRetCode |= Util.CODE_SECOND_SUCCESS;
                }
                if (expectedRetCode != returnCode) {
                    throw new RuntimeException("The test failed. Expected:" +
                                               expectedRetCode + ". Returned:" +
                                               returnCode);
                }
            }
            break;
        }
    } // start()
} // class InterJVMGetDropSuccessTest

final class Util implements AWTEventListener {
    public static final int CODE_NOT_RETURNED = -1;
    public static final int CODE_FIRST_SUCCESS = 0x2;
    public static final int CODE_SECOND_SUCCESS = 0x2;
    public static final int CODE_FAILURE = 0x1;

    public static final int FRAME_ACTIVATION_TIMEOUT = 3000;

    static final Object SYNC_LOCK = new Object();
    static final int MOUSE_RELEASE_TIMEOUT = 1000;

    static final Util theInstance = new Util();

    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(theInstance, AWTEvent.MOUSE_EVENT_MASK);
    }

    public static Point getCenterLocationOnScreen(Component c) {
        Point p = c.getLocationOnScreen();
        Dimension d = c.getSize();
        p.translate(d.width / 2, d.height / 2);
        return p;
    }

    public static int sign(int n) {
        return n < 0 ? -1 : n == 0 ? 0 : 1;
    }

    private Component clickedComponent = null;

    private void reset() {
        clickedComponent = null;
    }

    public void eventDispatched(AWTEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            clickedComponent = (Component)e.getSource();
            synchronized (SYNC_LOCK) {
                SYNC_LOCK.notifyAll();
            }
        }
    }

    public static boolean pointInComponent(Robot robot, Point p, Component comp)
      throws InterruptedException {
        return theInstance.pointInComponentImpl(robot, p, comp);
    }

    private boolean pointInComponentImpl(Robot robot, Point p, Component comp)
      throws InterruptedException {
        robot.waitForIdle();
        reset();
        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        synchronized (SYNC_LOCK) {
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            SYNC_LOCK.wait(MOUSE_RELEASE_TIMEOUT);
        }

        Component c = clickedComponent;

        while (c != null && c != comp) {
            c = c.getParent();
        }

        return c == comp;
    }
}

class Child {
    static class DragSourceDropListener extends DragSourceAdapter {
        private boolean finished = false;
        private boolean dropSuccess = false;

        public void reset() {
            finished = false;
            dropSuccess = false;
        }

        public boolean isDropFinished() {
            return finished;
        }

        public boolean getDropSuccess() {
            return dropSuccess;
        }

        public void dragDropEnd(DragSourceDropEvent dsde) {
            finished = true;
            dropSuccess = dsde.getDropSuccess();
            synchronized (Util.SYNC_LOCK) {
                Util.SYNC_LOCK.notifyAll();
            }
        }
    }

    final Frame frame = new Frame("Source Frame");
    final DragSource dragSource = DragSource.getDefaultDragSource();
    final DragSourceDropListener dragSourceListener = new DragSourceDropListener();
    final Transferable transferable = new StringSelection("TEXT");
    final DragGestureListener dragGestureListener = new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent dge) {
                dge.startDrag(null, transferable, dragSourceListener);
            }
        };
    final DragGestureRecognizer dragGestureRecognizer =
        dragSource.createDefaultDragGestureRecognizer(frame, DnDConstants.ACTION_COPY,
                                                      dragGestureListener);

    public static void main(String[] args) {
        Child child = new Child();
        child.run(args);
    }

    public void run(String[] args) {
        try {
            if (args.length != 4) {
                throw new RuntimeException("Incorrect command line arguments.");
            }

            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int w = Integer.parseInt(args[2]);
            int h = Integer.parseInt(args[3]);

            frame.setBounds(300, 200, 150, 150);
            frame.setVisible(true);

            Thread.sleep(Util.FRAME_ACTIVATION_TIMEOUT);

            Point sourcePoint = Util.getCenterLocationOnScreen(frame);

            Point targetPoint = new Point(x + w / 2, y + h / 2);

            Robot robot = new Robot();
            robot.mouseMove(sourcePoint.x, sourcePoint.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            for (Point p = new Point(sourcePoint); !p.equals(targetPoint);
                 p.translate(Util.sign(targetPoint.x - p.x),
                             Util.sign(targetPoint.y - p.y))) {
                robot.mouseMove(p.x, p.y);
                Thread.sleep(50);
            }

            synchronized (Util.SYNC_LOCK) {
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                Util.SYNC_LOCK.wait(Util.FRAME_ACTIVATION_TIMEOUT);
            }

            if (!dragSourceListener.isDropFinished()) {
                throw new RuntimeException("Drop not finished");
            }

            boolean success1 = dragSourceListener.getDropSuccess();

            dragSourceListener.reset();
            robot.mouseMove(sourcePoint.x, sourcePoint.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            for (Point p = new Point(sourcePoint); !p.equals(targetPoint);
                 p.translate(Util.sign(targetPoint.x - p.x),
                             Util.sign(targetPoint.y - p.y))) {
                robot.mouseMove(p.x, p.y);
                Thread.sleep(50);
            }

            synchronized (Util.SYNC_LOCK) {
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                Util.SYNC_LOCK.wait(Util.FRAME_ACTIVATION_TIMEOUT);
            }

            if (!dragSourceListener.isDropFinished()) {
                throw new RuntimeException("Drop not finished");
            }

            boolean success2 = dragSourceListener.getDropSuccess();
            int retCode = 0;

            if (success1) {
                retCode |= Util.CODE_FIRST_SUCCESS;
            }
            if (success2) {
                retCode |= Util.CODE_SECOND_SUCCESS;
            }
            // This returns the diagnostic code from the child VM
            System.exit(retCode);
        } catch (Throwable e) {
            e.printStackTrace();
            // This returns the diagnostic code from the child VM
            System.exit(Util.CODE_FAILURE);
        }
    } // run()
} // class child
