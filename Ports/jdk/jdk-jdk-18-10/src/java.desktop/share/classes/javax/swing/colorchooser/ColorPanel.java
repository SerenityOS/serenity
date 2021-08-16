/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.colorchooser;

import java.awt.Color;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.JSpinner.DefaultEditor;

@SuppressWarnings("serial") // Superclass is not serializable across versions
final class ColorPanel extends JPanel implements ActionListener {

    private final SlidingSpinner[] spinners = new SlidingSpinner[5];
    private final float[] values = new float[this.spinners.length];

    private final ColorModel model;
    private Color color;
    private int x = 1;
    private int y = 2;
    private int z;

    ColorPanel(ColorModel model) {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 1;
        ButtonGroup group = new ButtonGroup();
        EmptyBorder border = null;
        for (int i = 0; i < this.spinners.length; i++) {
            if (i < 3) {
                JRadioButton button = new JRadioButton();
                if (i == 0) {
                    Insets insets = button.getInsets();
                    insets.left = button.getPreferredSize().width;
                    border = new EmptyBorder(insets);
                    button.setSelected(true);
                    gbc.insets.top = 5;
                }
                add(button, gbc);
                group.add(button);
                button.setActionCommand(Integer.toString(i));
                button.addActionListener(this);
                this.spinners[i] = new SlidingSpinner(this, button);
            }
            else {
                JLabel label = new JLabel();
                add(label, gbc);
                label.setBorder(border);
                label.setFocusable(false);
                this.spinners[i] = new SlidingSpinner(this, label);
            }
        }
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.insets.top = 0;
        gbc.insets.left = 5;
        for (SlidingSpinner spinner : this.spinners) {
            add(spinner.getSlider(), gbc);
            gbc.insets.top = 5;
        }
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        gbc.insets.top = 0;
        for (SlidingSpinner spinner : this.spinners) {
            add(spinner.getSpinner(), gbc);
            gbc.insets.top = 5;
        }
        setFocusable(false);

        this.model = model;
    }

    public void actionPerformed(ActionEvent event) {
        try {
            this.z = Integer.parseInt(event.getActionCommand());
            this.y = (this.z != 2) ? 2 : 1;
            this.x = (this.z != 0) ? 0 : 1;
            getParent().repaint();
        }
        catch (NumberFormatException exception) {
        }
    }

    void buildPanel() {
        int count = this.model.getCount();
        this.spinners[4].setVisible(count > 4);
        for (int i = 0; i < count; i++) {
            String text = this.model.getLabel(this, i);
            Object object = this.spinners[i].getLabel();
            if (object instanceof JRadioButton) {
                JRadioButton button = (JRadioButton) object;
                button.setText(text);
                button.getAccessibleContext().setAccessibleDescription(text);
            }
            else if (object instanceof JLabel) {
                JLabel label = (JLabel) object;
                label.setText(text);
            }
            this.spinners[i].setRange(this.model.getMinimum(i), this.model.getMaximum(i));
            this.spinners[i].setValue(this.values[i]);
            this.spinners[i].getSlider().getAccessibleContext().setAccessibleName(text);
            this.spinners[i].getSpinner().getAccessibleContext().setAccessibleName(text);
            DefaultEditor editor = (DefaultEditor) this.spinners[i].getSpinner().getEditor();
            editor.getTextField().getAccessibleContext().setAccessibleName(text);
            this.spinners[i].getSlider().getAccessibleContext().setAccessibleDescription(text);
            this.spinners[i].getSpinner().getAccessibleContext().setAccessibleDescription(text);
            editor.getTextField().getAccessibleContext().setAccessibleDescription(text);
        }
    }

    void colorChanged() {
        this.color = new Color(getColor(0), isColorTransparencySelectionEnabled());
        Object parent = getParent();
        if (parent instanceof ColorChooserPanel) {
            ColorChooserPanel chooser = (ColorChooserPanel) parent;
            chooser.setSelectedColor(this.color);
            chooser.repaint();
        }
    }

    float getValueX() {
        return this.spinners[this.x].getValue();
    }

    float getValueY() {
        return 1.0f - this.spinners[this.y].getValue();
    }

    float getValueZ() {
        return 1.0f - this.spinners[this.z].getValue();
    }

    void setValue(float z) {
        this.spinners[this.z].setValue(1.0f - z);
        colorChanged();
    }

    void setValue(float x, float y) {
        this.spinners[this.x].setValue(x);
        this.spinners[this.y].setValue(1.0f - y);
        colorChanged();
    }

    int getColor(float z) {
        setDefaultValue(this.x);
        setDefaultValue(this.y);
        this.values[this.z] = 1.0f - z;
        return getColor(3);
    }

    int getColor(float x, float y) {
        this.values[this.x] = x;
        this.values[this.y] = 1.0f - y;
        setValue(this.z);
        return getColor(3);
    }

    void setColor(Color color) {
        if (!color.equals(this.color)) {
            this.color = color;
            this.model.setColor(color.getRGB(), this.values);
            for (int i = 0; i < this.model.getCount(); i++) {
                this.spinners[i].setValue(this.values[i]);
            }
        }
    }

    private int getColor(int index) {
        while (index < this.model.getCount()) {
            setValue(index++);
        }
        return this.model.getColor(this.values);
    }

    void setColorTransparencySelectionEnabled(boolean b) {
        if (spinners[model.getCount() - 1].isVisible() != b) {
            spinners[model.getCount() - 1].setVisible(b);
            colorChanged();
        }
    }

    boolean isColorTransparencySelectionEnabled() {
        return spinners[model.getCount() - 1].isVisible();
    }

    private void setValue(int index) {
        this.values[index] = this.spinners[index].getValue();
    }

    private void setDefaultValue(int index) {
        float value = this.model.getDefault(index);
        this.values[index] = (value < 0.0f)
                ? this.spinners[index].getValue()
                : value;
    }
}
