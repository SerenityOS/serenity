/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.*;

import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;

import sun.jvm.hotspot.ui.action.*;

import com.sun.java.swing.ui.*;
import com.sun.java.swing.action.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/**
 * This panel contains a JTable which displays the list of Java
 * threads as their native thread identifiers combined with their
 * Java names. It allows selection and examination of any of the
 * threads.
 */
public class JavaThreadsPanel extends SAPanel implements ActionListener {
    private JavaThreadsTableModel dataModel;
    private StatusBar statusBar;
    private JTable     threadTable;
    private java.util.List<CachedThread> cachedThreads = new ArrayList<>();
    private static AddressField crashThread;


    static {
        VM.registerVMInitializedObserver(
                            (o, a) -> initialize(VM.getVM().getTypeDataBase()));
    }

    private static void initialize(TypeDataBase db) {
        crashThread = db.lookupType("VMError").getAddressField("_thread");
    }

    /** Constructor assumes the threads panel is created while the VM is
        suspended. Subsequent resume and suspend operations of the VM
        will cause the threads panel to clear and fill itself back in,
        respectively. */
    public JavaThreadsPanel() {
        VM.getVM().registerVMResumedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    decache();
                }
            });

        VM.getVM().registerVMSuspendedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    cache();
                }
            });

        cache();

        setLayout(new BorderLayout());

        dataModel = new JavaThreadsTableModel(cachedThreads);
        statusBar = new StatusBar();

        threadTable = new JTable(dataModel, new JavaThreadsColumnModel());
        threadTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        // double clicking will display the oop inspector.
                        fireShowThreadOopInspector();
                    }
                }
            });

        add(new JavaThreadsToolBar(statusBar), BorderLayout.NORTH);
        add(new ThreadPanel(threadTable), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        registerActions();
    }

    /**
     * A splitpane panel which contains the thread table and the Thread Info.
     * the thread info is toggleable
     */
    private class ThreadPanel extends JPanel {

        private JSplitPane splitPane;
        private JTable threadTable;
        private ThreadInfoPanel threadInfo;
        private int dividerSize;
        private int dividerLocation = -1;
        private boolean actionsEnabled = false;

        public ThreadPanel(JTable table) {
            setLayout(new BorderLayout());
            this.threadInfo = new ThreadInfoPanel();
            this.threadTable = table;

            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setOneTouchExpandable(true);
            splitPane.setTopComponent(new JScrollPane(table));

            // Set the size of the divider to 0 but save it so it can be restored
            dividerSize = splitPane.getDividerSize();
            splitPane.setDividerSize(0);

            add(splitPane, BorderLayout.CENTER);

            // Register an ItemListener on the LogViewerAction which toggles
            // the apearance of the ThreadInfoPanel
            ActionManager manager = HSDBActionManager.getInstance();
            StateChangeAction action = manager.getStateChangeAction(ThreadInfoAction.VALUE_COMMAND);
            if (action != null) {
                action.setItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent evt) {
                            if (evt.getStateChange() == ItemEvent.SELECTED) {
                                showOutputPane();
                            } else {
                                hideOutputPane();
                            }
                        }
                    });
            }

            // A listener is added to listen to changes in row selection
            // and changes the contents of the ThreadInfoPanel.
            ListSelectionModel selModel = table.getSelectionModel();
            selModel.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent evt) {
                        if (evt.getValueIsAdjusting() == false) {
                            setActionsEnabled(true);
                            if (isInfoVisible()) {
                                showCurrentThreadInfo();
                            }
                        }
                    }
                });
        }

        /**
         * Returns a flag to indicate if the thread info is visible
         */
        private boolean isInfoVisible() {
            return (splitPane.getBottomComponent() != null);
        }

        private void showOutputPane()  {
            if (splitPane.getBottomComponent() == null)  {
                splitPane.setBottomComponent(threadInfo);

                if (dividerLocation == -1)  {
                    // Calculate the divider location from the pref size.
                    Dimension pSize = this.getSize();
                    dividerLocation = pSize.height / 2;
                }

                splitPane.setDividerSize(dividerSize);
                splitPane.setDividerLocation(dividerLocation);
                showCurrentThreadInfo();
            }
        }

        private void hideOutputPane()  {
            dividerLocation = splitPane.getDividerLocation();
            splitPane.remove(threadInfo);
            splitPane.setDividerSize(0);
        }

        private void showCurrentThreadInfo() {
            int row = threadTable.getSelectedRow();
            if (row >= 0) {
                threadInfo.setJavaThread(dataModel.getJavaThread(row));
            }
        }

        private void setActionsEnabled(boolean enabled) {
            if (actionsEnabled != enabled) {
                ActionManager manager = ActionManager.getInstance();
                manager.setActionEnabled(InspectAction.VALUE_COMMAND, enabled);
                manager.setActionEnabled(MemoryAction.VALUE_COMMAND, enabled);
                manager.setActionEnabled(JavaStackTraceAction.VALUE_COMMAND, enabled);
                actionsEnabled = enabled;
            }
        }

    } // end ThreadPanel

    private class JavaThreadsToolBar extends CommonToolBar {
        public JavaThreadsToolBar(StatusBar status) {
            super(HSDBActionManager.getInstance(), status);
        }

        protected void addComponents() {
            addButton(manager.getAction(InspectAction.VALUE_COMMAND));
            addButton(manager.getAction(MemoryAction.VALUE_COMMAND));
            addButton(manager.getAction(JavaStackTraceAction.VALUE_COMMAND));

            addToggleButton(manager.getStateChangeAction(ThreadInfoAction.VALUE_COMMAND));
            addButton(manager.getAction(FindCrashesAction.VALUE_COMMAND));
        }
    }

    private class JavaThreadsColumnModel extends DefaultTableColumnModel {
        private String[] columnNames = { "OS Thread ID", "Java Thread Name" };

        public JavaThreadsColumnModel() {
            // Should actually get the line metrics for
            int PREF_WIDTH = 80;
            int MAX_WIDTH = 100;
            int HUGE_WIDTH = 140;

            TableColumn column;

            // Thread ID
            column = new TableColumn(0, MAX_WIDTH);
            column.setHeaderValue(columnNames[0]);
            column.setMaxWidth(MAX_WIDTH);
            column.setResizable(false);
            addColumn(column);

            // Thread name
            column = new TableColumn(1, HUGE_WIDTH);
            column.setHeaderValue(columnNames[1]);
            column.setResizable(false);
            addColumn(column);
        }
    } // end class JavaThreadsColumnModel

    /**
     * Encapsulates the set of threads in a table model
     */
    private class JavaThreadsTableModel extends AbstractTableModel {
        private String[] columnNames = { "OS Thread ID", "Java Thread Name" };

        private java.util.List<CachedThread> elements;

        public JavaThreadsTableModel(java.util.List<CachedThread> threads) {
            this.elements = threads;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return elements.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            CachedThread thread = getRow(row);
            switch (col) {
            case 0:
                return thread.getThreadID();
            case 1:
                return thread.getThreadName();
            default:
                throw new RuntimeException("Index (" + col + ", " + row + ") out of bounds");
            }
        }

        /**
         * Returns the selected Java Thread indexed by the row or null.
         */
        public JavaThread getJavaThread(int index) {
            return getRow(index).getThread();
        }

        private CachedThread getRow(int row) {
            return elements.get(row);
        }

        private String threadIDAt(int index) {
            return cachedThreads.get(index).getThreadID();
        }

        private String threadNameAt(int index) {
            try {
                return cachedThreads.get(index).getThreadName();
            } catch (AddressException e) {
                return "<Error: AddressException>";
            } catch (NullPointerException e) {
                return "<Error: NullPointerException>";
            }
        }
    } // end class JavaThreadsTableModel

    public void actionPerformed(ActionEvent evt) {
        String command = evt.getActionCommand();

        if (command.equals(InspectAction.VALUE_COMMAND)) {
            fireShowThreadOopInspector();
        } else if (command.equals(MemoryAction.VALUE_COMMAND)) {
            fireShowThreadStackMemory();
        } else if (command.equals(ThreadInfoAction.VALUE_COMMAND)) {
            fireShowThreadInfo();
        } else if (command.equals(FindCrashesAction.VALUE_COMMAND)) {
            if (fireShowThreadCrashes()) {
                statusBar.setMessage("Some thread crashes were encountered");
            } else {
                statusBar.setMessage("No thread crashes encountered");
            }
        } else if (command.equals(JavaStackTraceAction.VALUE_COMMAND)) {
           fireShowJavaStackTrace();
        }
    }

    // Cached data for a thread
    private class CachedThread {
        private JavaThread thread;
        private String     threadID;
        private String     threadName;
        private boolean    computed;

        public CachedThread(JavaThread thread) {
            this.thread = thread;
        }

        public JavaThread getThread() {
            return thread;
        }

        public String getThreadID() {
            if (!computed) {
                compute();
            }

            return threadID;
        }

        public String getThreadName() {
            if (!computed) {
                compute();
            }

            return threadName;
        }

        private void compute() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            thread.printThreadIDOn(new PrintStream(bos));
            threadID   = bos.toString();
            threadName = thread.getThreadName();

            computed = true;
        }
    }

    //--------------------------------------------------------------------------------
    // Internals only below this point
    //

    protected void registerActions() {
        registerAction(InspectAction.VALUE_COMMAND);
        registerAction(MemoryAction.VALUE_COMMAND);
        registerAction(FindCrashesAction.VALUE_COMMAND);
        registerAction(JavaStackTraceAction.VALUE_COMMAND);

        // disable Inspector,  Memory and Java Stack trace action until a thread is selected
        ActionManager manager = ActionManager.getInstance();
        manager.setActionEnabled(InspectAction.VALUE_COMMAND, false);
        manager.setActionEnabled(MemoryAction.VALUE_COMMAND, false);
        manager.setActionEnabled(JavaStackTraceAction.VALUE_COMMAND, false);
    }

    private void registerAction(String actionName) {
        ActionManager manager = ActionManager.getInstance();
        DelegateAction action = manager.getDelegateAction(actionName);
        action.addActionListener(this);
    }



    private void fireShowThreadOopInspector() {
        int i = threadTable.getSelectedRow();
        if (i < 0) {
            return;
        }

        JavaThread t = dataModel.getJavaThread(i);
        showThreadOopInspector(t);
    }

    private void fireShowThreadStackMemory() {
        int i = threadTable.getSelectedRow();
        if (i < 0) {
            return;
        }
        showThreadStackMemory(dataModel.getJavaThread(i));
    }

    private void fireShowJavaStackTrace() {
        int i = threadTable.getSelectedRow();
        if (i < 0) {
            return;
        }
        showJavaStackTrace(dataModel.getJavaThread(i));
    }

    private void fireShowThreadInfo() {
        int i = threadTable.getSelectedRow();
        if (i < 0) {
            return;
        }
        showThreadInfo(dataModel.getJavaThread(i));
    }

    /**
     * Shows stack memory for threads which have crashed (defined as
     * having taken a signal above a Java frame)
     *
     * @return a flag which indicates if crashes were encountered.
     */
    private boolean fireShowThreadCrashes() {
        Optional<JavaThread> crashed =
                         cachedThreads.stream()
                                      .map(t -> t.getThread())
                                      .filter(t -> t.getAddress().equals(
                                                        crashThread.getValue()))
                                      .findAny();
        crashed.ifPresent(this::showThreadStackMemory);
        return crashed.isPresent();
    }

    private void cache() {
      Threads threads = VM.getVM().getThreads();
      for (int i = 0; i < threads.getNumberOfThreads(); i++) {
        JavaThread t = threads.getJavaThreadAt(i);
        if (t.isJavaThread()) {
            cachedThreads.add(new CachedThread(t));
        }
      }
    }

    private void decache() {
        cachedThreads.clear();
    }

}
