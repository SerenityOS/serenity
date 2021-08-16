/*
 * Copyright (c) 2004, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

/**
 * A combo box to control the visible time range for one or more Plotter components.
 * When used with two or more Plotters, it also acts to coordinate the range between
 * them.
 */
@SuppressWarnings("serial")
public class TimeComboBox extends JComboBox<String> implements ItemListener, PropertyChangeListener {
    private ArrayList<Plotter> plotters = new ArrayList<Plotter>();

    public TimeComboBox(Plotter... plotterArray) {
        super(Plotter.rangeNames);

        addItemListener(this);

        if (plotterArray != null && plotterArray.length > 0) {
            plotters.addAll(Arrays.asList(plotterArray));
            selectValue(plotterArray[0].getViewRange());
            for (Plotter plotter : plotters) {
                plotter.addPropertyChangeListener(this);
            }
        }
    }

    public void addPlotter(Plotter plotter) {
        plotters.add(plotter);
        if (plotters.size() == 1) {
            selectValue(plotter.getViewRange());
        }
        plotter.addPropertyChangeListener(this);
    }

    public void itemStateChanged(ItemEvent ev) {
        for (Plotter plotter : plotters) {
            plotter.setViewRange(Plotter.rangeValues[getSelectedIndex()]);
        }
    }

    private void selectValue(int value) {
        // Set the selected value
        for (int i = 0; i < Plotter.rangeValues.length; i++) {
            if (Plotter.rangeValues[i] == value) {
                setSelectedItem(Plotter.rangeNames[i]);
            }
        }
        // Make sure all plotters show this value
        if (plotters.size() > 1) {
            for (Plotter plotter : plotters) {
                plotter.setViewRange(value);
            }
        }
    }

    public void propertyChange(PropertyChangeEvent ev) {
        if (ev.getPropertyName() == "viewRange") {
            selectValue((Integer)ev.getNewValue());
        }
    }
}
