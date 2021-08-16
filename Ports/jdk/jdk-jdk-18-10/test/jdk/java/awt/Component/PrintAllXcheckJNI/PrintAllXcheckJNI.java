/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6736247
  @summary Component.printAll Invalid local JNI handle
  @library ../../regtesthelpers
  @build Util
  @author Dmitry Cherepanov: area=awt.component
  @run  main/othervm -Xcheck:jni PrintAllXcheckJNI
*/

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import test.java.awt.regtesthelpers.Util;

public class PrintAllXcheckJNI
{
    public static void main(String []s)
    {
        Frame frame = new Frame();
        frame.setBounds(100, 100, 100, 100);
        frame.setVisible(true);
        Util.waitForIdle(Util.createRobot());

        BufferedImage img = new BufferedImage(frame.getWidth(),
                                              frame.getHeight(),
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        frame.printAll(g);

        g.dispose();
        img.flush();
    }
}
