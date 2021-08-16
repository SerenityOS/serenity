/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211267
 * @summary verifies TextField.setFont()
 */

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class FontChangeTest {
    static Frame frame;
    static Panel p1;
    static TextField tf1;
    static boolean failed = false;

    public static void main(String[] args) throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                 try {
                     testFont1();
                 } catch (StackOverflowError soe) {
                     failed = true;
                 }
            }
        });
        frame.dispose();
        if (failed) {
            throw new Exception("Test failed");
        }
    }

    private static void testFont1() {
        frame = new Frame();
        frame.setLayout(new BorderLayout());
        p1 = new Panel();
        p1.setLayout(new FlowLayout());
        tf1 = new TextField("ABC");
        tf1.setFont(new Font("Dialog", Font.PLAIN, 12));
        p1.add(tf1);
        frame.add(p1, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        p1.setVisible(false);
        tf1.setText("xyz");
        tf1.setFont(new Font("Dialog", Font.PLAIN, 24));
        p1.setVisible(true);
        frame.pack();
    }
}
