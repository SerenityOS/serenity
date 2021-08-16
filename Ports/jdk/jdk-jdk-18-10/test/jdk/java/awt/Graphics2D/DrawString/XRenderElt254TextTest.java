/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.MultipleGradientPaint.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

/**
 * @test
 * @key headful
 * @bug 8028722
 * @summary tests whether drawString with 254 characters causes the xrender
 *          pipeline to hang.
 * @author ceisserer
 */
public class XRenderElt254TextTest extends Frame implements Runnable {
    public volatile boolean success = false;

    public void run() {
        Image dstImg = getGraphicsConfiguration().createCompatibleVolatileImage(400, 400);
        Graphics2D g = (Graphics2D) dstImg.getGraphics();

        StringBuilder strBuilder = new StringBuilder(254);
        for (int c = 0; c < 254; c++) {
          strBuilder.append('a');
        }

        for (int i = 0; i < 100; i++) {
            g.drawString(strBuilder.toString(), 20, 20);
            Toolkit.getDefaultToolkit().sync();
        }
        success = true;
    }

    public static void main(String[] args) throws Exception {
        XRenderElt254TextTest test = new XRenderElt254TextTest();
        new Thread(test).start();

        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            if (test.success) {
            return; // Test finished successful
            }
        }

        throw new RuntimeException("Test Failed");
    }
}
