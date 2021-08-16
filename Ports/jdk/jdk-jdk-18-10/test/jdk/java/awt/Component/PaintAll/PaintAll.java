/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

/*
  @test
  @key headful
  @bug 6596915
  @summary Test Component.paintAll() method
  @author sergey.bylokhov@oracle.com: area=awt.component
  @library /lib/client/
  @build ExtendedRobot
  @run main PaintAll
*/
public class PaintAll {

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
    private static ExtendedRobot robot = null;

    private static final Button buttonStub = new Button() {
        @Override
        public void paint(final Graphics g) {
            buttonPainted = true;
        }
    };

    private static final Canvas canvasStub = new Canvas() {
        @Override
        public void paint(final Graphics g) {
            canvasPainted = true;
        }
    };

    private static final Checkbox checkboxStub = new Checkbox() {
        @Override
        public void paint(final Graphics g) {
            checkboxPainted = true;
        }
    };

    private static final Choice choiceStub = new Choice() {
        @Override
        public void paint(final Graphics g) {
            choicePainted = true;
        }
    };

    private static final Component lwComponentStub = new Component() {
        @Override
        public void paint(final Graphics g) {
            lwPainted = true;
        }
    };

    private static final Container containerStub = new Container() {
        @Override
        public void paint(final Graphics g) {
            containerPainted = true;
        }
    };

    private static final Frame frame = new Frame() {
        @Override
        public void paint(final Graphics g) {
            super.paint(g);
            framePainted = true;
        }
    };

    private static final Label labelStub = new Label() {
        @Override
        public void paint(final Graphics g) {
            labelPainted = true;
        }
    };

    private static final List listStub = new List() {
        @Override
        public void paint(final Graphics g) {
            listPainted = true;
        }
    };

    private static final Panel panelStub = new Panel() {
        @Override
        public void paint(final Graphics g) {
            panelPainted = true;
        }
    };

    private static final Scrollbar scrollbarStub = new Scrollbar() {
        @Override
        public void paint(final Graphics g) {
            scrollbarPainted = true;
        }
    };

    private static final ScrollPane scrollPaneStub = new ScrollPane() {
        @Override
        public void paint(final Graphics g) {
            scrollPanePainted = true;
        }
    };

    private static final TextArea textAreaStub = new TextArea() {
        @Override
        public void paint(final Graphics g) {
            textAreaPainted = true;
        }
    };

    private static final TextField textFieldStub = new TextField() {
        @Override
        public void paint(final Graphics g) {
            textFieldPainted = true;
        }
    };

    public static void main(final String[] args) throws Exception {
        //Frame initialisation
        final BufferedImage graphicsProducer =
                new BufferedImage(BufferedImage.TYPE_INT_ARGB, 1, 1);

        final Graphics g = graphicsProducer.getGraphics();

        frame.setLayout(new GridLayout());
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
        frame.setSize(new Dimension(500, 500));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        sleep();

        //Check results.
        validation();

        //Reset all flags to 'false'.
        initPaintedFlags();

        //Tested method.
        frame.paintAll(g);
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
            fail("Paint is not called a Button "
                 + "when paintAll() invoked on a parent");
        }
        if (!canvasPainted) {
            fail("Paint is not called a Canvas "
                 + "when paintAll() invoked on a parent");
        }
        if (!checkboxPainted) {
            fail("Paint is not called a Checkbox "
                 + "when paintAll() invoked on a parent");
        }
        if (!choicePainted) {
            fail("Paint is not called a Choice "
                 + "when paintAll() invoked on a parent");
        }
        if (!lwPainted) {
            fail("Paint is not called on a lightweight"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!containerPainted) {
            fail("Paint is not called on a Container"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!labelPainted) {
            fail("Paint is not called on a Label"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!listPainted) {
            fail("Paint is not called on a List"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!panelPainted) {
            fail("Paint is not called on a Panel"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!scrollbarPainted) {
            fail("Paint is not called on a Scrollbar"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!scrollPanePainted) {
            fail("Paint is not called on a ScrollPane"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!textAreaPainted) {
            fail("Paint is not called on a TextArea"
                 + " subcomponent when paintAll() invoked on a parent");
        }
        if (!textFieldPainted) {
            fail("Paint is not called on a TextField"
                 + " subcomponent when paintAll() invoked on a parent");
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
        robot.waitForIdle(500);
    }

    private static void fail(final String message) {
        cleanup();
        throw new RuntimeException(message);
    }

    private static void cleanup() {
        frame.dispose();
    }
}
