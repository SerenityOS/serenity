/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import java.util.concurrent.*;

import javax.accessibility.*;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.swing.*;
import javax.swing.border.*;


import static sun.tools.jconsole.Formatter.*;
import static sun.tools.jconsole.Utilities.*;

@SuppressWarnings("serial")
class MemoryTab extends Tab implements ActionListener, ItemListener {
    JComboBox<Plotter> plotterChoice;
    TimeComboBox timeComboBox;
    JButton gcButton;

    PlotterPanel plotterPanel;
    JPanel bottomPanel;
    HTMLPane details;
    PoolChart poolChart;

    ArrayList<Plotter> plotterList;
    Plotter heapPlotter, nonHeapPlotter;

    private MemoryOverviewPanel overviewPanel;

    private static final String usedKey        = "used";
    private static final String committedKey   = "committed";
    private static final String maxKey         = "max";
    private static final String thresholdKey   = "threshold";
    private static final Color  usedColor      = Plotter.defaultColor;
    private static final Color  committedColor = null;
    private static final Color  maxColor       = null;
    private static final Color  thresholdColor = Color.red;

    /*
      Hierarchy of panels and layouts for this tab:

        MemoryTab (BorderLayout)

            North:  topPanel (BorderLayout)

                        Center: controlPanel (FlowLayout)
                                    plotterChoice, timeComboBox

                        East:   topRightPanel (FlowLayout)
                                    gcButton

            Center: plotterPanel

                        Center: plotter

            South:  bottomPanel (BorderLayout)

                        Center: details
                        East:   poolChart
    */


    public static String getTabName() {
        return Messages.MEMORY;
    }

    public MemoryTab(VMPanel vmPanel) {
        super(vmPanel, getTabName());

        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(4, 4, 3, 4));

        JPanel topPanel     = new JPanel(new BorderLayout());
               plotterPanel = new PlotterPanel(null);
               bottomPanel  = new JPanel(new BorderLayout());

        add(topPanel,     BorderLayout.NORTH);
        add(plotterPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 5));
        topPanel.add(controlPanel, BorderLayout.CENTER);

        // Plotter choice
        plotterChoice = new JComboBox<Plotter>();
        plotterChoice.addItemListener(this);
        controlPanel.add(new LabeledComponent(Messages.CHART_COLON,
                                              Resources.getMnemonicInt(Messages.CHART_COLON),
                                              plotterChoice));

        // Range control
        timeComboBox = new TimeComboBox();
        controlPanel.add(new LabeledComponent(Messages.TIME_RANGE_COLON,
                                              Resources.getMnemonicInt(Messages.TIME_RANGE_COLON),
                                              timeComboBox));

        gcButton = new JButton(Messages.PERFORM_GC);
        gcButton.setMnemonic(Resources.getMnemonicInt(Messages.PERFORM_GC));
        gcButton.addActionListener(this);
        gcButton.setToolTipText(Messages.PERFORM_GC_TOOLTIP);
        JPanel topRightPanel = new JPanel();
        topRightPanel.setBorder(new EmptyBorder(0, 65-8, 0, 70));
        topRightPanel.add(gcButton);
        topPanel.add(topRightPanel, BorderLayout.AFTER_LINE_ENDS);

        bottomPanel.setBorder(new CompoundBorder(new TitledBorder(Messages.DETAILS),
                                                  new EmptyBorder(10, 10, 10, 10)));

        details = new HTMLPane();
        setAccessibleName(details, Messages.DETAILS);
        bottomPanel.add(new JScrollPane(details), BorderLayout.CENTER);

        poolChart = new PoolChart();
        bottomPanel.add(poolChart, BorderLayout.AFTER_LINE_ENDS);
    }


    private void createPlotters() throws IOException {
        plotterList = new ArrayList<Plotter>();

        ProxyClient proxyClient = vmPanel.getProxyClient();

        heapPlotter = new Plotter(Plotter.Unit.BYTES) {
            public String toString() {
                return Messages.HEAP_MEMORY_USAGE;
            }
        };
        proxyClient.addWeakPropertyChangeListener(heapPlotter);

        nonHeapPlotter = new Plotter(Plotter.Unit.BYTES) {
            public String toString() {
                return Messages.NON_HEAP_MEMORY_USAGE;
            }
        };

        setAccessibleName(heapPlotter,
                          Messages.MEMORY_TAB_HEAP_PLOTTER_ACCESSIBLE_NAME);
        setAccessibleName(nonHeapPlotter,
                          Messages.MEMORY_TAB_NON_HEAP_PLOTTER_ACCESSIBLE_NAME);

        proxyClient.addWeakPropertyChangeListener(nonHeapPlotter);

        heapPlotter.createSequence(usedKey,         Messages.USED,      usedColor,      true);
        heapPlotter.createSequence(committedKey,    Messages.COMMITTED, committedColor, false);
        heapPlotter.createSequence(maxKey,          Messages.MAX,       maxColor,       false);

        nonHeapPlotter.createSequence(usedKey,      Messages.USED,      usedColor,      true);
        nonHeapPlotter.createSequence(committedKey, Messages.COMMITTED, committedColor, false);
        nonHeapPlotter.createSequence(maxKey,       Messages.MAX,       maxColor,       false);

        plotterList.add(heapPlotter);
        plotterList.add(nonHeapPlotter);

        // Now add memory pools
        Map<ObjectName, MBeanInfo> mBeanMap = proxyClient.getMBeans("java.lang");
        Set<ObjectName> keys = mBeanMap.keySet();
        ObjectName[] objectNames = keys.toArray(new ObjectName[keys.size()]);
        ArrayList<PoolPlotter> nonHeapPlotters = new ArrayList<PoolPlotter>(2);
        for (ObjectName objectName : objectNames) {
            String type = objectName.getKeyProperty("type");
            if (type.equals("MemoryPool")) {
                String name = Resources.format(Messages.MEMORY_POOL_LABEL,
                                               objectName.getKeyProperty("name"));
                // Heap or non-heap?
                boolean isHeap = false;
                AttributeList al =
                    proxyClient.getAttributes(objectName,
                                              new String[] { "Type" });
                if (al.size() > 0) {
                    isHeap = MemoryType.HEAP.name().equals(((Attribute)al.get(0)).getValue());
                }
                PoolPlotter poolPlotter = new PoolPlotter(objectName, name, isHeap);
                proxyClient.addWeakPropertyChangeListener(poolPlotter);

                poolPlotter.createSequence(usedKey,      Messages.USED,      usedColor,      true);
                poolPlotter.createSequence(committedKey, Messages.COMMITTED, committedColor, false);
                poolPlotter.createSequence(maxKey,       Messages.MAX,       maxColor,       false);
                poolPlotter.createSequence(thresholdKey, Messages.THRESHOLD, thresholdColor, false);
                poolPlotter.setUseDashedTransitions(thresholdKey, true);

                if (isHeap) {
                    plotterList.add(poolPlotter);
                } else {
                    // Will be added to plotterList below
                    nonHeapPlotters.add(poolPlotter);
                }
            }
        }
        // Add non-heap plotters last
        for (PoolPlotter poolPlotter : nonHeapPlotters) {
            plotterList.add(poolPlotter);
        }
    }


    public void itemStateChanged(ItemEvent ev) {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            Plotter plotter = (Plotter)plotterChoice.getSelectedItem();
            plotterPanel.setPlotter(plotter);
            plotterPanel.repaint();
        }
    }

    public void gc() {
        new Thread("MemoryPanel.gc") {
            public void run() {
                ProxyClient proxyClient = vmPanel.getProxyClient();
                try {
                    proxyClient.getMemoryMXBean().gc();
                } catch (UndeclaredThrowableException e) {
                    proxyClient.markAsDead();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }.start();
    }

    public SwingWorker<?, ?> newSwingWorker() {
        return new SwingWorker<Boolean, Object>() {
            private long[] used, committed, max, threshold;
            private long timeStamp;
            private String detailsStr;
            private boolean initialRun = false;

            public Boolean doInBackground() {
                ProxyClient proxyClient = vmPanel.getProxyClient();

                if (plotterList == null) {
                    try {
                        createPlotters();
                    } catch (UndeclaredThrowableException e) {
                        proxyClient.markAsDead();
                        return false;
                    } catch (final IOException ex) {
                        return false;
                    }
                    initialRun = true;
                }

                int n = plotterList.size();
                used      = new long[n];
                committed = new long[n];
                max       = new long[n];
                threshold = new long[n];
                timeStamp = System.currentTimeMillis();

                for (int i = 0; i < n; i++) {
                    Plotter plotter = plotterList.get(i);
                    MemoryUsage mu = null;
                    used[i] = -1L;
                    threshold[i] = -1L;

                    try {
                        if (plotter instanceof PoolPlotter) {
                            PoolPlotter poolPlotter = (PoolPlotter)plotter;
                            ObjectName objectName = poolPlotter.objectName;
                            AttributeList al =
                                proxyClient.getAttributes(objectName,
                                                          new String[] { "Usage", "UsageThreshold" });
                            if (al.size() > 0) {
                                CompositeData cd = (CompositeData)((Attribute)al.get(0)).getValue();
                                mu = MemoryUsage.from(cd);

                                if (al.size() > 1) {
                                    threshold[i] = (Long)((Attribute)al.get(1)).getValue();
                                }
                            }
                        } else if (plotter == heapPlotter) {
                            mu = proxyClient.getMemoryMXBean().getHeapMemoryUsage();
                        } else if (plotter == nonHeapPlotter) {
                            mu = proxyClient.getMemoryMXBean().getNonHeapMemoryUsage();
                        }
                    } catch (UndeclaredThrowableException e) {
                        proxyClient.markAsDead();
                        return false;
                    } catch (IOException ex) {
                        // Skip this plotter
                    }

                    if (mu != null) {
                        used[i]      = mu.getUsed();
                        committed[i] = mu.getCommitted();
                        max[i]       = mu.getMax();
                    }
                }
                detailsStr = formatDetails();

                return true;
            }

            protected void done() {
                try {
                    if (!get()) {
                        return;
                    }
                } catch (InterruptedException ex) {
                    return;
                } catch (ExecutionException ex) {
                    if (JConsole.isDebug()) {
                        ex.printStackTrace();
                    }
                    return;
                }

                if (initialRun) {
                    // Add Memory Pools
                    for (Plotter p : plotterList) {
                        plotterChoice.addItem(p);
                        timeComboBox.addPlotter(p);
                    }
                    add(bottomPanel,  BorderLayout.SOUTH);
                }


                int n = plotterList.size();
                int poolCount = 0;

                for (int i = 0; i < n; i++) {
                    Plotter plotter = plotterList.get(i);
                    if (used[i] >= 0L) {
                        if (plotter instanceof PoolPlotter) {
                            plotter.addValues(timeStamp, used[i], committed[i], max[i], threshold[i]);
                            if (threshold[i] > 0L) {
                                plotter.setIsPlotted(thresholdKey, true);
                            }
                            poolChart.setValue(poolCount++, (PoolPlotter)plotter,
                                               used[i], threshold[i], max[i]);
                        } else {
                            plotter.addValues(timeStamp, used[i], committed[i], max[i]);
                        }

                        if (plotter == heapPlotter && overviewPanel != null) {
                            overviewPanel.getPlotter().addValues(timeStamp, used[i]);
                            overviewPanel.updateMemoryInfo(used[i], committed[i], max[i]);
                        }
                    }
                }
                details.setText(detailsStr);
            }
        };
    }

    private String formatDetails() {
        ProxyClient proxyClient = vmPanel.getProxyClient();
        if (proxyClient.isDead()) {
            return "";
        }

        String text = "<table cellspacing=0 cellpadding=0>";

        Plotter plotter = (Plotter)plotterChoice.getSelectedItem();
        if (plotter == null) {
            return "";
        }

        //long time = plotter.getLastTimeStamp();
        long time = System.currentTimeMillis();
        String timeStamp = formatDateTime(time);
        text += newRow(Messages.TIME, timeStamp);

        long used = plotter.getLastValue(usedKey);
        long committed = plotter.getLastValue(committedKey);
        long max = plotter.getLastValue(maxKey);
        long threshold = plotter.getLastValue(thresholdKey);

        text += newRow(Messages.USED, formatKBytes(used));
        if (committed > 0L) {
            text += newRow(Messages.COMMITTED, formatKBytes(committed));
        }
        if (max > 0L) {
            text += newRow(Messages.MAX, formatKBytes(max));
        }
        if (threshold > 0L) {
            text += newRow(Messages.USAGE_THRESHOLD, formatKBytes(threshold));
        }

        try {
            Collection<GarbageCollectorMXBean> garbageCollectors =
                proxyClient.getGarbageCollectorMXBeans();

            boolean descPrinted = false;
            for (GarbageCollectorMXBean garbageCollectorMBean : garbageCollectors) {
                String gcName = garbageCollectorMBean.getName();
                long gcCount = garbageCollectorMBean.getCollectionCount();
                long gcTime = garbageCollectorMBean.getCollectionTime();
                String str = Resources.format(Messages.GC_TIME_DETAILS, justify(formatTime(gcTime), 14),
                                              gcName,
                                              String.format("%,d",gcCount));
                if (!descPrinted) {
                    text += newRow(Messages.GC_TIME, str);
                    descPrinted = true;
                } else {
                    text += newRow(null, str);
                }
           }
        } catch (IOException e) {
        }

        return text;
    }

    public void actionPerformed(ActionEvent ev) {
        Object src = ev.getSource();
        if (src == gcButton) {
            gc();
        }
    }

    private class PoolPlotter extends Plotter {
        ObjectName objectName;
        String name;
        boolean isHeap;
        long value, threshold, max;
        int barX;

        public PoolPlotter(ObjectName objectName, String name, boolean isHeap) {
            super(Plotter.Unit.BYTES);

            this.objectName = objectName;
            this.name       = name;
            this.isHeap     = isHeap;

            setAccessibleName(this,
                              Resources.format(Messages.MEMORY_TAB_POOL_PLOTTER_ACCESSIBLE_NAME,
                                               name));
        }


        public String toString() {
            return name;
        }
    }

    private class PoolChart extends BorderedComponent
                            implements Accessible, MouseListener {
        final int height       = 150;
        final int leftMargin   =  50;
        final int rightMargin  =  23;
        final int bottomMargin =  35;
        final int barWidth     =  22;
        final int barGap       =   3;
        final int groupGap     =   8;
        final int barHeight    = height * 2 / 3;

        final Color greenBar           = new Color(100, 255, 100);
        final Color greenBarBackground = new Color(210, 255, 210);
        final Color redBarBackground   = new Color(255, 210, 210);

        Font smallFont = null;

        ArrayList<PoolPlotter> poolPlotters = new ArrayList<PoolPlotter>(5);

        int nHeapPools    = 0;
        int nNonHeapPools = 0;
        Rectangle heapRect    = new Rectangle(leftMargin,            height - bottomMargin + 6, barWidth, 20);
        Rectangle nonHeapRect = new Rectangle(leftMargin + groupGap, height - bottomMargin + 6, barWidth, 20);

        public PoolChart() {
            super(null, null);

            setFocusable(true);
            addMouseListener(this);
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        public void setValue(int poolIndex, PoolPlotter poolPlotter,
                             long value, long threshold, long max) {
            poolPlotter.value = value;
            poolPlotter.threshold = threshold;
            poolPlotter.max = max;

            if (poolIndex == poolPlotters.size()) {
                poolPlotters.add(poolPlotter);
                if (poolPlotter.isHeap) {
                    poolPlotter.barX = nHeapPools * (barWidth + barGap);
                    nHeapPools++;
                    heapRect.width = nHeapPools * barWidth + (nHeapPools - 1) * barGap;
                    nonHeapRect.x  = leftMargin + heapRect.width + groupGap;
                } else {
                    poolPlotter.barX = nonHeapRect.x - leftMargin + nNonHeapPools * (barWidth + barGap);
                    nNonHeapPools++;
                    nonHeapRect.width = nNonHeapPools * barWidth + (nNonHeapPools - 1) * barGap;
                }
            } else {
                poolPlotters.set(poolIndex, poolPlotter);
            }
            repaint();
        }

        private void paintPoolBar(Graphics g, PoolPlotter poolPlotter) {
            Rectangle barRect = getBarRect(poolPlotter);
            g.setColor(Color.gray);
            g.drawRect(barRect.x, barRect.y, barRect.width, barRect.height);

            long value = poolPlotter.value;
            long max   = poolPlotter.max;
            if (max > 0L) {
                g.translate(barRect.x, barRect.y);

                // Paint green background
                g.setColor(greenBarBackground);
                g.fillRect(1, 1, barRect.width - 1, barRect.height - 1);

                int greenHeight = (int)(value * barRect.height / max);
                long threshold = poolPlotter.threshold;
                if (threshold > 0L) {
                    int redHeight = (int)(threshold * barRect.height / max);

                    // Paint red background
                    g.setColor(redBarBackground);
                    g.fillRect(1, 1, barRect.width - 1, barRect.height - redHeight);

                    if (value > threshold) {
                        // Over threshold, paint red bar
                        g.setColor(thresholdColor);
                        g.fillRect(1, barRect.height - greenHeight,
                                   barRect.width - 1, greenHeight - redHeight);
                        greenHeight = redHeight;
                    }
                }

                // Paint green bar
                g.setColor(greenBar);
                g.fillRect(1, barRect.height - greenHeight,
                           barRect.width - 1, greenHeight);

                g.translate(-barRect.x, -barRect.y);
            }
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (poolPlotters.size() == 0) {
                return;
            }

            if (smallFont == null) {
                smallFont = g.getFont().deriveFont(9.0F);
            }

            // Paint background for chart area
            g.setColor(getBackground());
            Rectangle r = g.getClipBounds();
            g.fillRect(r.x, r.y, r.width, r.height);

            g.setFont(smallFont);
            FontMetrics fm = g.getFontMetrics();
            int fontDescent = fm.getDescent();

            // Paint percentage axis
            g.setColor(getForeground());
            for (int pc : new int[] { 0, 25, 50, 75, 100 }) {
                String str = pc + "% --";
                g.drawString(str,
                             leftMargin - fm.stringWidth(str) - 4,
                             height - bottomMargin - (pc * barHeight / 100) + fontDescent + 1);
            }

            for (PoolPlotter poolPlotter : poolPlotters) {
                paintPoolBar(g, poolPlotter);
            }

            g.setColor(Color.gray);
            g.drawRect(heapRect.x,    heapRect.y,    heapRect.width,    heapRect.height);
            g.drawRect(nonHeapRect.x, nonHeapRect.y, nonHeapRect.width, nonHeapRect.height);

            Color heapColor    = greenBar;
            Color nonHeapColor = greenBar;


            for (PoolPlotter poolPlotter : poolPlotters) {
                if (poolPlotter.threshold > 0L && poolPlotter.value > poolPlotter.threshold) {
                    if (poolPlotter.isHeap) {
                        heapColor = thresholdColor;
                    } else {
                        nonHeapColor = thresholdColor;
                    }
                }
            }
            g.setColor(heapColor);
            g.fillRect(heapRect.x + 1,    heapRect.y + 1,    heapRect.width - 1,    heapRect.height - 1);
            g.setColor(nonHeapColor);
            g.fillRect(nonHeapRect.x + 1, nonHeapRect.y + 1, nonHeapRect.width - 1, nonHeapRect.height - 1);

            String str = Messages.HEAP;
            int stringWidth = fm.stringWidth(str);
            int x = heapRect.x + (heapRect.width - stringWidth) / 2;
            int y = heapRect.y + heapRect.height - 6;
            g.setColor(Color.white);
            g.drawString(str, x-1, y-1);
            g.drawString(str, x+1, y-1);
            g.drawString(str, x-1, y+1);
            g.drawString(str, x+1, y+1);
            g.setColor(Color.black);
            g.drawString(str, x, y);

            str = Messages.NON_HEAP;
            stringWidth = fm.stringWidth(str);
            x = nonHeapRect.x + (nonHeapRect.width - stringWidth) / 2;
            y = nonHeapRect.y + nonHeapRect.height - 6;
            g.setColor(Color.white);
            g.drawString(str, x-1, y-1);
            g.drawString(str, x+1, y-1);
            g.drawString(str, x-1, y+1);
            g.drawString(str, x+1, y+1);
            g.setColor(Color.black);
            g.drawString(str, x, y);

            // Highlight current plotter
            g.setColor(Color.blue);
            r = null;
            Plotter plotter = (Plotter)plotterChoice.getSelectedItem();
            if (plotter == heapPlotter) {
                r = heapRect;
            } else if (plotter == nonHeapPlotter) {
                r = nonHeapRect;
            } else if (plotter instanceof PoolPlotter) {
                r = getBarRect((PoolPlotter)plotter);
            }
            if (r != null) {
                g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
            }
        }

        private Rectangle getBarRect(PoolPlotter poolPlotter) {
            return new Rectangle(leftMargin + poolPlotter.barX,
                                 height - bottomMargin - barHeight,
                                 barWidth, barHeight);
        }

        public Dimension getPreferredSize() {
            return new Dimension(nonHeapRect.x + nonHeapRect.width + rightMargin,
                                 height);
        }

        public void mouseClicked(MouseEvent e) {
            requestFocusInWindow();
            Plotter plotter = getPlotter(e);

            if (plotter != null && plotter != plotterChoice.getSelectedItem()) {
                plotterChoice.setSelectedItem(plotter);
                repaint();
            }
        }

        public String getToolTipText(MouseEvent e) {
            Plotter plotter = getPlotter(e);

            return (plotter != null) ? plotter.toString() : null;
        }

        private Plotter getPlotter(MouseEvent e) {
            Point p = e.getPoint();
            Plotter plotter = null;

            if (heapRect.contains(p)) {
                plotter = heapPlotter;
            } else if (nonHeapRect.contains(p)) {
                plotter = nonHeapPlotter;
            } else {
                for (PoolPlotter poolPlotter : poolPlotters) {
                    if (getBarRect(poolPlotter).contains(p)) {
                        plotter = poolPlotter;
                        break;
                    }
                }
            }
            return plotter;
        }

        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}


        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessiblePoolChart();
            }
            return accessibleContext;
        }

        protected class AccessiblePoolChart extends AccessibleJPanel {
            public String getAccessibleName() {
                String name = Messages.MEMORY_TAB_POOL_CHART_ACCESSIBLE_NAME;

                String keyValueList = "";
                for (PoolPlotter poolPlotter : poolPlotters) {
                    String value = (poolPlotter.value * 100 / poolPlotter.max) + "%";
                    // Assume format string ends with newline
                    keyValueList +=
                        Resources.format(Messages.PLOTTER_ACCESSIBLE_NAME_KEY_AND_VALUE,
                                         poolPlotter.toString(), value);
                    if (poolPlotter.threshold > 0L) {
                        String threshold =
                            (poolPlotter.threshold * 100 / poolPlotter.max) + "%";
                        if (poolPlotter.value > poolPlotter.threshold) {
                            keyValueList +=
                               Resources.format(Messages.MEMORY_TAB_POOL_CHART_ABOVE_THRESHOLD,
                                                threshold);
                        } else {
                            keyValueList +=
                                    Resources.format(Messages.MEMORY_TAB_POOL_CHART_BELOW_THRESHOLD,
                                                     threshold);
                        }
                    }
                }

                return name + "\n" + keyValueList + ".";
            }
        }
    }


    OverviewPanel[] getOverviewPanels() {
        if (overviewPanel == null) {
            overviewPanel = new MemoryOverviewPanel();
        }
        return new OverviewPanel[] { overviewPanel };
    }

    private static class MemoryOverviewPanel extends OverviewPanel {
        MemoryOverviewPanel() {
            super(Messages.HEAP_MEMORY_USAGE, usedKey, Messages.USED, Plotter.Unit.BYTES);
        }

        private void updateMemoryInfo(long used, long committed, long max) {
            getInfoLabel().setText(Resources.format(Messages.MEMORY_TAB_INFO_LABEL_FORMAT,
                                                    formatBytes(used, true),
                                                    formatBytes(committed, true),
                                                    formatBytes(max, true)));
        }
    }
}
