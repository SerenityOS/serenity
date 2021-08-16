/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6579827
 * @requires (os.family == "windows")
 * @summary vista : JSlider on JColorchooser is not properly render or can't be seen completely
 * @author Pavel Porvatov
 * @library /test/lib
 * @build jdk.test.lib.OSVersion jdk.test.lib.Platform
   @run main bug6579827
 */

import jdk.test.lib.Platform;
import jdk.test.lib.OSVersion;

import javax.swing.*;
import java.awt.*;

public class bug6579827 {
    public static void main(String[] args) throws Exception {
        if (!Platform.isWindows() ||
                !OSVersion.current().equals(OSVersion.WINDOWS_VISTA)) {
            System.out.println("This test is only for Windows Vista. Skipped.");

            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                } catch (Exception e) {
                    e.printStackTrace();

                    throw new RuntimeException(e);
                }

                JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, 0);

                Dimension prefferdSize = slider.getPreferredSize();

                slider.setPaintTrack(false);
                slider.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);

                if (prefferdSize.equals(slider.getPreferredSize())) {
                    throw new RuntimeException();
                }
            }
        });
    }
}
