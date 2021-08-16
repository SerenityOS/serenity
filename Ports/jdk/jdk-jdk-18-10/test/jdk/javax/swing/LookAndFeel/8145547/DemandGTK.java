/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @summary  Tests that GTK LaF is supported on solaris
            regardless of jdk.gtk.version flag values.
  @bug 8156121
  @key headful
  @requires os.family == "linux"
  @run main/othervm -Djdk.gtk.version=2 DemandGTK
  @run main/othervm -Djdk.gtk.version=3 DemandGTK
*/

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.awt.Robot;

public class DemandGTK {

    static JFrame frame;
    public static void createAndShow() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch(Exception cnf) {
            cnf.printStackTrace();
            throw new RuntimeException("GTK LaF must be supported");
        }
        frame = new JFrame("JFrame");
        frame.setSize(200, 200);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(DemandGTK::createAndShow);
        Robot robot = new Robot();
        robot.waitForIdle();
        robot.delay(1000);
        SwingUtilities.invokeAndWait( () -> {
            frame.setVisible(false);
            frame.dispose();
        });

    }
}

