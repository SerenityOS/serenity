/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole.inspector;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;

import sun.tools.jconsole.*;

@SuppressWarnings("serial")
public class XPlottingViewer extends PlotterPanel implements ActionListener {
    // TODO: Make number of decimal places customizable
    private static final int PLOTTER_DECIMALS = 4;

    private JButton plotButton;
    // The plotter cache holds Plotter instances for the various attributes
    private static HashMap<String, XPlottingViewer> plotterCache =
        new HashMap<String, XPlottingViewer>();
     private static HashMap<String, Timer> timerCache =
         new HashMap<String, Timer>();
    private MBeansTab tab;
    private String attributeName;
    private String key;
    private JTable table;
    private XPlottingViewer(String key,
                            XMBean mbean,
                            String attributeName,
                            Object value,
                            JTable table,
                            MBeansTab tab) {
        super(null);

        this.tab = tab;
        this.key = key;
        this.table = table;
        this.attributeName = attributeName;
        Plotter plotter = createPlotter(mbean, attributeName, key, table);
        setupDisplay(plotter);
    }

    static void dispose(MBeansTab tab) {
        Iterator<String> it = plotterCache.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            if(key.startsWith(String.valueOf(tab.hashCode()))) {
                it.remove();
            }
        }
        //plotterCache.clear();
        it = timerCache.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            if(key.startsWith(String.valueOf(tab.hashCode()))) {
                Timer t = timerCache.get(key);
                t.cancel();
                it.remove();
            }
        }
    }

    public static boolean isViewableValue(Object value) {
        return (value instanceof Number);
    }

    //Fired by dbl click
    public  static Component loadPlotting(XMBean mbean,
                                          String attributeName,
                                          Object value,
                                          JTable table,
                                          MBeansTab tab) {
        Component comp = null;
        if(isViewableValue(value)) {
            String key = String.valueOf(tab.hashCode()) + " " + String.valueOf(mbean.hashCode()) + " " + mbean.getObjectName().getCanonicalName() + attributeName;
            XPlottingViewer plotter = plotterCache.get(key);
            if(plotter == null) {
                plotter = new XPlottingViewer(key,
                                              mbean,
                                              attributeName,
                                              value,
                                              table,
                                              tab);
                plotterCache.put(key, plotter);
            }

            comp = plotter;
        }
        return comp;
    }

    /*public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(g.getColor());
        plotter.paintComponent(g);
        }*/
    @Override
    public void actionPerformed(ActionEvent evt) {
        plotterCache.remove(key);
        Timer t = timerCache.remove(key);
        t.cancel();
        ((XMBeanAttributes) table).collapse(attributeName, this);
    }

    //Create plotter instance
    public Plotter createPlotter(final XMBean xmbean,
                                 final String attributeName,
                                 String key,
                                 JTable table) {
        final Plotter plotter = new XPlotter(table, Plotter.Unit.NONE) {
                Dimension prefSize = new Dimension(400, 170);
            @Override
                public Dimension getPreferredSize() {
                    return prefSize;
                }
            @Override
                public Dimension getMinimumSize() {
                    return prefSize;
                }
            };

        plotter.createSequence(attributeName, attributeName, null, true);

        TimerTask timerTask = new TimerTask() {
                public void run() {
                    tab.workerAdd(new Runnable() {
                            public void run() {
                                try {
                                    Number n =
                                        (Number) xmbean.getSnapshotMBeanServerConnection().getAttribute(xmbean.getObjectName(), attributeName);
                                    long v;
                                    if (n instanceof Float || n instanceof Double) {
                                        plotter.setDecimals(PLOTTER_DECIMALS);
                                        double d = (n instanceof Float) ? (Float)n : (Double)n;
                                        v = Math.round(d * Math.pow(10.0, PLOTTER_DECIMALS));
                                    } else {
                                        v = n.longValue();
                                    }
                                    plotter.addValues(System.currentTimeMillis(), v);
                                } catch (Exception ex) {
                                    // Should have a trace logged with proper
                                    // trace mecchanism
                                }
                            }
                        });
                }
            };

        String timerName = "Timer-" + key;
        Timer timer = new Timer(timerName, true);
        timer.schedule(timerTask, 0, tab.getUpdateInterval());
        timerCache.put(key, timer);
        return plotter;
    }

    private void setupDisplay(Plotter plotter) {
        final JPanel buttonPanel = new JPanel();
        final GridBagLayout gbl = new GridBagLayout();
        buttonPanel.setLayout(gbl);
        setLayout(new BorderLayout());
        plotButton = new JButton(Messages.DISCARD_CHART);
        plotButton.addActionListener(this);
        plotButton.setEnabled(true);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 0;
        buttonConstraints.fill = GridBagConstraints.VERTICAL;
        buttonConstraints.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(plotButton, buttonConstraints);
        buttonPanel.add(plotButton);

        if (attributeName != null && attributeName.length()!=0) {
            final JPanel plotterLabelPanel = new JPanel();
            final JLabel label = new JLabel(attributeName);
            final GridBagLayout gbl2 = new GridBagLayout();
            plotterLabelPanel.setLayout(gbl2);
            final GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = 0;
            labelConstraints.fill = GridBagConstraints.VERTICAL;
            labelConstraints.anchor = GridBagConstraints.CENTER;
            labelConstraints.ipady = 10;
            gbl2.setConstraints(label, labelConstraints);
            plotterLabelPanel.add(label);
            add(plotterLabelPanel, BorderLayout.NORTH);
        }
        setPlotter(plotter);
        add(buttonPanel, BorderLayout.SOUTH);
        repaint();
    }

}
