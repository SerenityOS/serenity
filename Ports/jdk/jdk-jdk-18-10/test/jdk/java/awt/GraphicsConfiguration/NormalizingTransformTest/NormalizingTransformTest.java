/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 6373505
 * @summary Tests that the result of Toolkit.getScreenResolution() is
 * consistent with GraphicsConfiguration.getNormalizingTransform().
 * @author Dmitri.Trembovetski@Sun.COM: area=GraphicsConfiguration
 * @run main NormalizingTransformTest
 */

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;

public class NormalizingTransformTest {

    public static void main(String[] args) {
        GraphicsConfiguration gc =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();
        AffineTransform normTransform = gc.getNormalizingTransform();
        int dpiX = Toolkit.getDefaultToolkit().getScreenResolution();
        int normDpiX = (int)(normTransform.getScaleX() * 72.0);
        if (dpiX != normDpiX) {
            throw new RuntimeException(
                "Test FAILED. Toolkit.getScreenResolution()=" + dpiX +
                " GraphicsConfiguration.getNormalizingTransform()="+normDpiX);
        }
        System.out.println("Test PASSED. DPI="+normDpiX);
    }

}
