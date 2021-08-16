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
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

@SuppressWarnings("serial") // Superclass is not serializable across versions
final class ColorChooserPanel extends AbstractColorChooserPanel implements PropertyChangeListener {
    private static final int MASK = 0xFF000000;
    private final ColorModel model;
    private final ColorPanel panel;
    private final DiagramComponent slider;
    private final DiagramComponent diagram;
    private final JFormattedTextField text;
    private final JLabel label;

    ColorChooserPanel(ColorModel model) {
        this.model = model;
        this.panel = new ColorPanel(this.model);
        this.slider = new DiagramComponent(this.panel, false);
        this.diagram = new DiagramComponent(this.panel, true);
        this.text = new JFormattedTextField();
        this.label = new JLabel(null, null, SwingConstants.RIGHT);
        ValueFormatter.init(6, true, this.text);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setEnabled(this, enabled);
    }

    private static void setEnabled(Container container, boolean enabled) {
        for (Component component : container.getComponents()) {
            component.setEnabled(enabled);
            if (component instanceof Container) {
                setEnabled((Container) component, enabled);
            }
        }
    }

    @Override
    public void updateChooser() {
        Color color = getColorFromModel();
        if (color != null) {
            this.panel.setColor(color);
            this.text.setValue(Integer.valueOf(color.getRGB()));
            this.slider.repaint();
            this.diagram.repaint();
        }
    }

    @Override
    protected void buildChooser() {
        if (0 == getComponentCount()) {
            setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 3;
            gbc.gridwidth = 2;
            gbc.weighty = 1.0;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets.top = 10;
            gbc.insets.right = 10;
            add(this.panel, gbc);

            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets.right = 5;
            gbc.insets.bottom = 10;
            add(this.label, gbc);

            gbc.gridx = 4;
            gbc.weightx = 0.0;
            gbc.insets.right = 10;
            add(this.text, gbc);

            gbc.gridx = 2;
            gbc.gridheight = 2;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.ipadx = this.text.getPreferredSize().height;
            gbc.ipady = getPreferredSize().height;
            add(this.slider, gbc);

            gbc.gridx = 1;
            gbc.insets.left = 10;
            gbc.ipadx = gbc.ipady;
            add(this.diagram, gbc);

            this.label.setLabelFor(this.text);
            this.text.addPropertyChangeListener("value", this); // NON-NLS: the property name
            this.slider.setBorder(this.text.getBorder());
            this.diagram.setBorder(this.text.getBorder());

            setInheritsPopupMenu(this, true); // CR:4966112
        }
        String label = this.model.getText(this, "HexCode"); // NON-NLS: suffix
        boolean visible = label != null;
        this.text.setVisible(visible);
        this.text.getAccessibleContext().setAccessibleDescription(label);
        this.label.setVisible(visible);
        if (visible) {
            this.label.setText(label);
            int mnemonic = this.model.getInteger(this, "HexCodeMnemonic"); // NON-NLS: suffix
            if (mnemonic > 0) {
                this.label.setDisplayedMnemonic(mnemonic);
                mnemonic = this.model.getInteger(this, "HexCodeMnemonicIndex"); // NON-NLS: suffix
                if (mnemonic >= 0) {
                    this.label.setDisplayedMnemonicIndex(mnemonic);
                }
            }
        }
        this.panel.buildPanel();
    }

    @Override
    public String getDisplayName() {
        return this.model.getText(this, "Name"); // NON-NLS: suffix
    }

    @Override
    public int getMnemonic() {
        return this.model.getInteger(this, "Mnemonic"); // NON-NLS: suffix
    }

    @Override
    public int getDisplayedMnemonicIndex() {
        return this.model.getInteger(this, "DisplayedMnemonicIndex"); // NON-NLS: suffix
    }

    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }

    @Override
    public void setColorTransparencySelectionEnabled(boolean b){
        boolean oldValue = isColorTransparencySelectionEnabled();
        if (b != oldValue) {
            panel.setColorTransparencySelectionEnabled(b);
            firePropertyChange(TRANSPARENCY_ENABLED_PROPERTY,
                    oldValue, b);
        }
    }

    @Override
    public boolean isColorTransparencySelectionEnabled(){
        return panel.isColorTransparencySelectionEnabled();
    }

    public void propertyChange(PropertyChangeEvent event) {
        ColorSelectionModel model = getColorSelectionModel();
        if (model != null) {
            Object object = event.getNewValue();
            if (object instanceof Integer) {
                int value = MASK & model.getSelectedColor().getRGB() | (Integer) object;
                model.setSelectedColor(new Color(value, true));
            }
        }
        this.text.selectAll();
    }

    /**
     * Allows to show context popup for all components recursively.
     *
     * @param component  the root component of the tree
     * @param value      whether or not the popup menu is inherited
     */
    private static void setInheritsPopupMenu(JComponent component, boolean value) {
        component.setInheritsPopupMenu(value);
        for (Object object : component.getComponents()) {
            if (object instanceof JComponent) {
                setInheritsPopupMenu((JComponent) object, value);
            }
        }
    }
}
