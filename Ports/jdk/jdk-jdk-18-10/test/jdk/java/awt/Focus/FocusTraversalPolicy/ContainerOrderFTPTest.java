/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 8025001
  @summary Tests java.awt.ContainerOrderFocusTraversalPolicy functionality.
  @run main ContainerOrderFTPTest
*/

import java.awt.Frame;
import java.awt.Button;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.ContainerOrderFocusTraversalPolicy;

public class ContainerOrderFTPTest {

    private final ContainerOrderFocusTraversalPolicy coftp;
    private final Frame frame;
    private final Button b1;
    private final Button b2;
    private final String expectedTraversal;

    public ContainerOrderFTPTest() {
        expectedTraversal = "B1B2F1";
        b1 = new Button("B1");
        b2 = new Button("B2");
        frame = new Frame("F1");

        frame.setLayout(new FlowLayout());
        frame.setSize(200, 200);
        coftp = new ContainerOrderFocusTraversalPolicy();
        frame.setFocusTraversalPolicy(coftp);
        frame.add(b1);
        frame.add(b2);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        ContainerOrderFTPTest test = new ContainerOrderFTPTest();
        test.performTest();
        test.dispose();
    }

    public void performTest() {
        int count = 0;
        Component comp = coftp.getFirstComponent(frame);
        String traversal = "";
        do {
            comp = coftp.getComponentAfter(frame, comp);
            if (comp instanceof Button) {
                traversal += ((Button)comp).getLabel();
            } else if (comp instanceof Frame) {
                traversal += ((Frame)comp).getTitle();
            }
            count++;
        } while(count < 3);

        if (!expectedTraversal.equals(traversal)) {
            dispose();
            throw new RuntimeException("Incorrect Traversal. Expected : "
                + expectedTraversal + "Actual : " + traversal);
        }
    }

    public void dispose() {
        frame.dispose();
    }
}
