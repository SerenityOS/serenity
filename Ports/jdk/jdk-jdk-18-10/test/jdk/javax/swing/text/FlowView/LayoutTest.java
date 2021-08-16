/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 6452106 6606443 8161195
 * @author Peter Zhelezniakov
 * @library ../../regtesthelpers
 * @build Test JRobot SwingTestHelper
 * @run main/timeout=300 LayoutTest
 */

import javax.swing.text.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class LayoutTest extends SwingTestHelper {
    JTextPane text;

    public static void main(String[] args) throws Throwable {
        new LayoutTest().run(args);
    }

    protected Component createContentPane() {
        return text = new JTextPane();
    }

    @Test(value=10, onEDT=true)
    private void onEDT10() {
        requestAndWaitForFocus(text);
    }


    @Test(value=100, onEDT=true)
    private void prepare6452106() {
        text.setText("This is easily generated on my\nmachine");
        Document doc = text.getDocument();

        // wrap the long paragraph
        Dimension d = text.getPreferredSize();
        Dimension size = new Dimension(d.width * 2 / 3, d.height * 5);
        window.setSize(size);

        // place caret at the end of 2nd line
        Element p1 = doc.getDefaultRootElement().getElement(0);
        int pos = p1.getEndOffset();
        text.setCaretPosition(pos - 1);
    }

    @Test(value=110, onEDT=false)
    private void test6452106() {
        robot.setDelay(300);
        robot.hitKey(KeyEvent.VK_DELETE);
        robot.hitKey(KeyEvent.VK_SPACE);
        robot.hitKey(KeyEvent.VK_SPACE);
    }


    @Test(value=200, onEDT=true)
    private void prepare6606443() {
        text.setText("This is easily\ngenerated\non my machine");
        text.setSelectionStart(15);
        text.setSelectionEnd(24);
    }

    @Test(value=210, onEDT=false)
    private void test6606443() {
        robot.hitKey(KeyEvent.VK_ENTER);
    }
}
