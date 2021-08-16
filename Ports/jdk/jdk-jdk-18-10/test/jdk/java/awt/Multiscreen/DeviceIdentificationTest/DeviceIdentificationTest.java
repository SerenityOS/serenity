/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6614214 8198613
 * @summary Verifies that we enter the fs mode on the correct screen.
 * Here is how to test: start the test on on a multi-screen system.
 * Verify that the display is correctly tracked by dragging the frame back
 * and forth between screens. Then verify that the correct device enters
 * the full-screen mode - when "Enter FS mode" is pressed it should enter on
 * the device where the frame is.
 *
 * Then change the order of the monitors in the DisplayProperties dialog,
 * (while the app is running) and see that it still works.
 * Restart the app, verify again.
 *
 * Now change the primary monitor on the system and verify with the
 * app running, as well as after restarting it that we still enter the
 * fs mode on the right device.
 *
 * @run main/manual/othervm DeviceIdentificationTest
 * @run main/manual/othervm -Dsun.java2d.noddraw=true DeviceIdentificationTest
 */

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DeviceIdentificationTest {

    public static void main(String args[]) {
        final Frame f = new Frame("DeviceIdentificationTest");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                f.dispose();
            }
        });
        f.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                f.setTitle("Currently on: "+
                           f.getGraphicsConfiguration().getDevice());
            }
        });

        Panel p = new Panel();
        Button b = new Button("Print Current Devices");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GraphicsDevice gds[] =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().
                        getScreenDevices();
                int i = 0;
                System.err.println("--- Devices: ---");
                for (GraphicsDevice gd : gds) {
                    System.err.println("Device["+i+"]= "+ gd);
                    System.err.println("  bounds = "+
                        gd.getDefaultConfiguration().getBounds());
                    i++;
                }
                System.err.println("-------------------");
            }
        });
        p.add(b);

        b = new Button("Print My Device");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GraphicsConfiguration gc = f.getGraphicsConfiguration();
                GraphicsDevice gd = gc.getDevice();
                System.err.println("--- My Device ---");
                System.err.println("Device  = "+ gd);
                System.err.println(" bounds = "+
                        gd.getDefaultConfiguration().getBounds());
            }
        });
        p.add(b);

        b = new Button("Create FS Frame on my Device");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GraphicsConfiguration gc = f.getGraphicsConfiguration();
                final GraphicsDevice gd = gc.getDevice();
                System.err.println("--- Creating FS Frame on Device ---");
                System.err.println("Device  = "+ gd);
                System.err.println(" bounds = "+
                        gd.getDefaultConfiguration().getBounds());
                final Frame fsf = new Frame("Full-screen Frame on dev"+gd, gc) {
                    public void paint(Graphics g) {
                        g.setColor(Color.green);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.red);
                        g.drawString("FS on device: "+gd, 200, 200);
                        g.drawString("Click to exit Full-screen.", 200, 250);
                    }
                };
                fsf.setUndecorated(true);
                fsf.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        gd.setFullScreenWindow(null);
                        fsf.dispose();
                    }
                });
                gd.setFullScreenWindow(fsf);
            }
        });
        p.add(b);
        f.add("North", p);

        p = new Panel();
        b = new Button("Test Passed");
        b.setBackground(Color.green);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Test Passed");
                f.dispose();
            }
        });
        p.add(b);
        b = new Button("Test Failed");
        b.setBackground(Color.red);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Test FAILED");
                f.dispose();
                throw new RuntimeException("Test FAILED");
            }
        });
        p.add(b);
        f.add("South", p);

        f.pack();
        f.setVisible(true);
    }
}
