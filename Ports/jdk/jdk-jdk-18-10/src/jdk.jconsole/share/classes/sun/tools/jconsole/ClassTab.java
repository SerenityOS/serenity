/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.border.*;


import java.util.concurrent.*;

import static sun.tools.jconsole.Formatter.*;
import static sun.tools.jconsole.Utilities.*;


@SuppressWarnings("serial")
class ClassTab extends Tab implements ActionListener {
    PlotterPanel loadedClassesMeter;
    TimeComboBox timeComboBox;
    private JCheckBox verboseCheckBox;
    private HTMLPane details;
    private ClassOverviewPanel overviewPanel;
    private boolean plotterListening = false;

    private static final String loadedPlotterKey        = "loaded";
    private static final String totalLoadedPlotterKey   = "totalLoaded";
    private static final Color  loadedPlotterColor      = Plotter.defaultColor;
    private static final Color  totalLoadedPlotterColor = Color.red;

    /*
      Hierarchy of panels and layouts for this tab:

        ClassTab (BorderLayout)

            North:  topPanel (BorderLayout)

                        Center: controlPanel (FlowLayout)
                                    timeComboBox

                        East:   topRightPanel (FlowLayout)
                                    verboseCheckBox

            Center: plotterPanel (BorderLayout)

                        Center: plotter

            South:  bottomPanel (BorderLayout)

                        Center: details
    */

    public static String getTabName() {
        return Messages.CLASSES;
    }

    public ClassTab(VMPanel vmPanel) {
        super(vmPanel, getTabName());

        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(4, 4, 3, 4));

        JPanel topPanel     = new JPanel(new BorderLayout());
        JPanel plotterPanel = new JPanel(new BorderLayout());
        JPanel bottomPanel  = new JPanel(new BorderLayout());

        add(topPanel,     BorderLayout.NORTH);
        add(plotterPanel, BorderLayout.CENTER);
        add(bottomPanel,  BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        topPanel.add(controlPanel, BorderLayout.CENTER);

        verboseCheckBox = new JCheckBox(Messages.VERBOSE_OUTPUT);
        verboseCheckBox.addActionListener(this);
        verboseCheckBox.setToolTipText(Messages.VERBOSE_OUTPUT_TOOLTIP);
        JPanel topRightPanel = new JPanel();
        topRightPanel.setBorder(new EmptyBorder(0, 65-8, 0, 70));
        topRightPanel.add(verboseCheckBox);
        topPanel.add(topRightPanel, BorderLayout.AFTER_LINE_ENDS);

        loadedClassesMeter = new PlotterPanel(Messages.NUMBER_OF_LOADED_CLASSES,
                                              Plotter.Unit.NONE, false);
        loadedClassesMeter.plotter.createSequence(loadedPlotterKey,
                                                  Messages.LOADED,
                                                  loadedPlotterColor,
                                                  true);
        loadedClassesMeter.plotter.createSequence(totalLoadedPlotterKey,
                                                  Messages.TOTAL_LOADED,
                                                  totalLoadedPlotterColor,
                                                  true);
        setAccessibleName(loadedClassesMeter.plotter,
                          Messages.CLASS_TAB_LOADED_CLASSES_PLOTTER_ACCESSIBLE_NAME);
        plotterPanel.add(loadedClassesMeter);

        timeComboBox = new TimeComboBox(loadedClassesMeter.plotter);
        controlPanel.add(new LabeledComponent(Messages.TIME_RANGE_COLON,
                                              Resources.getMnemonicInt(Messages.TIME_RANGE_COLON),
                                              timeComboBox));

        LabeledComponent.layout(plotterPanel);

        bottomPanel.setBorder(new CompoundBorder(new TitledBorder(Messages.DETAILS),
                                                 new EmptyBorder(10, 10, 10, 10)));

        details = new HTMLPane();
        setAccessibleName(details, Messages.DETAILS);
        JScrollPane scrollPane = new JScrollPane(details);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        bottomPanel.add(scrollPane, BorderLayout.SOUTH);

    }

    public void actionPerformed(ActionEvent ev) {
        final boolean b = verboseCheckBox.isSelected();
        workerAdd(new Runnable() {
            public void run() {
                ProxyClient proxyClient = vmPanel.getProxyClient();
                try {
                    proxyClient.getClassLoadingMXBean().setVerbose(b);
                } catch (UndeclaredThrowableException e) {
                    proxyClient.markAsDead();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        });
    }


    public SwingWorker<?, ?> newSwingWorker() {
        final ProxyClient proxyClient = vmPanel.getProxyClient();

        if (!plotterListening) {
            proxyClient.addWeakPropertyChangeListener(loadedClassesMeter.plotter);
            plotterListening = true;
        }

        return new SwingWorker<Boolean, Object>() {
            private long clCount, cuCount, ctCount;
            private boolean isVerbose;
            private String detailsStr;
            private long timeStamp;

            public Boolean doInBackground() {
                try {
                    ClassLoadingMXBean classLoadingMBean = proxyClient.getClassLoadingMXBean();

                    clCount = classLoadingMBean.getLoadedClassCount();
                    cuCount = classLoadingMBean.getUnloadedClassCount();
                    ctCount = classLoadingMBean.getTotalLoadedClassCount();
                    isVerbose = classLoadingMBean.isVerbose();
                    detailsStr = formatDetails();
                    timeStamp = System.currentTimeMillis();

                    return true;
                } catch (UndeclaredThrowableException e) {
                    proxyClient.markAsDead();
                    return false;
                } catch (IOException e) {
                    return false;
                }
            }

            protected void done() {
                try {
                    if (get()) {
                        loadedClassesMeter.plotter.addValues(timeStamp, clCount, ctCount);

                        if (overviewPanel != null) {
                            overviewPanel.updateClassInfo(ctCount, clCount);
                            overviewPanel.getPlotter().addValues(timeStamp, clCount);
                        }

                        loadedClassesMeter.setValueLabel(clCount + "");
                        verboseCheckBox.setSelected(isVerbose);
                        details.setText(detailsStr);
                    }
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                    if (JConsole.isDebug()) {
                        ex.printStackTrace();
                    }
                }
            }

            private String formatDetails() {
                String text = "<table cellspacing=0 cellpadding=0>";

                long time = System.currentTimeMillis();
                String timeStamp = formatDateTime(time);
                text += newRow(Messages.TIME, timeStamp);
                text += newRow(Messages.CURRENT_CLASSES_LOADED, justify(clCount, 5));
                text += newRow(Messages.TOTAL_CLASSES_LOADED,   justify(ctCount, 5));
                text += newRow(Messages.TOTAL_CLASSES_UNLOADED, justify(cuCount, 5));

                return text;
            }
        };
    }


    OverviewPanel[] getOverviewPanels() {
        if (overviewPanel == null) {
            overviewPanel = new ClassOverviewPanel();
        }
        return new OverviewPanel[] { overviewPanel };
    }

    private static class ClassOverviewPanel extends OverviewPanel {
        ClassOverviewPanel() {
            super(Messages.CLASSES, loadedPlotterKey, Messages.LOADED, null);
        }

        private void updateClassInfo(long total, long loaded) {
            long unloaded = (total - loaded);
            getInfoLabel().setText(Resources.format(Messages.CLASS_TAB_INFO_LABEL_FORMAT,
                                   loaded, unloaded, total));
        }
    }
}
