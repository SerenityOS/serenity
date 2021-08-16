/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.swingset3.demos.togglebutton;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import com.sun.swingset3.demos.ResourceManager;

/*
 * The LayoutControlPanel contains controls for setting an
 * AbstractButton's horizontal and vertical text position and
 * horizontal and vertical alignment.
 */
public final class LayoutControlPanel extends JPanel implements SwingConstants {

    private static final Dimension VGAP20 = new Dimension(1, 20);
    private static final ResourceManager resourceManager = ToggleButtonDemo.resourceManager;
    public static final String CONTENT_ALIGNMENT = resourceManager.getString("LayoutControlPanel.contentalignment_label");
    public static final String TEXT_POSITION = resourceManager.getString("LayoutControlPanel.textposition_label");
    private final boolean absolutePositions;
    private ToggleButtonDemo demo = null;

    // private ComponentOrientChanger componentOrientChanger = null;
    LayoutControlPanel(ToggleButtonDemo demo) {
        this.demo = demo;

        // this.componentOrientationChanger = componentOrientationChanger;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);
        setAlignmentY(TOP_ALIGNMENT);

        JLabel l;

        // If SwingSet has a ComponentOrientationChanger, then include control
        // for choosing between absolute and relative positioning.  This will
        // only happen when we're running on JDK 1.2 or above.
        //
        // if(componentOrientationChanger != null ) {
        //     l = new JLabel("Positioning:");
        //     add(l);
        //
        //    ButtonGroup group = new ButtonGroup();
        //    PositioningListener positioningListener = new PositioningListener();
        //    JRadioButton absolutePos = new JRadioButton("Absolute");
        //    absolutePos.setMnemonic('a');
        //    absolutePos.setToolTipText("Text/Content positioning is independent of line direction");
        //    group.add(absolutePos);
        //    absolutePos.addItemListener(positioningListener);
        //    add(absolutePos);
        //
        //    JRadioButton relativePos = new JRadioButton("Relative");
        //    relativePos.setMnemonic('r');
        //    relativePos.setToolTipText("Text/Content positioning depends on line direction.");
        //    group.add(relativePos);
        //    relativePos.addItemListener(positioningListener);
        //    add(relativePos);
        //
        //    add(Box.createRigidArea(demo.VGAP20));
        //
        //    absolutePositions = false;
        //    relativePos.setSelected(true);
        //
        //    componentOrientationChanger.addActionListener( new OrientationChangeListener() );
        //} else {
        absolutePositions = true;
        //}

        DirectionPanel textPosition = new DirectionPanel(true, "E", new TextPositionListener());
        DirectionPanel labelAlignment = new DirectionPanel(true, "C", new LabelAlignmentListener());

        // Make sure the controls' text position and label alignment match
        // the initial value of the associated direction panel.
        for (JComponent control : demo.getCurrentControls()) {
            setPosition(control, RIGHT, CENTER);
            setAlignment(control, CENTER, CENTER);
        }

        l = new JLabel(TEXT_POSITION);
        add(l);
        add(textPosition);

        add(Box.createRigidArea(VGAP20));

        l = new JLabel(CONTENT_ALIGNMENT);
        add(l);
        add(labelAlignment);

        add(Box.createGlue());
    }

    // Text Position Listener
    private class TextPositionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JRadioButton rb = (JRadioButton) e.getSource();
            if (!rb.isSelected()) {
                return;
            }
            String cmd = rb.getActionCommand();
            int hPos, vPos;
            switch (cmd) {
                case "NW":
                    hPos = LEFT;
                    vPos = TOP;
                    break;
                case "N":
                    hPos = CENTER;
                    vPos = TOP;
                    break;
                case "NE":
                    hPos = RIGHT;
                    vPos = TOP;
                    break;
                case "W":
                    hPos = LEFT;
                    vPos = CENTER;
                    break;
                case "C":
                    hPos = CENTER;
                    vPos = CENTER;
                    break;
                case "E":
                    hPos = RIGHT;
                    vPos = CENTER;
                    break;
                case "SW":
                    hPos = LEFT;
                    vPos = BOTTOM;
                    break;
                case "S":
                    hPos = CENTER;
                    vPos = BOTTOM;
                    break;
                /*if(cmd.equals("SE"))*/
                default:
                    hPos = RIGHT;
                    vPos = BOTTOM;
                    break;
            }
            for (JComponent control : demo.getCurrentControls()) {
                setPosition(control, hPos, vPos);
            }
            demo.invalidate();
            demo.validate();
            demo.repaint();
        }
    }

    // Label Alignment Listener
    private class LabelAlignmentListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JRadioButton rb = (JRadioButton) e.getSource();
            if (!rb.isSelected()) {
                return;
            }
            String cmd = rb.getActionCommand();
            int hPos, vPos;
            switch (cmd) {
                case "NW":
                    hPos = LEFT;
                    vPos = TOP;
                    break;
                case "N":
                    hPos = CENTER;
                    vPos = TOP;
                    break;
                case "NE":
                    hPos = RIGHT;
                    vPos = TOP;
                    break;
                case "W":
                    hPos = LEFT;
                    vPos = CENTER;
                    break;
                case "C":
                    hPos = CENTER;
                    vPos = CENTER;
                    break;
                case "E":
                    hPos = RIGHT;
                    vPos = CENTER;
                    break;
                case "SW":
                    hPos = LEFT;
                    vPos = BOTTOM;
                    break;
                case "S":
                    hPos = CENTER;
                    vPos = BOTTOM;
                    break;
                /*if(cmd.equals("SE"))*/
                default:
                    hPos = RIGHT;
                    vPos = BOTTOM;
                    break;
            }
            for (JComponent control : demo.getCurrentControls()) {
                setAlignment(control, hPos, vPos);
                control.invalidate();
            }
            demo.invalidate();
            demo.validate();
            demo.repaint();
        }
    }

    // Position
    void setPosition(Component c, int hPos, int vPos) {
        boolean ltr = c.getComponentOrientation().isLeftToRight();
        if (absolutePositions) {
            if (hPos == LEADING) {
                hPos = ltr ? LEFT : RIGHT;
            } else if (hPos == TRAILING) {
                hPos = ltr ? RIGHT : LEFT;
            }
        } else if (hPos == LEFT) {
            hPos = ltr ? LEADING : TRAILING;
        } else if (hPos == RIGHT) {
            hPos = ltr ? TRAILING : LEADING;
        }
        if (c instanceof AbstractButton) {
            AbstractButton x = (AbstractButton) c;
            x.setHorizontalTextPosition(hPos);
            x.setVerticalTextPosition(vPos);
        } else if (c instanceof JLabel) {
            JLabel x = (JLabel) c;
            x.setHorizontalTextPosition(hPos);
            x.setVerticalTextPosition(vPos);
        }
    }

    void setAlignment(Component c, int hPos, int vPos) {
        boolean ltr = c.getComponentOrientation().isLeftToRight();
        if (absolutePositions) {
            if (hPos == LEADING) {
                hPos = ltr ? LEFT : RIGHT;
            } else if (hPos == TRAILING) {
                hPos = ltr ? RIGHT : LEFT;
            }
        } else if (hPos == LEFT) {
            hPos = ltr ? LEADING : TRAILING;
        } else if (hPos == RIGHT) {
            hPos = ltr ? TRAILING : LEADING;
        }
        if (c instanceof AbstractButton) {
            AbstractButton x = (AbstractButton) c;
            x.setHorizontalAlignment(hPos);
            x.setVerticalAlignment(vPos);
        } else if (c instanceof JLabel) {
            JLabel x = (JLabel) c;
            x.setHorizontalAlignment(hPos);
            x.setVerticalAlignment(vPos);
        }
    }
}
