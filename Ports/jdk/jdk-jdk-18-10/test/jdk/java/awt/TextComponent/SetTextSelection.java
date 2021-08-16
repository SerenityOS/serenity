/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.TextArea;
import java.awt.TextComponent;
import java.awt.TextField;

/**
 * @test
 * @key headful
 * @bug 6278172
 * @summary Tests that TextComponent#setText() preserves the selection
 */
public final class SetTextSelection {

    private static final String LONG_TEXT = "text field";
    private static final String SHORT_TEXT = "text";

    public static void main(String[] args) {
        testNoFrame(true);
        testNoFrame(false);
        for (int i = 0; i < 5; i++) {
            testFrame(true, i);
            testFrame(false, i);
        }
        testDisposedFrame(true);
        testDisposedFrame(false);
    }

    private static void testNoFrame(boolean field) {
        TextComponent tf = field ? new TextField(LONG_TEXT) :
                                   new TextArea(LONG_TEXT);
        tf.selectAll();
        tf.setText(SHORT_TEXT);
        test(tf);
    }

    private static void testDisposedFrame(boolean field) {
        Frame frame = new Frame();
        try {
            TextComponent tf = field ? new TextField(LONG_TEXT) :
                                       new TextArea(LONG_TEXT);
            frame.add(tf);
            frame.pack();
            tf.selectAll();
            frame.dispose();
            tf.setText(SHORT_TEXT);
            test(tf);
        } finally {
            frame.dispose();
        }
    }

    private static void testFrame(boolean field, int step) {
        Frame frame = new Frame();
        try {
            TextComponent tf = field ? new TextField(LONG_TEXT) :
                                       new TextArea(LONG_TEXT);
            if (step == 1) {
                frame.pack();
            }
            frame.add(tf);
            if (step == 2) {
                frame.pack();
            }
            tf.selectAll();
            if (step == 3) {
                frame.pack();
            }
            tf.setText(SHORT_TEXT);
            if (step == 4) {
                frame.pack();
            }
            test(tf);
        } finally {
            frame.dispose();
        }
    }

    private static void test(TextComponent tf) {
        String str = tf.getSelectedText();
        if (!str.equals(SHORT_TEXT)) {
            throw new RuntimeException(str);
        }
    }
}
