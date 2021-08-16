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

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.table.*;



import static java.awt.BorderLayout.*;
import static javax.swing.ListSelectionModel.*;
import static sun.tools.jconsole.Utilities.*;

@SuppressWarnings("serial")
public class ConnectDialog extends InternalDialog
                implements DocumentListener, FocusListener,
                           ItemListener, ListSelectionListener, KeyListener {

    private static final int COL_NAME = 0;
    private static final int COL_PID  = 1;


    JConsole jConsole;
    JTextField userNameTF, passwordTF;
    JRadioButton localRadioButton, remoteRadioButton;
    JLabel localMessageLabel, remoteMessageLabel;
    JTextField remoteTF;
    JButton connectButton, cancelButton;
    JPanel radioButtonPanel;

    private Icon mastheadIcon =
        new MastheadIcon(Messages.CONNECT_DIALOG_MASTHEAD_TITLE);
    private Color hintTextColor, disabledTableCellColor;

    // The table of managed VM (local process)
    JTable vmTable;
    ManagedVmTableModel vmModel = null;

    JScrollPane localTableScrollPane = null;

    private Action connectAction, cancelAction;


    public ConnectDialog(JConsole jConsole) {
        super(jConsole, Messages.CONNECT_DIALOG_TITLE, true);

        this.jConsole = jConsole;
        setAccessibleDescription(this,
                                 Messages.CONNECT_DIALOG_ACCESSIBLE_DESCRIPTION);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        Container cp = (JComponent)getContentPane();

        radioButtonPanel = new JPanel(new BorderLayout(0, 12));
        radioButtonPanel.setBorder(new EmptyBorder(6, 12, 12, 12));
        ButtonGroup radioButtonGroup = new ButtonGroup();
        JPanel bottomPanel = new JPanel(new BorderLayout());

        statusBar = new JLabel(" ", JLabel.CENTER);
        setAccessibleName(statusBar,
                          Messages.CONNECT_DIALOG_STATUS_BAR_ACCESSIBLE_NAME);

        Font normalLabelFont = statusBar.getFont();
        Font boldLabelFont = normalLabelFont.deriveFont(Font.BOLD);
        Font smallLabelFont = normalLabelFont.deriveFont(normalLabelFont.getSize2D() - 1);

        JLabel mastheadLabel = new JLabel(mastheadIcon);
        setAccessibleName(mastheadLabel,
                          Messages.CONNECT_DIALOG_MASTHEAD_ACCESSIBLE_NAME);

        cp.add(mastheadLabel, NORTH);
        cp.add(radioButtonPanel, CENTER);
        cp.add(bottomPanel, SOUTH);

        createActions();

        remoteTF = new JTextField();
        remoteTF.addActionListener(connectAction);
        remoteTF.getDocument().addDocumentListener(this);
        remoteTF.addFocusListener(this);
        remoteTF.setPreferredSize(remoteTF.getPreferredSize());
        setAccessibleName(remoteTF,
                          Messages.REMOTE_PROCESS_TEXT_FIELD_ACCESSIBLE_NAME);

        //
        // If the VM supports the local attach mechanism (is: Sun
        // implementation) then the Local Process panel is created.
        //
        if (JConsole.isLocalAttachAvailable()) {
            vmModel = new ManagedVmTableModel();
            vmTable = new LocalTabJTable(vmModel);
            vmTable.setSelectionMode(SINGLE_SELECTION);
            vmTable.setPreferredScrollableViewportSize(new Dimension(400, 250));
            vmTable.setColumnSelectionAllowed(false);
            vmTable.addFocusListener(this);
            vmTable.getSelectionModel().addListSelectionListener(this);

            TableColumnModel columnModel = vmTable.getColumnModel();

            TableColumn pidColumn = columnModel.getColumn(COL_PID);
            pidColumn.setMaxWidth(getLabelWidth("9999999"));
            pidColumn.setResizable(false);

            TableColumn cmdLineColumn = columnModel.getColumn(COL_NAME);
            cmdLineColumn.setResizable(false);

            localRadioButton = new JRadioButton(Messages.LOCAL_PROCESS_COLON);
            localRadioButton.setMnemonic(Resources.getMnemonicInt(Messages.LOCAL_PROCESS_COLON));
            localRadioButton.setFont(boldLabelFont);
            localRadioButton.addItemListener(this);
            radioButtonGroup.add(localRadioButton);

            JPanel localPanel = new JPanel(new BorderLayout());

            JPanel localTablePanel = new JPanel(new BorderLayout());

            radioButtonPanel.add(localPanel, NORTH);

            localPanel.add(localRadioButton, NORTH);
            localPanel.add(new Padder(localRadioButton), LINE_START);
            localPanel.add(localTablePanel, CENTER);

            localTableScrollPane = new JScrollPane(vmTable);

            localTablePanel.add(localTableScrollPane, NORTH);

            localMessageLabel = new JLabel(" ");
            localMessageLabel.setFont(smallLabelFont);
            localMessageLabel.setForeground(hintTextColor);
            localTablePanel.add(localMessageLabel, SOUTH);
        }

        remoteRadioButton = new JRadioButton(Messages.REMOTE_PROCESS_COLON);
        remoteRadioButton.setMnemonic(Resources.getMnemonicInt(Messages.REMOTE_PROCESS_COLON));
        remoteRadioButton.setFont(boldLabelFont);
        radioButtonGroup.add(remoteRadioButton);

        JPanel remotePanel = new JPanel(new BorderLayout());
        if (localRadioButton != null) {
            remotePanel.add(remoteRadioButton, NORTH);
            remotePanel.add(new Padder(remoteRadioButton), LINE_START);

            Action nextRadioButtonAction =
                new AbstractAction("nextRadioButton") {
                    public void actionPerformed(ActionEvent ev) {
                        JRadioButton rb =
                            (ev.getSource() == localRadioButton) ? remoteRadioButton
                                                                 : localRadioButton;
                        rb.doClick();
                        rb.requestFocus();
                    }
                };

            localRadioButton.getActionMap().put("nextRadioButton", nextRadioButtonAction);
            remoteRadioButton.getActionMap().put("nextRadioButton", nextRadioButtonAction);

            localRadioButton.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                                               "nextRadioButton");
            remoteRadioButton.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                                                "nextRadioButton");
        } else {
            JLabel remoteLabel = new JLabel(remoteRadioButton.getText());
            remoteLabel.setFont(boldLabelFont);
            remotePanel.add(remoteLabel, NORTH);
        }
        radioButtonPanel.add(remotePanel, SOUTH);

        JPanel remoteTFPanel = new JPanel(new BorderLayout());
        remotePanel.add(remoteTFPanel, CENTER);

        remoteTFPanel.add(remoteTF, NORTH);

        remoteMessageLabel = new JLabel("<html>" + Messages.REMOTE_TF_USAGE + "</html>");
        remoteMessageLabel.setFont(smallLabelFont);
        remoteMessageLabel.setForeground(hintTextColor);
        remoteTFPanel.add(remoteMessageLabel, CENTER);

        JPanel userPwdPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        userPwdPanel.setBorder(new EmptyBorder(12, 0, 0, 0)); // top padding

        int tfWidth = JConsole.IS_WIN ? 12 : 8;

        userNameTF = new JTextField(tfWidth);
        userNameTF.addActionListener(connectAction);
        userNameTF.getDocument().addDocumentListener(this);
        userNameTF.addFocusListener(this);
        setAccessibleName(userNameTF,
            Messages.USERNAME_ACCESSIBLE_NAME);
        LabeledComponent lc;
        lc = new LabeledComponent(Messages.USERNAME_COLON_,
                                  Resources.getMnemonicInt(Messages.USERNAME_COLON_),
                                  userNameTF);
        lc.label.setFont(boldLabelFont);
        userPwdPanel.add(lc);

        passwordTF = new JPasswordField(tfWidth);
        // Heights differ, so fix here
        passwordTF.setPreferredSize(userNameTF.getPreferredSize());
        passwordTF.addActionListener(connectAction);
        passwordTF.getDocument().addDocumentListener(this);
        passwordTF.addFocusListener(this);
        setAccessibleName(passwordTF,
            Messages.PASSWORD_ACCESSIBLE_NAME);

        lc = new LabeledComponent(Messages.PASSWORD_COLON_,
                                  Resources.getMnemonicInt(Messages.PASSWORD_COLON_),
                                  passwordTF);
        lc.setBorder(new EmptyBorder(0, 12, 0, 0)); // Left padding
        lc.label.setFont(boldLabelFont);
        userPwdPanel.add(lc);

        remoteTFPanel.add(userPwdPanel, SOUTH);

        String connectButtonToolTipText =
            Messages.CONNECT_DIALOG_CONNECT_BUTTON_TOOLTIP;
        connectButton = new JButton(connectAction);
        connectButton.setToolTipText(connectButtonToolTipText);

        cancelButton = new JButton(cancelAction);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.setBorder(new EmptyBorder(12, 12, 2, 12));
        if (JConsole.IS_GTK) {
            buttonPanel.add(cancelButton);
            buttonPanel.add(connectButton);
        } else {
            buttonPanel.add(connectButton);
            buttonPanel.add(cancelButton);
        }
        bottomPanel.add(buttonPanel, NORTH);

        bottomPanel.add(statusBar, SOUTH);

        updateButtonStates();
        Utilities.updateTransparency(this);
    }

    public void revalidate() {
        // Adjust some colors
        Color disabledForeground = UIManager.getColor("Label.disabledForeground");
        if (disabledForeground == null) {
            // fall back for Nimbus that doesn't support 'Label.disabledForeground'
            disabledForeground = UIManager.getColor("Label.disabledText");
        }
        hintTextColor =
            ensureContrast(disabledForeground,
                           UIManager.getColor("Panel.background"));
        disabledTableCellColor =
            ensureContrast(new Color(0x808080),
                           UIManager.getColor("Table.background"));

        if (remoteMessageLabel != null) {
            remoteMessageLabel.setForeground(hintTextColor);
            // Update html color setting
            String colorStr =
                String.format("%06x", hintTextColor.getRGB() & 0xFFFFFF);
            remoteMessageLabel.setText("<html><font color=#" + colorStr + ">" +
                                       Messages.REMOTE_TF_USAGE);
        }
        if (localMessageLabel != null) {
            localMessageLabel.setForeground(hintTextColor);
            // Update html color setting
            valueChanged(null);
        }

        super.revalidate();
    }

    private void createActions() {
        connectAction = new AbstractAction(Messages.CONNECT) {
            /* init */ {
                putValue(Action.MNEMONIC_KEY, Resources.getMnemonicInt(Messages.CONNECT));
            }

            public void actionPerformed(ActionEvent ev) {
                if (!isEnabled() || !isVisible()) {
                    return;
                }
                setVisible(false);
                statusBar.setText("");

                if (remoteRadioButton.isSelected()) {
                    String txt = remoteTF.getText().trim();
                    String userName = userNameTF.getText().trim();
                    userName = userName.isEmpty() ? null : userName;
                    String password = passwordTF.getText();
                    password = password.isEmpty() ? null : password;
                    try {
                        if (txt.startsWith(JConsole.ROOT_URL)) {
                            String url = txt;
                            jConsole.addUrl(url, userName, password, false);
                            remoteTF.setText(JConsole.ROOT_URL);
                            return;
                        } else {
                            String host = remoteTF.getText().trim();
                            String port = "0";
                            int index = host.lastIndexOf(':');
                            if (index >= 0) {
                                port = host.substring(index + 1);
                                host = host.substring(0, index);
                            }
                            if (host.length() > 0 && port.length() > 0) {
                                int p = Integer.parseInt(port.trim());
                                jConsole.addHost(host, p, userName, password);
                                remoteTF.setText("");
                                userNameTF.setText("");
                                passwordTF.setText("");
                                return;
                            }
                        }
                    } catch (Exception ex) {
                        statusBar.setText(ex.toString());
                    }
                    setVisible(true);
                } else if (localRadioButton != null && localRadioButton.isSelected()) {
                    // Try to connect to selected VM. If a connection
                    // cannot be established for some reason (the process has
                    // terminated for example) then keep the dialog open showing
                    // the connect error.
                    //
                    int row = vmTable.getSelectedRow();
                    if (row >= 0) {
                        jConsole.addVmid(vmModel.vmAt(row));
                    }
                    refresh();
                }
            }
        };

        cancelAction = new AbstractAction(Messages.CANCEL) {
            public void actionPerformed(ActionEvent ev) {
                setVisible(false);
                statusBar.setText("");
            }
        };
    }


    // a label used solely for calculating the width
    private static JLabel tmpLabel = new JLabel();
    public static int getLabelWidth(String text) {
        tmpLabel.setText(text);
        return (int) tmpLabel.getPreferredSize().getWidth() + 1;
    }

    private class LocalTabJTable extends JTable {
        ManagedVmTableModel vmModel;
        Border rendererBorder = new EmptyBorder(0, 6, 0, 6);

        public LocalTabJTable(ManagedVmTableModel model) {
            super(model);
            this.vmModel = model;

            // Remove vertical lines, expect for GTK L&F.
            // (because GTK doesn't show header dividers)
            if (!JConsole.IS_GTK) {
                setShowVerticalLines(false);
                setIntercellSpacing(new Dimension(0, 1));
            }

            // Double-click handler
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        connectButton.doClick();
                    }
                }
            });

            // Enter should call default action
            getActionMap().put("connect", connectAction);
            InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "connect");
        }

        public String getToolTipText(MouseEvent e) {
            String tip = null;
            java.awt.Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);
            int realColumnIndex = convertColumnIndexToModel(colIndex);

            if (realColumnIndex == COL_NAME) {
                LocalVirtualMachine vmd = vmModel.vmAt(rowIndex);
                tip = vmd.toString();
            }
            return tip;
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    Component comp =
                        super.getTableCellRendererComponent(table, value, isSelected,
                                                            hasFocus, row, column);

                    if (!isSelected) {
                        LocalVirtualMachine lvm = vmModel.vmAt(row);
                        if (!lvm.isManageable() && !lvm.isAttachable()) {
                            comp.setForeground(disabledTableCellColor);
                        }
                    }

                    if (comp instanceof JLabel) {
                        JLabel label = (JLabel)comp;
                        label.setBorder(rendererBorder);

                        if (value instanceof Integer) {
                            label.setHorizontalAlignment(JLabel.RIGHT);
                        }
                    }

                    return comp;
                }
            };
        }
    }

    public void setConnectionParameters(String url,
                                        String host,
                                        int port,
                                        String userName,
                                        String password,
                                        String msg) {
        if ((url != null && url.length() > 0) ||
            (host != null && host.length() > 0 && port > 0)) {

            remoteRadioButton.setSelected(true);
            if (url != null && url.length() > 0) {
                remoteTF.setText(url);
            } else {
                remoteTF.setText(host+":"+port);
            }
            userNameTF.setText((userName != null) ? userName : "");
            passwordTF.setText((password != null) ? password : "");

            statusBar.setText((msg != null) ? msg : "");
            if (getPreferredSize().width > getWidth()) {
                pack();
            }
            remoteTF.requestFocus();
            remoteTF.selectAll();
        }
    }


    public void itemStateChanged(ItemEvent ev) {
        if (!localRadioButton.isSelected()) {
            vmTable.getSelectionModel().clearSelection();
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean connectEnabled = false;

        if (remoteRadioButton.isSelected()) {
            connectEnabled = JConsole.isValidRemoteString(remoteTF.getText());
        } else if (localRadioButton != null && localRadioButton.isSelected()) {
            int row = vmTable.getSelectedRow();
            if (row >= 0) {
                LocalVirtualMachine lvm = vmModel.vmAt(row);
                connectEnabled = (lvm.isManageable() || lvm.isAttachable());
            }
        }

        connectAction.setEnabled(connectEnabled);
    }

    public void insertUpdate(DocumentEvent e) {
        updateButtonStates();
    }

    public void removeUpdate(DocumentEvent e) {
        updateButtonStates();
    }

    public void changedUpdate(DocumentEvent e) {
        updateButtonStates();
    }

    public void focusGained(FocusEvent e) {
        Object source = e.getSource();
        Component opposite = e.getOppositeComponent();

        if (!e.isTemporary() &&
            source instanceof JTextField &&
            opposite instanceof JComponent &&
            SwingUtilities.getRootPane(opposite) == getRootPane()) {

            ((JTextField)source).selectAll();
        }

        if (source == remoteTF) {
            remoteRadioButton.setSelected(true);
        } else if (source == vmTable) {
            localRadioButton.setSelected(true);
            if (vmModel.getRowCount() == 1) {
                // if there's only one process then select the row
                vmTable.setRowSelectionInterval(0, 0);
            }
        }
        updateButtonStates();
    }

    public void focusLost(FocusEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c == KeyEvent.VK_ESCAPE) {
            setVisible(false);
        } else if (!(Character.isDigit(c) ||
                     c == KeyEvent.VK_BACK_SPACE ||
                     c == KeyEvent.VK_DELETE)) {
            getToolkit().beep();
            e.consume();
        }
    }

    public void setVisible(boolean b) {
        boolean wasVisible = isVisible();
        super.setVisible(b);
        if (b && !wasVisible) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (remoteRadioButton.isSelected()) {
                        remoteTF.requestFocus();
                        remoteTF.selectAll();
                    }
                }
            });
        }
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }


    // ListSelectionListener interface
    public void valueChanged(ListSelectionEvent e) {
        updateButtonStates();
        String labelText = " "; // Non-empty to reserve vertical space
        int row = vmTable.getSelectedRow();
        if (row >= 0) {
            LocalVirtualMachine lvm = vmModel.vmAt(row);
            if (!lvm.isManageable()) {
                if (lvm.isAttachable()) {
                    labelText = Messages.MANAGEMENT_WILL_BE_ENABLED;
                } else {
                    labelText = Messages.MANAGEMENT_NOT_ENABLED;
                }
            }
        }
        String colorStr =
            String.format("%06x", hintTextColor.getRGB() & 0xFFFFFF);
        localMessageLabel.setText("<html><font color=#" + colorStr + ">" + labelText);
    }
    // ----


    // Refresh the list of managed VMs
    public void refresh() {
        if (vmModel != null) {
            // Remember selection
            LocalVirtualMachine selected = null;
            int row = vmTable.getSelectedRow();
            if (row >= 0) {
                selected = vmModel.vmAt(row);
            }

            vmModel.refresh();

            int selectRow = -1;
            int n = vmModel.getRowCount();
            if (selected != null) {
                for (int i = 0; i < n; i++) {
                    LocalVirtualMachine lvm = vmModel.vmAt(i);
                    if (selected.vmid() == lvm.vmid() &&
                        selected.toString().equals(lvm.toString())) {

                        selectRow = i;
                        break;
                    }
                }
            }
            if (selectRow > -1) {
                vmTable.setRowSelectionInterval(selectRow, selectRow);
            } else {
                vmTable.getSelectionModel().clearSelection();
            }

            Dimension dim = vmTable.getPreferredSize();

            // Tricky. Reduce height by one to avoid double line at bottom,
            // but that causes a scroll bar to appear, so remove it.
            dim.height = Math.min(dim.height-1, 100);
            localTableScrollPane.setVerticalScrollBarPolicy((dim.height < 100)
                                                ? JScrollPane.VERTICAL_SCROLLBAR_NEVER
                                                : JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            localTableScrollPane.getViewport().setMinimumSize(dim);
            localTableScrollPane.getViewport().setPreferredSize(dim);
        }
        pack();
        setLocationRelativeTo(jConsole);
    }

    // Represents the list of managed VMs as a tabular data model.
    private static class ManagedVmTableModel extends AbstractTableModel {
        private static String[] columnNames = {
            Messages.COLUMN_NAME,
            Messages.COLUMN_PID,
        };

        private List<LocalVirtualMachine> vmList;

        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public synchronized int getRowCount() {
            return vmList.size();
        }

        public synchronized Object getValueAt(int row, int col) {
            assert col >= 0 && col <= columnNames.length;
            LocalVirtualMachine vm = vmList.get(row);
            switch (col) {
                case COL_NAME: return vm.displayName();
                case COL_PID:  return vm.vmid();
                default: return null;
            }
        }

        public Class<?> getColumnClass(int column) {
            switch (column) {
                case COL_NAME: return String.class;
                case COL_PID:  return Integer.class;
                default: return super.getColumnClass(column);
            }
        }

        public ManagedVmTableModel() {
            refresh();
        }


        public synchronized LocalVirtualMachine vmAt(int pos) {
            return vmList.get(pos);
        }

        public synchronized void refresh() {
            Map<Integer, LocalVirtualMachine> map =
                LocalVirtualMachine.getAllVirtualMachines();
            vmList = new ArrayList<LocalVirtualMachine>();
            vmList.addAll(map.values());

            // data has changed
            fireTableDataChanged();
        }
    }

    // A blank component that takes up as much space as the
    // button part of a JRadioButton.
    private static class Padder extends JPanel {
        JRadioButton radioButton;

        Padder(JRadioButton radioButton) {
            this.radioButton = radioButton;

            setAccessibleName(this, Messages.BLANK);
        }

        public Dimension getPreferredSize() {
            Rectangle r = getTextRectangle(radioButton);
            int w = (r != null && r.x > 8) ? r.x : 22;

            return new Dimension(w, 0);
        }

        private static Rectangle getTextRectangle(AbstractButton button) {
            String text = button.getText();
            Icon icon = (button.isEnabled()) ? button.getIcon() : button.getDisabledIcon();

            if (icon == null && button.getUI() instanceof BasicRadioButtonUI) {
                icon = ((BasicRadioButtonUI)button.getUI()).getDefaultIcon();
            }

            if ((icon == null) && (text == null)) {
                return null;
            }

            Rectangle paintIconR = new Rectangle();
            Rectangle paintTextR = new Rectangle();
            Rectangle paintViewR = new Rectangle();
            Insets paintViewInsets = new Insets(0, 0, 0, 0);

            paintViewInsets = button.getInsets(paintViewInsets);
            paintViewR.x = paintViewInsets.left;
            paintViewR.y = paintViewInsets.top;
            paintViewR.width = button.getWidth() - (paintViewInsets.left + paintViewInsets.right);
            paintViewR.height = button.getHeight() - (paintViewInsets.top + paintViewInsets.bottom);

            Graphics g = button.getGraphics();
            if (g == null) {
                return null;
            }
                SwingUtilities.layoutCompoundLabel(button,
                                                   g.getFontMetrics(),
                                                   text,
                                                   icon,
                                                   button.getVerticalAlignment(),
                                                   button.getHorizontalAlignment(),
                                                   button.getVerticalTextPosition(),
                                                   button.getHorizontalTextPosition(),
                                                   paintViewR,
                                                   paintIconR,
                                                   paintTextR,
                                                   button.getIconTextGap());

            return paintTextR;
        }
    }

}
