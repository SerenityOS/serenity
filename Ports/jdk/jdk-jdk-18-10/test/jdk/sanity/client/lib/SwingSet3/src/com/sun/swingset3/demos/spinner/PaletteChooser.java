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
package com.sun.swingset3.demos.spinner;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.sun.swingset3.demos.ResourceManager;

/**
 * @author Mikhail Lapshin
 */
public class PaletteChooser extends JPanel {

    private static final int MIN_COLOR = 50;
    private static final int MAX_COLOR = 255;
    private static final int R_STEPS = 5;
    private static final int G_STEPS = 5;
    private static final int B_STEPS = 5;
    private static final int R_ANGLE = 270;
    private static final int G_ANGLE = 90;
    private static final int B_ANGLE = 0;

    public static final String PALETTE_PROPERTY_NAME = "palette";

    private final ResourceManager resourceManager;
    private Palette palette;
    private final JPaletteShower shower;
    private final ChangeListener changeListener;

    private JSpinner rsSpinner;
    private JSpinner gsSpinner;
    private JSpinner bsSpinner;
    private JSpinner raSpinner;
    private JSpinner gaSpinner;
    private JSpinner baSpinner;

    public PaletteChooser(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        palette = new Palette(MAX_COLOR - MIN_COLOR, MIN_COLOR, MAX_COLOR,
                Math.toRadians(R_ANGLE), Math.toRadians(G_ANGLE),
                Math.toRadians(B_ANGLE), R_STEPS, G_STEPS, B_STEPS);
        shower = new JPaletteShower(palette, 250, 25);

        //<snip>Use single change listener for several spinners
        changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setPalette(createPalette());
                shower.setPalette(palette);
                repaint();
            }
        };
        //</snip>

        setBorder(BorderFactory.createTitledBorder(
                resourceManager.getString("SpinnerDemo.colorPalette")));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(shower);
        add(createControlPanel());
    }

    private double toRadians(JSpinner spinner) {
        return Math.toRadians(getIntValue(spinner));
    }

    private Palette createPalette() {
        return new Palette(getWidth(), MIN_COLOR, MAX_COLOR,
                toRadians(raSpinner), toRadians(gaSpinner),
                toRadians(baSpinner), getIntValue(rsSpinner),
                getIntValue(gsSpinner), getIntValue(bsSpinner));
    }

    private static int getIntValue(JSpinner spinner) {
        return (Integer) spinner.getValue();
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(createStepPanel());
        controlPanel.add(createStartAnglePanel());
        return controlPanel;
    }

    private JPanel createStartAnglePanel() {
        JSpinnerPanel startAnglePanel = new JSpinnerPanel();
        startAnglePanel.setBorder(BorderFactory.createTitledBorder(
                resourceManager.getString("SpinnerDemo.startAngles")));

        raSpinner = createAngleSpinner(R_ANGLE, "SpinnerDemo.r", startAnglePanel);
        gaSpinner = createAngleSpinner(G_ANGLE, "SpinnerDemo.g", startAnglePanel);
        baSpinner = createAngleSpinner(B_ANGLE, "SpinnerDemo.b", startAnglePanel);

        return startAnglePanel;
    }

    private JPanel createStepPanel() {
        JSpinnerPanel stepPanel = new JSpinnerPanel();
        stepPanel.setBorder(BorderFactory.createTitledBorder(
                resourceManager.getString("SpinnerDemo.steps")));

        rsSpinner = createStepSpinner(R_STEPS, "SpinnerDemo.r", stepPanel);
        gsSpinner = createStepSpinner(G_STEPS, "SpinnerDemo.g", stepPanel);
        bsSpinner = createStepSpinner(B_STEPS, "SpinnerDemo.b", stepPanel);

        return stepPanel;
    }

    private JSpinner createAngleSpinner(int startAngle, String resourceName,
            JSpinnerPanel parent) {
        SpinnerModel model = new SpinnerNumberModel(startAngle, 0, 360, 10);
        return createSpinner(model, resourceName, parent);
    }

    private JSpinner createStepSpinner(int startSteps, String resourceName,
            JSpinnerPanel parent) {
        SpinnerModel model = new SpinnerNumberModel(startSteps, 1, 1000, 1);
        return createSpinner(model, resourceName, parent);
    }

    private JSpinner createSpinner(SpinnerModel model, String resourceName,
            JSpinnerPanel parent) {

        //<snip>Create spinner
        JSpinner spinner = new JSpinner(model);
        //</snip>
        //<snip>Use single change listener for several spinners
        spinner.addChangeListener(changeListener);
        //</snip>
        parent.addSpinner(resourceManager.getString(resourceName), spinner);
        return spinner;
    }

    public Palette getPalette() {
        return palette;
    }

    private void setPalette(Palette palette) {
        Palette oldValue = this.palette;
        this.palette = palette;
        firePropertyChange(PALETTE_PROPERTY_NAME, oldValue, palette);
    }
}
