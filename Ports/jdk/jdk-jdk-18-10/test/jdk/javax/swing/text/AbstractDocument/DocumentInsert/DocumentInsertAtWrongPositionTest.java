/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import javax.swing.*;
import javax.swing.text.*;

/*
 * @test
 * @bug 8151015
 * @summary JTextArea.insert() does not behave as expected with invalid position
 * @run main DocumentInsertAtWrongPositionTest
 */
public class DocumentInsertAtWrongPositionTest {
    public static void main(String[] args) throws Exception {
        JTextField te = new JTextField("1234567890");
        JTextPane tp = new JTextPane();
        tp.setText("1234567890");
        JTextArea ta = new JTextArea("1234567890");

        try {
            ta.insert("abc", 11);

            throw new RuntimeException("failed");
        } catch (IllegalArgumentException e) {
        }
        try {

            te.getDocument().insertString(11, "abc", new SimpleAttributeSet());

            throw new RuntimeException("failed");
        } catch (BadLocationException e) {
        }
        try {
            tp.getDocument().insertString(11, "abc", new SimpleAttributeSet());
            throw new RuntimeException("failed");
        } catch (BadLocationException e) {
        }
    }
}
