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
import java.awt.geom.Point2D;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.sun.swingset3.demos.ResourceManager;

/**
 * @author Mikhail Lapshin
 */
public class MandelbrotControl extends JPanel {

    private final JMandelbrot mandelbrot;
    private JSpinner iterSpinner;
    private CoordSpinner xSpinner;
    private CoordSpinner ySpinner;
    private static final double COORD_SPINNER_STEP = 0.1d; // part of width or height
    private final ResourceManager resourceManager;

    public MandelbrotControl(JMandelbrot mandelbrot,
            ResourceManager resourceManager) {
        this.mandelbrot = mandelbrot;
        this.resourceManager = resourceManager;
        createUI();
        installListeners();
    }

    private void createUI() {
        setLayout(new FlowLayout(FlowLayout.LEADING, 5, 0));
        setBorder(BorderFactory.createTitledBorder(
                resourceManager.getString("SpinnerDemo.fractalControls")));
        JSpinnerPanel spinnerPanel = new JSpinnerPanel();

        //<snip>Create spinner
        iterSpinner = new JSpinner(new SpinnerNumberModel(
                mandelbrot.getMaxIteration(), 10, 100000, 50));
        //</snip>
        //<snip>Add change listener using anonymus inner class
        iterSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                mandelbrot.setMaxIteration((Integer) iterSpinner.getValue());
                mandelbrot.calculatePicture();
            }
        });
        //</snip>
        spinnerPanel.addSpinner(
                resourceManager.getString("SpinnerDemo.iterations"), iterSpinner);

        //<snip>Create spinner
        final double xValue = mandelbrot.getCenter().getX();
        double width = mandelbrot.getXHighLimit() - mandelbrot.getXLowLimit();
        xSpinner = new CoordSpinner(xValue, width * COORD_SPINNER_STEP);
        //</snip>
        //<snip>Add change listener using anonymus inner class
        xSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Double newX = (Double) xSpinner.getValue();
                mandelbrot.setCenter(new Point2D.Double(
                        newX, mandelbrot.getCenter().getY()));
                mandelbrot.calculatePicture();
            }
        });
        //</snip>
        spinnerPanel.addSpinner(
                resourceManager.getString("SpinnerDemo.x"), xSpinner);

        //<snip>Create spinner
        final double yValue = mandelbrot.getCenter().getY();
        double height = mandelbrot.getYHighLimit() - mandelbrot.getYLowLimit();
        ySpinner = new CoordSpinner(yValue, height * COORD_SPINNER_STEP);
        //</snip>
        //<snip>Add change listener using anonymus inner class
        ySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Double newY = (Double) ySpinner.getValue();
                mandelbrot.setCenter(new Point2D.Double(
                        mandelbrot.getCenter().getX(), newY));
                mandelbrot.calculatePicture();
            }
        });
        //</snip>
        spinnerPanel.addSpinner(
                resourceManager.getString("SpinnerDemo.y"), ySpinner);

        add(spinnerPanel);
    }

    private void installListeners() {
        mandelbrot.addPropertyChangeListener(
                JMandelbrot.CENTER_PROPERTY_NAME,
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                double width = mandelbrot.getXHighLimit()
                        - mandelbrot.getXLowLimit();
                double newX = mandelbrot.getCenter().getX();
                xSpinner.updateModel(newX, width * COORD_SPINNER_STEP);
                double height = mandelbrot.getYHighLimit()
                        - mandelbrot.getYLowLimit();
                double newY = mandelbrot.getCenter().getY();
                ySpinner.updateModel(newY, height * COORD_SPINNER_STEP);
            }
        }
        );
    }

    //<snip>Customized spinner class
    // It uses special format for NumberEditor and has constant preferred width
    private static class CoordSpinner extends JSpinner {

        @Override
        protected JComponent createEditor(SpinnerModel model) {
            return new NumberEditor(this, "#.####################");
        }

        public CoordSpinner(double value, double stepSize) {
            super(new SpinnerNumberModel(value, -100, 100, stepSize));
        }

        //A useful shortcut method
        public void updateModel(double value, double stepSize) {
            SpinnerNumberModel model = (SpinnerNumberModel) getModel();
            model.setValue(value);
            model.setStepSize(stepSize);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension prefSize = super.getPreferredSize();
            prefSize.setSize(180, prefSize.getHeight());
            return prefSize;
        }
    }
    //</snip>
}
