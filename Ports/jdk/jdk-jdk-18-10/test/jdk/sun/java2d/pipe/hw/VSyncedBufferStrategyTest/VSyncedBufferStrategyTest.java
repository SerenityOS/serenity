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
/*
 * @test
 * @bug 6678218 6681745 6691737 8198613
 * @summary Tests that v-synced BufferStrategies works (if vsync is supported)
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @modules java.desktop/sun.java2d.pipe.hw
 * @compile -XDignore.symbol.file=true VSyncedBufferStrategyTest.java
 * @run main/manual/othervm VSyncedBufferStrategyTest
 */

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.ImageCapabilities;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class VSyncedBufferStrategyTest extends Canvas implements Runnable {

    private static final int BLOCK_W = 50;
    private static final int BLOCK_H = 200;

    BufferStrategy bs;
    Thread renderThread;

    int blockX = 10;
    int blockY = 10;

    private volatile boolean done = false;
    private volatile boolean requestVSync;
    private boolean currentBSVSynced;

    public VSyncedBufferStrategyTest(boolean requestVSync) {
        this.requestVSync = requestVSync;
        this.currentBSVSynced = !requestVSync;
        renderThread = new Thread(this);
        renderThread.start();
    }

    private static final BufferCapabilities defaultBC =
        new BufferCapabilities(
                new ImageCapabilities(true),
                new ImageCapabilities(true),
                null);

    private void createBS(boolean requestVSync) {
        if (bs != null && requestVSync == currentBSVSynced) {
            return;
        }

        BufferCapabilities bc = defaultBC;
        if (requestVSync) {
            bc = new sun.java2d.pipe.hw.ExtendedBufferCapabilities(
                    new ImageCapabilities(true),
                    new ImageCapabilities(true),
                    FlipContents.COPIED,
                    sun.java2d.pipe.hw.ExtendedBufferCapabilities.VSyncType.VSYNC_ON);
        }
        try {
            createBufferStrategy(2, bc);
        } catch (AWTException e) {
            System.err.println("Warning: cap is not supported: "+bc);
            e.printStackTrace();
            createBufferStrategy(2);
        }
        currentBSVSynced = requestVSync;
        bs = getBufferStrategy();
        String s =
            getParent() instanceof Frame ?
                ((Frame)getParent()).getTitle() : "parent";
        System.out.println("Created BS for \"" + s + "\" frame, bs="+bs);
    }

    @Override
    public void paint(Graphics g) {
    }
    @Override
    public void update(Graphics g) {
    }

    @Override
    public void run() {
        while (!isShowing()) {
            try { Thread.sleep(5); } catch (InterruptedException e) {}
        }
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        try {
            while (!done && isShowing()) {
                createBS(requestVSync);
                do {
                    step();
                    Graphics g = bs.getDrawGraphics();
                    render(g);
                    if (!bs.contentsRestored()) {
                        bs.show();
                    }
                } while (bs.contentsLost());
                Thread.yield();
            }
        } catch (Throwable e) {
            // since we're not bothering with proper synchronization, exceptions
            // may be thrown when the frame is closed
            if (isShowing()) {
                throw new RuntimeException(e);
            }
        }
    }

    int inc = 5;
    private void step() {
        blockX += inc;
        if (blockX > getWidth() - BLOCK_W - 10) {
            inc = -inc;
            blockX += inc;
        }
        if (blockX < 10) {
            inc = -inc;
            blockX += inc;
        }
    }

    private void render(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.black);
        g.fillRect(blockX, blockY, BLOCK_W, BLOCK_H);
    }

    private void setRequestVSync(boolean reqVSync) {
        requestVSync = reqVSync;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(BLOCK_W*10+20, BLOCK_H+20);
    }

    private static int frameNum = 0;
    private static Frame createAndShowBSFrame() {
        final Frame f = new Frame("Not V-Synced");

        int myNum;
        synchronized (VSyncedBufferStrategyTest.class) {
            myNum = frameNum++;
        }

        final VSyncedBufferStrategyTest component =
                new VSyncedBufferStrategyTest(false);
        f.setIgnoreRepaint(true);
        f.add("Center", component);

        Panel p = new Panel();

        Button b = new Button("Request VSync");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                f.setTitle("Possibly V-Synced");
                component.setRequestVSync(true);
            }
        });
        p.add(b);

        b = new Button("Relinquish VSync");
        b.addActionListener(new ActionListener() {
            int inc = 1;
            public void actionPerformed(ActionEvent e) {
                f.setTitle("Not V-Synced");
                component.setRequestVSync(false);
                f.setSize(f.getWidth()+inc, f.getHeight());
                inc = -inc;
            }
        });
        p.add(b);

        f.add("South", p);

        f.pack();
        f.setLocation(10, myNum * f.getHeight());
        f.setVisible(true);
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                component.done = true;
                f.dispose();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                component.done = true;
            }
        });

        return f;
    }

    private static final String description =
        "Tests that v-synced BufferStrategy works. Note that it in some\n" +
        "cases the v-sync can not be enabled, and it is accepted.\n" +
        "The following however is true: only one buffer strategy at a time can\n"+
        "be created v-synced. In order for other BS to become v-synced, the one\n"+
        "that currently is v-synched (or its window) needs to be disposed.\n" +
        "Try the following scenarios:\n" +
        "  - click the \"Request VSync\" button in one of the frames. If the\n"+
        "    behavior of the animation changes - the animation becomes smooth\n" +
        "    it had successfully created a v-synced BS. Note that the animation\n" +
        "    in other frames may also become smoother - this is a side-effect\n"+
        "    of one of the BS-es becoming v-synched\n" +
        "  - click the \"Relinquish VSync\" button on the same frame. If the\n"+
        "    behavior changes to the original (tearing)- it had successfully\n" +
        "    created a non-vsynced strategy.\n" +
        "  - next, try making another one v-synced. It should succeed.\n" +
        "  - next, try making another one v-synced - while there's already\n" +
        "    a v-synced frame. It should not succeed - meaning, it shouldn't\n" +
        "    appear to become smoother, and the behavior of the current v-synced\n" +
        "    frame shouldn't change.\n" +
        "\n" +
        "If there aren't any BufferStrategy-related exceptions or other\n" +
        "issues, and the scenarios worked, the test passed, otherwise it\n"+
        "failed.\n";

    private static void createAndShowDescGUI(final Frame f3, final Frame f1,
                                             final Frame f2)
        throws HeadlessException, RuntimeException
    {
        final JFrame desc =
            new JFrame("VSyncedBufferStrategyTest - Description");
        desc.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                f1.dispose();
                f2.dispose();
                f3.dispose();
                l.countDown();
            }
        });
        JPanel p = new JPanel();
        JButton bPassed = new JButton("Passed");
        bPassed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                desc.dispose();
                f1.dispose();
                f2.dispose();
                f3.dispose();
                l.countDown();
            }
        });
        JButton bFailed = new JButton("Failed");
        bFailed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                failed = true;
                desc.dispose();
                f1.dispose();
                f2.dispose();
                f3.dispose();
                l.countDown();
            }
        });
        p.setLayout(new FlowLayout());
        p.add(bPassed);
        p.add(bFailed);
        JTextArea ta = new JTextArea(24, 75);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setText(description);
        desc.add("Center", new JScrollPane(ta));
        desc.add("South", p);
        desc.pack();
        desc.setLocation(BLOCK_W*10+50, 0);
        desc.setVisible(true);
    }

    private static void createTestFrames() {
        Frame f1 = createAndShowBSFrame();
        Frame f2 = createAndShowBSFrame();
        Frame f3 = createAndShowBSFrame();
        createAndShowDescGUI(f1, f2, f3);
    }

    static boolean failed = false;
    static CountDownLatch l = new CountDownLatch(1);
    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                createTestFrames();
            }
        });
        l.await();
        if (failed) {
            throw new RuntimeException("Test FAILED");
        }
        System.out.println("Test PASSED");
    }

}
