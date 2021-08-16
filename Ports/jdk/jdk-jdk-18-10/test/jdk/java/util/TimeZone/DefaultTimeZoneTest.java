/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4296930 5033603 7092679
 * @summary Make sure that Java runtime detects the platform time zone
 * correctly. Also make sure that the system time zone detection code
 * detects the "Automatically adjust clock for daylight saving
 * changes" setting correctly on Windows.
 * @run applet/manual=yesno DefaultTimeZoneTest.html
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

public class DefaultTimeZoneTest extends JApplet implements Runnable {
    static final String FORMAT = "yyyy-MM-dd HH:mm:ss zzzz (XXX)";
    JLabel tzid;
    JLabel label;
    SimpleDateFormat sdf = new SimpleDateFormat(FORMAT);
    JButton button = new JButton("English");
    Thread clock;
    boolean english = false;

    @Override
    public void init() {
        tzid = new JLabel("Time zone ID: " + sdf.getTimeZone().getID(), SwingConstants.CENTER);
        tzid.setAlignmentX(Component.CENTER_ALIGNMENT);
        label = new JLabel(sdf.format(new Date()), SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(new ActionListener() {
                @Override
                @SuppressWarnings("deprecation")
                public void actionPerformed(ActionEvent e) {
                    english = (english == false);
                    Locale loc = english ? Locale.US : Locale.getDefault();
                    sdf = new SimpleDateFormat(FORMAT, loc);
                    button.setLabel(!english ? "English" : "Local");
                }
            });
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(tzid);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(button);
        getContentPane().add(panel);
    }

    @Override
    public void start() {
        clock = new Thread(this);
        clock.start();
    }

    @Override
    public void stop() {
        clock = null;
    }

    @Override
    public void run() {
        Thread me = Thread.currentThread();

        while (clock == me) {
            // Reset the default time zone so that
            // TimeZone.getDefault will detect the platform time zone
            TimeZone.setDefault(null);
            System.setProperty("user.timezone", "");
            TimeZone tz = TimeZone.getDefault();
            sdf.setTimeZone(tz);
            tzid.setText("Time zone ID: " + tz.getID());
            label.setText(sdf.format(new Date()));
            repaint();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
}
