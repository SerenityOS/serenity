/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.motif;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import sun.awt.shell.ShellFolder;
import sun.swing.SwingUtilities2;

/**
 * Motif FileChooserUI.
 *
 * @author Jeff Dinkins
 */
public class MotifFileChooserUI extends BasicFileChooserUI {

    private FilterComboBoxModel filterComboBoxModel;

    protected JList<File> directoryList = null;
    protected JList<File> fileList = null;

    protected JTextField pathField = null;
    protected JComboBox<FileFilter> filterComboBox = null;
    protected JTextField filenameTextField = null;

    private static final Dimension hstrut10 = new Dimension(10, 1);
    private static final Dimension vstrut10 = new Dimension(1, 10);

    private static final Insets insets = new Insets(10, 10, 10, 10);

    private static Dimension prefListSize = new Dimension(75, 150);

    private static Dimension WITH_ACCELERATOR_PREF_SIZE = new Dimension(650, 450);
    private static Dimension PREF_SIZE = new Dimension(350, 450);
    private static final int MIN_WIDTH = 200;
    private static final int MIN_HEIGHT = 300;
    private static Dimension PREF_ACC_SIZE = new Dimension(10, 10);
    private static Dimension ZERO_ACC_SIZE = new Dimension(1, 1);

    private static Dimension MAX_SIZE = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);

    private static final Insets buttonMargin = new Insets(3, 3, 3, 3);

    private JPanel bottomPanel;

    protected JButton approveButton;

    private String enterFolderNameLabelText = null;
    private int enterFolderNameLabelMnemonic = 0;
    private String enterFileNameLabelText = null;
    private int enterFileNameLabelMnemonic = 0;

    private String filesLabelText = null;
    private int filesLabelMnemonic = 0;

    private String foldersLabelText = null;
    private int foldersLabelMnemonic = 0;

    private String pathLabelText = null;
    private int pathLabelMnemonic = 0;

    private String filterLabelText = null;
    private int filterLabelMnemonic = 0;

    private JLabel fileNameLabel;

    private void populateFileNameLabel() {
        if (getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
            fileNameLabel.setText(enterFolderNameLabelText);
            fileNameLabel.setDisplayedMnemonic(enterFolderNameLabelMnemonic);
        } else {
            fileNameLabel.setText(enterFileNameLabelText);
            fileNameLabel.setDisplayedMnemonic(enterFileNameLabelMnemonic);
        }
    }

    private String fileNameString(File file) {
        if (file == null) {
            return null;
        } else {
            JFileChooser fc = getFileChooser();
            if (fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) {
                return file.getPath();
            } else {
                return file.getName();
            }
        }
    }

    private String fileNameString(File[] files) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; files != null && i < files.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            if (files.length > 1) {
                sb.append("\"");
            }
            sb.append(fileNameString(files[i]));
            if (files.length > 1) {
                sb.append("\"");
            }
        }
        return sb.toString();
    }

    public MotifFileChooserUI(JFileChooser filechooser) {
        super(filechooser);
    }

    public String getFileName() {
        if(filenameTextField != null) {
            return filenameTextField.getText();
        } else {
            return null;
        }
    }

    public void setFileName(String filename) {
        if(filenameTextField != null) {
            filenameTextField.setText(filename);
        }
    }

    public String getDirectoryName() {
        return pathField.getText();
    }

    public void setDirectoryName(String dirname) {
        pathField.setText(dirname);
    }

    public void ensureFileIsVisible(JFileChooser fc, File f) {
        // PENDING(jeff)
    }

    public void rescanCurrentDirectory(JFileChooser fc) {
        getModel().validateFileCache();
    }

    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();
                if(prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    File f = (File) e.getNewValue();
                    if(f != null) {
                        setFileName(getFileChooser().getName(f));
                    }
                } else if (prop.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
                    File[] files = (File[]) e.getNewValue();
                    JFileChooser fc = getFileChooser();
                    if (files != null && files.length > 0 && (files.length > 1 || fc.isDirectorySelectionEnabled()
                            || !files[0].isDirectory())) {
                        setFileName(fileNameString(files));
                    }
                } else if (prop.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
                    fileList.clearSelection();
                } else if(prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                    directoryList.clearSelection();
                    ListSelectionModel sm = directoryList.getSelectionModel();
                    if (sm instanceof DefaultListSelectionModel) {
                        ((DefaultListSelectionModel)sm).moveLeadSelectionIndex(0);
                        sm.setAnchorSelectionIndex(0);
                    }
                    fileList.clearSelection();
                    sm = fileList.getSelectionModel();
                    if (sm instanceof DefaultListSelectionModel) {
                        ((DefaultListSelectionModel)sm).moveLeadSelectionIndex(0);
                        sm.setAnchorSelectionIndex(0);
                    }
                    File currentDirectory = getFileChooser().getCurrentDirectory();
                    if(currentDirectory != null) {
                        try {
                            setDirectoryName(ShellFolder.getNormalizedFile((File)e.getNewValue()).getPath());
                        } catch (IOException ioe) {
                            setDirectoryName(((File)e.getNewValue()).getAbsolutePath());
                        }
                        if ((getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) && !getFileChooser().isMultiSelectionEnabled()) {
                            setFileName(getDirectoryName());
                        }
                    }
                } else if(prop.equals(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
                    if (fileNameLabel != null) {
                        populateFileNameLabel();
                    }
                    directoryList.clearSelection();
                } else if (prop.equals(JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY)) {
                    if(getFileChooser().isMultiSelectionEnabled()) {
                        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    } else {
                        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        fileList.clearSelection();
                        getFileChooser().setSelectedFiles(null);
                    }
                } else if (prop.equals(JFileChooser.ACCESSORY_CHANGED_PROPERTY)) {
                    if(getAccessoryPanel() != null) {
                        if(e.getOldValue() != null) {
                            getAccessoryPanel().remove((JComponent) e.getOldValue());
                        }
                        JComponent accessory = (JComponent) e.getNewValue();
                        if(accessory != null) {
                            getAccessoryPanel().add(accessory, BorderLayout.CENTER);
                            getAccessoryPanel().setPreferredSize(PREF_ACC_SIZE);
                            getAccessoryPanel().setMaximumSize(MAX_SIZE);
                        } else {
                            getAccessoryPanel().setPreferredSize(ZERO_ACC_SIZE);
                            getAccessoryPanel().setMaximumSize(ZERO_ACC_SIZE);
                        }
                    }
                } else if (prop.equals(JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY) ||
                        prop.equals(JFileChooser.APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY) ||
                        prop.equals(JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY)) {
                    approveButton.setText(getApproveButtonText(getFileChooser()));
                    approveButton.setToolTipText(getApproveButtonToolTipText(getFileChooser()));
                } else if (prop.equals(JFileChooser.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY)) {
                    doControlButtonsChanged(e);
                } else if (prop.equals("componentOrientation")) {
                    ComponentOrientation o = (ComponentOrientation)e.getNewValue();
                    JFileChooser cc = (JFileChooser)e.getSource();
                    if (o != (ComponentOrientation)e.getOldValue()) {
                        cc.applyComponentOrientation(o);
                    }
                }
            }
        };
    }

    //
    // ComponentUI Interface Implementation methods
    //
    public static ComponentUI createUI(JComponent c) {
        return new MotifFileChooserUI((JFileChooser)c);
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        c.removePropertyChangeListener(filterComboBoxModel);
        approveButton.removeActionListener(getApproveSelectionAction());
        filenameTextField.removeActionListener(getApproveSelectionAction());
        super.uninstallUI(c);
    }

    public void installComponents(JFileChooser fc) {
        fc.setLayout(new BorderLayout(10, 10));
        fc.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        @SuppressWarnings("serial") // anonymous class
        JPanel interior = new JPanel() {
            public Insets getInsets() {
                return insets;
            }
        };
        interior.setInheritsPopupMenu(true);
        align(interior);
        interior.setLayout(new BoxLayout(interior, BoxLayout.PAGE_AXIS));

        fc.add(interior, BorderLayout.CENTER);

        // PENDING(jeff) - I18N
        JLabel l = new JLabel(pathLabelText);
        l.setDisplayedMnemonic(pathLabelMnemonic);
        align(l);
        interior.add(l);

        File currentDirectory = fc.getCurrentDirectory();
        String curDirName = null;
        if(currentDirectory != null) {
            curDirName = currentDirectory.getPath();
        }

        @SuppressWarnings("serial") // anonymous class
        JTextField tmp1 = new JTextField(curDirName, 35) {
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = getPreferredSize().height;
                return d;
            }
        };
        pathField = tmp1;
        pathField.setInheritsPopupMenu(true);
        l.setLabelFor(pathField);
        align(pathField);

        // Change to folder on return
        pathField.addActionListener(getUpdateAction());
        interior.add(pathField);

        interior.add(Box.createRigidArea(vstrut10));


        // CENTER: left, right accessory
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.LINE_AXIS));
        align(centerPanel);

        // left panel - Filter & folderList
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
        align(leftPanel);

        // add the filter PENDING(jeff) - I18N
        l = new JLabel(filterLabelText);
        l.setDisplayedMnemonic(filterLabelMnemonic);
        align(l);
        leftPanel.add(l);

        @SuppressWarnings("serial") // anonymous class
        JComboBox<FileFilter> tmp2 = new JComboBox<FileFilter>() {
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = getPreferredSize().height;
                return d;
            }
        };
        filterComboBox = tmp2;
        filterComboBox.setInheritsPopupMenu(true);
        l.setLabelFor(filterComboBox);
        filterComboBoxModel = createFilterComboBoxModel();
        filterComboBox.setModel(filterComboBoxModel);
        filterComboBox.setRenderer(createFilterComboBoxRenderer());
        fc.addPropertyChangeListener(filterComboBoxModel);
        align(filterComboBox);
        leftPanel.add(filterComboBox);

        // leftPanel.add(Box.createRigidArea(vstrut10));

        // Add the Folder List PENDING(jeff) - I18N
        l = new JLabel(foldersLabelText);
        l.setDisplayedMnemonic(foldersLabelMnemonic);
        align(l);
        leftPanel.add(l);
        JScrollPane sp = createDirectoryList();
        sp.getVerticalScrollBar().setFocusable(false);
        sp.getHorizontalScrollBar().setFocusable(false);
        sp.setInheritsPopupMenu(true);
        l.setLabelFor(sp.getViewport().getView());
        leftPanel.add(sp);
        leftPanel.setInheritsPopupMenu(true);


        // create files list
        JPanel rightPanel = new JPanel();
        align(rightPanel);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
        rightPanel.setInheritsPopupMenu(true);

        l = new JLabel(filesLabelText);
        l.setDisplayedMnemonic(filesLabelMnemonic);
        align(l);
        rightPanel.add(l);
        sp = createFilesList();
        l.setLabelFor(sp.getViewport().getView());
        rightPanel.add(sp);
        sp.setInheritsPopupMenu(true);

        centerPanel.add(leftPanel);
        centerPanel.add(Box.createRigidArea(hstrut10));
        centerPanel.add(rightPanel);
        centerPanel.setInheritsPopupMenu(true);

        JComponent accessoryPanel = getAccessoryPanel();
        JComponent accessory = fc.getAccessory();
        if(accessoryPanel != null) {
            if(accessory == null) {
                accessoryPanel.setPreferredSize(ZERO_ACC_SIZE);
                accessoryPanel.setMaximumSize(ZERO_ACC_SIZE);
            } else {
                getAccessoryPanel().add(accessory, BorderLayout.CENTER);
                accessoryPanel.setPreferredSize(PREF_ACC_SIZE);
                accessoryPanel.setMaximumSize(MAX_SIZE);
            }
            align(accessoryPanel);
            centerPanel.add(accessoryPanel);
            accessoryPanel.setInheritsPopupMenu(true);
        }
        interior.add(centerPanel);
        interior.add(Box.createRigidArea(vstrut10));

        // add the filename field PENDING(jeff) - I18N
        fileNameLabel = new JLabel();
        populateFileNameLabel();
        align(fileNameLabel);
        interior.add(fileNameLabel);

        @SuppressWarnings("serial") // anonymous class
        JTextField tmp3 = new JTextField(35) {
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = getPreferredSize().height;
                return d;
            }
        };
        filenameTextField = tmp3;
        filenameTextField.setInheritsPopupMenu(true);
        fileNameLabel.setLabelFor(filenameTextField);
        filenameTextField.addActionListener(getApproveSelectionAction());
        align(filenameTextField);
        filenameTextField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        interior.add(filenameTextField);

        bottomPanel = getBottomPanel();
        bottomPanel.add(new JSeparator(), BorderLayout.NORTH);

        // Add buttons
        JPanel buttonPanel = new JPanel();
        align(buttonPanel);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(Box.createGlue());

        @SuppressWarnings("serial") // anonymous class
        JButton tmp4 = new JButton(getApproveButtonText(fc)) {
            public Dimension getMaximumSize() {
                return new Dimension(MAX_SIZE.width, this.getPreferredSize().height);
            }
        };
        approveButton = tmp4;
        approveButton.setMnemonic(getApproveButtonMnemonic(fc));
        approveButton.setToolTipText(getApproveButtonToolTipText(fc));
        approveButton.setInheritsPopupMenu(true);
        align(approveButton);
        approveButton.setMargin(buttonMargin);
        approveButton.addActionListener(getApproveSelectionAction());
        buttonPanel.add(approveButton);
        buttonPanel.add(Box.createGlue());

        @SuppressWarnings("serial") // anonymous class
        JButton updateButton = new JButton(updateButtonText) {
            public Dimension getMaximumSize() {
                return new Dimension(MAX_SIZE.width, this.getPreferredSize().height);
            }
        };
        updateButton.setMnemonic(updateButtonMnemonic);
        updateButton.setToolTipText(updateButtonToolTipText);
        updateButton.setInheritsPopupMenu(true);
        align(updateButton);
        updateButton.setMargin(buttonMargin);
        updateButton.addActionListener(getUpdateAction());
        buttonPanel.add(updateButton);
        buttonPanel.add(Box.createGlue());

        @SuppressWarnings("serial") // anonymous class
        JButton cancelButton = new JButton(cancelButtonText) {
            public Dimension getMaximumSize() {
                return new Dimension(MAX_SIZE.width, this.getPreferredSize().height);
            }
        };
        cancelButton.setMnemonic(cancelButtonMnemonic);
        cancelButton.setToolTipText(cancelButtonToolTipText);
        cancelButton.setInheritsPopupMenu(true);
        align(cancelButton);
        cancelButton.setMargin(buttonMargin);
        cancelButton.addActionListener(getCancelSelectionAction());
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createGlue());

        @SuppressWarnings("serial") // anonymous class
        JButton helpButton = new JButton(helpButtonText) {
            public Dimension getMaximumSize() {
                return new Dimension(MAX_SIZE.width, this.getPreferredSize().height);
            }
        };
        helpButton.setMnemonic(helpButtonMnemonic);
        helpButton.setToolTipText(helpButtonToolTipText);
        align(helpButton);
        helpButton.setMargin(buttonMargin);
        helpButton.setEnabled(false);
        helpButton.setInheritsPopupMenu(true);
        buttonPanel.add(helpButton);
        buttonPanel.add(Box.createGlue());
        buttonPanel.setInheritsPopupMenu(true);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        bottomPanel.setInheritsPopupMenu(true);
        if (fc.getControlButtonsAreShown()) {
           fc.add(bottomPanel, BorderLayout.SOUTH);
        }
    }

    protected JPanel getBottomPanel() {
        if (bottomPanel == null) {
            bottomPanel = new JPanel(new BorderLayout(0, 4));
        }
        return bottomPanel;
    }

    private void doControlButtonsChanged(PropertyChangeEvent e) {
        if (getFileChooser().getControlButtonsAreShown()) {
            getFileChooser().add(bottomPanel,BorderLayout.SOUTH);
        } else {
            getFileChooser().remove(getBottomPanel());
        }
    }

    public void uninstallComponents(JFileChooser fc) {
        fc.removeAll();
        bottomPanel = null;
        if (filterComboBoxModel != null) {
            fc.removePropertyChangeListener(filterComboBoxModel);
        }
    }

    protected void installStrings(JFileChooser fc) {
        super.installStrings(fc);

        Locale l = fc.getLocale();

        enterFolderNameLabelText = UIManager.getString("FileChooser.enterFolderNameLabelText",l);
        enterFolderNameLabelMnemonic = getMnemonic("FileChooser.enterFolderNameLabelMnemonic", l);
        enterFileNameLabelText = UIManager.getString("FileChooser.enterFileNameLabelText",l);
        enterFileNameLabelMnemonic = getMnemonic("FileChooser.enterFileNameLabelMnemonic", l);

        filesLabelText = UIManager.getString("FileChooser.filesLabelText",l);
        filesLabelMnemonic = getMnemonic("FileChooser.filesLabelMnemonic", l);

        foldersLabelText = UIManager.getString("FileChooser.foldersLabelText",l);
        foldersLabelMnemonic = getMnemonic("FileChooser.foldersLabelMnemonic", l);

        pathLabelText = UIManager.getString("FileChooser.pathLabelText",l);
        pathLabelMnemonic = getMnemonic("FileChooser.pathLabelMnemonic", l);

        filterLabelText = UIManager.getString("FileChooser.filterLabelText",l);
        filterLabelMnemonic = getMnemonic("FileChooser.filterLabelMnemonic", l);
    }

    private Integer getMnemonic(String key, Locale l) {
        return SwingUtilities2.getUIDefaultsInt(key, l);
    }

    protected void installIcons(JFileChooser fc) {
        // Since motif doesn't have button icons, leave this empty
        // which overrides the supertype icon loading
    }

    protected void uninstallIcons(JFileChooser fc) {
        // Since motif doesn't have button icons, leave this empty
        // which overrides the supertype icon loading
    }

    protected JScrollPane createFilesList() {
        fileList = new JList<File>();

        if(getFileChooser().isMultiSelectionEnabled()) {
            fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        fileList.setModel(new MotifFileListModel());
        fileList.getSelectionModel().removeSelectionInterval(0, 0);
        fileList.setCellRenderer(new FileCellRenderer());
        fileList.addListSelectionListener(createListSelectionListener(getFileChooser()));
        fileList.addMouseListener(createDoubleClickListener(getFileChooser(), fileList));
        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = getFileChooser();
                if (SwingUtilities.isLeftMouseButton(e) && !chooser.isMultiSelectionEnabled()) {
                    int index = SwingUtilities2.loc2IndexFileList(fileList, e.getPoint());
                    if (index >= 0) {
                        File file = fileList.getModel().getElementAt(index);
                        setFileName(chooser.getName(file));
                    }
                }
            }
        });
        align(fileList);
        JScrollPane scrollpane = new JScrollPane(fileList);
        scrollpane.setPreferredSize(prefListSize);
        scrollpane.setMaximumSize(MAX_SIZE);
        align(scrollpane);
        fileList.setInheritsPopupMenu(true);
        scrollpane.setInheritsPopupMenu(true);
        return scrollpane;
    }

    protected JScrollPane createDirectoryList() {
        directoryList = new JList<File>();
        align(directoryList);

        directoryList.setCellRenderer(new DirectoryCellRenderer());
        directoryList.setModel(new MotifDirectoryListModel());
        directoryList.getSelectionModel().removeSelectionInterval(0, 0);
        directoryList.addMouseListener(createDoubleClickListener(getFileChooser(), directoryList));
        directoryList.addListSelectionListener(createListSelectionListener(getFileChooser()));
        directoryList.setInheritsPopupMenu(true);

        JScrollPane scrollpane = new JScrollPane(directoryList);
        scrollpane.setMaximumSize(MAX_SIZE);
        scrollpane.setPreferredSize(prefListSize);
        scrollpane.setInheritsPopupMenu(true);
        align(scrollpane);
        return scrollpane;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension prefSize =
            (getFileChooser().getAccessory() != null) ? WITH_ACCELERATOR_PREF_SIZE : PREF_SIZE;
        Dimension d = c.getLayout().preferredLayoutSize(c);
        if (d != null) {
            return new Dimension(d.width < prefSize.width ? prefSize.width : d.width,
                                 d.height < prefSize.height ? prefSize.height : d.height);
        } else {
            return prefSize;
        }
    }

    @Override
    public Dimension getMinimumSize(JComponent x) {
        return new Dimension(MIN_WIDTH, MIN_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize(JComponent x) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected void align(JComponent c) {
        c.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        c.setAlignmentY(JComponent.TOP_ALIGNMENT);
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class FileCellRenderer extends DefaultListCellRenderer  {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(getFileChooser().getName((File) value));
            setInheritsPopupMenu(true);
            return this;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DirectoryCellRenderer extends DefaultListCellRenderer  {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(getFileChooser().getName((File) value));
            setInheritsPopupMenu(true);
            return this;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class MotifDirectoryListModel extends AbstractListModel<File> implements ListDataListener {
        public MotifDirectoryListModel() {
            getModel().addListDataListener(this);
        }

        public int getSize() {
            return getModel().getDirectories().size();
        }

        public File getElementAt(int index) {
            return getModel().getDirectories().elementAt(index);
        }

        public void intervalAdded(ListDataEvent e) {
            fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
        }

        public void intervalRemoved(ListDataEvent e) {
            fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
        }

        // PENDING(jeff) - this is inefficient - should sent out
        // incremental adjustment values instead of saying that the
        // whole list has changed.
        public void fireContentsChanged() {
            fireContentsChanged(this, 0, getModel().getDirectories().size()-1);
        }

        // PENDING(jeff) - fire the correct interval changed - currently sending
        // out that everything has changed
        public void contentsChanged(ListDataEvent e) {
            fireContentsChanged();
        }

    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class MotifFileListModel extends AbstractListModel<File> implements ListDataListener {
        public MotifFileListModel() {
            getModel().addListDataListener(this);
        }

        public int getSize() {
            return getModel().getFiles().size();
        }

        public boolean contains(Object o) {
            return getModel().getFiles().contains(o);
        }

        public int indexOf(Object o) {
            return getModel().getFiles().indexOf(o);
        }

        public File getElementAt(int index) {
            return getModel().getFiles().elementAt(index);
        }

        public void intervalAdded(ListDataEvent e) {
            fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
        }

        public void intervalRemoved(ListDataEvent e) {
            fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
        }

        // PENDING(jeff) - this is inefficient - should sent out
        // incremental adjustment values instead of saying that the
        // whole list has changed.
        public void fireContentsChanged() {
            fireContentsChanged(this, 0, getModel().getFiles().size()-1);
        }

        // PENDING(jeff) - fire the interval changed
        public void contentsChanged(ListDataEvent e) {
            fireContentsChanged();
        }

    }

    //
    // DataModel for Types Comboxbox
    //
    protected FilterComboBoxModel createFilterComboBoxModel() {
        return new FilterComboBoxModel();
    }

    //
    // Renderer for Types ComboBox
    //
    protected FilterComboBoxRenderer createFilterComboBoxRenderer() {
        return new FilterComboBoxRenderer();
    }


    /**
     * Render different type sizes and styles.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class FilterComboBoxRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list,
            Object value, int index, boolean isSelected,
            boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value != null && value instanceof FileFilter) {
                setText(((FileFilter)value).getDescription());
            }

            return this;
        }
    }

    /**
     * Data model for a type-face selection combo-box.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class FilterComboBoxModel extends AbstractListModel<FileFilter> implements ComboBoxModel<FileFilter>,
            PropertyChangeListener {
        protected FileFilter[] filters;
        protected FilterComboBoxModel() {
            super();
            filters = getFileChooser().getChoosableFileFilters();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if(prop.equals(JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY)) {
                filters = (FileFilter[]) e.getNewValue();
                fireContentsChanged(this, -1, -1);
            } else if (prop.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
                fireContentsChanged(this, -1, -1);
            }
        }

        public void setSelectedItem(Object filter) {
            if(filter != null) {
                getFileChooser().setFileFilter((FileFilter) filter);
                fireContentsChanged(this, -1, -1);
            }
        }

        public Object getSelectedItem() {
            // Ensure that the current filter is in the list.
            // NOTE: we shouldnt' have to do this, since JFileChooser adds
            // the filter to the choosable filters list when the filter
            // is set. Lets be paranoid just in case someone overrides
            // setFileFilter in JFileChooser.
            FileFilter currentFilter = getFileChooser().getFileFilter();
            boolean found = false;
            if(currentFilter != null) {
                for (FileFilter filter : filters) {
                    if (filter == currentFilter) {
                        found = true;
                    }
                }
                if (!found) {
                    getFileChooser().addChoosableFileFilter(currentFilter);
                }
            }
            return getFileChooser().getFileFilter();
        }

        public int getSize() {
            if(filters != null) {
                return filters.length;
            } else {
                return 0;
            }
        }

        public FileFilter getElementAt(int index) {
            if(index > getSize() - 1) {
                // This shouldn't happen. Try to recover gracefully.
                return getFileChooser().getFileFilter();
            }
            if(filters != null) {
                return filters[index];
            } else {
                return null;
            }
        }
    }

    protected JButton getApproveButton(JFileChooser fc) {
        return approveButton;
    }

}
