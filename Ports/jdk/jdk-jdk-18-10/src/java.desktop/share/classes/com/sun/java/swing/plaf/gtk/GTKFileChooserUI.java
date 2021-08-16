/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.gtk;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDirectoryModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import sun.awt.shell.ShellFolder;
import sun.swing.FilePane;
import sun.swing.SwingUtilities2;
import sun.swing.plaf.synth.SynthFileChooserUI;

/**
 * GTK FileChooserUI.
 *
 * @author Leif Samuelsson
 * @author Jeff Dinkins
 */
class GTKFileChooserUI extends SynthFileChooserUI {

    // The accessoryPanel is a container to place the JFileChooser accessory component
    private JPanel accessoryPanel = null;

    private String newFolderButtonText = null;
    private String newFolderErrorSeparator = null;
    private String newFolderErrorText = null;
    private String newFolderDialogText = null;
    private String newFolderNoDirectoryErrorTitleText = null;
    private String newFolderNoDirectoryErrorText = null;

    private String deleteFileButtonText = null;
    private String renameFileButtonText = null;

    private String newFolderButtonToolTipText = null;
    private String deleteFileButtonToolTipText = null;
    private String renameFileButtonToolTipText = null;

    private int newFolderButtonMnemonic = 0;
    private int deleteFileButtonMnemonic = 0;
    private int renameFileButtonMnemonic = 0;
    private int foldersLabelMnemonic = 0;
    private int filesLabelMnemonic = 0;

    private String renameFileDialogText = null;
    private String renameFileErrorTitle = null;
    private String renameFileErrorText = null;

    private JComboBox<FileFilter> filterComboBox;
    private FilterComboBoxModel filterComboBoxModel;

    // From Motif

    private JPanel rightPanel;
    private JList<File> directoryList;
    private JList<File> fileList;

    private JLabel pathField;
    private JTextField fileNameTextField;

    private static final Dimension hstrut3 = new Dimension(3, 1);
    private static final Dimension vstrut10 = new Dimension(1, 10);

    private static Dimension prefListSize = new Dimension(75, 150);

    private static Dimension PREF_SIZE = new Dimension(435, 360);
    private static final int MIN_WIDTH = 200;
    private static final int MIN_HEIGHT = 300;

    private static Dimension ZERO_ACC_SIZE = new Dimension(1, 1);

    private static Dimension MAX_SIZE = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);

    private static final Insets buttonMargin = new Insets(3, 3, 3, 3);

    private String filesLabelText = null;
    private String foldersLabelText = null;
    private String pathLabelText = null;
    private String filterLabelText = null;

    private int pathLabelMnemonic = 0;
    private int filterLabelMnemonic = 0;

    private JComboBox<File> directoryComboBox;
    private DirectoryComboBoxModel directoryComboBoxModel;
    private Action directoryComboBoxAction = new DirectoryComboBoxAction();
    private JPanel bottomButtonPanel;
    private GTKDirectoryModel model = null;
    private Action newFolderAction;
    private boolean readOnly;
    private boolean showDirectoryIcons;
    private boolean showFileIcons;
    private GTKFileView fileView = new GTKFileView();
    private PropertyChangeListener gtkFCPropertyChangeListener;
    private Action approveSelectionAction = new GTKApproveSelectionAction();
    private GTKDirectoryListModel directoryListModel;

    public GTKFileChooserUI(JFileChooser filechooser) {
        super(filechooser);
    }

    protected ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        map.put("approveSelection", getApproveSelectionAction());
        map.put("cancelSelection", getCancelSelectionAction());
        map.put("Go Up", getChangeToParentDirectoryAction());
        map.put("fileNameCompletion", getFileNameCompletionAction());
        return map;
    }

    @SuppressWarnings("deprecation")
    public String getFileName() {
        JFileChooser fc = getFileChooser();
        String typedInName = fileNameTextField != null ?
            fileNameTextField.getText() : null;

        if (!fc.isMultiSelectionEnabled()) {
            return typedInName;
        }

        int mode = fc.getFileSelectionMode();
        JList<File> list = mode == JFileChooser.DIRECTORIES_ONLY ?
            directoryList : fileList;
        Object[] files = list.getSelectedValues();
        int len = files.length;
        Vector<String> result = new Vector<String>(len + 1);

        // we return all selected file names
        for (int i = 0; i < len; i++) {
            File file = (File)files[i];
            result.add(file.getName());
        }
        // plus the file name typed into the text field, if not already there
        if (typedInName != null && !result.contains(typedInName)) {
            result.add(typedInName);
        }

        StringBuilder sb = new StringBuilder();
        len = result.size();

        // construct the resulting string
        for (int i=0; i<len; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            if (len > 1) {
                sb.append("\"");
            }
            sb.append(result.get(i));
            if (len > 1) {
                sb.append("\"");
            }
        }
        return sb.toString();
    }

    public void setFileName(String fileName) {
        if (fileNameTextField != null) {
            fileNameTextField.setText(fileName);
        }
    }

//     public String getDirectoryName() {
//      return pathField.getText();
//     }

    public void setDirectoryName(String dirname) {
        pathField.setText(dirname);
    }

    public void ensureFileIsVisible(JFileChooser fc, File f) {
        // PENDING
    }

    public void rescanCurrentDirectory(JFileChooser fc) {
        getModel().validateFileCache();
    }

    public JPanel getAccessoryPanel() {
        return accessoryPanel;
    }

    // ***********************
    // * FileView operations *
    // ***********************

    public FileView getFileView(JFileChooser fc) {
        return fileView;
    }

    private class GTKFileView extends BasicFileView {
        public GTKFileView() {
            iconCache = null;
        }

        public void clearIconCache() {
        }

        public Icon getCachedIcon(File f) {
            return null;
        }

        public void cacheIcon(File f, Icon i) {
        }

        public Icon getIcon(File f) {
            return (f != null && f.isDirectory()) ? directoryIcon : fileIcon;
        }
    }


    private void updateDefaultButton() {
        JFileChooser filechooser = getFileChooser();
        JRootPane root = SwingUtilities.getRootPane(filechooser);
        if (root == null) {
            return;
        }

        if (filechooser.getControlButtonsAreShown()) {
            if (root.getDefaultButton() == null) {
                root.setDefaultButton(getApproveButton(filechooser));
                getCancelButton(filechooser).setDefaultCapable(false);
            }
        } else {
            if (root.getDefaultButton() == getApproveButton(filechooser)) {
                root.setDefaultButton(null);
            }
        }
    }

    protected void doSelectedFileChanged(PropertyChangeEvent e) {
        super.doSelectedFileChanged(e);
        File f = (File) e.getNewValue();
        if (f != null) {
            setFileName(getFileChooser().getName(f));
        }
    }

    protected void doDirectoryChanged(PropertyChangeEvent e) {
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
        if (currentDirectory != null) {
            try {
                setDirectoryName(ShellFolder.getNormalizedFile((File)e.getNewValue()).getPath());
            } catch (IOException ioe) {
                setDirectoryName(((File)e.getNewValue()).getAbsolutePath());
            }
            if ((getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) && !getFileChooser().isMultiSelectionEnabled()) {
                setFileName(pathField.getText());
            }
            directoryComboBoxModel.addItem(currentDirectory);
            directoryListModel.directoryChanged();
        }
        super.doDirectoryChanged(e);
    }

    protected void doAccessoryChanged(PropertyChangeEvent e) {
        if (getAccessoryPanel() != null) {
            if (e.getOldValue() != null) {
                getAccessoryPanel().remove((JComponent)e.getOldValue());
            }
            JComponent accessory = (JComponent)e.getNewValue();
            if (accessory != null) {
                getAccessoryPanel().add(accessory, BorderLayout.CENTER);
                getAccessoryPanel().setPreferredSize(accessory.getPreferredSize());
                getAccessoryPanel().setMaximumSize(MAX_SIZE);
            } else {
                getAccessoryPanel().setPreferredSize(ZERO_ACC_SIZE);
                getAccessoryPanel().setMaximumSize(ZERO_ACC_SIZE);
            }
        }
    }

    protected void doFileSelectionModeChanged(PropertyChangeEvent e) {
        directoryList.clearSelection();
        rightPanel.setVisible(((Integer)e.getNewValue()).intValue() != JFileChooser.DIRECTORIES_ONLY);

        super.doFileSelectionModeChanged(e);
    }

    protected void doMultiSelectionChanged(PropertyChangeEvent e) {
        if (getFileChooser().isMultiSelectionEnabled()) {
            fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fileList.clearSelection();
        }

        super.doMultiSelectionChanged(e);
    }

    protected void doControlButtonsChanged(PropertyChangeEvent e) {
        super.doControlButtonsChanged(e);

        JFileChooser filechooser = getFileChooser();
        if (filechooser.getControlButtonsAreShown()) {
            filechooser.add(bottomButtonPanel, BorderLayout.SOUTH);
        } else {
            filechooser.remove(bottomButtonPanel);
        }
        updateDefaultButton();
    }

    protected void doAncestorChanged(PropertyChangeEvent e) {
        if (e.getOldValue() == null && e.getNewValue() != null) {
            // Ancestor was added, set initial focus
            fileNameTextField.selectAll();
            fileNameTextField.requestFocus();
            updateDefaultButton();
        }

        super.doAncestorChanged(e);
    }



    // ********************************************
    // ************ Create Listeners **************
    // ********************************************

    public ListSelectionListener createListSelectionListener(JFileChooser fc) {
        return new SelectionListener();
    }

    class DoubleClickListener extends MouseAdapter {
        JList<?> list;
        public  DoubleClickListener(JList<?> list) {
            this.list = list;
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    File f = (File) list.getModel().getElementAt(index);
                    try {
                        // Strip trailing ".."
                        f = ShellFolder.getNormalizedFile(f);
                    } catch (IOException ex) {
                        // That's ok, we'll use f as is
                    }
                    if (getFileChooser().isTraversable(f)) {
                        list.clearSelection();
                        if (getFileChooser().getCurrentDirectory().equals(f)){
                            rescanCurrentDirectory(getFileChooser());
                        } else {
                            getFileChooser().setCurrentDirectory(f);
                        }
                    } else {
                        getFileChooser().approveSelection();
                    }
                }
            }
        }

        public void mouseEntered(MouseEvent evt) {
            if (list != null) {
                TransferHandler th1 = getFileChooser().getTransferHandler();
                TransferHandler th2 = list.getTransferHandler();
                if (th1 != th2) {
                    list.setTransferHandler(th1);
                }
                if (getFileChooser().getDragEnabled() != list.getDragEnabled()) {
                    list.setDragEnabled(getFileChooser().getDragEnabled());
                }
            }
        }
    }

    protected MouseListener createDoubleClickListener(JFileChooser fc, JList<?> list) {
        return new DoubleClickListener(list);
    }



    @SuppressWarnings("deprecation")
    protected class SelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                JFileChooser chooser = getFileChooser();
                JList<?> list = (JList) e.getSource();

                if (chooser.isMultiSelectionEnabled()) {
                    File[] files = null;
                    Object[] objects = list.getSelectedValues();
                    if (objects != null) {
                        if (objects.length == 1
                            && ((File)objects[0]).isDirectory()
                            && chooser.isTraversable(((File)objects[0]))
                            && (chooser.getFileSelectionMode() != JFileChooser.DIRECTORIES_ONLY
                                || !chooser.getFileSystemView().isFileSystem(((File)objects[0])))) {
                            setDirectorySelected(true);
                            setDirectory(((File)objects[0]));
                        } else {
                            ArrayList<File> fList = new ArrayList<File>(objects.length);
                            for (Object object : objects) {
                                File f = (File) object;
                                if ((chooser.isFileSelectionEnabled() && f.isFile())
                                    || (chooser.isDirectorySelectionEnabled() && f.isDirectory())) {
                                    fList.add(f);
                                }
                            }
                            if (fList.size() > 0) {
                                files = fList.toArray(new File[fList.size()]);
                            }
                            setDirectorySelected(false);
                        }
                    }
                    chooser.setSelectedFiles(files);
                } else {
                    File file = (File)list.getSelectedValue();
                    if (file != null
                        && file.isDirectory()
                        && chooser.isTraversable(file)
                        && (chooser.getFileSelectionMode() == JFileChooser.FILES_ONLY
                            || !chooser.getFileSystemView().isFileSystem(file))) {

                        setDirectorySelected(true);
                        setDirectory(file);
                    } else {
                        setDirectorySelected(false);
                        if (file != null) {
                            chooser.setSelectedFile(file);
                        }
                    }
                }
            }
        }
    }


    //
    // ComponentUI Interface Implementation methods
    //
    public static ComponentUI createUI(JComponent c) {
        return new GTKFileChooserUI((JFileChooser)c);
    }

    public void installUI(JComponent c) {
        accessoryPanel = new JPanel(new BorderLayout(10, 10));
        accessoryPanel.setName("GTKFileChooser.accessoryPanel");

        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        c.removePropertyChangeListener(filterComboBoxModel);
        super.uninstallUI(c);

        if (accessoryPanel != null) {
            accessoryPanel.removeAll();
        }
        accessoryPanel = null;
        getFileChooser().removeAll();
    }

    public void installComponents(JFileChooser fc) {
        super.installComponents(fc);

        boolean leftToRight = fc.getComponentOrientation().isLeftToRight();

        fc.setLayout(new BorderLayout());
        fc.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        // Top row of buttons
        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        topButtonPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        topButtonPanel.setName("GTKFileChooser.topButtonPanel");

        if (!UIManager.getBoolean("FileChooser.readOnly")) {
            JButton newFolderButton = new JButton(getNewFolderAction());
            newFolderButton.setName("GTKFileChooser.newFolderButton");
            newFolderButton.setMnemonic(newFolderButtonMnemonic);
            newFolderButton.setToolTipText(newFolderButtonToolTipText);
            newFolderButton.setText(newFolderButtonText);
            topButtonPanel.add(newFolderButton);
        }
        JButton deleteFileButton = new JButton(deleteFileButtonText);
        deleteFileButton.setName("GTKFileChooser.deleteFileButton");
        deleteFileButton.setMnemonic(deleteFileButtonMnemonic);
        deleteFileButton.setToolTipText(deleteFileButtonToolTipText);
        deleteFileButton.setEnabled(false);
        topButtonPanel.add(deleteFileButton);

        RenameFileAction rfa = new RenameFileAction();
        JButton renameFileButton = new JButton(rfa);
        if (readOnly) {
            rfa.setEnabled(false);
        }
        renameFileButton.setText(renameFileButtonText);
        renameFileButton.setName("GTKFileChooser.renameFileButton");
        renameFileButton.setMnemonic(renameFileButtonMnemonic);
        renameFileButton.setToolTipText(renameFileButtonToolTipText);
        topButtonPanel.add(renameFileButton);

        fc.add(topButtonPanel, BorderLayout.NORTH);


        JPanel interior = new JPanel();
        interior.setBorder(new EmptyBorder(0, 10, 10, 10));
        interior.setName("GTKFileChooser.interiorPanel");
        align(interior);
        interior.setLayout(new BoxLayout(interior, BoxLayout.PAGE_AXIS));

        fc.add(interior, BorderLayout.CENTER);

        @SuppressWarnings("serial") // anonymous class
        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
                                                         0, 0) {
            public void layoutContainer(Container target) {
                super.layoutContainer(target);
                JComboBox<?> comboBox = directoryComboBox;
                if (comboBox.getWidth() > target.getWidth()) {
                    comboBox.setBounds(0, comboBox.getY(), target.getWidth(),
                                       comboBox.getHeight());
                }
            }
        });
        comboBoxPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
        comboBoxPanel.setName("GTKFileChooser.directoryComboBoxPanel");
        // CurrentDir ComboBox
        directoryComboBoxModel = createDirectoryComboBoxModel(fc);
        directoryComboBox = new JComboBox<>(directoryComboBoxModel);
        directoryComboBox.setName("GTKFileChooser.directoryComboBox");
        directoryComboBox.putClientProperty( "JComboBox.lightweightKeyboardNavigation", "Lightweight" );
        directoryComboBox.addActionListener(directoryComboBoxAction);
        directoryComboBox.setMaximumRowCount(8);
        comboBoxPanel.add(directoryComboBox);
        interior.add(comboBoxPanel);


        // CENTER: left, right, accessory
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setName("GTKFileChooser.centerPanel");

        // SPLIT PANEL: left, right
        JSplitPane splitPanel = new JSplitPane();
        splitPanel.setName("GTKFileChooser.splitPanel");
        splitPanel.setDividerLocation((PREF_SIZE.width-8)/2);

        // left panel - Filter & directoryList
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setName("GTKFileChooser.directoryListPanel");

        // Add the Directory List
        // Create a label that looks like button (should be a table header)
        TableCellRenderer headerRenderer = new JTableHeader().getDefaultRenderer();
        JLabel directoryListLabel =
            (JLabel)headerRenderer.getTableCellRendererComponent(null, foldersLabelText,
                                                                     false, false, 0, 0);
        directoryListLabel.setName("GTKFileChooser.directoryListLabel");
        leftPanel.add(directoryListLabel, new GridBagConstraints(
                          0, 0, 1, 1, 1, 0, GridBagConstraints.WEST,
                          GridBagConstraints.HORIZONTAL,
                          new Insets(0, 0, 0, 0), 0, 0));
        leftPanel.add(createDirectoryList(), new GridBagConstraints(
                          0, 1, 1, 1, 1, 1, GridBagConstraints.EAST,
                          GridBagConstraints.BOTH,
                          new Insets(0, 0, 0, 0), 0, 0));
        directoryListLabel.setDisplayedMnemonic(foldersLabelMnemonic);
        directoryListLabel.setLabelFor(directoryList);

        // create files list
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setName("GTKFileChooser.fileListPanel");

        headerRenderer = new JTableHeader().getDefaultRenderer();
        JLabel fileListLabel =
            (JLabel)headerRenderer.getTableCellRendererComponent(null, filesLabelText,
                                                                     false, false, 0, 0);
        fileListLabel.setName("GTKFileChooser.fileListLabel");
        rightPanel.add(fileListLabel, new GridBagConstraints(
                          0, 0, 1, 1, 1, 0, GridBagConstraints.WEST,
                          GridBagConstraints.HORIZONTAL,
                          new Insets(0, 0, 0, 0), 0, 0));
        rightPanel.add(createFilesList(), new GridBagConstraints(
                          0, 1, 1, 1, 1, 1, GridBagConstraints.EAST,
                          GridBagConstraints.BOTH,
                          new Insets(0, 0, 0, 0), 0, 0));
        fileListLabel.setDisplayedMnemonic(filesLabelMnemonic);
        fileListLabel.setLabelFor(fileList);

        splitPanel.add(leftPanel,  leftToRight ? JSplitPane.LEFT : JSplitPane.RIGHT);
        splitPanel.add(rightPanel, leftToRight ? JSplitPane.RIGHT : JSplitPane.LEFT);
        centerPanel.add(splitPanel, BorderLayout.CENTER);

        JComponent accessoryPanel = getAccessoryPanel();
        JComponent accessory = fc.getAccessory();
        if (accessoryPanel != null) {
            if (accessory == null) {
                accessoryPanel.setPreferredSize(ZERO_ACC_SIZE);
                accessoryPanel.setMaximumSize(ZERO_ACC_SIZE);
            } else {
                getAccessoryPanel().add(accessory, BorderLayout.CENTER);
                accessoryPanel.setPreferredSize(accessory.getPreferredSize());
                accessoryPanel.setMaximumSize(MAX_SIZE);
            }
            align(accessoryPanel);
            centerPanel.add(accessoryPanel, BorderLayout.AFTER_LINE_ENDS);
        }
        interior.add(centerPanel);
        interior.add(Box.createRigidArea(vstrut10));

        JPanel pathFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEADING,
                                                          0, 0));
        pathFieldPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
        JLabel pathFieldLabel = new JLabel(pathLabelText);
        pathFieldLabel.setName("GTKFileChooser.pathFieldLabel");
        pathFieldLabel.setDisplayedMnemonic(pathLabelMnemonic);
        align(pathFieldLabel);
        pathFieldPanel.add(pathFieldLabel);
        pathFieldPanel.add(Box.createRigidArea(hstrut3));

        File currentDirectory = fc.getCurrentDirectory();
        String curDirName = null;
        if (currentDirectory != null) {
            curDirName = currentDirectory.getPath();
        }
        @SuppressWarnings("serial") // anonymous class
        JLabel tmp = new JLabel(curDirName) {
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = getPreferredSize().height;
                return d;
            }
        };
        pathField =  tmp;
        pathField.setName("GTKFileChooser.pathField");
        align(pathField);
        pathFieldPanel.add(pathField);
        interior.add(pathFieldPanel);

        // add the fileName field
        @SuppressWarnings("serial") // anonymous class
        JTextField tmp2 = new JTextField() {
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = getPreferredSize().height;
                return d;
            }
        };
        fileNameTextField = tmp2;

        pathFieldLabel.setLabelFor(fileNameTextField);

        Set<AWTKeyStroke> forwardTraversalKeys = fileNameTextField.getFocusTraversalKeys(
            KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        forwardTraversalKeys = new HashSet<AWTKeyStroke>(forwardTraversalKeys);
        forwardTraversalKeys.remove(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        fileNameTextField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalKeys);

        fileNameTextField.setName("GTKFileChooser.fileNameTextField");
        fileNameTextField.getActionMap().put("fileNameCompletionAction", getFileNameCompletionAction());
        fileNameTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "fileNameCompletionAction");
        interior.add(fileNameTextField);

        // Add the filter combo box
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        panel.setBorder(new EmptyBorder(0, 0, 4, 0));
        JLabel filterLabel = new JLabel(filterLabelText);
        filterLabel.setName("GTKFileChooser.filterLabel");
        filterLabel.setDisplayedMnemonic(filterLabelMnemonic);
        panel.add(filterLabel);

        filterComboBoxModel = createFilterComboBoxModel();
        fc.addPropertyChangeListener(filterComboBoxModel);
        filterComboBox = new JComboBox<>(filterComboBoxModel);
        filterComboBox.setRenderer(createFilterComboBoxRenderer());
        filterLabel.setLabelFor(filterComboBox);

        interior.add(Box.createRigidArea(vstrut10));
        interior.add(panel);
        interior.add(filterComboBox);

        // Add buttons
        bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        bottomButtonPanel.setName("GTKFileChooser.bottomButtonPanel");
        align(bottomButtonPanel);

        JPanel pnButtons = new JPanel(new GridLayout(1, 2, 5, 0));

        JButton cancelButton = getCancelButton(fc);
        align(cancelButton);
        cancelButton.setMargin(buttonMargin);
        pnButtons.add(cancelButton);

        JButton approveButton = getApproveButton(fc);
        align(approveButton);
        approveButton.setMargin(buttonMargin);
        pnButtons.add(approveButton);

        bottomButtonPanel.add(pnButtons);

        if (fc.getControlButtonsAreShown()) {
            fc.add(bottomButtonPanel, BorderLayout.SOUTH);
        }
    }

    protected void installListeners(JFileChooser fc) {
        super.installListeners(fc);

        gtkFCPropertyChangeListener = new GTKFCPropertyChangeListener();
        fc.addPropertyChangeListener(gtkFCPropertyChangeListener);
    }

    private int getMnemonic(String key, Locale l) {
        return SwingUtilities2.getUIDefaultsInt(key, l);
    }

    protected void uninstallListeners(JFileChooser fc) {
        super.uninstallListeners(fc);

        if (gtkFCPropertyChangeListener != null) {
            fc.removePropertyChangeListener(gtkFCPropertyChangeListener);
        }
    }

    private class GTKFCPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (prop.equals("GTKFileChooser.showDirectoryIcons")) {
                showDirectoryIcons = Boolean.TRUE.equals(e.getNewValue());
            } else if (prop.equals("GTKFileChooser.showFileIcons")) {
                showFileIcons      = Boolean.TRUE.equals(e.getNewValue());
            }
        }
    }

    protected void installDefaults(JFileChooser fc) {
        super.installDefaults(fc);
        readOnly = UIManager.getBoolean("FileChooser.readOnly");
        showDirectoryIcons =
            Boolean.TRUE.equals(fc.getClientProperty("GTKFileChooser.showDirectoryIcons"));
        showFileIcons =
            Boolean.TRUE.equals(fc.getClientProperty("GTKFileChooser.showFileIcons"));
    }

    protected void installIcons(JFileChooser fc) {
        directoryIcon    = UIManager.getIcon("FileView.directoryIcon");
        fileIcon         = UIManager.getIcon("FileView.fileIcon");
    }

    protected void installStrings(JFileChooser fc) {
        super.installStrings(fc);

        Locale l = fc.getLocale();

        newFolderDialogText = UIManager.getString("FileChooser.newFolderDialogText", l);
        newFolderErrorText = UIManager.getString("FileChooser.newFolderErrorText",l);
        newFolderErrorSeparator = UIManager.getString("FileChooser.newFolderErrorSeparator",l);
        newFolderButtonText = UIManager.getString("FileChooser.newFolderButtonText", l);
        newFolderNoDirectoryErrorTitleText = UIManager.getString("FileChooser.newFolderNoDirectoryErrorTitleText", l);
        newFolderNoDirectoryErrorText = UIManager.getString("FileChooser.newFolderNoDirectoryErrorText", l);
        deleteFileButtonText = UIManager.getString("FileChooser.deleteFileButtonText", l);
        renameFileButtonText = UIManager.getString("FileChooser.renameFileButtonText", l);

        newFolderButtonMnemonic = getMnemonic("FileChooser.newFolderButtonMnemonic", l);
        deleteFileButtonMnemonic = getMnemonic("FileChooser.deleteFileButtonMnemonic", l);
        renameFileButtonMnemonic = getMnemonic("FileChooser.renameFileButtonMnemonic", l);

        newFolderButtonToolTipText = UIManager.getString("FileChooser.newFolderButtonToolTipText", l);
        deleteFileButtonToolTipText = UIManager.getString("FileChooser.deleteFileButtonToolTipText", l);
        renameFileButtonToolTipText = UIManager.getString("FileChooser.renameFileButtonToolTipText", l);

        renameFileDialogText = UIManager.getString("FileChooser.renameFileDialogText", l);
        renameFileErrorTitle = UIManager.getString("FileChooser.renameFileErrorTitle", l);
        renameFileErrorText = UIManager.getString("FileChooser.renameFileErrorText", l);

        foldersLabelText = UIManager.getString("FileChooser.foldersLabelText",l);
        foldersLabelMnemonic = getMnemonic("FileChooser.foldersLabelMnemonic", l);

        filesLabelText = UIManager.getString("FileChooser.filesLabelText",l);
        filesLabelMnemonic = getMnemonic("FileChooser.filesLabelMnemonic", l);

        pathLabelText = UIManager.getString("FileChooser.pathLabelText",l);
        pathLabelMnemonic = getMnemonic("FileChooser.pathLabelMnemonic", l);

        filterLabelText = UIManager.getString("FileChooser.filterLabelText", l);
        filterLabelMnemonic = UIManager.getInt("FileChooser.filterLabelMnemonic");
    }

    protected void uninstallStrings(JFileChooser fc) {
        super.uninstallStrings(fc);

        newFolderButtonText = null;
        deleteFileButtonText = null;
        renameFileButtonText = null;

        newFolderButtonToolTipText = null;
        deleteFileButtonToolTipText = null;
        renameFileButtonToolTipText = null;

        renameFileDialogText = null;
        renameFileErrorTitle = null;
        renameFileErrorText = null;

        foldersLabelText = null;
        filesLabelText = null;

        pathLabelText = null;

        newFolderDialogText = null;
        newFolderErrorText = null;
        newFolderErrorSeparator = null;
    }

    protected JScrollPane createFilesList() {
        fileList = new JList<>();
        fileList.setName("GTKFileChooser.fileList");
        fileList.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, filesLabelText);

        if (getFileChooser().isMultiSelectionEnabled()) {
            fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        fileList.setModel(new GTKFileListModel());
        fileList.getSelectionModel().removeSelectionInterval(0, 0);
        fileList.setCellRenderer(new FileCellRenderer());
        fileList.addListSelectionListener(createListSelectionListener(getFileChooser()));
        fileList.addMouseListener(createDoubleClickListener(getFileChooser(), fileList));
        align(fileList);
        JScrollPane scrollpane = new JScrollPane(fileList);
    scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollpane.setName("GTKFileChooser.fileListScrollPane");
        scrollpane.setPreferredSize(prefListSize);
        scrollpane.setMaximumSize(MAX_SIZE);
        align(scrollpane);
        return scrollpane;
    }

    protected JScrollPane createDirectoryList() {
        directoryList = new JList<>();
        directoryList.setName("GTKFileChooser.directoryList");
        directoryList.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, foldersLabelText);
        align(directoryList);

        directoryList.setCellRenderer(new DirectoryCellRenderer());
        directoryListModel = new GTKDirectoryListModel();
        directoryList.getSelectionModel().removeSelectionInterval(0, 0);
        directoryList.setModel(directoryListModel);
        directoryList.addMouseListener(createDoubleClickListener(getFileChooser(), directoryList));
        directoryList.addListSelectionListener(createListSelectionListener(getFileChooser()));

        JScrollPane scrollpane = new JScrollPane(directoryList);
    scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollpane.setName("GTKFileChooser.directoryListScrollPane");
        scrollpane.setMaximumSize(MAX_SIZE);
        scrollpane.setPreferredSize(prefListSize);
        align(scrollpane);
        return scrollpane;
    }

    protected void createModel() {
        model = new GTKDirectoryModel();
    }

    public BasicDirectoryModel getModel() {
        return model;
    }

    public Action getApproveSelectionAction() {
        return approveSelectionAction;
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private final class GTKDirectoryModel extends BasicDirectoryModel {
        private GTKDirectoryModel() {
            super(getFileChooser());
        }

        protected void sort(Vector<? extends File> v) {
            FileSystemView fsv = getFileChooser().getFileSystemView();
            if (fsv == null) {
                super.sort(v);
            } else {
                v.sort(Comparator.comparing(fsv::getSystemDisplayName));
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class GTKDirectoryListModel extends AbstractListModel<File> implements ListDataListener {
        File curDir;
        public GTKDirectoryListModel() {
            getModel().addListDataListener(this);
            directoryChanged();
        }

        public int getSize() {
            return getModel().getDirectories().size() + 1;
        }

        @Override
        public File getElementAt(int index) {
            return index > 0 ? getModel().getDirectories().elementAt(index - 1):
                    curDir;
        }

        public void intervalAdded(ListDataEvent e) {
            fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
        }

        public void intervalRemoved(ListDataEvent e) {
            fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
        }

        // PENDING - this is inefficient - should sent out
        // incremental adjustment values instead of saying that the
        // whole list has changed.
        public void fireContentsChanged() {
            fireContentsChanged(this, 0, getModel().getDirectories().size()-1);
        }

        // PENDING - fire the correct interval changed - currently sending
        // out that everything has changed
        public void contentsChanged(ListDataEvent e) {
            fireContentsChanged();
        }

        private void directoryChanged() {
            curDir = getFileChooser().getFileSystemView().createFileObject(
                    getFileChooser().getCurrentDirectory(), ".");
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class GTKFileListModel extends AbstractListModel<File> implements ListDataListener {
        public GTKFileListModel() {
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

        @Override
        public File getElementAt(int index) {
            return getModel().getFiles().elementAt(index);
        }

        public void intervalAdded(ListDataEvent e) {
            fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
        }

        public void intervalRemoved(ListDataEvent e) {
            fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
        }

        // PENDING - this is inefficient - should sent out
        // incremental adjustment values instead of saying that the
        // whole list has changed.
        public void fireContentsChanged() {
            fireContentsChanged(this, 0, getModel().getFiles().size()-1);
        }

        // PENDING - fire the interval changed
        public void contentsChanged(ListDataEvent e) {
            fireContentsChanged();
        }
    }


    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class FileCellRenderer extends DefaultListCellRenderer  {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(getFileChooser().getName((File) value));
            if (showFileIcons) {
                setIcon(getFileChooser().getIcon((File)value));
            }
            return this;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DirectoryCellRenderer extends DefaultListCellRenderer  {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (showDirectoryIcons) {
                setIcon(getFileChooser().getIcon((File)value));
                setText(getFileChooser().getName((File)value));
            } else {
                setText(getFileChooser().getName((File)value) + "/");
            }
            return this;
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension prefSize = new Dimension(PREF_SIZE);
        JComponent accessory = getFileChooser().getAccessory();
        if (accessory != null) {
            prefSize.width += accessory.getPreferredSize().width + 20;
        }
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

    public Action getNewFolderAction() {
        if (newFolderAction == null) {
            newFolderAction = new NewFolderAction();
            newFolderAction.setEnabled(!readOnly);
        }
        return newFolderAction;
    }

    //
    // DataModel for DirectoryComboxbox
    //
    protected DirectoryComboBoxModel createDirectoryComboBoxModel(JFileChooser fc) {
        return new DirectoryComboBoxModel();
    }

    /**
     * Data model for a type-face selection combo-box.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DirectoryComboBoxModel extends AbstractListModel<File> implements ComboBoxModel<File> {
        Vector<File> directories = new Vector<File>();
        File selectedDirectory = null;
        JFileChooser chooser = getFileChooser();
        FileSystemView fsv = chooser.getFileSystemView();

        public DirectoryComboBoxModel() {
            // Add the current directory to the model, and make it the
            // selectedDirectory
            File dir = getFileChooser().getCurrentDirectory();
            if (dir != null) {
                addItem(dir);
            }
        }

        /**
         * Adds the directory to the model and sets it to be selected,
         * additionally clears out the previous selected directory and
         * the paths leading up to it, if any.
         */
        private void addItem(File directory) {

            if (directory == null) {
                return;
            }

            int oldSize = directories.size();
            directories.clear();
            if (oldSize > 0) {
                fireIntervalRemoved(this, 0, oldSize);
            }

            // Get the canonical (full) path. This has the side
            // benefit of removing extraneous chars from the path,
            // for example /foo/bar/ becomes /foo/bar
            File canonical;
            try {
                canonical = fsv.createFileObject(ShellFolder.getNormalizedFile(directory).getPath());
            } catch (IOException e) {
                // Maybe drive is not ready. Can't abort here.
                canonical = directory;
            }

            // create File instances of each directory leading up to the top
            File f = canonical;
            do {
                directories.add(f);
            } while ((f = f.getParentFile()) != null);
            int newSize = directories.size();
            if (newSize > 0) {
                fireIntervalAdded(this, 0, newSize);
            }
            setSelectedItem(canonical);
        }

        public void setSelectedItem(Object selectedDirectory) {
            this.selectedDirectory = (File)selectedDirectory;
            fireContentsChanged(this, -1, -1);
        }

        public Object getSelectedItem() {
            return selectedDirectory;
        }

        public int getSize() {
            return directories.size();
        }

        @Override
        public File getElementAt(int index) {
            return directories.elementAt(index);
        }
    }

    /**
     * Acts when DirectoryComboBox has changed the selected item.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DirectoryComboBoxAction extends AbstractAction {
        protected DirectoryComboBoxAction() {
            super("DirectoryComboBoxAction");
        }

        public void actionPerformed(ActionEvent e) {
            File f = (File)directoryComboBox.getSelectedItem();
            getFileChooser().setCurrentDirectory(f);
        }
    }

    /**
     * Creates a new folder.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class NewFolderAction extends AbstractAction {
        protected NewFolderAction() {
            super(FilePane.ACTION_NEW_FOLDER);
        }
        public void actionPerformed(ActionEvent e) {
            if (readOnly) {
                return;
            }
            JFileChooser fc = getFileChooser();
            File currentDirectory = fc.getCurrentDirectory();
            String dirName = JOptionPane.showInputDialog(fc,
                    newFolderDialogText, newFolderButtonText,
                    JOptionPane.PLAIN_MESSAGE);

            if (dirName != null) {
                if (!currentDirectory.exists()) {
                    JOptionPane.showMessageDialog(fc,
                            MessageFormat.format(newFolderNoDirectoryErrorText, dirName),
                            newFolderNoDirectoryErrorTitleText, JOptionPane.ERROR_MESSAGE);
                    return;
                }

                File newDir = fc.getFileSystemView().createFileObject
                        (currentDirectory, dirName);
                if (newDir == null || !newDir.mkdir()) {
                    JOptionPane.showMessageDialog(fc,
                            newFolderErrorText + newFolderErrorSeparator + " \"" +
                            dirName + "\"",
                            newFolderErrorText, JOptionPane.ERROR_MESSAGE);
                }
                fc.rescanCurrentDirectory();
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class GTKApproveSelectionAction extends ApproveSelectionAction {
        public void actionPerformed(ActionEvent e) {
            if (isDirectorySelected()) {
                File dir = getDirectory();
                try {
                    // Strip trailing ".."
                    if (dir != null) {
                        dir = ShellFolder.getNormalizedFile(dir);
                    }
                } catch (IOException ex) {
                    // Ok, use f as is
                }
                if (getFileChooser().getCurrentDirectory().equals(dir)) {
                    directoryList.clearSelection();
                    fileList.clearSelection();
                    ListSelectionModel sm = fileList.getSelectionModel();
                    if (sm instanceof DefaultListSelectionModel) {
                        ((DefaultListSelectionModel)sm).moveLeadSelectionIndex(0);
                        sm.setAnchorSelectionIndex(0);
                    }
                    rescanCurrentDirectory(getFileChooser());
                    return;
                }
            }
            super.actionPerformed(e);
        }
    }

    /**
     * Renames file
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class RenameFileAction extends AbstractAction {
        protected RenameFileAction() {
            super(FilePane.ACTION_EDIT_FILE_NAME);
        }
        public void actionPerformed(ActionEvent e) {
            if (getFileName().isEmpty()) {
                return;
            }
            JFileChooser fc = getFileChooser();
            File currentDirectory = fc.getCurrentDirectory();
            String newFileName = (String) JOptionPane.showInputDialog
                   (fc, new MessageFormat(renameFileDialogText).format
                           (new Object[] { getFileName() }),
                           renameFileButtonText, JOptionPane.PLAIN_MESSAGE, null, null,
                           getFileName());

            if (newFileName != null) {
                File oldFile = fc.getFileSystemView().createFileObject
                        (currentDirectory, getFileName());
                File newFile = fc.getFileSystemView().createFileObject
                        (currentDirectory, newFileName);
                if (oldFile == null || newFile == null ||
                        !getModel().renameFile(oldFile, newFile)) {
                    JOptionPane.showMessageDialog(fc,
                            new MessageFormat(renameFileErrorText).
                            format(new Object[] { getFileName(), newFileName}),
                            renameFileErrorTitle, JOptionPane.ERROR_MESSAGE);
                } else {
                    setFileName(getFileChooser().getName(newFile));
                    fc.rescanCurrentDirectory();
                }
            }
        }
    }

    //
    // Renderer for Filter ComboBox
    //
    protected FilterComboBoxRenderer createFilterComboBoxRenderer() {
        return new FilterComboBoxRenderer();
    }

    /**
     * Render different filters
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public class FilterComboBoxRenderer extends DefaultListCellRenderer {
        public String getName() {
            // As SynthComboBoxRenderer's are asked for a size BEFORE they
            // are parented getName is overriden to force the name to be
            // ComboBox.renderer if it isn't set. If we didn't do this the
            // wrong style could be used for size calculations.
            String name = super.getName();
            if (name == null) {
                return "ComboBox.renderer";
            }
            return name;
        }

        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            setName("ComboBox.listRenderer");

            if (value != null) {
                if (value instanceof FileFilter) {
                    setText(((FileFilter) value).getDescription());
                }
            } else {
                setText("");
            }

            return this;
        }
    }

    //
    // DataModel for Filter Combobox
    //
    protected FilterComboBoxModel createFilterComboBoxModel() {
        return new FilterComboBoxModel();
    }

    /**
     * Data model for filter combo-box.
     */
    @SuppressWarnings("serial") // JDK implementation class
    protected class FilterComboBoxModel extends AbstractListModel<FileFilter>
            implements ComboBoxModel<FileFilter>, PropertyChangeListener {
        protected FileFilter[] filters;

        protected FilterComboBoxModel() {
            super();
            filters = getFileChooser().getChoosableFileFilters();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (prop == JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY) {
                filters = (FileFilter[]) e.getNewValue();
                fireContentsChanged(this, -1, -1);
            } else if (prop == JFileChooser.FILE_FILTER_CHANGED_PROPERTY) {
                fireContentsChanged(this, -1, -1);
            }
        }

        public void setSelectedItem(Object filter) {
            if (filter != null) {
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
            if (currentFilter != null) {
                for (FileFilter filter : filters) {
                    if (filter == currentFilter) {
                        found = true;
                    }
                }
                if (found == false) {
                    getFileChooser().addChoosableFileFilter(currentFilter);
                }
            }
            return getFileChooser().getFileFilter();
        }

        public int getSize() {
            if (filters != null) {
                return filters.length;
            } else {
                return 0;
            }
        }

        @Override
        public FileFilter getElementAt(int index) {
            if (index > getSize() - 1) {
                // This shouldn't happen. Try to recover gracefully.
                return getFileChooser().getFileFilter();
            }
            if (filters != null) {
                return filters[index];
            } else {
                return null;
            }
        }
    }
}
