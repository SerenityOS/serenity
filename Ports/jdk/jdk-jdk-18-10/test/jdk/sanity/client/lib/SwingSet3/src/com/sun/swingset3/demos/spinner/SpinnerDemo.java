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
import java.beans.PropertyChangeEvent;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * Demonstrates JSpinner and SwingWorker
 *
 * @author Mikhail Lapshin
 */
@DemoProperties(
        value = "Spinner Demo",
        category = "Controls",
        description = "Demonstrates JSpinner and SwingWorker",
        sourceFiles = {
            "com/sun/swingset3/demos/spinner/SpinnerDemo.java",
            "com/sun/swingset3/demos/spinner/JMandelbrot.java",
            "com/sun/swingset3/demos/spinner/JPaletteShower.java",
            "com/sun/swingset3/demos/spinner/JSpinnerPanel.java",
            "com/sun/swingset3/demos/spinner/MandelbrotControl.java",
            "com/sun/swingset3/demos/spinner/Palette.java",
            "com/sun/swingset3/demos/spinner/PaletteChooser.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/spinner/resources/SpinnerDemo.properties",
            "com/sun/swingset3/demos/spinner/resources/images/SpinnerDemo.gif"
        }
)
public class SpinnerDemo extends JPanel {

    private final ResourceManager resourceManager = new ResourceManager(getClass());
    public static final String DEMO_TITLE = SpinnerDemo.class.getAnnotation(DemoProperties.class).value();

    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);
        frame.getContentPane().add(new SpinnerDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public SpinnerDemo() {
        setLayout(new BorderLayout());

        // Create main components
        PaletteChooser chooser
                = new PaletteChooser(resourceManager);
        final JMandelbrot mandelbrot
                = new JMandelbrot(400, 400, chooser.getPalette(), resourceManager);
        MandelbrotControl mandelbrotControl
                = new MandelbrotControl(mandelbrot, resourceManager);

        // Connect palette chooser and mandelbrot component
        chooser.addPropertyChangeListener(PaletteChooser.PALETTE_PROPERTY_NAME,
                (PropertyChangeEvent evt) -> {
                    mandelbrot.setPalette((Palette) evt.getNewValue());
                    mandelbrot.calculatePicture();
                });

        // Layout components
        add(mandelbrot);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(chooser, BorderLayout.NORTH);

        JPanel mandelbrotControlPanel = new JPanel();
        mandelbrotControlPanel.setLayout(new BorderLayout());
        mandelbrotControlPanel.add(mandelbrotControl, BorderLayout.NORTH);
        controlPanel.add(mandelbrotControlPanel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.EAST);
    }
}
