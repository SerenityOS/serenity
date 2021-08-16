/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8153732 8212202 8221263 8221412 8222108 8263311
 * @requires (os.family == "Windows")
 * @summary Windows remote printer changes do not reflect in lookupPrintServices()
 * @run main/manual RemotePrinterStatusRefresh
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import static javax.swing.BorderFactory.createTitledBorder;

public class RemotePrinterStatusRefresh extends WindowAdapter {

    private static final long TIMEOUT = 15L * 60;


    private static final CountDownLatch latch = new CountDownLatch(1);
    private static volatile RemotePrinterStatusRefresh test;

    private volatile boolean testResult;
    private volatile boolean testTimedOut;

    private final JFrame frame;

    private JButton refreshButton;
    private JButton passButton;
    private JButton failButton;

    private final ServiceItemListModel beforeList;
    private final ServiceItemListModel afterList;

    private JTextField timeLeft;

    private final Timer timer;
    private final long startTime;


    private static class ServiceItem {
        private enum State {
            REMOVED, UNCHANGED, ADDED
        }

        final String name;
        State state;

        private ServiceItem(final String name) {
            this.name = name;
            state = State.UNCHANGED;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof ServiceItem)
                    && ((ServiceItem) obj).name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private static class ServiceItemListModel extends AbstractListModel<ServiceItem> {
        private final List<ServiceItem> list;

        private ServiceItemListModel(List<ServiceItem> list) {
            this.list = list;
        }

        @Override
        public int getSize() {
            return list.size();
        }

        @Override
        public ServiceItem getElementAt(int index) {
            return list.get(index);
        }

        private void refreshList(List<ServiceItem> newList) {
            list.clear();
            list.addAll(newList);
            fireChanged();
        }

        private void fireChanged() {
            fireContentsChanged(this, 0, list.size() - 1);
        }
    }

    private static class ServiceItemListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Component component =
                    super.getListCellRendererComponent(list, value, index,
                                                       isSelected, cellHasFocus);
            switch (((ServiceItem) value).state) {
                case REMOVED:
                    component.setBackground(Color.RED);
                    component.setForeground(Color.WHITE);
                    break;
                case ADDED:
                    component.setBackground(Color.GREEN);
                    component.setForeground(Color.BLACK);
                    break;
                case UNCHANGED:
                default:
                    break;
            }
            return component;
        }
    }

    private static final String INSTRUCTIONS_TEXT =
            "Please follow the steps for this manual test:\n"
                    + "Step 0: \"Before\" list is populated with currently "
                    +          "configured printers.\n"
                    + "Step 1: Add or Remove a network printer using "
                    +          "Windows Control Panel.\n"
                    + "Step 2: Click Refresh."
                    +          "\"After\" list is populated with updated list "
                    +          "of printers.\n"
                    + "Step 3: Compare the list of printers in \"Before\" and "
                    +          "\"After\" lists.\n"
                    + "              Added printers are highlighted with "
                    +               "green color, removed ones \u2014 with "
                    +               "red color.\n"
                    + "Step 4: Click Pass if the list of printers is correctly "
                    +          "updated.\n"
                    + "Step 5: If the list is not updated, click Refresh again.\n"
                    + "Step 6: If the list does not update, click Fail.\n"
                    + "\n"
                    + "You have to click Refresh to enable Pass and Fail buttons. "
                    + "If no button is pressed,\n"
                    + "the test will time out. "
                    + "Closing the window also fails the test.";

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(RemotePrinterStatusRefresh::createUI);

        latch.await();
        if (!test.testResult) {
            throw new RuntimeException("Test failed"
                + (test.testTimedOut ? " because of time out" : ""));
        }
    }

    private static void createUI() {
        test = new RemotePrinterStatusRefresh();
    }

    private RemotePrinterStatusRefresh() {
        frame = new JFrame("RemotePrinterStatusRefresh");
        frame.addWindowListener(this);


        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(createInfoPanel(), BorderLayout.NORTH);
        northPanel.add(createInstructionsPanel(), BorderLayout.SOUTH);


        beforeList = new ServiceItemListModel(
                Collections.unmodifiableList(collectPrinterList()));
        afterList = new ServiceItemListModel(new ArrayList<>());
        logList("Before:", beforeList.list);

        JPanel listPanel = new JPanel(new GridLayout(1, 2));
        listPanel.setBorder(createTitledBorder("Print Services"));
        listPanel.add(createListPanel(beforeList, "Before:", 'b'));
        listPanel.add(createListPanel(afterList, "After:", 'a'));


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(listPanel, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);


        frame.add(mainPanel);
        frame.pack();
        refreshButton.requestFocusInWindow();
        frame.setVisible(true);


        timer = new Timer(1000, this::updateTimeLeft);
        timer.start();
        startTime = System.currentTimeMillis();
        updateTimeLeft(null);
    }

    private JPanel createInfoPanel() {
        JLabel javaLabel = new JLabel("Java version:");
        JTextField javaVersion =
                new JTextField(System.getProperty("java.runtime.version"));
        javaVersion.setEditable(false);
        javaLabel.setLabelFor(javaVersion);

        JLabel timeoutLabel = new JLabel("Time left:");
        timeLeft = new JTextField();
        timeLeft.setEditable(false);
        timeoutLabel.setLabelFor(timeLeft);

        JPanel infoPanel = new JPanel();
        GroupLayout layout = new GroupLayout(infoPanel);
        infoPanel.setLayout(layout);
        infoPanel.setBorder(BorderFactory.createTitledBorder("Info"));
        layout.setAutoCreateGaps(true);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(javaLabel)
                    .addComponent(timeoutLabel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(javaVersion)
                    .addComponent(timeLeft)
                )
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(javaLabel)
                    .addComponent(javaVersion)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(timeoutLabel)
                    .addComponent(timeLeft))
        );
        return infoPanel;
    }

    private JPanel createInstructionsPanel() {
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        JTextArea instructionText = new JTextArea(INSTRUCTIONS_TEXT);
        instructionText.setEditable(false);
        instructionsPanel.setBorder(createTitledBorder("Test Instructions"));
        instructionsPanel.add(new JScrollPane(instructionText));
        return  instructionsPanel;
    }

    private JPanel createListPanel(final ServiceItemListModel model,
                                   final String title,
                                   final char mnemonic) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JList<ServiceItem> list = new JList<>(model);
        list.setCellRenderer(new ServiceItemListRenderer());

        JLabel label = new JLabel(title);
        label.setLabelFor(list);
        label.setDisplayedMnemonic(mnemonic);
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.add(label, BorderLayout.EAST);
        labelPanel.add(Box.createHorizontalGlue());

        panel.add(labelPanel);
        panel.add(new JScrollPane(list));
        return panel;
    }

    private JPanel createButtonPanel() {
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this::refresh);

        passButton = new JButton("Pass");
        passButton.addActionListener(this::pass);
        passButton.setEnabled(false);

        failButton = new JButton("Fail");
        failButton.addActionListener(this::fail);
        failButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(refreshButton);
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        buttonPanel.add(Box.createHorizontalGlue());
        return buttonPanel;
    }

    private static List<ServiceItem> collectPrinterList() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        List<ServiceItem> list = new ArrayList<>(printServices.length);
        for (PrintService service : printServices) {
            list.add(new ServiceItem(service.getName()));
        }
        return list;
    }

    private static void logList(final String title, final List<ServiceItem> list) {
        System.out.println(title);
        for (ServiceItem item : list) {
            System.out.println(item.name);
        }
        System.out.println();
    }

    private static void compareLists(final ServiceItemListModel before, final ServiceItemListModel after) {
        boolean beforeUpdated = false;
        boolean afterUpdated = false;

        for (ServiceItem item : before.list) {
            if (!after.list.contains(item)) {
                item.state = ServiceItem.State.REMOVED;
                beforeUpdated = true;
            } else if (item.state != ServiceItem.State.UNCHANGED) {
                item.state = ServiceItem.State.UNCHANGED;
                beforeUpdated = true;
            }
        }

        for (ServiceItem item : after.list) {
            if (!before.list.contains(item)) {
                item.state = ServiceItem.State.ADDED;
                afterUpdated = true;
            } else if (item.state != ServiceItem.State.UNCHANGED) {
                item.state = ServiceItem.State.UNCHANGED;
                afterUpdated = true;
            }
        }

        if (beforeUpdated) {
            before.fireChanged();
        }
        if (afterUpdated) {
            after.fireChanged();
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println("The window closed");
        disposeUI();
    }

    private void disposeUI() {
        timer.stop();
        latch.countDown();
        frame.dispose();
    }

    @SuppressWarnings("unused")
    private void refresh(ActionEvent e) {
        System.out.println("Refresh button pressed");
        afterList.refreshList(collectPrinterList());
        compareLists(beforeList, afterList);
        passButton.setEnabled(true);
        failButton.setEnabled(true);
        logList("After:", afterList.list);
    }

    @SuppressWarnings("unused")
    private void pass(ActionEvent e) {
        System.out.println("Pass button pressed");
        testResult = true;
        disposeUI();
    }

    @SuppressWarnings("unused")
    private void fail(ActionEvent e) {
        System.out.println("Fail button pressed");
        testResult = false;
        disposeUI();
    }

    @SuppressWarnings("unused")
    private void updateTimeLeft(ActionEvent e) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long left = TIMEOUT - elapsed;
        if (left < 0) {
            testTimedOut = true;
            disposeUI();
        }
        timeLeft.setText(formatTime(left));
    }

    private static String formatTime(final long seconds) {
        long minutes = seconds / 60;
        return String.format("%d:%02d", minutes, seconds - minutes * 60);
    }

}
