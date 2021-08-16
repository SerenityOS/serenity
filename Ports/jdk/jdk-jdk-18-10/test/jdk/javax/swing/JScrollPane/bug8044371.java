/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8044371
 * @summary setOneTouchExpandable functionality of JSplitPane will reduce vertical Scrollbar
 * @author Anton Nashatyrev
 * @library ..
 */


import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class bug8044371 implements AdjustmentListener {
    JSplitPane sptPane;
    int lastAdjust = 0;
    int initialAdjust = 0;

    public static void main(String[] args) throws Throwable {
        UIManager.setLookAndFeel(MetalLookAndFeel.class.getName());
        SwingTest.start(bug8044371.class);
    }

    public bug8044371(JFrame frame) {
        JPanel p = new JPanel();
        final JScrollPane jScrollPane = new JScrollPane(new JPanel()
        {{setPreferredSize(new Dimension(1500,1500));}},
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.getViewport().scrollRectToVisible(new Rectangle(1500, 1500, 0, 0));
        jScrollPane.getVerticalScrollBar().addAdjustmentListener(this);

        sptPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jScrollPane, p);
        sptPane.setDividerLocation(90);
        sptPane.setOneTouchExpandable(true);
        frame.getContentPane().add(sptPane);
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        System.out.println( "adjustmentValueChanged: "  + e.getValue());
        lastAdjust = e.getValue();
    }

    public void step1() {
        if (lastAdjust == 0) {
            throw new RuntimeException("Adjustment == 0");
        }
        initialAdjust =  lastAdjust;
        sptPane.setDividerLocation(0);
    }

    public void step2() {
        if (lastAdjust < initialAdjust) {
            throw new RuntimeException("Failed: Adjustment decreased: " + lastAdjust);
        }
        sptPane.setDividerLocation(90);
    }
    public void step3() {
        if (lastAdjust < initialAdjust) {
            throw new RuntimeException("Failed: Adjustment decreased: " + lastAdjust);
        }
    }
}
