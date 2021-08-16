/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6771184
 * @summary Some methods in text package don't throw BadLocationException when expected
 * @author Pavel Porvatov
 */

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class bug6771184 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTextArea textArea = new JTextArea("Tested string");

                Highlighter highlighter = textArea.getHighlighter();
                Highlighter.HighlightPainter myPainter = new Highlighter.HighlightPainter() {
                    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
                    }
                };

                int negativeTestedData[][] = {{50, 0},
                        {-1, 1},
                        {-5, -4},
                        {Integer.MAX_VALUE, Integer.MIN_VALUE},
                        {Integer.MIN_VALUE, Integer.MAX_VALUE},
                        {Integer.MIN_VALUE, Integer.MIN_VALUE}};

                for (int[] data : negativeTestedData) {
                    try {
                        highlighter.addHighlight(data[0], data[1], myPainter);

                        throw new RuntimeException("Method addHighlight() does not throw BadLocationException for (" +
                                data[0] + ", " + data[1] + ") ");
                    } catch (BadLocationException e) {
                        // Ok
                    }

                    Object objRef;

                    try {
                        objRef = highlighter.addHighlight(0, 1, myPainter);
                    } catch (BadLocationException e) {
                        throw new RuntimeException("highlighter.addHighlight(0, 1, myPainter) throws exception", e);
                    }

                    try {
                        highlighter.changeHighlight(objRef, data[0], data[1]);

                        throw new RuntimeException("Method changeHighlight() does not throw BadLocationException for (" +
                                data[0] + ", " + data[1] + ") ");
                    } catch (BadLocationException e) {
                        // Ok
                    }
                }
            }
        });
    }
}
