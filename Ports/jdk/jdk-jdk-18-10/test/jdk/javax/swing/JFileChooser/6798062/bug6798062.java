/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test %W% %E%
   @bug 6798062
   @summary Memory Leak on using getFiles of FileSystemView
   @author Pavel Porvatov
   @modules java.desktop/sun.awt
            java.desktop/sun.awt.shell
   @run applet/manual=done bug6798062.html
*/

import sun.awt.OSInfo;
import sun.awt.shell.ShellFolder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;

public class bug6798062 extends JApplet {

    private final JSlider slider = new JSlider(0, 100);

    private final JTextField tfLink = new JTextField();

    private final JButton btnStart = new JButton("Start");

    private final JButton btnStop = new JButton("Stop");

    private final JButton btnGC = new JButton("Run System.gc()");

    private ShellFolder folder;

    private Thread thread;

    public static void main(String[] args) {
        JFrame frame = new JFrame("bug6798062");

        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new bug6798062().initialize());

        frame.setVisible(true);
    }

    public void init() {
        add(initialize());
    }

    private JComponent initialize() {
        if (OSInfo.getOSType() != OSInfo.OSType.WINDOWS) {
            return new JLabel("The test is suitable only for Windows");
        }

        String tempDir = System.getProperty("java.io.tmpdir");

        if (tempDir.length() == 0) { // 'java.io.tmpdir' isn't guaranteed to be defined
            tempDir = System.getProperty("user.home");
        }

        System.out.println("Temp directory: " + tempDir);

        try {
            folder = ShellFolder.getShellFolder(new File(tempDir));
        } catch (FileNotFoundException e) {
            fail("Directory " + tempDir + " not found");
        }

        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setValue(10);

        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setEnabledState(false);

                thread = new MyThread(slider.getValue(), tfLink.getText());
                thread.start();
            }
        });

        btnStop.setEnabled(false);

        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                thread.interrupt();
                thread = null;

                setEnabledState(true);
            }
        });

        btnGC.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.gc();
            }
        });

        setEnabledState(true);

        JPanel pnButtons = new JPanel();

        pnButtons.setLayout(new BoxLayout(pnButtons, BoxLayout.X_AXIS));

        pnButtons.add(btnStart);
        pnButtons.add(btnStop);
        pnButtons.add(btnGC);

        tfLink.setMaximumSize(new Dimension(300, 20));

        JPanel pnContent = new JPanel();

        pnContent.setLayout(new BoxLayout(pnContent, BoxLayout.Y_AXIS));
        pnContent.add(new JLabel("Delay between listFiles() invocation (ms):"));
        pnContent.add(slider);
        pnContent.add(new JLabel("Provide link here:"));
        pnContent.add(tfLink);
        pnContent.add(pnButtons);

        return pnContent;
    }

    private void setEnabledState(boolean enabled) {
        slider.setEnabled(enabled);
        btnStart.setEnabled(enabled);
        btnStop.setEnabled(!enabled);
    }

    private static void fail(String msg) {
        throw new RuntimeException(msg);
    }

    private class MyThread extends Thread {
        private final int delay;

        private final ShellFolder link;

        private MyThread(int delay, String link) {
            this.delay = delay;

            ShellFolder linkFolder;

            try {
                linkFolder = ShellFolder.getShellFolder(new File(link));
            } catch (FileNotFoundException e) {
                e.printStackTrace();

                linkFolder = null;
            }

            this.link = linkFolder;
        }

        public void run() {
            while (!isInterrupted()) {
                folder.listFiles();
                if (link != null) {
                    try {
                        link.getLinkLocation();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e1) {
                        // The thread was interrupted
                        return;
                    }
                }
            }
        }
    }
}
