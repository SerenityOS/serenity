/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;



/* @test
   @bug 8160696
   @summary IllegalArgumentException: adding a component to a container on a different GraphicsDevice
   @author Mikhail Cherkasov
   @run main MoveToOtherScreenTest
   @key headful
*/
public class MoveToOtherScreenTest {

    private static volatile boolean twoDisplays = true;
    private static final Canvas canvas = new Canvas();
    private static final Frame[] frms = new JFrame[2];

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                GraphicsEnvironment ge = GraphicsEnvironment.
                        getLocalGraphicsEnvironment();
                GraphicsDevice[] gds = ge.getScreenDevices();
                if (gds.length < 2) {
                    System.out.println("Test requires at least 2 displays");
                    twoDisplays = false;
                    return;
                }
                for (int i = 0; i < 2; i++) {
                    GraphicsConfiguration conf = gds[i].getConfigurations()[0];
                    JFrame frm = new JFrame("Frame " + i);
                    frm.setLocation(conf.getBounds().x, 0); // On first screen
                    frm.setSize(new Dimension(400, 400));
                    frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frm.setVisible(true);
                    frms[i] = frm;
                }
                canvas.setBackground(Color.red);
                frms[0].add(canvas);
            }
        });
        if(!twoDisplays){
           return;
        }
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frms[1].add(canvas);
            }
        });
        for (Frame frm : frms) {
            frm.dispose();
        }
    }
}
