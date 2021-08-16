/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.java.swing.plaf.gtk;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.colorchooser.*;
import javax.swing.event.*;
import javax.swing.plaf.*;

/**
 * A color chooser panel mimicking that of GTK's: a color wheel showing
 * hue and a triangle that varies saturation and brightness.
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class GTKColorChooserPanel extends AbstractColorChooserPanel implements
              ChangeListener {
    private static final float PI_3 = (float)(Math.PI / 3);

    private ColorTriangle triangle;
    private JLabel lastLabel;
    private JLabel label;

    private JSpinner hueSpinner;
    private JSpinner saturationSpinner;
    private JSpinner valueSpinner;

    private JSpinner redSpinner;
    private JSpinner greenSpinner;
    private JSpinner blueSpinner;

    private JTextField colorNameTF;

    private boolean settingColor;

    // The colors are mirrored to avoid creep in adjusting an individual
    // value.
    private float hue;
    private float saturation;
    private float brightness;



    /**
     * Convenience method to transfer focus to the next child of component.
     */
    // PENDING: remove this when a variant of this is added to awt.
    static void compositeRequestFocus(Component component, boolean direction) {
        if (component instanceof Container) {
            Container container = (Container)component;
            if (container.isFocusCycleRoot()) {
                FocusTraversalPolicy policy = container.
                                              getFocusTraversalPolicy();
                Component comp = policy.getDefaultComponent(container);
                if (comp!=null) {
                    comp.requestFocus();
                    return;
                }
            }
            Container rootAncestor = container.getFocusCycleRootAncestor();
            if (rootAncestor!=null) {
                FocusTraversalPolicy policy = rootAncestor.
                                                  getFocusTraversalPolicy();
                Component comp;

                if (direction) {
                    comp = policy.getComponentAfter(rootAncestor, container);
                }
                else {
                    comp = policy.getComponentBefore(rootAncestor, container);
                }
                if (comp != null) {
                    comp.requestFocus();
                    return;
                }
            }
        }
        component.requestFocus();
    }


    /**
     * Returns a user presentable description of this GTKColorChooserPane.
     */
    public String getDisplayName() {
        return (String)UIManager.get("GTKColorChooserPanel.nameText");
    }

    /**
     * Returns the mnemonic to use with <code>getDisplayName</code>.
     */
    public int getMnemonic() {
        String m = (String)UIManager.get("GTKColorChooserPanel.mnemonic");

        if (m != null) {
            try {
                int value = Integer.parseInt(m);

                return value;
            } catch (NumberFormatException nfe) {}
        }
        return -1;
    }

    /**
     * Character to underline that represents the mnemonic.
     */
    public int getDisplayedMnemonicIndex() {
        String m = (String)UIManager.get(
                           "GTKColorChooserPanel.displayedMnemonicIndex");

        if (m != null) {
            try {
                int value = Integer.parseInt(m);

                return value;
            } catch (NumberFormatException nfe) {}
        }
        return -1;
    }

    public Icon getSmallDisplayIcon() {
        return null;
    }

    public Icon getLargeDisplayIcon() {
        return null;
    }

    public void uninstallChooserPanel(JColorChooser enclosingChooser) {
        super.uninstallChooserPanel(enclosingChooser);
        removeAll();
    }

    /**
     * Builds and configures the widgets for the GTKColorChooserPanel.
     */
    protected void buildChooser() {
        triangle = new ColorTriangle();
        triangle.setName("GTKColorChooserPanel.triangle");

        // PENDING: when we straighten out user setting opacity, this should
        // be changed.
        label = new OpaqueLabel();
        label.setName("GTKColorChooserPanel.colorWell");
        label.setOpaque(true);
        label.setMinimumSize(new Dimension(67, 32));
        label.setPreferredSize(new Dimension(67, 32));
        label.setMaximumSize(new Dimension(67, 32));

        // PENDING: when we straighten out user setting opacity, this should
        // be changed.
        lastLabel = new OpaqueLabel();
        lastLabel.setName("GTKColorChooserPanel.lastColorWell");
        lastLabel.setOpaque(true);
        lastLabel.setMinimumSize(new Dimension(67, 32));
        lastLabel.setPreferredSize(new Dimension(67, 32));
        lastLabel.setMaximumSize(new Dimension(67, 32));

        hueSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
        configureSpinner(hueSpinner, "GTKColorChooserPanel.hueSpinner");
        saturationSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        configureSpinner(saturationSpinner,
                         "GTKColorChooserPanel.saturationSpinner");
        valueSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        configureSpinner(valueSpinner, "GTKColorChooserPanel.valueSpinner");
        redSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        configureSpinner(redSpinner, "GTKColorChooserPanel.redSpinner");
        greenSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        configureSpinner(greenSpinner, "GTKColorChooserPanel.greenSpinner");
        blueSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        configureSpinner(blueSpinner, "GTKColorChooserPanel.blueSpinner");

        colorNameTF = new JTextField(8);

        setLayout(new GridBagLayout());

        add(this, "GTKColorChooserPanel.hue", hueSpinner, -1, -1);
        add(this, "GTKColorChooserPanel.red", redSpinner, -1, -1);
        add(this, "GTKColorChooserPanel.saturation", saturationSpinner, -1,-1);
        add(this, "GTKColorChooserPanel.green", greenSpinner, -1, -1);
        add(this, "GTKColorChooserPanel.value", valueSpinner, -1, -1);
        add(this, "GTKColorChooserPanel.blue", blueSpinner, -1, -1);

        add(new JSeparator(SwingConstants.HORIZONTAL), new
                  GridBagConstraints(1, 3, 4, 1, 1, 0,
                  GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
                  new Insets(14, 0, 0, 0), 0, 0));

        add(this, "GTKColorChooserPanel.colorName", colorNameTF, 0, 4);

        add(triangle, new GridBagConstraints(0, 0, 1, 5, 0, 0,
                      GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                      new Insets(14, 20, 2, 9), 0, 0));

        Box hBox = Box.createHorizontalBox();
        hBox.add(lastLabel);
        hBox.add(label);
        add(hBox, new GridBagConstraints(0, 5, 1, 1, 0, 0,
                      GridBagConstraints.CENTER, GridBagConstraints.NONE,
                      new Insets(0, 0, 0, 0), 0, 0));

        add(new JSeparator(SwingConstants.HORIZONTAL), new
                  GridBagConstraints(0, 6, 5, 1, 1, 0,
                  GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
                  new Insets(12, 0, 0, 0), 0, 0));
    }

    /**
     * Configures the spinner.
     */
    private void configureSpinner(JSpinner spinner, String name) {
        spinner.addChangeListener(this);
        spinner.setName(name);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField ftf = ((JSpinner.DefaultEditor)editor).
                                                 getTextField();

            ftf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        }
    }

    /**
     * Adds the widget creating a JLabel with the specified name.
     */
    private void add(Container parent, String key, JComponent widget,
                     int x, int y) {
        JLabel label = new JLabel(UIManager.getString(key + "Text",
                                                      getLocale()));
        String mnemonic = (String)UIManager.get(key + "Mnemonic", getLocale());

        if (mnemonic != null) {
            try {
                label.setDisplayedMnemonic(Integer.parseInt(mnemonic));
            } catch (NumberFormatException nfe) {
            }
            String mnemonicIndex = (String)UIManager.get(key + "MnemonicIndex",
                                                    getLocale());

            if (mnemonicIndex != null) {
                try {
                    label.setDisplayedMnemonicIndex(Integer.parseInt(
                                                        mnemonicIndex));
                } catch (NumberFormatException nfe) {
                }
            }
        }
        label.setLabelFor(widget);
        if (x < 0) {
            x = parent.getComponentCount() % 4;
        }
        if (y < 0) {
            y = parent.getComponentCount() / 4;
        }
        GridBagConstraints con = new GridBagConstraints(x + 1, y, 1, 1, 0, 0,
                   GridBagConstraints.FIRST_LINE_END, GridBagConstraints.NONE,
                   new Insets(4, 0, 0, 4), 0, 0);
        if (y == 0) {
            con.insets.top = 14;
        }
        parent.add(label, con);
        con.gridx++;
        parent.add(widget, con);
    }

    /**
     * Refreshes the display from the model.
     */
    public void updateChooser() {
        if (!settingColor) {
            lastLabel.setBackground(getColorFromModel());
            setColor(getColorFromModel(), true, true, false);
        }
    }

    /**
     * Resets the red component of the selected color.
     */
    private void setRed(int red) {
        setRGB(red << 16 | getColor().getGreen() << 8 | getColor().getBlue());
    }

    /**
     * Resets the green component of the selected color.
     */
    private void setGreen(int green) {
        setRGB(getColor().getRed() << 16 | green << 8 | getColor().getBlue());
    }

    /**
     * Resets the blue component of the selected color.
     */
    private void setBlue(int blue) {
        setRGB(getColor().getRed() << 16 | getColor().getGreen() << 8 | blue);
    }

    /**
     * Sets the hue of the selected color and updates the display if
     * necessary.
     */
    private void setHue(float hue, boolean update) {
        setHSB(hue, saturation, brightness);
        if (update) {
            settingColor = true;
            hueSpinner.setValue(Integer.valueOf((int)(hue * 360)));
            settingColor = false;
        }
    }

    /**
     * Returns the current amount of hue.
     */
    private float getHue() {
        return hue;
    }

    /**
     * Resets the saturation.
     */
    private void setSaturation(float saturation) {
        setHSB(hue, saturation, brightness);
    }

    /**
     * Returns the saturation.
     */
    private float getSaturation() {
        return saturation;
    }

    /**
     * Sets the brightness.
     */
    private void setBrightness(float brightness) {
        setHSB(hue, saturation, brightness);
    }

    /**
     * Returns the brightness.
     */
    private float getBrightness() {
        return brightness;
    }

    /**
     * Sets the saturation and brightness and updates the display if
     * necessary.
     */
    private void setSaturationAndBrightness(float s, float b, boolean update) {
        setHSB(hue, s, b);
        if (update) {
            settingColor = true;
            saturationSpinner.setValue(Integer.valueOf((int)(s * 255)));
            valueSpinner.setValue(Integer.valueOf((int)(b * 255)));
            settingColor = false;
        }
    }

    /**
     * Resets the rgb values.
     */
    private void setRGB(int rgb) {
        Color color = new Color(rgb);

        setColor(color, false, true, true);

        settingColor = true;
        hueSpinner.setValue(Integer.valueOf((int)(hue * 360)));
        saturationSpinner.setValue(Integer.valueOf((int)(saturation * 255)));
        valueSpinner.setValue(Integer.valueOf((int)(brightness * 255)));
        settingColor = false;
    }

    /**
     * Resets the hsb values.
     */
    private void setHSB(float h, float s, float b) {
        Color color = Color.getHSBColor(h, s, b);

        this.hue = h;
        this.saturation = s;
        this.brightness = b;
        setColor(color, false, false, true);

        settingColor = true;
        redSpinner.setValue(Integer.valueOf(color.getRed()));
        greenSpinner.setValue(Integer.valueOf(color.getGreen()));
        blueSpinner.setValue(Integer.valueOf(color.getBlue()));
        settingColor = false;
    }


    /**
     * Rests the color.
     *
     * @param color new Color
     * @param updateSpinners whether or not to update the spinners.
     * @param updateHSB if true, the hsb fields are updated based on the
     *                  new color
     * @param updateModel if true, the model is set.
     */
    private void setColor(Color color, boolean updateSpinners,
                          boolean updateHSB, boolean updateModel) {
        if (color == null) {
            color = Color.BLACK;
        }

        settingColor = true;

        if (updateHSB) {
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(),
                                         color.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        }

        if (updateModel) {
            ColorSelectionModel model = getColorSelectionModel();
            if (model != null) {
                model.setSelectedColor(color);
            }
        }

        triangle.setColor(hue, saturation, brightness);
        label.setBackground(color);
        // Force Integer to pad the string with 0's by adding 0x1000000 and
        // then removing the first character.
        String hexString = Integer.toHexString(
                  (color.getRGB() & 0xFFFFFF) | 0x1000000);
        colorNameTF.setText("#" + hexString.substring(1));

        if (updateSpinners) {
            redSpinner.setValue(Integer.valueOf(color.getRed()));
            greenSpinner.setValue(Integer.valueOf(color.getGreen()));
            blueSpinner.setValue(Integer.valueOf(color.getBlue()));

            hueSpinner.setValue(Integer.valueOf((int)(hue * 360)));
            saturationSpinner.setValue(Integer.valueOf((int)(saturation * 255)));
            valueSpinner.setValue(Integer.valueOf((int)(brightness * 255)));
        }
        settingColor = false;
    }

    public Color getColor() {
        return label.getBackground();
    }

    /**
     * ChangeListener method, updates the necessary display widgets.
     */
    public void stateChanged(ChangeEvent e) {
        if (settingColor) {
            return;
        }
        Color color = getColor();

        if (e.getSource() == hueSpinner) {
            setHue(((Number)hueSpinner.getValue()).floatValue() / 360, false);
        }
        else if (e.getSource() == saturationSpinner) {
            setSaturation(((Number)saturationSpinner.getValue()).
                          floatValue() / 255);
        }
        else if (e.getSource() == valueSpinner) {
            setBrightness(((Number)valueSpinner.getValue()).
                          floatValue() / 255);
        }
        else if (e.getSource() == redSpinner) {
            setRed(((Number)redSpinner.getValue()).intValue());
        }
        else if (e.getSource() == greenSpinner) {
            setGreen(((Number)greenSpinner.getValue()).intValue());
        }
        else if (e.getSource() == blueSpinner) {
            setBlue(((Number)blueSpinner.getValue()).intValue());
        }
    }



    /**
     * Flag indicating the angle, or hue, has changed and the triangle
     * needs to be recreated.
     */
    private static final int FLAGS_CHANGED_ANGLE = 1 << 0;
    /**
     * Indicates the wheel is being dragged.
     */
    private static final int FLAGS_DRAGGING = 1 << 1;
    /**
     * Indicates the triangle is being dragged.
     */
    private static final int FLAGS_DRAGGING_TRIANGLE = 1 << 2;
    /**
     * Indicates a color is being set and we should ignore setColor
     */
    private static final int FLAGS_SETTING_COLOR = 1 << 3;
    /**
     * Indicates the wheel has focus.
     */
    private static final int FLAGS_FOCUSED_WHEEL = 1 << 4;
    /**
     * Indicates the triangle has focus.
     */
    private static final int FLAGS_FOCUSED_TRIANGLE = 1 << 5;


    /**
     * Class responsible for rendering a color wheel and color triangle.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class ColorTriangle extends JPanel {
        /**
         * Cached image of the wheel.
         */
        private Image wheelImage;

        /**
         * Cached image of the triangle.
         */
        private Image triangleImage;

        /**
         * Angle triangle is rotated by.
         */
        private double angle;

        /**
         * Boolean bitmask.
         */
        private int flags;

        /**
         * X location of selected color indicator.
         */
        private int circleX;
        /**
         * Y location of selected color indicator.
         */
        private int circleY;


        public ColorTriangle() {
            enableEvents(AWTEvent.FOCUS_EVENT_MASK);
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);

            setMinimumSize(new Dimension(getWheelRadius() * 2 + 2,
                                         getWheelRadius() * 2 + 2));
            setPreferredSize(new Dimension(getWheelRadius() * 2 + 2,
                                           getWheelRadius() * 2 + 2));

            // We want to handle tab ourself.
            setFocusTraversalKeysEnabled(false);

            // PENDING: this should come from the style.
            getInputMap().put(KeyStroke.getKeyStroke("UP"), "up");
            getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "down");
            getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "left");
            getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "right");

            getInputMap().put(KeyStroke.getKeyStroke("KP_UP"), "up");
            getInputMap().put(KeyStroke.getKeyStroke("KP_DOWN"), "down");
            getInputMap().put(KeyStroke.getKeyStroke("KP_LEFT"), "left");
            getInputMap().put(KeyStroke.getKeyStroke("KP_RIGHT"), "right");

            getInputMap().put(KeyStroke.getKeyStroke("TAB"), "focusNext");
            getInputMap().put(KeyStroke.getKeyStroke("shift TAB"),"focusLast");

            ActionMap map = (ActionMap)UIManager.get(
                                       "GTKColorChooserPanel.actionMap");

            if (map == null) {
                map = new ActionMapUIResource();
                map.put("left", new ColorAction("left", 2));
                map.put("right", new ColorAction("right", 3));
                map.put("up", new ColorAction("up", 0));
                map.put("down", new ColorAction("down", 1));
                map.put("focusNext", new ColorAction("focusNext", 4));
                map.put("focusLast", new ColorAction("focusLast", 5));
                UIManager.getLookAndFeelDefaults().put(
                             "GTKColorChooserPanel.actionMap", map);
            }
            SwingUtilities.replaceUIActionMap(this, map);
        }

        /**
         * Returns the GTKColorChooserPanel.
         */
        GTKColorChooserPanel getGTKColorChooserPanel() {
            return GTKColorChooserPanel.this;
        }

        /**
         * Gives focus to the wheel.
         */
        void focusWheel() {
            setFocusType(1);
        }

        /**
         * Gives focus to the triangle.
         */
        void focusTriangle() {
            setFocusType(2);
        }

        /**
         * Returns true if the wheel currently has focus.
         */
        boolean isWheelFocused() {
            return isSet(FLAGS_FOCUSED_WHEEL);
        }

        /**
         * Resets the selected color.
         */
        public void setColor(float h, float s, float b) {
            if (isSet(FLAGS_SETTING_COLOR)) {
                return;
            }

            setAngleFromHue(h);
            setSaturationAndBrightness(s, b);
        }

        /**
         * Returns the selected color.
         */
        public Color getColor() {
            return GTKColorChooserPanel.this.getColor();
        }

        /**
         * Returns the x location of the selected color indicator.
         */
        int getColorX() {
            return circleX + getIndicatorSize() / 2 - getWheelXOrigin();
        }

        /**
         * Returns the y location of the selected color indicator.
         */
        int getColorY() {
            return circleY + getIndicatorSize() / 2 - getWheelYOrigin();
        }

        protected void processEvent(AWTEvent e) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED ||
                   ((isSet(FLAGS_DRAGGING) ||isSet(FLAGS_DRAGGING_TRIANGLE)) &&
                   e.getID() == MouseEvent.MOUSE_DRAGGED)) {
                // Assign focus to either the wheel or triangle and attempt
                // to drag either the wheel or triangle.
                int size = getWheelRadius();
                int x = ((MouseEvent)e).getX() - size;
                int y = ((MouseEvent)e).getY() - size;

                if (!hasFocus()) {
                    requestFocus();
                }
                if (!isSet(FLAGS_DRAGGING_TRIANGLE) &&
                      adjustHue(x, y, e.getID() == MouseEvent.MOUSE_PRESSED)) {
                    setFlag(FLAGS_DRAGGING, true);
                    setFocusType(1);
                }
                else if (adjustSB(x, y, e.getID() ==
                                        MouseEvent.MOUSE_PRESSED)) {
                    setFlag(FLAGS_DRAGGING_TRIANGLE, true);
                    setFocusType(2);
                }
                else {
                    setFocusType(2);
                }
            }
            else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                // Stopped dragging
                setFlag(FLAGS_DRAGGING_TRIANGLE, false);
                setFlag(FLAGS_DRAGGING, false);
            }
            else if (e.getID() == FocusEvent.FOCUS_LOST) {
                // Reset the flags to indicate no one has focus
                setFocusType(0);
            }
            else if (e.getID() == FocusEvent.FOCUS_GAINED) {
                // Gained focus, reassign focus to the wheel if no one
                // currently has focus.
                if (!isSet(FLAGS_FOCUSED_TRIANGLE) &&
                          !isSet(FLAGS_FOCUSED_WHEEL)) {
                    setFlag(FLAGS_FOCUSED_WHEEL, true);
                    setFocusType(1);
                }
                repaint();
            }
            super.processEvent(e);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw the wheel and triangle
            int size = getWheelRadius();
            int width = getWheelWidth();
            Image image = getImage(size);
            g.drawImage(image, getWheelXOrigin() - size,
                        getWheelYOrigin() - size, null);

            // Draw the focus indicator for the wheel
            if (hasFocus() && isSet(FLAGS_FOCUSED_WHEEL)) {
                g.setColor(Color.BLACK);
                g.drawOval(getWheelXOrigin() - size, getWheelYOrigin() - size,
                           2 * size, 2 * size);
                g.drawOval(getWheelXOrigin() - size + width, getWheelYOrigin()-
                           size + width, 2 * (size - width), 2 *
                           (size - width));
            }

            // Draw a line on the wheel indicating the selected hue.
            if (Math.toDegrees(Math.PI * 2 - angle) <= 20 ||
                     Math.toDegrees(Math.PI * 2 - angle) >= 201) {
                g.setColor(Color.WHITE);
            }
            else {
                g.setColor(Color.BLACK);
            }
            int lineX0 = (int)(Math.cos(angle) * size);
            int lineY0 = (int)(Math.sin(angle) * size);
            int lineX1 = (int)(Math.cos(angle) * (size - width));
            int lineY1 = (int)(Math.sin(angle) * (size - width));
            g.drawLine(lineX0 + size, lineY0 + size, lineX1 + size,
                       lineY1 + size);

            // Draw the focus indicator on the triangle
            if (hasFocus() && isSet(FLAGS_FOCUSED_TRIANGLE)) {
                Graphics g2 = g.create();
                int innerR = getTriangleCircumscribedRadius();
                int a = (int)(3 * innerR / Math.sqrt(3));
                g2.translate(getWheelXOrigin(), getWheelYOrigin());
                ((Graphics2D)g2).rotate(angle + Math.PI / 2);
                g2.setColor(Color.BLACK);
                g2.drawLine(0, -innerR, a / 2, innerR / 2);
                g2.drawLine(a / 2, innerR / 2, -a / 2, innerR / 2);
                g2.drawLine(-a / 2, innerR / 2, 0, -innerR);
                g2.dispose();
            }

            // Draw the selected color indicator.
            g.setColor(Color.BLACK);
            g.drawOval(circleX, circleY, getIndicatorSize() - 1,
                       getIndicatorSize() - 1);
            g.setColor(Color.WHITE);
            g.drawOval(circleX + 1, circleY + 1, getIndicatorSize() - 3,
                       getIndicatorSize() - 3);
        }

        /**
         * Returns an image representing the triangle and wheel.
         */
        private Image getImage(int size) {
            if (!isSet(FLAGS_CHANGED_ANGLE) && wheelImage != null &&
                        wheelImage.getWidth(null) == size * 2) {
                return wheelImage;
            }
            if (wheelImage == null || wheelImage.getWidth(null) != size) {
                wheelImage = getWheelImage(size);
            }
            int innerR = getTriangleCircumscribedRadius();
            int triangleSize = (int)(innerR * 3.0 / 2.0);
            int a = (int)(2 * triangleSize / Math.sqrt(3));
            if (triangleImage == null || triangleImage.getWidth(null) != a) {
                triangleImage = new BufferedImage(a, a,
                                                  BufferedImage.TYPE_INT_ARGB);
            }
            Graphics g = triangleImage.getGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, a, a);
            g.translate(a / 2, 0);
            paintTriangle(g, triangleSize, getColor());
            g.translate(-a / 2, 0);
            g.dispose();

            g = wheelImage.getGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillOval(getWheelWidth(), getWheelWidth(),
                       2 * (size - getWheelWidth()),
                       2 * (size - getWheelWidth()));

            double rotate = Math.toRadians(-30.0) + angle;
            g.translate(size, size);
            ((Graphics2D)g).rotate(rotate);
            g.drawImage(triangleImage, -a / 2,
                        getWheelWidth() - size, null);
            ((Graphics2D)g).rotate(-rotate);
            g.translate(a / 2, size - getWheelWidth());

            setFlag(FLAGS_CHANGED_ANGLE, false);

            return wheelImage;
        }

        private void paintTriangle(Graphics g, int size, Color color) {
            float[] colors = Color.RGBtoHSB(color.getRed(),
                                            color.getGreen(),
                                            color.getBlue(), null);
            float hue = colors[0];
            double dSize = (double)size;
            for (int y = 0; y < size; y++) {
                int maxX = (int)(y * Math.tan(Math.toRadians(30.0)));
                float factor = maxX * 2;
                if (maxX > 0) {
                    float value = (float)(y / dSize);
                    for (int x = -maxX; x <= maxX; x++) {
                        float saturation = (float)x / factor + .5f;
                        g.setColor(Color.getHSBColor(hue, saturation, value));
                        g.fillRect(x, y, 1, 1);
                    }
                }
                else {
                    g.setColor(color);
                    g.fillRect(0, y, 1, 1);
                }
            }
        }

        /**
         * Returns a color wheel image for the specified size.
         *
         * @param size Integer giving size of color wheel.
         * @return Color wheel image
         */
        private Image getWheelImage(int size) {
            int minSize = size - getWheelWidth();
            int doubleSize = size * 2;
            BufferedImage image = new BufferedImage(doubleSize, doubleSize,
                                              BufferedImage.TYPE_INT_ARGB);

            for (int y = -size; y < size; y++) {
                int ySquared = y * y;
                for (int x = -size; x < size; x++) {
                    double rad = Math.sqrt(ySquared + x * x);

                    if (rad < size && rad > minSize) {
                        int rgb = colorWheelLocationToRGB(x, y, rad) |
                              0xFF000000;
                        image.setRGB(x + size, y + size, rgb);
                    }
                }
            }
            wheelImage = image;
            return wheelImage;
        }

        /**
         * Adjusts the saturation and brightness. <code>x</code> and
         * <code>y</code> give the location to adjust to and are relative
         * to the origin of the wheel/triangle.
         *
         * @param x X coordinate on the triangle to adjust to
         * @param y Y coordinate on the triangle to adjust to
         * @param checkLoc if true the location is checked to make sure
         *        it is contained in the triangle, if false the location is
         *        constrained to fit in the triangle.
         * @return true if the location is valid
         */
        boolean adjustSB(int x, int y, boolean checkLoc) {
            int innerR = getWheelRadius() - getWheelWidth();
            boolean resetXY = false;
            // Invert the axis.
            y = -y;
            if (checkLoc && (x < -innerR || x > innerR || y < -innerR ||
                             y > innerR)) {
                return false;
            }
            // Rotate to origin and verify x is valid.
            int triangleSize = innerR * 3 / 2;
            double x1 = Math.cos(angle) * x - Math.sin(angle) * y;
            double y1 = Math.sin(angle) * x + Math.cos(angle) * y;
            if (x1 < -(innerR / 2)) {
                if (checkLoc) {
                    return false;
                }
                x1 = -innerR / 2;
                resetXY = true;
            }
            else if ((int)x1 > innerR) {
                if (checkLoc) {
                    return false;
                }
                x1 = innerR;
                resetXY = true;
            }
            // Verify y location is valid.
            int maxY = (int)((triangleSize - x1 - innerR / 2.0) *
                             Math.tan(Math.toRadians(30.0)));
            if (y1 <= -maxY) {
                if (checkLoc) {
                    return false;
                }
                y1 = -maxY;
                resetXY = true;
            }
            else if (y1 > maxY) {
                if (checkLoc) {
                    return false;
                }
                y1 = maxY;
                resetXY = true;
            }
            // Rotate again to determine value and scale
            double x2 = Math.cos(Math.toRadians(-30.0)) * x1 -
                 Math.sin(Math.toRadians(-30.0)) * y1;
            double y2 = Math.sin(Math.toRadians(-30.0)) * x1 +
                 Math.cos(Math.toRadians(-30.0)) * y1;
            float value = Math.min(1.0f, (float)((innerR - y2) /
                                                (double)triangleSize));
            float maxX = (float)(Math.tan(Math.toRadians(30)) * (innerR - y2));
            float saturation = Math.min(1.0f, (float)(x2 / maxX / 2 + .5));

            setFlag(FLAGS_SETTING_COLOR, true);
            if (resetXY) {
                setSaturationAndBrightness(saturation, value);
            }
            else {
                setSaturationAndBrightness(saturation, value, x +
                                      getWheelXOrigin(),getWheelYOrigin() - y);
            }
            GTKColorChooserPanel.this.setSaturationAndBrightness(saturation,
                                                                 value, true);
            setFlag(FLAGS_SETTING_COLOR, false);
            return true;
        }

        /**
         * Sets the saturation and brightness.
         */
        private void setSaturationAndBrightness(float s, float b) {
            int innerR = getTriangleCircumscribedRadius();
            int triangleSize = innerR * 3 / 2;
            double x = b * triangleSize;
            double maxY = x * Math.tan(Math.toRadians(30.0));
            double y = 2 * maxY * s - maxY;
            x = x - innerR;
            double x1 = Math.cos(Math.toRadians(-60.0) - angle) *
                        x - Math.sin(Math.toRadians(-60.0) - angle) * y;
            double y1 = Math.sin(Math.toRadians(-60.0) - angle) * x +
                        Math.cos(Math.toRadians(-60.0) - angle) * y;
            int newCircleX = (int)x1 + getWheelXOrigin();
            int newCircleY = getWheelYOrigin() - (int)y1;

            setSaturationAndBrightness(s, b, newCircleX, newCircleY);
        }


        /**
         * Sets the saturation and brightness.
         */
        private void setSaturationAndBrightness(float s, float b,
                                             int newCircleX, int newCircleY) {
            newCircleX -= getIndicatorSize() / 2;
            newCircleY -= getIndicatorSize() / 2;

            int minX = Math.min(newCircleX, circleX);
            int minY = Math.min(newCircleY, circleY);

            repaint(minX, minY, Math.max(circleX, newCircleX) - minX +
                    getIndicatorSize() + 1, Math.max(circleY, newCircleY) -
                    minY + getIndicatorSize() + 1);
            circleX = newCircleX;
            circleY = newCircleY;
        }

        /**
         * Adjusts the hue based on the passed in location.
         *
         * @param x X location to adjust to, relative to the origin of the
         *        wheel
         * @param y Y location to adjust to, relative to the origin of the
         *        wheel
         * @param check if true the location is checked to make sure
         *        it is contained in the wheel, if false the location is
         *        constrained to fit in the wheel
         * @return true if the location is valid.
         */
        private boolean adjustHue(int x, int y, boolean check) {
            double rad = Math.sqrt(x * x + y * y);
            int size = getWheelRadius();

            if (!check || (rad >= size - getWheelWidth() && rad < size)) {
                // Map the location to an angle and reset hue
                double angle;
                if (x == 0) {
                    if (y > 0) {
                        angle = Math.PI / 2.0;
                    }
                    else {
                        angle = Math.PI + Math.PI / 2.0;
                    }
                }
                else {
                    angle = Math.atan((double)y / (double)x);
                    if (x < 0) {
                        angle += Math.PI;
                    }
                    else if (angle < 0) {
                        angle += 2 * Math.PI;
                    }
                }
                setFlag(FLAGS_SETTING_COLOR, true);
                setHue((float)(1.0 - angle / Math.PI / 2), true);
                setFlag(FLAGS_SETTING_COLOR, false);
                setHueAngle(angle);
                setSaturationAndBrightness(getSaturation(), getBrightness());
                return true;
            }
            return false;
        }

        /**
         * Rotates the triangle to accommodate the passed in hue.
         */
        private void setAngleFromHue(float hue) {
            setHueAngle((1.0 - hue) * Math.PI * 2);
        }

        /**
         * Sets the angle representing the hue.
         */
        private void setHueAngle(double angle) {
            double oldAngle = this.angle;

            this.angle = angle;
            if (angle != oldAngle) {
                setFlag(FLAGS_CHANGED_ANGLE, true);
                repaint();
            }
        }

        /**
         * Returns the size of the color indicator.
         */
        private int getIndicatorSize() {
            return 8;
        }

        /**
         * Returns the circumscribed radius of the triangle.
         */
        private int getTriangleCircumscribedRadius() {
            return 72;
        }

        /**
         * Returns the x origin of the wheel and triangle.
         */
        private int getWheelXOrigin() {
            return 85;
        }

        /**
         * Returns y origin of the wheel and triangle.
         */
        private int getWheelYOrigin() {
            return 85;
        }

        /**
         * Returns the width of the wheel.
         */
        private int getWheelWidth() {
            return 13;
        }

        /**
         * Sets the focus to one of: 0 no one, 1 the wheel or 2 the triangle.
         */
        private void setFocusType(int type) {
            if (type == 0) {
                setFlag(FLAGS_FOCUSED_WHEEL, false);
                setFlag(FLAGS_FOCUSED_TRIANGLE, false);
                repaint();
            }
            else {
                int toSet = FLAGS_FOCUSED_WHEEL;
                int toUnset = FLAGS_FOCUSED_TRIANGLE;

                if (type == 2) {
                    toSet = FLAGS_FOCUSED_TRIANGLE;
                    toUnset = FLAGS_FOCUSED_WHEEL;
                }
                if (!isSet(toSet)) {
                    setFlag(toSet, true);
                    repaint();
                    setFlag(toUnset, false);
                }
            }
        }

        /**
         * Returns the radius of the wheel.
         */
        private int getWheelRadius() {
            // As far as I can tell, GTK doesn't allow stretching this
            // widget
            return 85;
        }

        /**
         * Updates the flags bitmask.
         */
        private void setFlag(int flag, boolean value) {
            if (value) {
                flags |= flag;
            }
            else {
                flags &= ~flag;
            }
        }

        /**
         * Returns true if a particular flag has been set.
         */
        private boolean isSet(int flag) {
            return ((flags & flag) == flag);
        }

        /**
         * Returns the RGB color to use for the specified location. The
         * passed in point must be on the color wheel and be relative to the
         * origin of the color wheel.
         *
         * @param x X location to get color for
         * @param y Y location to get color for
         * @param rad Radius from center of color wheel
         * @return integer with red, green and blue components
         */
        private int colorWheelLocationToRGB(int x, int y, double rad) {
            double angle = Math.acos((double)x / rad);
            int rgb;

            if (angle < PI_3) {
                if (y < 0) {
                    // FFFF00 - FF0000
                    rgb = 0xFF0000 | Math.min(255,
                                           (int)(255 * angle / PI_3)) << 8;
                }
                else {
                    // FF0000 - FF00FF
                    rgb = 0xFF0000 | Math.min(255,
                                           (int)(255 * angle / PI_3));
                }
            }
            else if (angle < 2 * PI_3) {
                angle -= PI_3;
                if (y < 0) {
                    // 00FF00 - FFFF00
                    rgb = 0x00FF00 | Math.max(0, 255 -
                                           (int)(255 * angle / PI_3)) << 16;
                }
                else {
                    // FF00FF - 0000FF
                    rgb = 0x0000FF | Math.max(0, 255 -
                                           (int)(255 * angle / PI_3)) << 16;
                }
            }
            else {
                angle -= 2 * PI_3;
                if (y < 0) {
                    // 00FFFF - 00FF00
                    rgb = 0x00FF00 | Math.min(255,
                                           (int)(255 * angle / PI_3));
                }
                else {
                    // 0000FF - 00FFFF
                    rgb = 0x0000FF | Math.min(255,
                                           (int)(255 * angle / PI_3)) << 8;
                }
            }
            return rgb;
        }

        /**
         * Increments the hue.
         */
        void incrementHue(boolean positive) {
            float hue = triangle.getGTKColorChooserPanel().getHue();

            if (positive) {
                hue += 1.0f / 360.0f;
            }
            else {
                hue -= 1.0f / 360.0f;
            }
            if (hue > 1) {
                hue -= 1;
            }
            else if (hue < 0) {
                hue += 1;
            }
            getGTKColorChooserPanel().setHue(hue, true);
        }
    }


    /**
     * Action class used for colors.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class ColorAction extends AbstractAction {
        private int type;

        ColorAction(String name, int type) {
            super(name);
            this.type = type;
        }

        public void actionPerformed(ActionEvent e) {
            ColorTriangle triangle = (ColorTriangle)e.getSource();

            if (triangle.isWheelFocused()) {
                float hue = triangle.getGTKColorChooserPanel().getHue();

                switch (type) {
                case 0:
                case 2:
                    triangle.incrementHue(true);
                    break;
                case 1:
                case 3:
                    triangle.incrementHue(false);
                    break;
                case 4:
                    triangle.focusTriangle();
                    break;
                case 5:
                    compositeRequestFocus(triangle, false);
                    break;
                }
            }
            else {
                int xDelta = 0;
                int yDelta = 0;

                switch (type) {
                case 0:
                    // up
                    yDelta--;
                    break;
                case 1:
                    // down
                    yDelta++;
                    break;
                case 2:
                    // left
                    xDelta--;
                    break;
                case 3:
                    // right
                    xDelta++;
                    break;
                case 4:
                    compositeRequestFocus(triangle, true);
                    return;
                case 5:
                    triangle.focusWheel();
                    return;
                }
                triangle.adjustSB(triangle.getColorX() + xDelta,
                                  triangle.getColorY() + yDelta, true);
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class OpaqueLabel extends JLabel {
        public boolean isOpaque() {
            return true;
        }
    }
}
