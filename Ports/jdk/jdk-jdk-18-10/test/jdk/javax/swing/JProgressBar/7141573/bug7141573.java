/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 7141573
   @requires (os.family == "windows")
   @summary JProgressBar resize exception, if setStringPainted in Windows LAF
   @author Pavel Porvatov
*/

import javax.swing.*;
import java.awt.image.BufferedImage;

public class bug7141573 {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            System.out.println("WindowsLookAndFeel is not supported. The test bug7141573 is skipped.");

            return;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);

                JProgressBar bar = new JProgressBar();

                bar.setStringPainted(true);

                bar.setSize(100, 1);
                bar.paint(image.getGraphics());

                bar.setSize(1, 100);
                bar.paint(image.getGraphics());

                System.out.println("The test bug7141573 is passed.");
            }
        });
    }
}
