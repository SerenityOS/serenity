/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024061
 * @summary Checks that no exception is thrown if dragGestureRecognized
 *          takes a while to complete.
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * If dragGestureRecognized() takes a while to complete and if user performs a drag quickly,
 * an exception is thrown from DropTargetListener.dragEnter when it calls
 * DropTargetDragEvent.getTransferable().
 * <p>
 * This class introduces a delay in dragGestureRecognized() to cause the exception.
 */
public class bug8024061 {
    private static final DataFlavor DropObjectFlavor;
    private static final int DELAY = 1000;

    static final DnDPanel panel1 = new DnDPanel(Color.yellow);
    static final DnDPanel panel2 = new DnDPanel(Color.pink);
    private final JFrame frame;
    static Point here;
    static Point there;
    static Dimension d;



    private static final CountDownLatch lock = new CountDownLatch(1);
    private static volatile Exception dragEnterException = null;

    static {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        DropObjectFlavor = flavor;
    }

    bug8024061() {
        frame = new JFrame("DnDWithRobot");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        d = new Dimension(100, 100);

        panel1.setPreferredSize(d);
        panel2.setPreferredSize(d);

        Container content = frame.getContentPane();
        content.setLayout(new GridLayout(1, 2, 5, 5));
        content.add(panel1);
        content.add(panel2);

        frame.pack();
        frame.setLocationRelativeTo(null);
        DropObject drop = new DropObject();
        drop.place(panel1, new Point(10, 10));
        frame.setVisible(true);
    }

    public static void main(String[] args) throws AWTException, InvocationTargetException, InterruptedException {
        final bug8024061[] dnd = {null};
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                dnd[0] = new bug8024061();
            }
        });
        final Robot robot = new Robot();
        robot.setAutoDelay(10);
        robot.waitForIdle();
        robot.delay(200);

        JFrame frame = dnd[0].frame;
        SwingUtilities.invokeAndWait(() -> {
            here = panel1.getLocationOnScreen();
            there = panel2.getLocationOnScreen();
        });
        here.translate(d.width / 2, d.height / 2);
        there.translate(d.width / 2, d.height / 2);
        robot.mouseMove(here.x, here.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        while (here.x < there.x) {
            here.x += 20;
            robot.mouseMove(here.x, here.y);
            System.out.println("x = " + here.x);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        System.out.println("finished");

        try {
            if (lock.await(5, TimeUnit.SECONDS)) {
                if (dragEnterException == null) {
                    System.out.println("Test passed.");
                } else {
                    System.out.println("Test failed.");
                    dragEnterException.printStackTrace();
                    throw new RuntimeException(dragEnterException);
                }
            } else {
                System.out.println("Test failed. Timeout reached");
                throw new RuntimeException("Timed out waiting for dragEnter()");
            }
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    class DropObject implements Transferable {
        DnDPanel panel;
        Color color = Color.CYAN;
        int width = 50;
        int height = 50;
        int x;
        int y;

        void draw(Graphics2D g) {
            Color savedColor = g.getColor();
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.lightGray);
            g.drawRect(x, y, width, height);
            g.setColor(savedColor);
        }

        boolean contains(int x, int y) {
            return (x > this.x && x < this.x + width)
                    && (y > this.y && y < this.y + height);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DropObjectFlavor};
        }

        void place(DnDPanel panel, Point location) {
            if (panel != this.panel) {
                x = location.x;
                y = location.y;
                if (this.panel != null) {
                    this.panel.setDropObject(null);
                    this.panel.repaint();
                }
                this.panel = panel;
                this.panel.setDropObject(this);
                this.panel.repaint();
            }
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DropObjectFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return this;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    static class DnDPanel extends JPanel {
        DropObject dropObject;
        final DragSource dragSource;
        final DropTarget dropTarget;
        final Color color;
        final DragGestureListener dgListener;
        final DragSourceListener dsListener;
        final DropTargetListener dtListener;

        DnDPanel(Color color) {
            this.color = color;
            this.dragSource = DragSource.getDefaultDragSource();
            dgListener = new DragGestureListener() {
                @Override
                public void dragGestureRecognized(DragGestureEvent dge) {
                    Point location = dge.getDragOrigin();
                    if (dropObject != null && dropObject.contains(location.x, location.y)) {
                        dragSource.startDrag(dge, DragSource.DefaultCopyNoDrop, dropObject, dsListener);
                        try {
                            Thread.sleep(DELAY);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            };

            dsListener = new DragSourceListener() {
                @Override
                public void dragEnter(DragSourceDragEvent dsde) {
                }

                @Override
                public void dragOver(DragSourceDragEvent dsde) {
                }

                @Override
                public void dropActionChanged(DragSourceDragEvent dsde) {
                }

                @Override
                public void dragExit(DragSourceEvent dse) {
                }

                @Override
                public void dragDropEnd(DragSourceDropEvent dsde) {
                }
            };

            dtListener = new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    if (dropObject != null) {
                        dtde.rejectDrag();
                        return;
                    }
                    dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                    try {
                        Transferable t = dtde.getTransferable();
                        Object data = t.getTransferData(DropObjectFlavor);
                    } catch (Exception e) {
                        dragEnterException = e;
                        e.printStackTrace();
                    } finally {
                        lock.countDown();
                    }
                }

                @Override
                public void dragOver(DropTargetDragEvent dtde) {
                    if (dropObject != null) {
                        dtde.rejectDrag();
                        return;
                    }
                    dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                }

                @Override
                public void dropActionChanged(DropTargetDragEvent dtde) {
                }

                @Override
                public void dragExit(DropTargetEvent dte) {
                }

                @Override
                public void drop(DropTargetDropEvent dtde) {
                    if (dropObject != null) {
                        dtde.rejectDrop();
                        return;
                    }
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                        Transferable t = dtde.getTransferable();
                        DropObject dropObject = (DropObject) t.getTransferData(DropObjectFlavor);
                        Point location = dtde.getLocation();
                        dropObject.place(DnDPanel.this, location);
                        dtde.dropComplete(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            };

            dragSource.createDefaultDragGestureRecognizer(this,
                    DnDConstants.ACTION_MOVE, dgListener);

            dropTarget = new DropTarget(this, DnDConstants.ACTION_MOVE, dtListener, true);

        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Color savedColor = g.getColor();
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(savedColor);
            if (dropObject != null) {
                dropObject.draw((Graphics2D) g);
            }
        }

        void setDropObject(DropObject dropObject) {
            this.dropObject = dropObject;
        }

        DropObject findDropObject(int x, int y) {
            if (dropObject != null && dropObject.contains(x, y)) {
                return dropObject;
            }
            return null;
        }
    }
}
