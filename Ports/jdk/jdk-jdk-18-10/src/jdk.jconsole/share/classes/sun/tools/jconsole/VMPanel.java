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
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import javax.swing.*;
import javax.swing.plaf.*;


import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.jconsole.JConsoleContext;

import static sun.tools.jconsole.ProxyClient.*;

@SuppressWarnings("serial")
public class VMPanel extends JTabbedPane implements PropertyChangeListener {

    private ProxyClient proxyClient;
    private Timer timer;
    private int updateInterval;
    private String hostName;
    private int port;
    private String userName;
    private String password;
    private String url;
    private VMInternalFrame vmIF = null;
    private static ArrayList<TabInfo> tabInfos = new ArrayList<TabInfo>();
    private boolean wasConnected = false;
    private boolean userDisconnected = false;
    private boolean shouldUseSSL = true;

    // The everConnected flag keeps track of whether the window can be
    // closed if the user clicks Cancel after a failed connection attempt.
    //
    private boolean everConnected = false;

    // The initialUpdate flag is used to enable/disable tabs each time
    // a connect or reconnect takes place. This flag avoids having to
    // enable/disable tabs on each update call.
    //
    private boolean initialUpdate = true;

    // Each VMPanel has its own instance of the JConsolePlugin
    // A map of JConsolePlugin to the previous SwingWorker
    private Map<ExceptionSafePlugin, SwingWorker<?, ?>> plugins = null;
    private boolean pluginTabsAdded = false;

    // Update these only on the EDT
    private JOptionPane optionPane;
    private JProgressBar progressBar;
    private long time0;

    static {
        tabInfos.add(new TabInfo(OverviewTab.class, OverviewTab.getTabName(), true));
        tabInfos.add(new TabInfo(MemoryTab.class, MemoryTab.getTabName(), true));
        tabInfos.add(new TabInfo(ThreadTab.class, ThreadTab.getTabName(), true));
        tabInfos.add(new TabInfo(ClassTab.class, ClassTab.getTabName(), true));
        tabInfos.add(new TabInfo(SummaryTab.class, SummaryTab.getTabName(), true));
        tabInfos.add(new TabInfo(MBeansTab.class, MBeansTab.getTabName(), true));
    }

    public static TabInfo[] getTabInfos() {
        return tabInfos.toArray(new TabInfo[tabInfos.size()]);
    }

    VMPanel(ProxyClient proxyClient, int updateInterval) {
        this.proxyClient = proxyClient;
        this.updateInterval = updateInterval;
        this.hostName = proxyClient.getHostName();
        this.port = proxyClient.getPort();
        this.userName = proxyClient.getUserName();
        this.password = proxyClient.getPassword();
        this.url = proxyClient.getUrl();

        for (TabInfo tabInfo : tabInfos) {
            if (tabInfo.tabVisible) {
                addTab(tabInfo);
            }
        }

        plugins = new LinkedHashMap<ExceptionSafePlugin, SwingWorker<?, ?>>();
        for (JConsolePlugin p : JConsole.getPlugins()) {
            p.setContext(proxyClient);
            plugins.put(new ExceptionSafePlugin(p), null);
        }

        Utilities.updateTransparency(this);

        ToolTipManager.sharedInstance().registerComponent(this);

        // Start listening to connection state events
        //
        proxyClient.addPropertyChangeListener(this);

        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (connectedIconBounds != null
                        && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0
                        && connectedIconBounds.contains(e.getPoint())) {

                    if (isConnected()) {
                        userDisconnected = true;
                        disconnect();
                        wasConnected = false;
                    } else {
                        connect();
                    }
                    repaint();
                }
            }
        });

    }
    private static Icon connectedIcon16 =
            new ImageIcon(VMPanel.class.getResource("resources/connected16.png"));
    private static Icon connectedIcon24 =
            new ImageIcon(VMPanel.class.getResource("resources/connected24.png"));
    private static Icon disconnectedIcon16 =
            new ImageIcon(VMPanel.class.getResource("resources/disconnected16.png"));
    private static Icon disconnectedIcon24 =
            new ImageIcon(VMPanel.class.getResource("resources/disconnected24.png"));
    private Rectangle connectedIconBounds;

    // Override to increase right inset for tab area,
    // in order to reserve space for the connect toggle.
    public void setUI(TabbedPaneUI ui) {
        Insets insets = (Insets) UIManager.getLookAndFeelDefaults().get("TabbedPane.tabAreaInsets");
        if (insets != null) {
            insets = (Insets) insets.clone();
            insets.right += connectedIcon24.getIconWidth() + 8;
            UIManager.put("TabbedPane.tabAreaInsets", insets);
        }
        super.setUI(ui);
    }

    // Override to paint the connect toggle
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Icon icon;
        Component c0 = getComponent(0);
        if (c0 != null && c0.getY() > 24) {
            icon = isConnected() ? connectedIcon24 : disconnectedIcon24;
        } else {
            icon = isConnected() ? connectedIcon16 : disconnectedIcon16;
        }
        Insets insets = getInsets();
        int x = getWidth() - insets.right - icon.getIconWidth() - 4;
        int y = insets.top;
        if (c0 != null) {
            y = (c0.getY() - icon.getIconHeight()) / 2;
        }
        icon.paintIcon(this, g, x, y);
        connectedIconBounds = new Rectangle(x, y, icon.getIconWidth(), icon.getIconHeight());
    }

    public String getToolTipText(MouseEvent event) {
        if (connectedIconBounds.contains(event.getPoint())) {
            if (isConnected()) {
                return Messages.CONNECTED_PUNCTUATION_CLICK_TO_DISCONNECT_;
            } else {
                return Messages.DISCONNECTED_PUNCTUATION_CLICK_TO_CONNECT_;
            }
        } else {
            return super.getToolTipText(event);
        }
    }

    private synchronized void addTab(TabInfo tabInfo) {
        Tab tab = instantiate(tabInfo);
        if (tab != null) {
            addTab(tabInfo.name, tab);
        } else {
            tabInfo.tabVisible = false;
        }
    }

    private synchronized void insertTab(TabInfo tabInfo, int index) {
        Tab tab = instantiate(tabInfo);
        if (tab != null) {
            insertTab(tabInfo.name, null, tab, null, index);
        } else {
            tabInfo.tabVisible = false;
        }
    }

    public synchronized void removeTabAt(int index) {
        super.removeTabAt(index);
    }

    private Tab instantiate(TabInfo tabInfo) {
        try {
            Constructor<?> con = tabInfo.tabClass.getConstructor(VMPanel.class);
            return (Tab) con.newInstance(this);
        } catch (Exception ex) {
            System.err.println(ex);
            return null;
        }
    }

    boolean isConnected() {
        return proxyClient.isConnected();
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * WARNING NEVER CALL THIS METHOD TO MAKE JMX REQUEST
     * IF  assertThread == false.
     * DISPATCHER THREAD IS NOT ASSERTED.
     * IT IS USED TO MAKE SOME LOCAL MANIPULATIONS.
     */
    ProxyClient getProxyClient(boolean assertThread) {
        if (assertThread) {
            return getProxyClient();
        } else {
            return proxyClient;
        }
    }

    public ProxyClient getProxyClient() {
        String threadClass = Thread.currentThread().getClass().getName();
        if (threadClass.equals("java.awt.EventDispatchThread")) {
            String msg = "Calling VMPanel.getProxyClient() from the Event Dispatch Thread!";
            new RuntimeException(msg).printStackTrace();
            System.exit(1);
        }
        return proxyClient;
    }

    public void cleanUp() {
        //proxyClient.disconnect();
        for (Tab tab : getTabs()) {
            tab.dispose();
        }
        for (JConsolePlugin p : plugins.keySet()) {
            p.dispose();
        }
        // Cancel pending update tasks
        //
        if (timer != null) {
            timer.cancel();
        }
        // Stop listening to connection state events
        //
        proxyClient.removePropertyChangeListener(this);
    }

    // Call on EDT
    public void connect() {
        if (isConnected()) {
            // create plugin tabs if not done
            createPluginTabs();
            // Notify tabs
            fireConnectedChange(true);
            // Enable/disable tabs on initial update
            initialUpdate = true;
            // Start/Restart update timer on connect/reconnect
            startUpdateTimer();
        } else {
            new Thread("VMPanel.connect") {

                public void run() {
                    proxyClient.connect(shouldUseSSL);
                }
            }.start();
        }
    }

    // Call on EDT
    public void disconnect() {
        proxyClient.disconnect();
        updateFrameTitle();
    }

    // Called on EDT
    public void propertyChange(PropertyChangeEvent ev) {
        String prop = ev.getPropertyName();

        if (prop == CONNECTION_STATE_PROPERTY) {
            ConnectionState oldState = (ConnectionState) ev.getOldValue();
            ConnectionState newState = (ConnectionState) ev.getNewValue();
            switch (newState) {
                case CONNECTING:
                    onConnecting();
                    break;

                case CONNECTED:
                    if (progressBar != null) {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(100);
                    }
                    closeOptionPane();
                    updateFrameTitle();
                    // create tabs if not done
                    createPluginTabs();
                    repaint();
                    // Notify tabs
                    fireConnectedChange(true);
                    // Enable/disable tabs on initial update
                    initialUpdate = true;
                    // Start/Restart update timer on connect/reconnect
                    startUpdateTimer();
                    break;

                case DISCONNECTED:
                    if (progressBar != null) {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(0);
                        closeOptionPane();
                    }
                    vmPanelDied();
                    if (oldState == ConnectionState.CONNECTED) {
                        // Notify tabs
                        fireConnectedChange(false);
                    }
                    break;
            }
        }
    }

    // Called on EDT
    private void onConnecting() {
        time0 = System.currentTimeMillis();

        SwingUtilities.getWindowAncestor(this);

        String connectionName = getConnectionName();
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.add(progressBar);

        Object[] message = {
            "<html><h3>" + Resources.format(Messages.CONNECTING_TO1, connectionName) + "</h3></html>",
            progressPanel,
            "<html><b>" + Resources.format(Messages.CONNECTING_TO2, connectionName) + "</b></html>"
        };

        optionPane =
                SheetDialog.showOptionDialog(this,
                message,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null,
                new String[]{Messages.CANCEL},
                0);


    }

    // Called on EDT
    private void closeOptionPane() {
        if (optionPane != null) {
            new Thread("VMPanel.sleeper") {
                public void run() {
                    long elapsed = System.currentTimeMillis() - time0;
                    if (elapsed < 2000) {
                        try {
                            sleep(2000 - elapsed);
                        } catch (InterruptedException ex) {
                        // Ignore
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            optionPane.setVisible(false);
                            progressBar = null;
                        }
                    });
                }
            }.start();
        }
    }

    void updateFrameTitle() {
        VMInternalFrame vmIF = getFrame();
        if (vmIF != null) {
            String displayName = getDisplayName();
            if (!proxyClient.isConnected()) {
                displayName = Resources.format(Messages.CONNECTION_NAME__DISCONNECTED_, displayName);
            }
            vmIF.setTitle(displayName);
        }
    }

    private VMInternalFrame getFrame() {
        if (vmIF == null) {
            vmIF = (VMInternalFrame) SwingUtilities.getAncestorOfClass(VMInternalFrame.class,
                    this);
        }
        return vmIF;
    }

    // TODO: this method is not needed when all JConsole tabs
    // are migrated to use the new JConsolePlugin API.
    //
    // A thread safe clone of all JConsole tabs
    synchronized List<Tab> getTabs() {
        ArrayList<Tab> list = new ArrayList<Tab>();
        int n = getTabCount();
        for (int i = 0; i < n; i++) {
            Component c = getComponentAt(i);
            if (c instanceof Tab) {
                list.add((Tab) c);
            }
        }
        return list;
    }

    private void startUpdateTimer() {
        if (timer != null) {
            timer.cancel();
        }
        TimerTask timerTask = new TimerTask() {

            public void run() {
                update();
            }
        };
        String timerName = "Timer-" + getConnectionName();
        timer = new Timer(timerName, true);
        timer.schedule(timerTask, 0, updateInterval);
    }

    // Call on EDT
    private void vmPanelDied() {
        disconnect();

        if (userDisconnected) {
            userDisconnected = false;
            return;
        }

        JOptionPane optionPane;
        String msgTitle, msgExplanation, buttonStr;

        if (wasConnected) {
            wasConnected = false;
            msgTitle = Messages.CONNECTION_LOST1;
            msgExplanation = Resources.format(Messages.CONNECTING_TO2, getConnectionName());
            buttonStr = Messages.RECONNECT;
        } else if (shouldUseSSL) {
            msgTitle = Messages.CONNECTION_FAILED_SSL1;
            msgExplanation = Resources.format(Messages.CONNECTION_FAILED_SSL2, getConnectionName());
            buttonStr = Messages.INSECURE;
        } else {
            msgTitle = Messages.CONNECTION_FAILED1;
            msgExplanation = Resources.format(Messages.CONNECTION_FAILED2, getConnectionName());
            buttonStr = Messages.CONNECT;
        }

        optionPane =
                SheetDialog.showOptionDialog(this,
                "<html><h3>" + msgTitle + "</h3>" +
                "<b>" + msgExplanation + "</b>",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null,
                new String[]{buttonStr, Messages.CANCEL},
                0);

        optionPane.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                    Object value = event.getNewValue();

                    if (value == Messages.RECONNECT || value == Messages.CONNECT) {
                        connect();
                    } else if (value == Messages.INSECURE) {
                        shouldUseSSL = false;
                        connect();
                    } else if (!everConnected) {
                        try {
                            getFrame().setClosed(true);
                        } catch (PropertyVetoException ex) {
                        // Should not happen, but can be ignored.
                        }
                    }
                }
            }
        });
    }

    // Note: This method is called on a TimerTask thread. Any GUI manipulation
    // must be performed with invokeLater() or invokeAndWait().
    private Object lockObject = new Object();

    private void update() {
        synchronized (lockObject) {
            if (!isConnected()) {
                if (wasConnected) {
                    EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            vmPanelDied();
                        }
                    });
                }
                wasConnected = false;
                return;
            } else {
                wasConnected = true;
                everConnected = true;
            }
            proxyClient.flush();
            List<Tab> tabs = getTabs();
            final int n = tabs.size();
            for (int i = 0; i < n; i++) {
                final int index = i;
                try {
                    if (!proxyClient.isDead()) {
                        // Update tab
                        //
                        tabs.get(index).update();
                        // Enable tab on initial update
                        //
                        if (initialUpdate) {
                            EventQueue.invokeLater(new Runnable() {

                                public void run() {
                                    setEnabledAt(index, true);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    // Disable tab on initial update
                    //
                    if (initialUpdate) {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                setEnabledAt(index, false);
                            }
                        });
                    }
                }
            }

            // plugin GUI update
            for (ExceptionSafePlugin p : plugins.keySet()) {
                SwingWorker<?, ?> sw = p.newSwingWorker();
                SwingWorker<?, ?> prevSW = plugins.get(p);
                // schedule SwingWorker to run only if the previous
                // SwingWorker has finished its task and it hasn't started.
                if (prevSW == null || prevSW.isDone()) {
                    if (sw == null || sw.getState() == SwingWorker.StateValue.PENDING) {
                        plugins.put(p, sw);
                        if (sw != null) {
                            p.executeSwingWorker(sw);
                        }
                    }
                }
            }

            // Set the first enabled tab in the tab's list
            // as the selected tab on initial update
            //
            if (initialUpdate) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        // Select first enabled tab if current tab isn't.
                        int index = getSelectedIndex();
                        if (index < 0 || !isEnabledAt(index)) {
                            for (int i = 0; i < n; i++) {
                                if (isEnabledAt(i)) {
                                    setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                    }
                });
                initialUpdate = false;
            }
        }
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getUrl() {
        return url;
    }

    public String getPassword() {
        return password;
    }

    public String getConnectionName() {
        return proxyClient.connectionName();
    }

    public String getDisplayName() {
        return proxyClient.getDisplayName();
    }

    static class TabInfo {

        Class<? extends Tab> tabClass;
        String name;
        boolean tabVisible;

        TabInfo(Class<? extends Tab> tabClass, String name, boolean tabVisible) {
            this.tabClass = tabClass;
            this.name = name;
            this.tabVisible = tabVisible;
        }
    }

    private void createPluginTabs() {
        // add plugin tabs if not done
        if (!pluginTabsAdded) {
            for (JConsolePlugin p : plugins.keySet()) {
                Map<String, JPanel> tabs = p.getTabs();
                for (Map.Entry<String, JPanel> e : tabs.entrySet()) {
                    addTab(e.getKey(), e.getValue());
                }
            }
            pluginTabsAdded = true;
        }
    }

    private void fireConnectedChange(boolean connected) {
        for (Tab tab : getTabs()) {
            tab.firePropertyChange(JConsoleContext.CONNECTION_STATE_PROPERTY, !connected, connected);
        }
    }
}
