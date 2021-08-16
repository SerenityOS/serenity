/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8051626
 * @summary Ensure no failure when using Java Accessibility Utility with security manager
 * @modules java.desktop jdk.accessibility
 *
 * @run main/othervm -Djava.security.manager=allow Bug8051626
 */

import com.sun.java.accessibility.util.AWTEventMonitor;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Bug8051626 {

    public static void main(final String[] args) throws InterruptedException,
                                                        InvocationTargetException {
            final Bug8051626 app = new Bug8051626();
            app.test();
        }

    private void test() throws InterruptedException, InvocationTargetException {
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final JFrame frame = new JFrame("Bug 8051626");
                try {
                    final JPanel panel = new JPanel();
                    final JButton okButton = new JButton("OK");
                    panel.add(okButton);
                    frame.getContentPane().add(panel);
                    frame.setMinimumSize(new Dimension(300, 180));
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setLocation(400, 300);
                    frame.setVisible(true);
                    // If the security manager is on this should not cause an exception.
                    // Prior to the 8051626 fix it would as follows:
                    // java.security.AccessControlException:
                    //   access denied ("java.lang.RuntimePermission" "accessClassInPackage.com.sun.java.accessibility.util")
                    AWTEventMonitor.getComponentWithFocus();
                } finally {
                    frame.dispose();
                }
            }
        });
    }

}
