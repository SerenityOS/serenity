/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5041219 5101561 5035272 5096011 5101712 5098624 8198613
 * @summary Here are a few assertions worth verification:
 *  - the fullscreen window is positioned at 0,0
 *  - the fs window appears on the correct screen
 *  - if the exclusive FS mode is supported, no other widndow should
 *    overlap the fs window (including the taskbar).
 *    You could, however, alt+tab out of a fullscreen window, or at least
 *    minimize it (if you've entered the fs mode with a Window, you'll need
 *    to minimize the owner frame).
 *    Note that there may be issues with FS exclusive mode with ddraw and
 *    multiple fullscreen windows (one per device).
 *  - if display mode is supported that it did change
 *  - that the original display mode is restored once
 *    the ws window is disposed
 *  All of the above should work with and w/o DirectDraw
 *  (-Dsun.java2d.noddraw=true) on windows, and w/ and w/o opengl on X11
 *  (-Dsun.java2d.opengl=True).
 * @run main/manual/othervm -Dsun.java2d.pmoffscreen=true MultimonFullscreenTest
 * @run main/manual/othervm -Dsun.java2d.pmoffscreen=false MultimonFullscreenTest
 * @run main/manual/othervm -Dsun.java2d.d3d=True MultimonFullscreenTest
 * @run main/manual/othervm -Dsun.java2d.noddraw=true MultimonFullscreenTest
 * @run main/manual/othervm MultimonFullscreenTest
 */

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.HashMap;
import java.util.Random;

/**
 */

public class MultimonFullscreenTest extends Frame implements ActionListener {
    GraphicsDevice  defDev = GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice();
    GraphicsDevice  gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment().
            getScreenDevices();
    HashMap<Button, GraphicsDevice> deviceMap;

    private static boolean dmChange = false;
    static boolean setNullOnDispose = false;
    static boolean useFSFrame = true;
    static boolean useFSWindow = false;
    static boolean useFSDialog = false;
    static boolean useBS = false;
    static boolean runRenderLoop = false;
    static boolean addHWChildren = false;
    static volatile boolean done = true;

    public MultimonFullscreenTest(String title) {
        super(title);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        Panel p = new Panel();
        deviceMap = new HashMap<Button, GraphicsDevice>(gd.length);
        int num = 0;
        for (GraphicsDevice dev : gd) {
            Button b;
            if (dev == defDev) {
                b = new Button("Primary screen: " + num);
                System.out.println("Primary Dev : " + dev + " Bounds: " +
                        dev.getDefaultConfiguration().getBounds());
            } else {
                b = new Button("Secondary screen " + num);
                System.out.println("Secondary Dev : " + dev + " Bounds: " +
                        dev.getDefaultConfiguration().getBounds());
            }
            b.addActionListener(this);
            p.add(b);
            deviceMap.put(b, dev);
            num++;
        }
        add("South", p);
        Panel p1 = new Panel();
        p1.setLayout(new GridLayout(2,0));
        Checkbox cb = new Checkbox("Change DM on entering FS");
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                dmChange = ((Checkbox)e.getSource()).getState();
            }
        });
        p1.add(cb);
//        cb = new Checkbox("Exit FS on window dispose");
//        cb.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent e) {
//                setNullOnDispose = ((Checkbox)e.getSource()).getState();
//            }
//        });
//        p1.add(cb);
        CheckboxGroup cbg = new CheckboxGroup();
        cb = new Checkbox("Use Frame to enter FS", cbg, true);
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useFSFrame = true;
                useFSWindow = false;
                useFSDialog = false;
            }
        });
        p1.add(cb);
        cb = new Checkbox("Use Window to enter FS", cbg, false);
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useFSFrame = false;
                useFSWindow = true;
                useFSDialog = false;
            }
        });
        p1.add(cb);
        cb = new Checkbox("Use Dialog to enter FS", cbg, false);
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useFSFrame = false;
                useFSWindow = false;
                useFSDialog = true;
            }
        });
        p1.add(cb);
        cb = new Checkbox("Run render loop");
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                runRenderLoop = ((Checkbox)e.getSource()).getState();
            }
        });
        p1.add(cb);
        cb = new Checkbox("Use BufferStrategy in render loop");
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useBS = ((Checkbox)e.getSource()).getState();
            }
        });
        p1.add(cb);
        cb = new Checkbox("Add Children to FS window");
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                addHWChildren = ((Checkbox)e.getSource()).getState();
            }
        });
        p1.add(cb);
        add("North", p1);

        pack();
        setVisible(true);
    }

    Font f = new Font("Dialog", Font.BOLD, 24);
    Random rnd = new Random();
    public void renderDimensions(Graphics g, Rectangle rectWndBounds,
                                 GraphicsConfiguration gc) {
        g.setColor(new Color(rnd.nextInt(0xffffff)));
        g.fillRect(0, 0, rectWndBounds.width, rectWndBounds.height);

        g.setColor(new Color(rnd.nextInt(0xffffff)));
        Rectangle rectStrBounds;

        g.setFont(f);

        rectStrBounds = g.getFontMetrics().
                getStringBounds(rectWndBounds.toString(), g).getBounds();
        rectStrBounds.height += 30;
        g.drawString(rectWndBounds.toString(), 50, rectStrBounds.height);
        int oldHeight = rectStrBounds.height;
        String isFSupported = "Exclusive Fullscreen mode supported: " +
                              gc.getDevice().isFullScreenSupported();
        rectStrBounds = g.getFontMetrics().
                getStringBounds(isFSupported, g).getBounds();
        rectStrBounds.height += (10 + oldHeight);
        g.drawString(isFSupported, 50, rectStrBounds.height);

        oldHeight = rectStrBounds.height;
        String isDMChangeSupported = "Display Mode Change supported: " +
                              gc.getDevice().isDisplayChangeSupported();
        rectStrBounds = g.getFontMetrics().
                getStringBounds(isDMChangeSupported, g).getBounds();
        rectStrBounds.height += (10 + oldHeight);
        g.drawString(isDMChangeSupported, 50, rectStrBounds.height);

        oldHeight = rectStrBounds.height;
        String usingBS = "Using BufferStrategy: " + useBS;
        rectStrBounds = g.getFontMetrics().
                getStringBounds(usingBS, g).getBounds();
        rectStrBounds.height += (10 + oldHeight);
        g.drawString(usingBS, 50, rectStrBounds.height);

        final String m_strQuitMsg = "Double-click to dispose FullScreen Window";
        rectStrBounds = g.getFontMetrics().
                getStringBounds(m_strQuitMsg, g).getBounds();
        g.drawString(m_strQuitMsg,
                (rectWndBounds.width - rectStrBounds.width) / 2,
                (rectWndBounds.height - rectStrBounds.height) / 2);


    }

    public void actionPerformed(ActionEvent ae) {
        GraphicsDevice dev = deviceMap.get(ae.getSource());
        System.err.println("Setting FS on device:"+dev);
        final Window fsWindow;

        if (useFSWindow) {
            fsWindow = new Window(this, dev.getDefaultConfiguration()) {
                public void paint(Graphics g) {
                    renderDimensions(g, getBounds(),
                                     this.getGraphicsConfiguration());
                }
            };
        } else if (useFSDialog) {
            fsWindow = new Dialog((Frame)null, "FS Dialog on device "+dev, false,
                                 dev.getDefaultConfiguration());
            fsWindow.add(new Component() {
                public void paint(Graphics g) {
                    renderDimensions(g, getBounds(),
                                     this.getGraphicsConfiguration());
                }
            });
        } else {
            fsWindow = new Frame("FS Frame on device "+dev,
                                 dev.getDefaultConfiguration())
            {
                public void paint(Graphics g) {
                    renderDimensions(g, getBounds(),
                                     this.getGraphicsConfiguration());
                }
            };
            if (addHWChildren) {
                fsWindow.add("South", new Panel() {
                    public void paint(Graphics g) {
                        g.setColor(Color.red);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                });
                fsWindow.add("North", new Button("Button, sucka!"));
            }
        }
        fsWindow.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    done = true;
                    fsWindow.dispose();
                }
            }
        });

        fsWindow.addWindowListener(new WindowHandler());
        dev.setFullScreenWindow(fsWindow);
        if (dmChange && dev.isDisplayChangeSupported()) {
            DisplayMode dms[] = dev.getDisplayModes();
            DisplayMode myDM = null;
            for (DisplayMode dm : dms) {
                if (dm.getWidth() == 800 && dm.getHeight() == 600 &&
                    (dm.getBitDepth() >= 16 ||
                     dm.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI) &&
                     (dm.getRefreshRate() >= 60 ||
                      dm.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN))
                {
                    myDM = dm;
                    break;
                }
            }
            if (myDM != null) {
                System.err.println("Setting Display Mode: "+
                        myDM.getWidth() + "x" + myDM.getHeight() + "x" +
                        myDM.getBitDepth() + "@" + myDM.getRefreshRate() +
                        "Hz on device" + dev);
                dev.setDisplayMode(myDM);
            } else {
                System.err.println("Can't find suitable display mode.");
            }
        }
        done = false;
        if (runRenderLoop) {
            Thread updateThread = new Thread(new Runnable() {
                public void run() {
                    BufferStrategy bs = null;
                    if (useBS) {
                        fsWindow.createBufferStrategy(2);
                        bs = fsWindow.getBufferStrategy();
                    }
                    while (!done) {
                        if (useBS) {
                            Graphics g = bs.getDrawGraphics();
                            renderDimensions(g, fsWindow.getBounds(),
                                           fsWindow.getGraphicsConfiguration());
                            bs.show();
                        } else {
                            fsWindow.repaint();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {}
                    }
                    if (useBS) {
                        bs.dispose();
                    }
                }
            });
            updateThread.start();
        }
    }

    public static void main(String args[]) {
        for (String s : args) {
            if (s.equalsIgnoreCase("-dm")) {
                System.err.println("Do Display Change after entering FS mode");
                dmChange = true;
            } else if (s.equalsIgnoreCase("-usewindow")) {
                System.err.println("Using Window to enter FS mode");
                useFSWindow = true;
            } else if (s.equalsIgnoreCase("-setnull")) {
                System.err.println("Setting null FS window on dispose");
                setNullOnDispose = true;
            } else {
                System.err.println("Usage: MultimonFullscreenTest " +
                        "[-dm][-usewindow][-setnull]");
            }

        }
        MultimonFullscreenTest fs =
                new MultimonFullscreenTest("Test Full Screen");
    }
    class WindowHandler extends WindowAdapter {
        public void windowClosing(WindowEvent we) {
            done = true;
            Window w = (Window)we.getSource();
            if (setNullOnDispose) {
                w.getGraphicsConfiguration().getDevice().setFullScreenWindow(null);
            }
            w.dispose();
        }
    }
}
