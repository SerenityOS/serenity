/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6526631
 * @summary Resizes right-oriented scroll pane
 * @author Sergey Malenkov
 * @library ..
 */

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;

public class Test6526631 {

    private static final int COLS = 90;
    private static final int ROWS = 50;
    private static final int OFFSET = 10;

    public static void main(String[] args) throws Throwable {
        SwingTest.start(Test6526631.class);
    }

    private final JScrollPane pane;
    private final JFrame frame;

    public Test6526631(JFrame frame) {
        this.pane = new JScrollPane(new JTextArea(ROWS, COLS));
        this.pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        this.frame = frame;
        this.frame.add(this.pane);
    }

    private void update(int offset) {
        Dimension size = this.frame.getSize();
        size.width += offset;
        this.frame.setSize(size);
    }

    public void validateFirst() {
        validateThird();
        update(OFFSET);
    }

    public void validateSecond() {
        validateThird();
        update(-OFFSET);
    }

    public void validateThird() {
        JViewport viewport = this.pane.getViewport();
        JScrollBar scroller = this.pane.getHorizontalScrollBar();
        if (!scroller.getComponentOrientation().equals(ComponentOrientation.RIGHT_TO_LEFT)) {
            throw new Error("unexpected component orientation");
        }
        int value = scroller.getValue();
        if (value != 0) {
            throw new Error("unexpected scroll value");
        }
        int extent = viewport.getExtentSize().width;
        if (extent != scroller.getVisibleAmount()) {
            throw new Error("unexpected visible amount");
        }
        int size = viewport.getViewSize().width;
        if (size != scroller.getMaximum()) {
            throw new Error("unexpected maximum");
        }
        int pos = size - extent - value;
        if (pos != viewport.getViewPosition().x) {
            throw new Error("unexpected position");
        }
    }
}
