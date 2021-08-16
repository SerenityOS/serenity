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
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;

import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.event.KeyEvent.VK_LEFT;
import static javax.swing.SwingUtilities.invokeAndWait;

/*
 * @test
 * @key headful
 * @bug 6968363
 * @summary Ensures that a custom document may not extend AbstractDocument
 * @author Sergey Malenkov
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main Test6968363
 */
public class Test6968363 implements Runnable, Thread.UncaughtExceptionHandler {
    private JFrame frame;

    public static void main(String[] args) throws Exception {
        Runnable task = new Test6968363();
        invokeAndWait(task);
        ExtendedRobot robot = new ExtendedRobot();
        robot.waitForIdle(100);
        robot.keyPress(VK_LEFT);
        robot.waitForIdle(100);
        robot.keyRelease(VK_LEFT);
        robot.waitForIdle(100);
        invokeAndWait(task);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace();
        System.exit(1);
    }

    @Override
    public void run() {
        if (this.frame == null) {
            Thread.setDefaultUncaughtExceptionHandler(this);
            this.frame = new JFrame(getClass().getSimpleName());
            this.frame.add(NORTH, new JLabel("Copy Paste a HINDI text into the field below"));
            this.frame.add(SOUTH, new JTextField(new MyDocument(), "\u0938", 10));
            this.frame.pack();
            this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.frame.setLocationRelativeTo(null);
            this.frame.setVisible(true);
        } else {
            this.frame.dispose();
            this.frame = null;
        }
    }

    private static class MyDocument implements Document {
        private final Document document = new PlainDocument();

        @Override
        public int getLength() {
            return this.document.getLength();
        }

        @Override
        public void addDocumentListener(DocumentListener listener) {
            this.document.addDocumentListener(listener);
        }

        @Override
        public void removeDocumentListener(DocumentListener listener) {
            this.document.removeDocumentListener(listener);
        }

        @Override
        public void addUndoableEditListener(UndoableEditListener listener) {
            this.document.addUndoableEditListener(listener);
        }

        @Override
        public void removeUndoableEditListener(UndoableEditListener listener) {
            this.document.removeUndoableEditListener(listener);
        }

        @Override
        public Object getProperty(Object key) {
            return this.document.getProperty(key);
        }

        @Override
        public void putProperty(Object key, Object value) {
            this.document.putProperty(key, value);
        }

        @Override
        public void remove(int offset, int length) throws BadLocationException {
            this.document.remove(offset, length);
        }

        @Override
        public void insertString(int offset, String string, AttributeSet set) throws BadLocationException {
            for (int i = 0; i < string.length(); i++) {
                System.out.println("i: " + i + "; ch: " + Integer.toHexString(string.charAt(i)));
            }
            this.document.insertString(offset, string, set);
        }

        @Override
        public String getText(int offset, int length) throws BadLocationException {
            return this.document.getText(offset, length);
        }

        @Override
        public void getText(int offset, int length, Segment segment) throws BadLocationException {
            this.document.getText(offset, length, segment);
        }

        @Override
        public Position getStartPosition() {
            return this.document.getStartPosition();
        }

        @Override
        public Position getEndPosition() {
            return this.document.getEndPosition();
        }

        @Override
        public Position createPosition(int offset) throws BadLocationException {
            return this.document.createPosition(offset);
        }

        @Override
        public Element[] getRootElements() {
            Element[] elements = this.document.getRootElements();
            Element[] wrappers = new Element[elements.length];
            for (int i = 0; i < elements.length; i++) {
                wrappers[i] = new MyElement(elements[i]);
            }
            return wrappers;
        }

        @Override
        public Element getDefaultRootElement() {
            return new MyElement(this.document.getDefaultRootElement());
        }

        @Override
        public void render(Runnable task) {
            this.document.render(task);
        }

        private class MyElement implements Element {
            private final Element element;

            private MyElement(Element element) {
                this.element = element;
            }

            @Override
            public Document getDocument() {
                return MyDocument.this;
            }

            @Override
            public Element getParentElement() {
                return new MyElement(this.element.getParentElement());
            }

            @Override
            public String getName() {
                return this.element.getName();
            }

            @Override
            public AttributeSet getAttributes() {
                return this.element.getAttributes();
            }

            @Override
            public int getStartOffset() {
                return this.element.getStartOffset();
            }

            @Override
            public int getEndOffset() {
                return this.element.getEndOffset();
            }

            @Override
            public int getElementIndex(int offset) {
                return this.element.getElementIndex(offset);
            }

            @Override
            public int getElementCount() {
                return this.element.getElementCount();
            }

            @Override
            public Element getElement(int index) {
                return new MyElement(this.element.getElement(index));
            }

            @Override
            public boolean isLeaf() {
                return this.element.isLeaf();
            }
        }
    }
}
