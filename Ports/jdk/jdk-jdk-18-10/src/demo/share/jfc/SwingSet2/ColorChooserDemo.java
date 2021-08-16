/*
 *
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.colorchooser.*;
import javax.swing.filechooser.*;
import javax.accessibility.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.io.*;
import java.applet.*;
import java.net.*;

/**
 * JColorChooserDemo
 *
 * @author Jeff Dinkins
 */
public class ColorChooserDemo extends DemoModule {

    BezierAnimationPanel bezAnim;
    JButton outerColorButton = null;
    JButton backgroundColorButton = null;
    JButton gradientAButton = null;
    JButton gradientBButton = null;

    // to store the color chosen from the JColorChooser
    private Color chosen;

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        ColorChooserDemo demo = new ColorChooserDemo(null);
        demo.mainImpl();
    }


    /**
     * ColorChooserDemo Constructor
     */
    public ColorChooserDemo(SwingSet2 swingset) {
        // Set the title for this demo, and an icon used to represent this
        // demo inside the SwingSet2 app.
        super(swingset, "ColorChooserDemo", "toolbar/JColorChooser.gif");

        // Create the bezier animation panel to put in the center of the panel.
        bezAnim = new BezierAnimationPanel();

        outerColorButton = new JButton(getString("ColorChooserDemo.outer_line"));
        outerColorButton.setIcon(new ColorSwatch("OuterLine", bezAnim));

        backgroundColorButton = new JButton(getString("ColorChooserDemo.background"));
        backgroundColorButton.setIcon(new ColorSwatch("Background", bezAnim));

        gradientAButton = new JButton(getString("ColorChooserDemo.grad_a"));
        gradientAButton.setIcon(new ColorSwatch("GradientA", bezAnim));

        gradientBButton = new JButton(getString("ColorChooserDemo.grad_b"));
        gradientBButton.setIcon(new ColorSwatch("GradientB", bezAnim));

        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color current = bezAnim.getOuterColor();

                if(e.getSource() == backgroundColorButton) {
                    current = bezAnim.getBackgroundColor();
                } else if(e.getSource() == gradientAButton) {
                    current = bezAnim.getGradientColorA();
                } else if(e.getSource() == gradientBButton) {
                    current = bezAnim.getGradientColorB();
                }

                final JColorChooser chooser = new JColorChooser(current != null ?
                                                                current :
                                                                Color.WHITE);
                if (getSwingSet2() != null && getSwingSet2().isDragEnabled()) {
                    chooser.setDragEnabled(true);
                }

                chosen = null;
                ActionListener okListener = new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        chosen = chooser.getColor();
                    }
                };

                JDialog dialog = JColorChooser.createDialog(getDemoPanel(),
                                                            getString("ColorChooserDemo.chooser_title"),
                                                            true,
                                                            chooser,
                                                            okListener,
                                                            null);

                dialog.setVisible(true);

                if(e.getSource() == outerColorButton) {
                    bezAnim.setOuterColor(chosen);
                } else if(e.getSource() == backgroundColorButton) {
                    bezAnim.setBackgroundColor(chosen);
                } else if(e.getSource() == gradientAButton) {
                    bezAnim.setGradientColorA(chosen);
                } else {
                    bezAnim.setGradientColorB(chosen);
                }
            }
        };

        outerColorButton.addActionListener(l);
        backgroundColorButton.addActionListener(l);
        gradientAButton.addActionListener(l);
        gradientBButton.addActionListener(l);

        // Add everything to the panel
        JPanel p = getDemoPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Add control buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(backgroundColorButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(15, 1)));

        buttonPanel.add(gradientAButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(15, 1)));

        buttonPanel.add(gradientBButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(15, 1)));

        buttonPanel.add(outerColorButton);

        // Add the panel midway down the panel
        p.add(Box.createRigidArea(new Dimension(1, 10)));
        p.add(buttonPanel);
        p.add(Box.createRigidArea(new Dimension(1, 5)));
        p.add(bezAnim);
    }

    class ColorSwatch implements Icon {
        String gradient;
        BezierAnimationPanel bez;

        public ColorSwatch(String g, BezierAnimationPanel b) {
            bez = b;
            gradient = g;
        }

        public int getIconWidth() {
            return 11;
        }

        public int getIconHeight() {
            return 11;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            if(gradient.equals("GradientA")) {
                g.setColor(bez.getGradientColorA());
            } else if(gradient.equals("GradientB")) {
                g.setColor(bez.getGradientColorB());
            } else if(gradient.equals("Background")) {
                g.setColor(bez.getBackgroundColor());
            } else if(gradient.equals("OuterLine")) {
                g.setColor(bez.getOuterColor());
            }
            g.fillRect(x+2, y+2, getIconWidth()-4, getIconHeight()-4);
        }
    }

}
