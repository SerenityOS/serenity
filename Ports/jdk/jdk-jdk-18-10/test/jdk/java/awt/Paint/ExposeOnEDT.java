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


import java.awt.*;

/**
 * @test
 * @key headful
 * @bug 7090424
 * @author Sergey Bylokhov
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main ExposeOnEDT
 */
public final class ExposeOnEDT {

    private static ExtendedRobot robot = null;
    private static final Button buttonStub = new Button() {
        @Override
        public void paint(final Graphics g) {
            buttonPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Canvas canvasStub = new Canvas() {
        @Override
        public void paint(final Graphics g) {
            canvasPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Checkbox checkboxStub = new Checkbox() {
        @Override
        public void paint(final Graphics g) {
            checkboxPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Choice choiceStub = new Choice() {
        @Override
        public void paint(final Graphics g) {
            choicePainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Component lwComponentStub = new Component() {
        @Override
        public void paint(final Graphics g) {
            lwPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Container containerStub = new Container() {
        @Override
        public void paint(final Graphics g) {
            containerPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Frame frame = new Frame() {
        @Override
        public void paint(final Graphics g) {
            super.paint(g);
            framePainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Label labelStub = new Label() {
        @Override
        public void paint(final Graphics g) {
            labelPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final List listStub = new List() {
        @Override
        public void paint(final Graphics g) {
            listPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Panel panelStub = new Panel() {
        @Override
        public void paint(final Graphics g) {
            panelPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final Scrollbar scrollbarStub = new Scrollbar() {
        @Override
        public void paint(final Graphics g) {
            scrollbarPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final ScrollPane scrollPaneStub = new ScrollPane() {
        @Override
        public void paint(final Graphics g) {
            scrollPanePainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final TextArea textAreaStub = new TextArea() {
        @Override
        public void paint(final Graphics g) {
            textAreaPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static final TextField textFieldStub = new TextField() {
        @Override
        public void paint(final Graphics g) {
            textFieldPainted = true;
            if (!EventQueue.isDispatchThread()) {
                throw new RuntimeException("Wrong thread");
            }
        }
    };
    private static volatile boolean lwPainted;
    private static volatile boolean buttonPainted;
    private static volatile boolean canvasPainted;
    private static volatile boolean checkboxPainted;
    private static volatile boolean choicePainted;
    private static volatile boolean containerPainted;
    private static volatile boolean framePainted;
    private static volatile boolean labelPainted;
    private static volatile boolean listPainted;
    private static volatile boolean panelPainted;
    private static volatile boolean scrollbarPainted;
    private static volatile boolean scrollPanePainted;
    private static volatile boolean textAreaPainted;
    private static volatile boolean textFieldPainted;

    public static void main(final String[] args) throws Exception {
        //Frame initialisation
        frame.setLayout(new GridLayout());
        frame.setSize(new Dimension(200, 200));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        sleep();

        frame.add(buttonStub);
        frame.add(canvasStub);
        frame.add(checkboxStub);
        frame.add(choiceStub);
        frame.add(lwComponentStub);
        frame.add(containerStub);
        frame.add(labelStub);
        frame.add(listStub);
        frame.add(panelStub);
        frame.add(scrollbarStub);
        frame.add(scrollPaneStub);
        frame.add(textAreaStub);
        frame.add(textFieldStub);
        frame.validate();
        sleep();

        // Force expose event from the native system.
        initPaintedFlags();
        frame.setSize(300, 300);
        frame.validate();
        sleep();

        //Check results.
        validation();

        cleanup();
    }

    private static void initPaintedFlags() {
        lwPainted = false;
        buttonPainted = false;
        canvasPainted = false;
        checkboxPainted = false;
        choicePainted = false;
        containerPainted = false;
        framePainted = false;
        labelPainted = false;
        listPainted = false;
        panelPainted = false;
        scrollbarPainted = false;
        scrollPanePainted = false;
        textAreaPainted = false;
        textFieldPainted = false;
    }

    private static void validation() {
        if (!buttonPainted) {
            fail("Paint is not called a Button ");
        }
        if (!canvasPainted) {
            fail("Paint is not called a Canvas ");
        }
        if (!checkboxPainted) {
            fail("Paint is not called a Checkbox ");
        }
        if (!choicePainted) {
            fail("Paint is not called a Choice ");
        }
        if (!lwPainted) {
            fail("Paint is not called on a lightweight");
        }
        if (!containerPainted) {
            fail("Paint is not called on a Container");
        }
        if (!labelPainted) {
            fail("Paint is not called on a Label");
        }
        if (!listPainted) {
            fail("Paint is not called on a List");
        }
        if (!panelPainted) {
            fail("Paint is not called on a Panel");
        }
        if (!scrollbarPainted) {
            fail("Paint is not called on a Scrollbar");
        }
        if (!scrollPanePainted) {
            fail("Paint is not called on a ScrollPane");
        }
        if (!textAreaPainted) {
            fail("Paint is not called on a TextArea");
        }
        if (!textFieldPainted) {
            fail("Paint is not called on a TextField");
        }
        if (!framePainted) {
            fail("Paint is not called on a Frame when paintAll()");
        }
    }

    private static void sleep() {
        if(robot == null) {
            try {
                robot = new ExtendedRobot();
            }catch(Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Unexpected failure");
            }
        }
        robot.waitForIdle(1000);
    }

    private static void fail(final String message) {
        cleanup();
        throw new RuntimeException(message);
    }

    private static void cleanup() {
        frame.dispose();
    }
}
