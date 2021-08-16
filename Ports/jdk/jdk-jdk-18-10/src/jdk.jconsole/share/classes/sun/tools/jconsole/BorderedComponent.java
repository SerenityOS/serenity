/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.tools.jconsole;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicGraphicsUtils;


import static javax.swing.SwingConstants.*;

import static sun.tools.jconsole.JConsole.*;

@SuppressWarnings("serial")
public class BorderedComponent extends JPanel implements ActionListener {
    JButton moreOrLessButton;
    String valueLabelStr;
    JLabel label;
    JComponent comp;
    boolean collapsed = false;

    private Icon collapseIcon;
    private Icon expandIcon;

    private static Image getImage(String name) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        name = "resources/" + name + ".png";
        return tk.getImage(BorderedComponent.class.getResource(name));
    }

    public BorderedComponent(String text) {
        this(text, null, false);
    }

    public BorderedComponent(String text, JComponent comp) {
        this(text, comp, false);
    }

    public BorderedComponent(String text, JComponent comp, boolean collapsible) {
        super(null);

        this.comp = comp;

        // Only add border if text is not null
        if (text != null) {
            TitledBorder border;
            if (collapsible) {
                final JLabel textLabel = new JLabel(text);
                JPanel borderLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)) {
                    public int getBaseline(int w, int h) {
                        Dimension dim = textLabel.getPreferredSize();
                        return textLabel.getBaseline(dim.width, dim.height) + textLabel.getY();
                    }
                };
                borderLabel.add(textLabel);
                border = new LabeledBorder(borderLabel);
                textLabel.setForeground(border.getTitleColor());

                if (IS_WIN) {
                    collapseIcon = new ImageIcon(getImage("collapse-winlf"));
                    expandIcon = new ImageIcon(getImage("expand-winlf"));
                } else {
                    collapseIcon = new ArrowIcon(SOUTH, textLabel);
                    expandIcon = new ArrowIcon(EAST, textLabel);
                }

                moreOrLessButton = new JButton(collapseIcon);
                moreOrLessButton.setContentAreaFilled(false);
                moreOrLessButton.setBorderPainted(false);
                moreOrLessButton.setMargin(new Insets(0, 0, 0, 0));
                moreOrLessButton.addActionListener(this);
                String toolTip =
                    Messages.BORDERED_COMPONENT_MORE_OR_LESS_BUTTON_TOOLTIP;
                moreOrLessButton.setToolTipText(toolTip);
                borderLabel.add(moreOrLessButton);
                borderLabel.setSize(borderLabel.getPreferredSize());
                add(borderLabel);
            } else {
                border = new TitledBorder(text);
            }
            setBorder(new CompoundBorder(new FocusBorder(this), border));
        } else {
            setBorder(new FocusBorder(this));
        }
        if (comp != null) {
            add(comp);
        }
    }

    public void setComponent(JComponent comp) {
        if (this.comp != null) {
            remove(this.comp);
        }
        this.comp = comp;
        if (!collapsed) {
            LayoutManager lm = getLayout();
            if (lm instanceof BorderLayout) {
                add(comp, BorderLayout.CENTER);
            } else {
                add(comp);
            }
        }
        revalidate();
    }

    public void setValueLabel(String str) {
        this.valueLabelStr = str;
        if (label != null) {
            label.setText(Resources.format(Messages.CURRENT_VALUE,
                                           valueLabelStr));
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (collapsed) {
            if (label != null) {
                remove(label);
            }
            add(comp);
            moreOrLessButton.setIcon(collapseIcon);
        } else {
            remove(comp);
            if (valueLabelStr != null) {
                if (label == null) {
                    label = new JLabel(Resources.format(Messages.CURRENT_VALUE,
                                                        valueLabelStr));
                }
                add(label);
            }
            moreOrLessButton.setIcon(expandIcon);
        }
        collapsed = !collapsed;

        JComponent container = (JComponent)getParent();
        if (container != null &&
            container.getLayout() instanceof VariableGridLayout) {

            ((VariableGridLayout)container.getLayout()).setFillRow(this, !collapsed);
            container.revalidate();
        }
    }

    public Dimension getMinimumSize() {
        if (getLayout() != null) {
            // A layout manager has been set, so delegate to it
            return super.getMinimumSize();
        }

        if (moreOrLessButton != null) {
            Dimension d = moreOrLessButton.getMinimumSize();
            Insets i = getInsets();
            d.width  += i.left + i.right;
            d.height += i.top + i.bottom;
            return d;
        } else {
            return super.getMinimumSize();
        }
    }

    public void doLayout() {
        if (getLayout() != null) {
            // A layout manager has been set, so delegate to it
            super.doLayout();
            return;
        }

        Dimension d = getSize();
        Insets i = getInsets();

        if (collapsed) {
            if (label != null) {
                Dimension p = label.getPreferredSize();
                label.setBounds(i.left,
                                i.top + (d.height - i.top - i.bottom - p.height) / 2,
                                p.width,
                                p.height);
            }
        } else {
            if (comp != null) {
                comp.setBounds(i.left,
                               i.top,
                               d.width - i.left - i.right,
                               d.height - i.top - i.bottom);
            }
        }
    }

    private static class ArrowIcon implements Icon {
        private int direction;
        private JLabel textLabel;

        public ArrowIcon(int direction, JLabel textLabel) {
            this.direction = direction;
            this.textLabel = textLabel;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int w = getIconWidth();
            int h = w;
            Polygon p = new Polygon();
            switch (direction) {
              case EAST:
                p.addPoint(x + 2,     y);
                p.addPoint(x + w - 2, y + h / 2);
                p.addPoint(x + 2,     y + h - 1);
                break;

              case SOUTH:
                p.addPoint(x,         y + 2);
                p.addPoint(x + w / 2, y + h - 2);
                p.addPoint(x + w - 1, y + 2);
                break;
            }
            g.fillPolygon(p);
        }

        public int getIconWidth() {
            return getIconHeight();
        }

        public int getIconHeight() {
            Graphics g = textLabel.getGraphics();
            if (g != null) {
                int h = g.getFontMetrics(textLabel.getFont()).getAscent() * 6/10;
                if (h % 2 == 0) {
                    h += 1;     // Make it odd
                }
                return h;
            } else {
                return 7;
            }
        }
    }


    /**
     * A subclass of <code>TitledBorder</code> which implements an arbitrary border
     * with the addition of a JComponent (JLabel, JPanel, etc) in the
     * default position.
     * <p>
     * If the border property value is not
     * specified in the constructor or by invoking the appropriate
     * set method, the property value will be defined by the current
     * look and feel, using the following property name in the
     * Defaults Table:
     * <ul>
     * <li>&quot;TitledBorder.border&quot;
     * </ul>
     */
    protected static class LabeledBorder extends TitledBorder {
        protected JComponent label;

        private Point compLoc = new Point();

        /**
         * Creates a LabeledBorder instance.
         *
         * @param label  the label the border should display
         */
        public LabeledBorder(JComponent label)     {
            this(null, label);
        }

        /**
         * Creates a LabeledBorder instance with the specified border
         * and an empty label.
         *
         * @param border  the border
         */
        public LabeledBorder(Border border)       {
            this(border, null);
        }

        /**
         * Creates a LabeledBorder instance with the specified border and
         * label.
         *
         * @param border  the border
         * @param label  the label the border should display
         */
        public LabeledBorder(Border border, JComponent label) {
            super(border);

            this.label = label;

            if (label instanceof JLabel &&
                label.getForeground() instanceof ColorUIResource) {

                label.setForeground(getTitleColor());
            }

        }

        /**
         * Paints the border for the specified component with the
         * specified position and size.
         * @param c the component for which this border is being painted
         * @param g the paint graphics
         * @param x the x position of the painted border
         * @param y the y position of the painted border
         * @param width the width of the painted border
         * @param height the height of the painted border
         */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

            Border border = getBorder();

            if (label == null) {
                if (border != null) {
                    border.paintBorder(c, g, x, y, width, height);
                }
                return;
            }

            Rectangle grooveRect = new Rectangle(x + EDGE_SPACING, y + EDGE_SPACING,
                                                 width - (EDGE_SPACING * 2),
                                                 height - (EDGE_SPACING * 2));

            Dimension   labelDim = label.getPreferredSize();
            int baseline = label.getBaseline(labelDim.width, labelDim.height);
            int         ascent = Math.max(0, baseline);
            int         descent = labelDim.height - ascent;
            int         diff;
            Insets      insets;

            if (border != null) {
                insets = border.getBorderInsets(c);
            } else {
                insets = new Insets(0, 0, 0, 0);
            }

            diff = Math.max(0, ascent/2 + TEXT_SPACING - EDGE_SPACING);
            grooveRect.y += diff;
            grooveRect.height -= diff;
            compLoc.y = grooveRect.y + insets.top/2 - (ascent + descent) / 2 - 1;

            int justification;
            if (c.getComponentOrientation().isLeftToRight()) {
                justification = LEFT;
            } else {
                justification = RIGHT;
            }

            switch (justification) {
                case LEFT:
                    compLoc.x = grooveRect.x + TEXT_INSET_H + insets.left;
                    break;
                case RIGHT:
                    compLoc.x = (grooveRect.x + grooveRect.width
                                 - (labelDim.width + TEXT_INSET_H + insets.right));
                    break;
            }

            // If title is positioned in middle of border AND its fontsize
            // is greater than the border's thickness, we'll need to paint
            // the border in sections to leave space for the component's background
            // to show through the title.
            //
            if (border != null) {
                if (grooveRect.y > compLoc.y - ascent) {
                    Rectangle clipRect = new Rectangle();

                    // save original clip
                    Rectangle saveClip = g.getClipBounds();

                    // paint strip left of text
                    clipRect.setBounds(saveClip);
                    if (computeIntersection(clipRect, x, y, compLoc.x-1-x, height)) {
                        g.setClip(clipRect);
                        border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                      grooveRect.width, grooveRect.height);
                    }

                    // paint strip right of text
                    clipRect.setBounds(saveClip);
                    if (computeIntersection(clipRect, compLoc.x+ labelDim.width +1, y,
                                   x+width-(compLoc.x+ labelDim.width +1), height)) {
                        g.setClip(clipRect);
                        border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                      grooveRect.width, grooveRect.height);
                    }

                    // paint strip below text
                    clipRect.setBounds(saveClip);
                    if (computeIntersection(clipRect,
                                            compLoc.x - 1, compLoc.y + ascent + descent,
                                            labelDim.width + 2,
                                            y + height - compLoc.y - ascent - descent)) {
                        g.setClip(clipRect);
                        border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
                    }

                    // restore clip
                    g.setClip(saveClip);

                } else {
                    border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                      grooveRect.width, grooveRect.height);
                }

                label.setLocation(compLoc);
                label.setSize(labelDim);
            }
        }

        /**
         * Reinitialize the insets parameter with this Border's current Insets.
         * @param c the component for which this border insets value applies
         * @param insets the object to be reinitialized
         */
        public Insets getBorderInsets(Component c, Insets insets) {
            Border border = getBorder();
            if (border != null) {
                if (border instanceof AbstractBorder) {
                    ((AbstractBorder)border).getBorderInsets(c, insets);
                } else {
                    // Can't reuse border insets because the Border interface
                    // can't be enhanced.
                    Insets i = border.getBorderInsets(c);
                    insets.top = i.top;
                    insets.right = i.right;
                    insets.bottom = i.bottom;
                    insets.left = i.left;
                }
            } else {
                insets.left = insets.top = insets.right = insets.bottom = 0;
            }

            insets.left += EDGE_SPACING + TEXT_SPACING;
            insets.right += EDGE_SPACING + TEXT_SPACING;
            insets.top += EDGE_SPACING + TEXT_SPACING;
            insets.bottom += EDGE_SPACING + TEXT_SPACING;

            if (c == null || label == null) {
                return insets;
            }

            insets.top += label.getHeight();

            return insets;
        }

        /**
         * Returns the label of the labeled border.
         */
        public JComponent getLabel() {
            return label;
        }


        /**
         * Sets the title of the titled border.
         * param title the title for the border
         */
        public void setLabel(JComponent label) {
            this.label = label;
        }



        /**
         * Returns the minimum dimensions this border requires
         * in order to fully display the border and title.
         * @param c the component where this border will be drawn
         */
        public Dimension getMinimumSize(Component c) {
            Insets insets = getBorderInsets(c);
            Dimension minSize = new Dimension(insets.right + insets.left,
                                              insets.top + insets.bottom);
            minSize.width += label.getWidth();

            return minSize;
        }


        private static boolean computeIntersection(Rectangle dest,
                                                   int rx, int ry, int rw, int rh) {
            int x1 = Math.max(rx, dest.x);
            int x2 = Math.min(rx + rw, dest.x + dest.width);
            int y1 = Math.max(ry, dest.y);
            int y2 = Math.min(ry + rh, dest.y + dest.height);
            dest.x = x1;
            dest.y = y1;
            dest.width = x2 - x1;
            dest.height = y2 - y1;

            if (dest.width <= 0 || dest.height <= 0) {
                return false;
            }
            return true;
        }
    }


    protected static class FocusBorder extends AbstractBorder implements FocusListener {
        private Component comp;
        private Color focusColor;
        private boolean focusLostTemporarily = false;

        public FocusBorder(Component comp) {
            this.comp = comp;

            comp.addFocusListener(this);

            // This is the best guess for a L&F specific color
            focusColor = UIManager.getColor("TabbedPane.focus");
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (comp.hasFocus() || focusLostTemporarily) {
                Color color = g.getColor();
                g.setColor(focusColor);
                BasicGraphicsUtils.drawDashedRect(g, x, y, width, height);
                g.setColor(color);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(2, 2, 2, 2);
            return insets;
        }


        public void focusGained(FocusEvent e) {
            comp.repaint();
        }

        public void focusLost(FocusEvent e) {
            // We will still paint focus even if lost temporarily
            focusLostTemporarily = e.isTemporary();
            if (!focusLostTemporarily) {
                comp.repaint();
            }
        }
    }
}
