/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6714836
 * @summary Tests if the maximum size of a JRootPane is appropriate.
 * @run main MaximumSizeTest
 */
import javax.swing.SwingUtilities;
import javax.swing.JRootPane;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

public class MaximumSizeTest {
    public static void main(String args[]) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JRootPane r = new JRootPane();
                r.getContentPane().add(new JLabel("foo"));
                System.out.println("Min Size: " + r.getMinimumSize());
                System.out.println("Preferred Size: " + r.getPreferredSize());

                Dimension d = r.getMaximumSize();
                if (d.width == 0 || d.height == 0) {
                    throw new RuntimeException("Maximum size is wrongly reported: " + d);
                }

                System.out.println("Max size: " + d);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            System.out.println(e.getCause());
            throw new RuntimeException(e.getMessage());
        }
    }
}
