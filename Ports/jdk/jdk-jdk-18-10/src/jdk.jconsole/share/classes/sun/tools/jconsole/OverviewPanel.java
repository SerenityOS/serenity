/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;


import static javax.swing.SwingConstants.*;
import static sun.tools.jconsole.JConsole.*;
import static sun.tools.jconsole.Utilities.*;


@SuppressWarnings("serial")
abstract class OverviewPanel extends PlotterPanel {
    private static final Dimension PREFERRED_PLOTTER_SIZE = new Dimension(300, 200);
    private static final Dimension MINIMUM_PLOTTER_SIZE = new Dimension(200, 150);

    // This is the default view range for all the overview plotters
    static final int VIEW_RANGE = -1;   // Show all data

    static Color PLOTTER_COLOR = IS_GTK ? new Color(231, 111, 80) : null;

    private JLabel infoLabel;

    public OverviewPanel(String title) {
        this(title, null, null, null);
    }

    public OverviewPanel(String title, String plotterKey,
                         String plotterName, Plotter.Unit plotterUnit) {
        super(title);
        setLayout(new BorderLayout(0, 0));

        if (plotterKey != null && plotterName != null) {
            Plotter plotter = new Plotter();
            plotter.setPreferredSize(PREFERRED_PLOTTER_SIZE);
            plotter.setMinimumSize(MINIMUM_PLOTTER_SIZE);
            plotter.setViewRange(VIEW_RANGE);
            if (plotterUnit != null) {
                plotter.setUnit(plotterUnit);
            }
            plotter.createSequence(plotterKey, plotterName, PLOTTER_COLOR, true);
            setAccessibleName(plotter,
                              Resources.format(Messages.OVERVIEW_PANEL_PLOTTER_ACCESSIBLE_NAME,
                                      title));
            setPlotter(plotter);
        }
    }


    public JLabel getInfoLabel() {
        if (infoLabel == null) {
            infoLabel = new JLabel("", CENTER) {
                @Override
                public void setText(String text) {
                    if (text.startsWith("<html>")) {
                        // Replace spaces with nbsp, except the
                        // last one of two or more (to allow wrapping)
                        StringBuilder buf = new StringBuilder();
                        char[] chars = text.toCharArray();
                        int n = chars.length;
                        for (int i = 0; i < n; i++) {
                            if (chars[i] == ' '
                                && ((i < n-1 && chars[i+1] == ' ')
                                    || ((i == 0 || chars[i-1] != ' ')
                                        && (i == n-1 || chars[i+1] != ' ')))) {
                                buf.append("&nbsp;");
                            } else {
                                buf.append(chars[i]);
                            }
                        }
                        text = buf.toString();
                    }
                    super.setText(text);
                }
            };

            if (IS_GTK) {
                JPanel southPanel = new JPanel(new BorderLayout());
                JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
                southPanel.add(separator, BorderLayout.NORTH);
                southPanel.add(infoLabel, BorderLayout.SOUTH);
                add(southPanel, BorderLayout.SOUTH);
            } else {
                add(infoLabel, BorderLayout.SOUTH);
            }
        }
        return infoLabel;
    }
}
