/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTextFieldUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;

/**
 * @test
 * @bug 8156217 8169922
 * @key headful
 * @summary Selected text is shifted on HiDPI display
 * @run main FPMethodCalledTest
 */
public class FPMethodCalledTest {

    private static JFrame frame;
    private static JTextField textField;

    public static void main(String[] args) throws Exception {

        for (Test test : TESTS) {
            test(test);
        }
    }

    static void test(final Test test) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(50);
            SwingUtilities.invokeAndWait(() -> {
                createAndShowGUI(test);
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(() -> {
                textField.select(1, 3);
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(() -> {
                Resultable resultable = test.resultable;
                if (!resultable.getResult()) {
                    throw new RuntimeException("Test fails for: " + resultable);
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    static void createAndShowGUI(Test test) {

        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        frame = new JFrame();
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new FlowLayout());

        String text = "AAAAAAA";
        textField = test.isPasswordField()
                ? new JPasswordField(text)
                : new JTextField(text);

        textField.setUI(new MetalTextFieldUI() {

            @Override
            public View create(Element elem) {
                return test.createView(elem);
            }
        });

        panel.add(textField);
        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private static final Test[] TESTS = {
        new Test() {
            @Override
            View createView(Element elem) {
                PlainViewINTAPI view = new PlainViewINTAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test() {
            @Override
            View createView(Element elem) {
                PlainViewFPAPI view = new PlainViewFPAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test() {
            @Override
            View createView(Element elem) {
                PlainViewMixedAPI view = new PlainViewMixedAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test() {
            @Override
            View createView(Element elem) {
                WrappedPlainViewINTAPI view = new WrappedPlainViewINTAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test() {
            @Override
            View createView(Element elem) {
                WrappedPlainViewFPAPI view = new WrappedPlainViewFPAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test() {
            @Override
            View createView(Element elem) {
                WrappedPlainViewMixedAPI view = new WrappedPlainViewMixedAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test(true) {

            @Override
            View createView(Element elem) {
                PasswordViewINTAPI view = new PasswordViewINTAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test(true) {

            @Override
            View createView(Element elem) {
                PasswordViewFPAPI view = new PasswordViewFPAPI(elem);
                resultable = view;
                return view;
            }
        },
        new Test(true) {

            @Override
            View createView(Element elem) {
                PasswordViewMixedAPI view = new PasswordViewMixedAPI(elem);
                resultable = view;
                return view;
            }
        }
    };

    static interface Resultable {

        boolean getResult();
    }

    static abstract class Test {

        Resultable resultable;
        final boolean isPasswordField;

        public Test() {
            this(false);
        }

        public Test(boolean isPasswordField) {
            this.isPasswordField = isPasswordField;
        }

        boolean isPasswordField() {
            return isPasswordField;
        }

        abstract View createView(Element elem);
    }

    static class PlainViewINTAPI extends PlainView implements Resultable {

        boolean drawLine = false;
        boolean drawSelected = false;
        boolean drawUnselected = false;

        public PlainViewINTAPI(Element elem) {
            super(elem);
        }

        @Override
        protected void drawLine(int lineIndex, Graphics g, int x, int y) {
            drawLine = true;
            super.drawLine(lineIndex, g, x, y);
        }

        @Override
        protected int drawSelectedText(Graphics g, int x, int y,
                int p0, int p1) throws BadLocationException {
            drawSelected = true;
            return super.drawSelectedText(g, x, y, p0, p1);
        }

        @Override
        protected int drawUnselectedText(Graphics g, int x, int y,
                int p0, int p1) throws BadLocationException {
            drawUnselected = true;
            return super.drawUnselectedText(g, x, y, p0, p1);
        }

        @Override
        public boolean getResult() {
            return drawLine && drawSelected && drawUnselected;
        }
    }

    static class PlainViewFPAPI extends PlainView implements Resultable {

        boolean drawLine = false;
        boolean drawSelected = false;
        boolean drawUnselected = false;

        public PlainViewFPAPI(Element elem) {
            super(elem);
        }

        @Override
        protected void drawLine(int lineIndex, Graphics2D g, float x, float y) {
            drawLine = true;
            super.drawLine(lineIndex, g, x, y);
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y,
                int p0, int p1) throws BadLocationException {
            drawSelected = true;
            return super.drawSelectedText(g, x, y, p0, p1);
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y,
                int p0, int p1) throws BadLocationException {
            drawUnselected = true;
            return super.drawUnselectedText(g, x, y, p0, p1);
        }

        @Override
        public boolean getResult() {
            return drawSelected;
        }
    }

    static class PlainViewMixedAPI extends PlainView implements Resultable {

        boolean isIntMethodCalled = false;
        boolean isFPMethodCalled = false;

        public PlainViewMixedAPI(Element elem) {
            super(elem);
        }

        @Override
        protected int drawSelectedText(Graphics g, int x, int y,
                int p0, int p1) throws BadLocationException {
            isIntMethodCalled = true;
            return super.drawSelectedText(g, x, y, p0, p1);
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y,
                int p0, int p1) throws BadLocationException {
            isFPMethodCalled = true;
            return super.drawSelectedText(g, x, y, p0, p1);
        }

        @Override
        public boolean getResult() {
            return !isIntMethodCalled && isFPMethodCalled;
        }
    }

    static class WrappedPlainViewINTAPI extends WrappedPlainView implements Resultable {

        boolean drawLine = false;
        boolean drawSelected = false;
        boolean drawUnselected = false;

        public WrappedPlainViewINTAPI(Element elem) {
            super(elem);
        }

        @Override
        protected void drawLine(int p0, int p1, Graphics g, int x, int y) {
            drawLine = true;
            super.drawLine(p0, p1, g, x, y);
        }

        @Override
        protected int drawSelectedText(Graphics g, int x, int y,
                int p0, int p1) throws BadLocationException {
            drawSelected = true;
            return super.drawSelectedText(g, x, y, p0, p1);
        }

        @Override
        protected int drawUnselectedText(Graphics g, int x, int y,
                int p0, int p1) throws BadLocationException {
            drawUnselected = true;
            return super.drawUnselectedText(g, x, y, p0, p1);
        }

        @Override
        public boolean getResult() {
            return drawLine && drawSelected && drawUnselected;
        }
    }

    static class WrappedPlainViewFPAPI extends WrappedPlainView implements Resultable {

        boolean drawLine = false;
        boolean drawSelected = false;
        boolean drawUnselected = false;

        public WrappedPlainViewFPAPI(Element elem) {
            super(elem);
        }

        @Override
        protected void drawLine(int p0, int p1, Graphics2D g, float x, float y) {
            drawLine = true;
            super.drawLine(p0, p1, g, x, y);
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y,
                int p0, int p1) throws BadLocationException {
            drawSelected = true;
            return super.drawSelectedText(g, x, y, p0, p1);
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y,
                int p0, int p1) throws BadLocationException {
            drawUnselected = true;
            return super.drawUnselectedText(g, x, y, p0, p1);
        }

        @Override
        public boolean getResult() {
            return drawLine && drawSelected && drawUnselected;
        }
    }

    static class WrappedPlainViewMixedAPI extends WrappedPlainView implements Resultable {

        boolean isIntMethodCalled = false;
        boolean isFPMethodCalled = false;

        public WrappedPlainViewMixedAPI(Element elem) {
            super(elem);
        }

        @Override
        protected int drawUnselectedText(Graphics g, int x, int y,
                int p0, int p1) throws BadLocationException {
            isIntMethodCalled = true;
            return super.drawUnselectedText(g, x, y, p0, p1);
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y,
                int p0, int p1) throws BadLocationException {
            isFPMethodCalled = true;
            return super.drawUnselectedText(g, x, y, p0, p1);
        }

        @Override
        public boolean getResult() {
            return !isIntMethodCalled && isFPMethodCalled;
        }
    }

    static class PasswordViewINTAPI extends PasswordView implements Resultable {

        boolean isIntMethodCalled = false;

        public PasswordViewINTAPI(Element elem) {
            super(elem);

        }

        @Override
        protected int drawEchoCharacter(Graphics g, int x, int y, char c) {
            isIntMethodCalled = true;
            return super.drawEchoCharacter(g, x, y, c);
        }

        @Override
        public boolean getResult() {
            return isIntMethodCalled;
        }
    }

    static class PasswordViewFPAPI extends PasswordView implements Resultable {

        boolean isFPMethodCalled = false;

        public PasswordViewFPAPI(Element elem) {
            super(elem);

        }

        @Override
        protected float drawEchoCharacter(Graphics2D g, float x, float y, char c) {
            isFPMethodCalled = true;
            return super.drawEchoCharacter(g, x, y, c);
        }

        @Override
        public boolean getResult() {
            return isFPMethodCalled;
        }
    }

    static class PasswordViewMixedAPI extends PasswordView implements Resultable {

        boolean isIntMethodCalled = false;
        boolean isFPMethodCalled = false;

        public PasswordViewMixedAPI(Element elem) {
            super(elem);

        }

        @Override
        protected int drawEchoCharacter(Graphics g, int x, int y, char c) {
            isIntMethodCalled = true;
            return super.drawEchoCharacter(g, x, y, c);
        }

        @Override
        protected float drawEchoCharacter(Graphics2D g, float x, float y, char c) {
            isFPMethodCalled = true;
            return super.drawEchoCharacter(g, x, y, c);
        }

        @Override
        public boolean getResult() {
            return !isIntMethodCalled && isFPMethodCalled;
        }
    }
}
