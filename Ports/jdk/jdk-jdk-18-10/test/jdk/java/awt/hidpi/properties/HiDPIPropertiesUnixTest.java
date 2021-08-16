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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.UIManager;

/*
 * @test
 * @key headful
 * @bug 8137571
 * @summary Linux HiDPI Graphics support
 * @author Alexander Scherbatiy
 * @requires (os.family == "linux" | os.family == "mac")
 * @run main/othervm -Dsun.java2d.uiScale.enabled=false
 *                   -Dsun.java2d.uiScale=2
 *                    HiDPIPropertiesUnixTest UISCALE_DISABLED
 * @run main/othervm -Dsun.java2d.uiScale.enabled=true
 *                   -Dsun.java2d.uiScale=3
 *                    HiDPIPropertiesUnixTest UISCALE_3
 * @run main/othervm -Dsun.java2d.uiScale=4
 *                    HiDPIPropertiesUnixTest UISCALE_4
 */
public class HiDPIPropertiesUnixTest {

    public static void main(String[] args) throws Exception {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        String testCase = args[0];
        switch (testCase) {
            case "UISCALE_DISABLED":
                testScale(1.0, 1.0);
                break;
            case "UISCALE_3":
                testScale(3.0, 3.0);
                break;
            case "UISCALE_4":
                testScale(4.0, 4.0);
                break;
            default:
                throw new RuntimeException("Unknown test case: " + testCase);
        }
    }

    private static void testScale(double scaleX, double scaleY) {

        Dialog dialog = new Dialog((Frame) null, true) {

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                AffineTransform tx = ((Graphics2D) g).getTransform();
                dispose();
                if (scaleX != tx.getScaleX() || scaleY != tx.getScaleY()) {
                    throw new RuntimeException(String.format("Wrong scale:"
                            + "[%f, %f] instead of [%f, %f].",
                            tx.getScaleX(), tx.getScaleY(), scaleX, scaleY));
                }
            }
        };
        dialog.setSize(200, 300);
        dialog.setVisible(true);
    }
}
