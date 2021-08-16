/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import sun.swing.SwingUtilities2;

public class AquaFileChooserUI extends FileChooserUI {
    /* FileView icons */
    protected Icon directoryIcon = null;
    protected Icon fileIcon = null;
    protected Icon computerIcon = null;
    protected Icon hardDriveIcon = null;
    protected Icon floppyDriveIcon = null;

    protected Icon upFolderIcon = null;
    protected Icon homeFolderIcon = null;
    protected Icon listViewIcon = null;
    protected Icon detailsViewIcon = null;

    protected int saveButtonMnemonic = 0;
    protected int openButtonMnemonic = 0;
    protected int cancelButtonMnemonic = 0;
    protected int updateButtonMnemonic = 0;
    protected int helpButtonMnemonic = 0;
    protected int chooseButtonMnemonic = 0;

    private String saveTitleText = null;
    private String openTitleText = null;
    String newFolderTitleText = null;

    protected String saveButtonText = null;
    protected String openButtonText = null;
    protected String cancelButtonText = null;
    protected String updateButtonText = null;
    protected String helpButtonText = null;
    protected String newFolderButtonText = null;
    protected String chooseButtonText = null;

    //private String newFolderErrorSeparator = null;
    String newFolderErrorText = null;
    String newFolderExistsErrorText = null;
    protected String fileDescriptionText = null;
    protected String directoryDescriptionText = null;

    protected String saveButtonToolTipText = null;
    protected String openButtonToolTipText = null;
    protected String cancelButtonToolTipText = null;
    protected String updateButtonToolTipText = null;
    protected String helpButtonToolTipText = null;
    protected String chooseItemButtonToolTipText = null; // Choose anything
    protected String chooseFolderButtonToolTipText = null; // Choose folder
    protected String directoryComboBoxToolTipText = null;
    protected String filenameTextFieldToolTipText = null;
    protected String filterComboBoxToolTipText = null;
    protected String openDirectoryButtonToolTipText = null;

    protected String cancelOpenButtonToolTipText = null;
    protected String cancelSaveButtonToolTipText = null;
    protected String cancelChooseButtonToolTipText = null;
    protected String cancelNewFolderButtonToolTipText = null;

    protected String desktopName = null;
    String newFolderDialogPrompt = null;
    String newFolderDefaultName = null;
    private String newFileDefaultName = null;
    String createButtonText = null;

    JFileChooser filechooser = null;

    private MouseListener doubleClickListener = null;
    private PropertyChangeListener propertyChangeListener = null;
    private AncestorListener ancestorListener = null;
    private DropTarget dragAndDropTarget = null;

    private static final AcceptAllFileFilter acceptAllFileFilter = new AcceptAllFileFilter();

    private AquaFileSystemModel model;

    final AquaFileView fileView = new AquaFileView(this);

    boolean selectionInProgress = false;

    // The accessoryPanel is a container to place the JFileChooser accessory component
    private JPanel accessoryPanel = null;

    //
    // ComponentUI Interface Implementation methods
    //
    public static ComponentUI createUI(final JComponent c) {
        return new AquaFileChooserUI((JFileChooser)c);
    }

    public AquaFileChooserUI(final JFileChooser filechooser) {
        super();
    }

    public void installUI(final JComponent c) {
        accessoryPanel = new JPanel(new BorderLayout());
        filechooser = (JFileChooser)c;

        createModel();

        installDefaults(filechooser);
        installComponents(filechooser);
        installListeners(filechooser);

        AquaUtils.enforceComponentOrientation(filechooser, ComponentOrientation.getOrientation(Locale.getDefault()));
    }

    public void uninstallUI(final JComponent c) {
        uninstallListeners(filechooser);
        uninstallComponents(filechooser);
        uninstallDefaults(filechooser);

        if (accessoryPanel != null) {
            accessoryPanel.removeAll();
        }

        accessoryPanel = null;
        getFileChooser().removeAll();
    }

    protected void installListeners(final JFileChooser fc) {
        doubleClickListener = createDoubleClickListener(fc, fFileList);
        fFileList.addMouseListener(doubleClickListener);

        propertyChangeListener = createPropertyChangeListener(fc);
        if (propertyChangeListener != null) {
            fc.addPropertyChangeListener(propertyChangeListener);
        }

        ancestorListener = new AncestorListener(){
            public void ancestorAdded(final AncestorEvent e) {
                // Request defaultness for the appropriate button based on mode
                setFocusForMode(getFileChooser());
                // Request defaultness for the appropriate button based on mode
                setDefaultButtonForMode(getFileChooser());
            }

            public void ancestorRemoved(final AncestorEvent e) {
            }

            public void ancestorMoved(final AncestorEvent e) {
            }
        };
        fc.addAncestorListener(ancestorListener);

        fc.registerKeyboardAction(new CancelSelectionAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        dragAndDropTarget = new DropTarget(fc, DnDConstants.ACTION_COPY, new DnDHandler(), true);
        fc.setDropTarget(dragAndDropTarget);
    }

    protected void uninstallListeners(final JFileChooser fc) {
        if (propertyChangeListener != null) {
            fc.removePropertyChangeListener(propertyChangeListener);
        }
        fFileList.removeMouseListener(doubleClickListener);
        fc.removePropertyChangeListener(filterComboBoxModel);
        fc.removePropertyChangeListener(model);
        fc.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        fc.removeAncestorListener(ancestorListener);
        fc.setDropTarget(null);
        ancestorListener = null;
    }

    protected void installDefaults(final JFileChooser fc) {
        installIcons(fc);
        installStrings(fc);
        setPackageIsTraversable(fc.getClientProperty(PACKAGE_TRAVERSABLE_PROPERTY));
        setApplicationIsTraversable(fc.getClientProperty(APPLICATION_TRAVERSABLE_PROPERTY));
    }

    protected void installIcons(final JFileChooser fc) {
        directoryIcon = UIManager.getIcon("FileView.directoryIcon");
        fileIcon = UIManager.getIcon("FileView.fileIcon");
        computerIcon = UIManager.getIcon("FileView.computerIcon");
        hardDriveIcon = UIManager.getIcon("FileView.hardDriveIcon");
    }

    String getString(final String uiKey, final String fallback) {
        final String result = UIManager.getString(uiKey);
        return (result == null ? fallback : result);
    }

    protected void installStrings(final JFileChooser fc) {
        // Exist in basic.properties (though we might want to override)
        fileDescriptionText = UIManager.getString("FileChooser.fileDescriptionText");
        directoryDescriptionText = UIManager.getString("FileChooser.directoryDescriptionText");
        newFolderErrorText = getString("FileChooser.newFolderErrorText", "Error occurred during folder creation");

        saveButtonText = UIManager.getString("FileChooser.saveButtonText");
        openButtonText = UIManager.getString("FileChooser.openButtonText");
        cancelButtonText = UIManager.getString("FileChooser.cancelButtonText");
        updateButtonText = UIManager.getString("FileChooser.updateButtonText");
        helpButtonText = UIManager.getString("FileChooser.helpButtonText");

        saveButtonMnemonic = UIManager.getInt("FileChooser.saveButtonMnemonic");
        openButtonMnemonic = UIManager.getInt("FileChooser.openButtonMnemonic");
        cancelButtonMnemonic = UIManager.getInt("FileChooser.cancelButtonMnemonic");
        updateButtonMnemonic = UIManager.getInt("FileChooser.updateButtonMnemonic");
        helpButtonMnemonic = UIManager.getInt("FileChooser.helpButtonMnemonic");
        chooseButtonMnemonic = UIManager.getInt("FileChooser.chooseButtonMnemonic");

        saveButtonToolTipText = UIManager.getString("FileChooser.saveButtonToolTipText");
        openButtonToolTipText = UIManager.getString("FileChooser.openButtonToolTipText");
        cancelButtonToolTipText = UIManager.getString("FileChooser.cancelButtonToolTipText");
        updateButtonToolTipText = UIManager.getString("FileChooser.updateButtonToolTipText");
        helpButtonToolTipText = UIManager.getString("FileChooser.helpButtonToolTipText");

        // Mac-specific, but fallback to basic if it's missing
        saveTitleText = getString("FileChooser.saveTitleText", saveButtonText);
        openTitleText = getString("FileChooser.openTitleText", openButtonText);

        // Mac-specific, required
        newFolderExistsErrorText = getString("FileChooser.newFolderExistsErrorText", "That name is already taken");
        chooseButtonText = getString("FileChooser.chooseButtonText", "Choose");
        newFolderButtonText = getString("FileChooser.newFolderButtonText", "New");
        newFolderTitleText = getString("FileChooser.newFolderTitleText", "New Folder");

        if (fc.getDialogType() == JFileChooser.SAVE_DIALOG) {
            fileNameLabelText = getString("FileChooser.saveDialogFileNameLabelText", "Save As:");
        } else {
            fileNameLabelText = getString("FileChooser.fileNameLabelText", "Name:");
        }

        filesOfTypeLabelText = getString("FileChooser.filesOfTypeLabelText", "Format:");

        desktopName = getString("FileChooser.desktopName", "Desktop");
        newFolderDialogPrompt = getString("FileChooser.newFolderPromptText", "Name of new folder:");
        newFolderDefaultName = getString("FileChooser.untitledFolderName", "untitled folder");
        newFileDefaultName = getString("FileChooser.untitledFileName", "untitled");
        createButtonText = getString("FileChooser.createButtonText", "Create");

        fColumnNames[1] = getString("FileChooser.byDateText", "Date Modified");
        fColumnNames[0] = getString("FileChooser.byNameText", "Name");

        // Mac-specific, optional
        chooseItemButtonToolTipText = UIManager.getString("FileChooser.chooseItemButtonToolTipText");
        chooseFolderButtonToolTipText = UIManager.getString("FileChooser.chooseFolderButtonToolTipText");
        openDirectoryButtonToolTipText = UIManager.getString("FileChooser.openDirectoryButtonToolTipText");

        directoryComboBoxToolTipText = UIManager.getString("FileChooser.directoryComboBoxToolTipText");
        filenameTextFieldToolTipText = UIManager.getString("FileChooser.filenameTextFieldToolTipText");
        filterComboBoxToolTipText = UIManager.getString("FileChooser.filterComboBoxToolTipText");

        cancelOpenButtonToolTipText = UIManager.getString("FileChooser.cancelOpenButtonToolTipText");
        cancelSaveButtonToolTipText = UIManager.getString("FileChooser.cancelSaveButtonToolTipText");
        cancelChooseButtonToolTipText = UIManager.getString("FileChooser.cancelChooseButtonToolTipText");
        cancelNewFolderButtonToolTipText = UIManager.getString("FileChooser.cancelNewFolderButtonToolTipText");

        newFolderTitleText = UIManager.getString("FileChooser.newFolderTitleText");
        newFolderToolTipText = UIManager.getString("FileChooser.newFolderToolTipText");
        newFolderAccessibleName = getString("FileChooser.newFolderAccessibleName", newFolderTitleText);
    }

    protected void uninstallDefaults(final JFileChooser fc) {
        uninstallIcons(fc);
        uninstallStrings(fc);
    }

    protected void uninstallIcons(final JFileChooser fc) {
        directoryIcon = null;
        fileIcon = null;
        computerIcon = null;
        hardDriveIcon = null;
        floppyDriveIcon = null;

        upFolderIcon = null;
        homeFolderIcon = null;
        detailsViewIcon = null;
        listViewIcon = null;
    }

    protected void uninstallStrings(final JFileChooser fc) {
        saveTitleText = null;
        openTitleText = null;
        newFolderTitleText = null;

        saveButtonText = null;
        openButtonText = null;
        cancelButtonText = null;
        updateButtonText = null;
        helpButtonText = null;
        newFolderButtonText = null;
        chooseButtonText = null;

        cancelOpenButtonToolTipText = null;
        cancelSaveButtonToolTipText = null;
        cancelChooseButtonToolTipText = null;
        cancelNewFolderButtonToolTipText = null;

        saveButtonToolTipText = null;
        openButtonToolTipText = null;
        cancelButtonToolTipText = null;
        updateButtonToolTipText = null;
        helpButtonToolTipText = null;
        chooseItemButtonToolTipText = null;
        chooseFolderButtonToolTipText = null;
        openDirectoryButtonToolTipText = null;
        directoryComboBoxToolTipText = null;
        filenameTextFieldToolTipText = null;
        filterComboBoxToolTipText = null;

        newFolderDefaultName = null;
        newFileDefaultName = null;

        desktopName = null;
    }

    protected void createModel() {
    }

    AquaFileSystemModel getModel() {
        return model;
    }

    /*
     * Listen for filechooser property changes, such as
     * the selected file changing, or the type of the dialog changing.
     */
    // Taken almost verbatim from Metal
    protected PropertyChangeListener createPropertyChangeListener(final JFileChooser fc) {
        return new PropertyChangeListener(){
            public void propertyChange(final PropertyChangeEvent e) {
                final String prop = e.getPropertyName();
                if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    final File f = (File)e.getNewValue();
                    if (f != null) {
                        // Select the file in the list if the selected file didn't change as
                        // a result of a list click.
                        if (!selectionInProgress && getModel().contains(f)) {
                            fFileList.setSelectedIndex(getModel().indexOf(f));
                        }

                        // [3643835] Need to populate the text field here.  No-op on Open dialogs
                        // Note that this was removed for 3514735, but should not have been.
                        if (!f.isDirectory()) {
                            setFileName(getFileChooser().getName(f));
                        }
                    }
                    updateButtonState(getFileChooser());
                } else if (prop.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
                    JFileChooser fileChooser = getFileChooser();
                    if (!fileChooser.isDirectorySelectionEnabled()) {
                        final File[] files = (File[]) e.getNewValue();
                        if (files != null) {
                            for (int selectedRow : fFileList.getSelectedRows()) {
                                File file = (File) fFileList.getValueAt(selectedRow, 0);
                                if (fileChooser.isTraversable(file)) {
                                    fFileList.removeSelectedIndex(selectedRow);
                                }
                            }
                        }
                    }
                } else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                    fFileList.clearSelection();
                    final File currentDirectory = getFileChooser().getCurrentDirectory();
                    if (currentDirectory != null) {
                        fDirectoryComboBoxModel.addItem(currentDirectory);
                        // Enable the newFolder action if the current directory
                        // is writable.
                        // PENDING(jeff) - broken - fix
                        getAction(kNewFolder).setEnabled(currentDirectory.canWrite());
                    }
                    updateButtonState(getFileChooser());
                } else if (prop.equals(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
                    fFileList.clearSelection();
                    setBottomPanelForMode(getFileChooser()); // Also updates approve button
                } else if (prop == JFileChooser.ACCESSORY_CHANGED_PROPERTY) {
                    if (getAccessoryPanel() != null) {
                        if (e.getOldValue() != null) {
                            getAccessoryPanel().remove((JComponent)e.getOldValue());
                        }
                        final JComponent accessory = (JComponent)e.getNewValue();
                        if (accessory != null) {
                            getAccessoryPanel().add(accessory, BorderLayout.CENTER);
                        }
                    }
                } else if (prop == JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY) {
                    updateApproveButton(getFileChooser());
                    getFileChooser().invalidate();
                } else if (prop == JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY) {
                    if (getFileChooser().getDialogType() == JFileChooser.SAVE_DIALOG) {
                        fileNameLabelText = getString("FileChooser.saveDialogFileNameLabelText", "Save As:");
                    } else {
                        fileNameLabelText = getString("FileChooser.fileNameLabelText", "Name:");
                    }
                    fTextFieldLabel.setText(fileNameLabelText);

                    // Mac doesn't show the text field or "new folder" button in 'Open' dialogs
                    setBottomPanelForMode(getFileChooser()); // Also updates approve button
                } else if (prop.equals(JFileChooser.APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY)) {
                    getApproveButton(getFileChooser()).setMnemonic(getApproveButtonMnemonic(getFileChooser()));
                } else if (prop.equals(PACKAGE_TRAVERSABLE_PROPERTY)) {
                    setPackageIsTraversable(e.getNewValue());
                } else if (prop.equals(APPLICATION_TRAVERSABLE_PROPERTY)) {
                    setApplicationIsTraversable(e.getNewValue());
                } else if (prop.equals(JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY)) {
                    if (getFileChooser().isMultiSelectionEnabled()) {
                        fFileList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    } else {
                        fFileList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    }
                } else if (prop.equals(JFileChooser.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY)) {
                    doControlButtonsChanged(e);
                }
            }
        };
    }

    void setPackageIsTraversable(final Object o) {
        int newProp = -1;
        if (o != null && o instanceof String) newProp = parseTraversableProperty((String)o);
        if (newProp != -1) fPackageIsTraversable = newProp;
        else fPackageIsTraversable = sGlobalPackageIsTraversable;
    }

    void setApplicationIsTraversable(final Object o) {
        int newProp = -1;
        if (o != null && o instanceof String) newProp = parseTraversableProperty((String)o);
        if (newProp != -1) fApplicationIsTraversable = newProp;
        else fApplicationIsTraversable = sGlobalApplicationIsTraversable;
    }

    void doControlButtonsChanged(final PropertyChangeEvent e) {
        if (getFileChooser().getControlButtonsAreShown()) {
            fBottomPanel.add(fDirectoryPanelSpacer);
            fBottomPanel.add(fDirectoryPanel);
        } else {
            fBottomPanel.remove(fDirectoryPanelSpacer);
            fBottomPanel.remove(fDirectoryPanel);
        }
    }

    public String getFileName() {
        if (filenameTextField != null) { return filenameTextField.getText(); }
        return null;
    }

    public String getDirectoryName() {
        // PENDING(jeff) - get the name from the directory combobox
        return null;
    }

    public void setFileName(final String filename) {
        if (filenameTextField != null) {
            filenameTextField.setText(filename);
        }
    }

    public void setDirectoryName(final String dirname) {
        // PENDING(jeff) - set the name in the directory combobox
    }

    public void rescanCurrentDirectory(final JFileChooser fc) {
        getModel().invalidateFileCache();
        getModel().validateFileCache();
    }

    public void ensureFileIsVisible(final JFileChooser fc, final File f) {
        if (f == null) {
            fFileList.requestFocusInWindow();
            fFileList.ensureIndexIsVisible(-1);
            return;
        }

        getModel().runWhenDone(new Runnable() {
            public void run() {
                fFileList.requestFocusInWindow();
                fFileList.ensureIndexIsVisible(getModel().indexOf(f));
            }
        });
    }

    public JFileChooser getFileChooser() {
        return filechooser;
    }

    public JPanel getAccessoryPanel() {
        return accessoryPanel;
    }

    protected JButton getApproveButton(final JFileChooser fc) {
        return fApproveButton;
    }

    public int getApproveButtonMnemonic(final JFileChooser fc) {
        return fSubPanel.getApproveButtonMnemonic(fc);
    }

    public String getApproveButtonToolTipText(final JFileChooser fc) {
        return fSubPanel.getApproveButtonToolTipText(fc);
    }

    public String getApproveButtonText(final JFileChooser fc) {
        return fSubPanel.getApproveButtonText(fc);
    }

    protected String getCancelButtonToolTipText(final JFileChooser fc) {
        return fSubPanel.getCancelButtonToolTipText(fc);
    }

    // If the item's not selectable, it'll be visible but disabled in the list
    boolean isSelectableInList(final File f) {
        return fSubPanel.isSelectableInList(getFileChooser(), f);
    }

    // Is this a file that the JFileChooser wants?
    // Directories can be selected in the list regardless of mode
    boolean isSelectableForMode(final JFileChooser fc, final File f) {
        if (f == null) return false;
        final int mode = fc.getFileSelectionMode();
        if (mode == JFileChooser.FILES_AND_DIRECTORIES) return true;
        boolean traversable = fc.isTraversable(f);
        if (mode == JFileChooser.DIRECTORIES_ONLY) return traversable;
        return !traversable;
    }

    // ********************************************
    // ************ Create Listeners **************
    // ********************************************

    // From Basic
    public ListSelectionListener createListSelectionListener(final JFileChooser fc) {
        return new SelectionListener();
    }

    protected class SelectionListener implements ListSelectionListener {
        public void valueChanged(final ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;

            File f = null;
            final int selectedRow = fFileList.getSelectedRow();
            final JFileChooser chooser = getFileChooser();
            boolean isSave = (chooser.getDialogType() == JFileChooser.SAVE_DIALOG);
            if (selectedRow >= 0) {
                f = (File)fFileList.getValueAt(selectedRow, 0);
            }

            // Save dialog lists can't be multi select, because all we're selecting is the next folder to open
            selectionInProgress = true;
            if (!isSave && chooser.isMultiSelectionEnabled()) {
                final int[] rows = fFileList.getSelectedRows();
                int selectableCount = 0;
                // Double-check that all the list selections are valid for this mode
                // Directories can be selected in the list regardless of mode
                if (rows.length > 0) {
                    for (final int element : rows) {
                        if (isSelectableForMode(chooser, (File)fFileList.getValueAt(element, 0))) selectableCount++;
                    }
                }
                if (selectableCount > 0) {
                    final File[] files = new File[selectableCount];
                    for (int i = 0, si = 0; i < rows.length; i++) {
                        f = (File)fFileList.getValueAt(rows[i], 0);
                        if (isSelectableForMode(chooser, f)) {
                            if (fileView.isAlias(f)) {
                                f = fileView.resolveAlias(f);
                            }
                            files[si++] = f;
                        }
                    }
                    chooser.setSelectedFiles(files);
                } else {
                    chooser.setSelectedFiles(null);
                }
            } else {
                chooser.setSelectedFiles(null);
                chooser.setSelectedFile(f);
            }
            selectionInProgress = false;
        }
    }

    // When the Save textfield has the focus, the button should say "Save"
    // Otherwise, it depends on the list selection
    protected class SaveTextFocusListener implements FocusListener {
        public void focusGained(final FocusEvent e) {
            updateButtonState(getFileChooser());
        }

        // Do nothing, we might be losing focus due to window deactivation
        public void focusLost(final FocusEvent e) {

        }
    }

    // When the Save textfield is empty and the button says "Save", it should be disabled
    // Otherwise, it depends on the list selection
    protected class SaveTextDocumentListener implements DocumentListener {
        public void insertUpdate(final DocumentEvent e) {
            textChanged();
        }

        public void removeUpdate(final DocumentEvent e) {
            textChanged();
        }

        public void changedUpdate(final DocumentEvent e) {

        }

        void textChanged() {
            updateButtonState(getFileChooser());
        }
    }

    // Opens the File object if it's a traversable directory
    protected boolean openDirectory(final File f) {
        if (getFileChooser().isTraversable(f)) {
            fFileList.clearSelection();
            // Resolve any aliases
            final File original = fileView.resolveAlias(f);
            getFileChooser().setCurrentDirectory(original);
            updateButtonState(getFileChooser());
            return true;
        }
        return false;
    }

    // From Basic
    protected class DoubleClickListener extends MouseAdapter {
        JTableExtension list;

        public DoubleClickListener(final JTableExtension list) {
            this.list = list;
        }

        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() != 2) return;

            final int index = list.locationToIndex(e.getPoint());
            if (index < 0) return;

            final File f = (File)((AquaFileSystemModel)list.getModel()).getElementAt(index);
            if (openDirectory(f)) return;

            if (!isSelectableInList(f)) return;
            getFileChooser().approveSelection();
        }
    }

    protected MouseListener createDoubleClickListener(final JFileChooser fc, final JTableExtension list) {
        return new DoubleClickListener(list);
    }

    // listens for drag events onto the JFileChooser and sets the selected file or directory
    class DnDHandler extends DropTargetAdapter {
        public void dragEnter(final DropTargetDragEvent dtde) {
            tryToAcceptDrag(dtde);
        }

        public void dragOver(final DropTargetDragEvent dtde) {
            tryToAcceptDrag(dtde);
        }

        public void dropActionChanged(final DropTargetDragEvent dtde) {
            tryToAcceptDrag(dtde);
        }

        public void drop(final DropTargetDropEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                handleFileDropEvent(dtde);
                return;
            }

            if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                handleStringDropEvent(dtde);
                return;
            }
        }

        protected void tryToAcceptDrag(final DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
                return;
            }

            dtde.rejectDrag();
        }

        protected void handleFileDropEvent(final DropTargetDropEvent dtde) {
            dtde.acceptDrop(dtde.getDropAction());
            final Transferable transferable = dtde.getTransferable();

            try {
                @SuppressWarnings("unchecked")
                final java.util.List<File> fileList = (java.util.List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
                dropFiles(fileList.toArray(new File[fileList.size()]));
                dtde.dropComplete(true);
            } catch (final Exception e) {
                dtde.dropComplete(false);
            }
        }

        protected void handleStringDropEvent(final DropTargetDropEvent dtde) {
            dtde.acceptDrop(dtde.getDropAction());
            final Transferable transferable = dtde.getTransferable();

            final String stringData;
            try {
                stringData = (String)transferable.getTransferData(DataFlavor.stringFlavor);
            } catch (final Exception e) {
                dtde.dropComplete(false);
                return;
            }

            try {
                final File fileAsPath = new File(stringData);
                if (fileAsPath.exists()) {
                    dropFiles(new File[] {fileAsPath});
                    dtde.dropComplete(true);
                    return;
                }
            } catch (final Exception e) {
                // try again
            }

            try {
                final File fileAsURI = new File(new URI(stringData));
                if (fileAsURI.exists()) {
                    dropFiles(new File[] {fileAsURI});
                    dtde.dropComplete(true);
                    return;
                }
            } catch (final Exception e) {
                // nothing more to do
            }

            dtde.dropComplete(false);
        }

        protected void dropFiles(final File[] files) {
            final JFileChooser jfc = getFileChooser();

            if (files.length == 1) {
                if (files[0].isDirectory()) {
                    jfc.setCurrentDirectory(files[0]);
                    return;
                }

                if (!isSelectableForMode(jfc, files[0])) {
                    return;
                }
            }

            jfc.setSelectedFiles(files);
            for (final File file : files) {
                jfc.ensureFileIsVisible(file);
            }
            getModel().runWhenDone(new Runnable() {
                public void run() {
                    final AquaFileSystemModel fileSystemModel = getModel();
                    for (final File element : files) {
                        final int index = fileSystemModel.indexOf(element);
                        if (index >= 0) fFileList.addRowSelectionInterval(index, index);
                    }
                }
            });
        }
    }

    // FileChooser UI PLAF methods

    /**
     * Returns the default accept all file filter
     */
    public FileFilter getAcceptAllFileFilter(final JFileChooser fc) {
        return acceptAllFileFilter;
    }

    public FileView getFileView(final JFileChooser fc) {
        return fileView;
    }

    /**
     * Returns the title of this dialog
     */
    public String getDialogTitle(final JFileChooser fc) {
        if (fc.getDialogTitle() == null) {
            if (getFileChooser().getDialogType() == JFileChooser.OPEN_DIALOG) {
                return openTitleText;
            } else if (getFileChooser().getDialogType() == JFileChooser.SAVE_DIALOG) { return saveTitleText; }
        }
        return fc.getDialogTitle();
    }

    // Utility to get the first selected item regardless of whether we're single or multi select
    File getFirstSelectedItem() {
        // Get the selected item
        File selectedFile = null;
        final int index = fFileList.getSelectedRow();
        if (index >= 0) {
            selectedFile = (File)((AquaFileSystemModel)fFileList.getModel()).getElementAt(index);
        }
        return selectedFile;
    }

    // Make a file from the filename
    File makeFile(final JFileChooser fc, final String filename) {
        File selectedFile = null;
        // whitespace is legal on Macs, even on beginning and end of filename
        if (filename != null && !filename.isEmpty()) {
            final FileSystemView fs = fc.getFileSystemView();
            selectedFile = fs.createFileObject(filename);
            if (!selectedFile.isAbsolute()) {
                selectedFile = fs.createFileObject(fc.getCurrentDirectory(), filename);
            }
        }
        return selectedFile;
    }

    // Utility to tell if the textfield has anything in it
    boolean textfieldIsValid() {
        final String s = getFileName();
        return (s != null && !s.isEmpty());
    }

    // Action to attach to the file list so we can override the default action
    // of the table for the return key, which is to select the next line.
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DefaultButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final JRootPane root = AquaFileChooserUI.this.getFileChooser().getRootPane();
            final JFileChooser fc = AquaFileChooserUI.this.getFileChooser();
            final JButton owner = root.getDefaultButton();
            if (owner != null && SwingUtilities.getRootPane(owner) == root && owner.isEnabled()) {
                owner.doClick(20);
            } else if (!fc.getControlButtonsAreShown()) {
                final JButton defaultButton = AquaFileChooserUI.this.fSubPanel.getDefaultButton(fc);

                if (defaultButton != null) {
                    defaultButton.doClick(20);
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }

    /**
     * Creates a new folder.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class NewFolderAction extends AbstractAction {
        protected NewFolderAction() {
            super(newFolderAccessibleName);
        }

        // Muchlike showInputDialog, but we give it options instead of selectionValues
        private Object showNewFolderDialog(final Component parentComponent, final Object message, final String title, final int messageType, final Icon icon, final Object[] options, final Object initialSelectionValue) {
            final JOptionPane pane = new JOptionPane(message, messageType, JOptionPane.OK_CANCEL_OPTION, icon, options, null);

            pane.setWantsInput(true);
            pane.setInitialSelectionValue(initialSelectionValue);

            final JDialog dialog = pane.createDialog(parentComponent, title);

            pane.selectInitialValue();
            dialog.setVisible(true);
            dialog.dispose();

            final Object value = pane.getValue();

            if (value == null || value.equals(cancelButtonText)
                    || value.equals(JOptionPane.CLOSED_OPTION)) {
                return null;
            }
            return pane.getInputValue();
        }

        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fc = getFileChooser();
            final File currentDirectory = fc.getCurrentDirectory();
            File newFolder = null;
            final String[] options = {createButtonText, cancelButtonText};
            final String filename = (String)showNewFolderDialog(fc, //parentComponent
                    newFolderDialogPrompt, // message
                    newFolderTitleText, // title
                    JOptionPane.PLAIN_MESSAGE, // messageType
                    null, // icon
                    options, // selectionValues
                    newFolderDefaultName); // initialSelectionValue

            if (filename != null) {
                try {
                    newFolder = fc.getFileSystemView().createFileObject(currentDirectory, filename);
                    if (newFolder.exists()) {
                        JOptionPane.showMessageDialog(fc, newFolderExistsErrorText, "", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    newFolder.mkdirs();
                } catch(final Exception exc) {
                    JOptionPane.showMessageDialog(fc, newFolderErrorText, "", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                openDirectory(newFolder);
            }
        }
    }

    /**
     * Responds to an Open, Save, or Choose request
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class ApproveSelectionAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            fSubPanel.approveSelection(getFileChooser());
        }
    }

    /**
     * Responds to an OpenDirectory request
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class OpenSelectionAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final int index = fFileList.getSelectedRow();
            if (index >= 0) {
                final File selectedFile = (File)((AquaFileSystemModel)fFileList.getModel()).getElementAt(index);
                if (selectedFile != null) openDirectory(selectedFile);
            }
        }
    }

    /**
     * Responds to a cancel request.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class CancelSelectionAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            getFileChooser().cancelSelection();
        }

        public boolean isEnabled() {
            return getFileChooser().isEnabled();
        }
    }

    /**
     * Rescans the files in the current directory
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class UpdateAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fc = getFileChooser();
            fc.setCurrentDirectory(fc.getFileSystemView().createFileObject(getDirectoryName()));
            fc.rescanCurrentDirectory();
        }
    }

    // *****************************************
    // ***** default AcceptAll file filter *****
    // *****************************************
    private static class AcceptAllFileFilter extends FileFilter {
        public AcceptAllFileFilter() {
        }

        public boolean accept(final File f) {
            return true;
        }

        public String getDescription() {
            return UIManager.getString("FileChooser.acceptAllFileFilterText");
        }
    }

    // Penultimate superclass is JLabel
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class MacFCTableCellRenderer extends DefaultTableCellRenderer {
        boolean fIsSelected = false;

        public MacFCTableCellRenderer(final Font f) {
            super();
            setFont(f);
            setIconTextGap(10);
        }

        public Component getTableCellRendererComponent(final JTable list, final Object value, final boolean isSelected, final boolean cellHasFocus, final int index, final int col) {
            super.getTableCellRendererComponent(list, value, isSelected, false, index, col); // No focus border, thanks
            fIsSelected = isSelected;
            return this;
        }

        public boolean isSelected() {
            return fIsSelected && isEnabled();
        }

        protected String layoutCL(final JLabel label, final FontMetrics fontMetrics, final String text, final Icon icon, final Rectangle viewR, final Rectangle iconR, final Rectangle textR) {
            return SwingUtilities.layoutCompoundLabel(label, fontMetrics, text, icon, label.getVerticalAlignment(), label.getHorizontalAlignment(), label.getVerticalTextPosition(), label.getHorizontalTextPosition(), viewR, iconR, textR, label.getIconTextGap());
        }

        protected void paintComponent(final Graphics g) {
            final String text = getText();
            Icon icon = getIcon();
            if (icon != null && !isEnabled()) {
                final Icon disabledIcon = getDisabledIcon();
                if (disabledIcon != null) icon = disabledIcon;
            }

            if ((icon == null) && (text == null)) { return; }

            // from ComponentUI update
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            // from BasicLabelUI paint
            final FontMetrics fm = g.getFontMetrics();
            Insets paintViewInsets = getInsets(null);
            paintViewInsets.left += 10;

            Rectangle paintViewR = new Rectangle(paintViewInsets.left, paintViewInsets.top, getWidth() - (paintViewInsets.left + paintViewInsets.right), getHeight() - (paintViewInsets.top + paintViewInsets.bottom));

            Rectangle paintIconR = new Rectangle();
            Rectangle paintTextR = new Rectangle();

            final String clippedText = layoutCL(this, fm, text, icon, paintViewR, paintIconR, paintTextR);

            if (icon != null) {
                icon.paintIcon(this, g, paintIconR.x + 5, paintIconR.y);
            }

            if (text != null) {
                final int textX = paintTextR.x;
                final int textY = paintTextR.y + fm.getAscent() + 1;
                if (isEnabled()) {
                    // Color background = fIsSelected ? getForeground() : getBackground();
                    final Color background = getBackground();

                    g.setColor(background);
                    g.fillRect(textX - 1, paintTextR.y, paintTextR.width + 2, fm.getAscent() + 2);

                    g.setColor(getForeground());
                    SwingUtilities2.drawString(filechooser, g, clippedText, textX, textY);
                } else {
                    final Color background = getBackground();
                    g.setColor(background);
                    g.fillRect(textX - 1, paintTextR.y, paintTextR.width + 2, fm.getAscent() + 2);

                    g.setColor(background.brighter());
                    SwingUtilities2.drawString(filechooser, g, clippedText, textX, textY);
                    g.setColor(background.darker());
                    SwingUtilities2.drawString(filechooser, g, clippedText, textX + 1, textY + 1);
                }
            }
        }

    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class FileRenderer extends MacFCTableCellRenderer {
        public FileRenderer(final Font f) {
            super(f);
        }

        public Component getTableCellRendererComponent(final JTable list,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean cellHasFocus,
                                                       final int index,
                                                       final int col) {
            super.getTableCellRendererComponent(list, value, isSelected, false,
                                                index,
                                                col); // No focus border, thanks
            final File file = (File)value;
            final JFileChooser fc = getFileChooser();
            setText(fc.getName(file));
            setIcon(fc.getIcon(file));
            setEnabled(isSelectableInList(file));
            return this;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DateRenderer extends MacFCTableCellRenderer {
        public DateRenderer(final Font f) {
            super(f);
        }

        public Component getTableCellRendererComponent(final JTable list,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean cellHasFocus,
                                                       final int index,
                                                       final int col) {
            super.getTableCellRendererComponent(list, value, isSelected, false,
                                                index, col);
            final File file = (File)fFileList.getValueAt(index, 0);
            setEnabled(isSelectableInList(file));
            final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
            final Date date = (Date)value;

            if (date != null) {
                setText(formatter.format(date));
            } else {
                setText("");
            }

            return this;
        }
    }

    @Override
    public Dimension getPreferredSize(final JComponent c) {
        return new Dimension(PREF_WIDTH, PREF_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize(final JComponent c) {
        return new Dimension(MIN_WIDTH, MIN_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @SuppressWarnings("serial") // anonymous class
    protected ListCellRenderer<File> createDirectoryComboBoxRenderer(final JFileChooser fc) {
        return new AquaComboBoxRendererInternal<File>(directoryComboBox) {
            public Component getListCellRendererComponent(final JList<? extends File> list,
                                                          final File directory,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, directory, index, isSelected, cellHasFocus);
                if (directory == null) {
                    setText("");
                    return this;
                }

                final JFileChooser chooser = getFileChooser();
                setText(chooser.getName(directory));
                setIcon(chooser.getIcon(directory));
                return this;
            }
        };
    }

    //
    // DataModel for DirectoryComboxbox
    //
    protected DirectoryComboBoxModel createDirectoryComboBoxModel(final JFileChooser fc) {
        return new DirectoryComboBoxModel();
    }

    /**
     * Data model for a type-face selection combo-box.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class DirectoryComboBoxModel extends AbstractListModel<File> implements ComboBoxModel<File> {
        Vector<File> fDirectories = new Vector<File>();
        int topIndex = -1;
        int fPathCount = 0;

        File fSelectedDirectory = null;

        public DirectoryComboBoxModel() {
            super();
            // Add the current directory to the model, and make it the
            // selectedDirectory
            addItem(getFileChooser().getCurrentDirectory());
        }

        /**
         * Removes the selected directory, and clears out the
         * path file entries leading up to that directory.
         */
        private void removeSelectedDirectory() {
            fDirectories.removeAllElements();
            fPathCount = 0;
            fSelectedDirectory = null;
            // dump();
        }

        /**
         * Adds the directory to the model and sets it to be selected,
         * additionally clears out the previous selected directory and
         * the paths leading up to it, if any.
         */
        void addItem(final File directory) {
            if (directory == null) { return; }
            if (fSelectedDirectory != null) {
                removeSelectedDirectory();
            }

            // create File instances of each directory leading up to the top
            File f = directory.getAbsoluteFile();
            final Vector<File> path = new Vector<File>(10);
            while (f.getParent() != null) {
                path.addElement(f);
                f = getFileChooser().getFileSystemView().createFileObject(f.getParent());
            };

            // Add root file (the desktop) to the model
            final File[] roots = getFileChooser().getFileSystemView().getRoots();
            for (final File element : roots) {
                path.addElement(element);
            }
            fPathCount = path.size();

            // insert all the path fDirectories leading up to the
            // selected directory in reverse order (current directory at top)
            for (int i = 0; i < path.size(); i++) {
                fDirectories.addElement(path.elementAt(i));
            }

            setSelectedItem(fDirectories.elementAt(0));

            // dump();
        }

        public void setSelectedItem(final Object selectedDirectory) {
            this.fSelectedDirectory = (File)selectedDirectory;
            fireContentsChanged(this, -1, -1);
        }

        public Object getSelectedItem() {
            return fSelectedDirectory;
        }

        public int getSize() {
            return fDirectories.size();
        }

        public File getElementAt(final int index) {
            return fDirectories.elementAt(index);
        }
    }

    //
    // Renderer for Types ComboBox
    //
    @SuppressWarnings("serial") // anonymous class
    protected ListCellRenderer<FileFilter> createFilterComboBoxRenderer() {
        return new AquaComboBoxRendererInternal<FileFilter>(filterComboBox) {
            public Component getListCellRendererComponent(final JList<? extends FileFilter> list,
                                                          final FileFilter filter,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, filter, index, isSelected, cellHasFocus);
                if (filter != null) setText(filter.getDescription());
                return this;
            }
        };
    }

    //
    // DataModel for Types Comboxbox
    //
    protected FilterComboBoxModel createFilterComboBoxModel() {
        return new FilterComboBoxModel();
    }

    /**
     * Data model for a type-face selection combo-box.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class FilterComboBoxModel extends AbstractListModel<FileFilter> implements ComboBoxModel<FileFilter>,
            PropertyChangeListener {
        protected FileFilter[] filters;
        Object oldFileFilter = getFileChooser().getFileFilter();

        protected FilterComboBoxModel() {
            super();
            filters = getFileChooser().getChoosableFileFilters();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if(prop == JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY) {
                filters = (FileFilter[]) e.getNewValue();
                fireContentsChanged(this, -1, -1);
            } else if (prop == JFileChooser.FILE_FILTER_CHANGED_PROPERTY) {
                setSelectedItem(e.getNewValue());
            }
        }

        public void setSelectedItem(Object filter) {
            if (filter != null && !isSelectedFileFilterInModel(filter)) {
                oldFileFilter = filter;
                getFileChooser().setFileFilter((FileFilter) filter);
                fireContentsChanged(this, -1, -1);
            }
        }

        private boolean isSelectedFileFilterInModel(Object filter) {
            return Objects.equals(filter, oldFileFilter);
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
                if(found == false) {
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

    private boolean containsFileFilter(Object fileFilter) {
        return Objects.equals(fileFilter, getFileChooser().getFileFilter());
    }

    /**
     * Acts when FilterComboBox has changed the selected item.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class FilterComboBoxAction extends AbstractAction {
        protected FilterComboBoxAction() {
            super("FilterComboBoxAction");
        }

        public void actionPerformed(final ActionEvent e) {
            Object selectedFilter = filterComboBox.getSelectedItem();
            if (!containsFileFilter(selectedFilter)) {
                getFileChooser().setFileFilter((FileFilter) selectedFilter);
            }
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

        public void actionPerformed(final ActionEvent e) {
            getFileChooser().setCurrentDirectory((File)directoryComboBox.getSelectedItem());
        }
    }

    // Sorting Table operations
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class JSortingTableHeader extends JTableHeader {
        public JSortingTableHeader(final TableColumnModel cm) {
            super(cm);
            setReorderingAllowed(true); // This causes mousePress to call setDraggedColumn
        }

        // One sort state for each column.  Both are ascending by default
        final boolean[] fSortAscending = {true, true};

        // Instead of dragging, it selects which one to sort by
        public void setDraggedColumn(final TableColumn aColumn) {
            if (aColumn != null) {
                final int colIndex = aColumn.getModelIndex();
                if (colIndex != fSortColumn) {
                    filechooser.firePropertyChange(AquaFileSystemModel.SORT_BY_CHANGED, fSortColumn, colIndex);
                    fSortColumn = colIndex;
                } else {
                    fSortAscending[colIndex] = !fSortAscending[colIndex];
                    filechooser.firePropertyChange(AquaFileSystemModel.SORT_ASCENDING_CHANGED, !fSortAscending[colIndex], fSortAscending[colIndex]);
                }
                // Need to repaint the highlighted column.
                repaint();
            }
        }

        // This stops mouseDrags from moving the column
        public TableColumn getDraggedColumn() {
            return null;
        }

        protected TableCellRenderer createDefaultRenderer() {
            final DefaultTableCellRenderer label = new AquaTableCellRenderer();
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }

        @SuppressWarnings("serial") // Superclass is not serializable across versions
        class AquaTableCellRenderer extends DefaultTableCellRenderer implements UIResource {
            public Component getTableCellRendererComponent(final JTable localTable, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                if (localTable != null) {
                    final JTableHeader header = localTable.getTableHeader();
                    if (header != null) {
                        setForeground(header.getForeground());
                        setBackground(header.getBackground());
                        setFont(UIManager.getFont("TableHeader.font"));
                    }
                }

                setText((value == null) ? "" : value.toString());

                // Modify the table "border" to draw smaller, and with the titles in the right position
                // and sort indicators, just like an NSSave/Open panel.
                final AquaTableHeaderBorder cellBorder = AquaTableHeaderBorder.getListHeaderBorder();
                cellBorder.setSelected(column == fSortColumn);
                final int horizontalShift = (column == 0 ? 35 : 10);
                cellBorder.setHorizontalShift(horizontalShift);

                if (column == fSortColumn) {
                    cellBorder.setSortOrder(fSortAscending[column] ? AquaTableHeaderBorder.SORT_ASCENDING : AquaTableHeaderBorder.SORT_DECENDING);
                } else {
                    cellBorder.setSortOrder(AquaTableHeaderBorder.SORT_NONE);
                }
                setBorder(cellBorder);
                return this;
            }
        }
    }

    public void installComponents(final JFileChooser fc) {
        JPanel tPanel; // temp panel
        // set to a Y BoxLayout. The chooser will be laid out top to bottom.
        fc.setLayout(new BoxLayout(fc, BoxLayout.Y_AXIS));
        fc.add(Box.createRigidArea(vstrut10));

        // construct the top panel

        final JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        fc.add(topPanel);
        fc.add(Box.createRigidArea(vstrut10));

        // Add the textfield pane

        fTextfieldPanel = new JPanel();
        fTextfieldPanel.setLayout(new BorderLayout());
        // setBottomPanelForMode will make this visible if we need it
        fTextfieldPanel.setVisible(false);
        topPanel.add(fTextfieldPanel);

        tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.Y_AXIS));
        final JPanel labelArea = new JPanel();
        labelArea.setLayout(new FlowLayout(FlowLayout.CENTER));
        fTextFieldLabel = new JLabel(fileNameLabelText);
        labelArea.add(fTextFieldLabel);

        // text field
        filenameTextField = new JTextField();
        fTextFieldLabel.setLabelFor(filenameTextField);
        filenameTextField.addActionListener(getAction(kOpen));
        filenameTextField.addFocusListener(new SaveTextFocusListener());
        final Dimension minSize = filenameTextField.getMinimumSize();
        Dimension d = new Dimension(250, (int)minSize.getHeight());
        filenameTextField.setPreferredSize(d);
        filenameTextField.setMaximumSize(d);
        labelArea.add(filenameTextField);
        final File f = fc.getSelectedFile();
        if (f != null) {
            setFileName(fc.getName(f));
        } else if (fc.getDialogType() == JFileChooser.SAVE_DIALOG) {
            setFileName(newFileDefaultName);
        }

        tPanel.add(labelArea);
        // separator line
        @SuppressWarnings("serial") // anonymous class
        final JSeparator sep = new JSeparator(){
            public Dimension getPreferredSize() {
                return new Dimension(((JComponent)getParent()).getWidth(), 3);
            }
        };
        tPanel.add(Box.createRigidArea(new Dimension(1, 8)));
        tPanel.add(sep);
        tPanel.add(Box.createRigidArea(new Dimension(1, 7)));
        fTextfieldPanel.add(tPanel, BorderLayout.CENTER);

        // DirectoryComboBox, left-justified, 200x20 not including drop shadow
        directoryComboBox = new JComboBox<>();
        directoryComboBox.putClientProperty("JComboBox.lightweightKeyboardNavigation", "Lightweight");
        fDirectoryComboBoxModel = createDirectoryComboBoxModel(fc);
        directoryComboBox.setModel(fDirectoryComboBoxModel);
        directoryComboBox.addActionListener(directoryComboBoxAction);
        directoryComboBox.setRenderer(createDirectoryComboBoxRenderer(fc));
        directoryComboBox.setToolTipText(directoryComboBoxToolTipText);
        d = new Dimension(250, (int)directoryComboBox.getMinimumSize().getHeight());
        directoryComboBox.setPreferredSize(d);
        directoryComboBox.setMaximumSize(d);
        topPanel.add(directoryComboBox);

        // ************************************** //
        // ** Add the directory/Accessory pane ** //
        // ************************************** //
        final JPanel centerPanel = new JPanel(new BorderLayout());
        fc.add(centerPanel);

        // Accessory pane (equiv to Preview pane in NavServices)
        final JComponent accessory = fc.getAccessory();
        if (accessory != null) {
            getAccessoryPanel().add(accessory);
        }
        centerPanel.add(getAccessoryPanel(), BorderLayout.LINE_START);

        // Directory list(table), right-justified, resizable
        final JPanel p = createList(fc);
        p.setMinimumSize(LIST_MIN_SIZE);
        centerPanel.add(p, BorderLayout.CENTER);

        // ********************************** //
        // **** Construct the bottom panel ** //
        // ********************************** //
        fBottomPanel = new JPanel();
        fBottomPanel.setLayout(new BoxLayout(fBottomPanel, BoxLayout.Y_AXIS));
        fc.add(fBottomPanel);

        // Filter label and combobox.
        // I know it's unMaclike, but the filter goes on Directory_only too.
        tPanel = new JPanel();
        tPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        tPanel.setBorder(AquaGroupBorder.getTitlelessBorder());
        final JLabel formatLabel = new JLabel(filesOfTypeLabelText);
        tPanel.add(formatLabel);

        // Combobox
        filterComboBoxModel = createFilterComboBoxModel();
        fc.addPropertyChangeListener(filterComboBoxModel);
        filterComboBox = new JComboBox<>(filterComboBoxModel);
        formatLabel.setLabelFor(filterComboBox);
        filterComboBox.setRenderer(createFilterComboBoxRenderer());
        d = new Dimension(220, (int)filterComboBox.getMinimumSize().getHeight());
        filterComboBox.setPreferredSize(d);
        filterComboBox.setMaximumSize(d);
        filterComboBox.addActionListener(filterComboBoxAction);
        filterComboBox.setOpaque(false);
        tPanel.add(filterComboBox);

        fBottomPanel.add(tPanel);

        // fDirectoryPanel: New, Open, Cancel, Approve buttons, right-justified, 82x22
        // (sometimes the NewFolder and OpenFolder buttons are invisible)
        fDirectoryPanel = new JPanel();
        fDirectoryPanel.setLayout(new BoxLayout(fDirectoryPanel, BoxLayout.PAGE_AXIS));
        JPanel directoryPanel = new JPanel(new BorderLayout());
        JPanel newFolderButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        newFolderButtonPanel.add(Box.createHorizontalStrut(20));
        fNewFolderButton = createNewFolderButton(); // Because we hide it depending on style
        newFolderButtonPanel.add(fNewFolderButton);
        directoryPanel.add(newFolderButtonPanel, BorderLayout.LINE_START);
        JPanel approveCancelButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        fOpenButton = createButton(kOpenDirectory, openButtonText);
        approveCancelButtonPanel.add(fOpenButton);
        approveCancelButtonPanel.add(Box.createHorizontalStrut(8));
        fCancelButton = createButton(kCancel, null);
        approveCancelButtonPanel.add(fCancelButton);
        approveCancelButtonPanel.add(Box.createHorizontalStrut(8));
        // The ApproveSelection button
        fApproveButton = new JButton();
        fApproveButton.addActionListener(fApproveSelectionAction);
        approveCancelButtonPanel.add(fApproveButton);
        approveCancelButtonPanel.add(Box.createHorizontalStrut(20));
        directoryPanel.add(approveCancelButtonPanel, BorderLayout.LINE_END);
        fDirectoryPanel.add(Box.createVerticalStrut(5));
        fDirectoryPanel.add(directoryPanel);
        fDirectoryPanel.add(Box.createVerticalStrut(12));
        fDirectoryPanelSpacer = Box.createRigidArea(hstrut10);

        if (fc.getControlButtonsAreShown()) {
            fBottomPanel.add(fDirectoryPanelSpacer);
            fBottomPanel.add(fDirectoryPanel);
        }

        setBottomPanelForMode(fc); // updates ApproveButtonText etc

        // don't create til after the FCSubpanel and buttons are made
        filenameTextField.getDocument().addDocumentListener(new SaveTextDocumentListener());
    }

    void setDefaultButtonForMode(final JFileChooser fc) {
        final JButton defaultButton = fSubPanel.getDefaultButton(fc);
        final JRootPane root = defaultButton.getRootPane();
        if (root != null) {
            root.setDefaultButton(defaultButton);
        }
    }

    // Macs start with their focus in text areas if they have them,
    // lists otherwise (the other plafs start with the focus on approveButton)
    void setFocusForMode(final JFileChooser fc) {
        final JComponent focusComponent = fSubPanel.getFocusComponent(fc);
        if (focusComponent != null) {
            focusComponent.requestFocus();
        }
    }

    // Enable/disable buttons as needed for the current selection/focus state
    void updateButtonState(final JFileChooser fc) {
        fSubPanel.updateButtonState(fc, getFirstSelectedItem());
        updateApproveButton(fc);
    }

    void updateApproveButton(final JFileChooser chooser) {
        fApproveButton.setText(getApproveButtonText(chooser));
        fApproveButton.setToolTipText(getApproveButtonToolTipText(chooser));
        fApproveButton.setMnemonic(getApproveButtonMnemonic(chooser));
        fCancelButton.setToolTipText(getCancelButtonToolTipText(chooser));
    }

    // Lazy-init the subpanels
    synchronized FCSubpanel getSaveFilePanel() {
        if (fSaveFilePanel == null) fSaveFilePanel = new SaveFilePanel();
        return fSaveFilePanel;
    }

    synchronized FCSubpanel getOpenFilePanel() {
        if (fOpenFilePanel == null) fOpenFilePanel = new OpenFilePanel();
        return fOpenFilePanel;
    }

    synchronized FCSubpanel getOpenDirOrAnyPanel() {
        if (fOpenDirOrAnyPanel == null) fOpenDirOrAnyPanel = new OpenDirOrAnyPanel();
        return fOpenDirOrAnyPanel;
    }

    synchronized FCSubpanel getCustomFilePanel() {
        if (fCustomFilePanel == null) fCustomFilePanel = new CustomFilePanel();
        return fCustomFilePanel;
    }

    synchronized FCSubpanel getCustomDirOrAnyPanel() {
        if (fCustomDirOrAnyPanel == null) fCustomDirOrAnyPanel = new CustomDirOrAnyPanel();
        return fCustomDirOrAnyPanel;
    }

    void setBottomPanelForMode(final JFileChooser fc) {
        if (fc.getDialogType() == JFileChooser.SAVE_DIALOG) fSubPanel = getSaveFilePanel();
        else if (fc.getDialogType() == JFileChooser.OPEN_DIALOG) {
            if (fc.getFileSelectionMode() == JFileChooser.FILES_ONLY) fSubPanel = getOpenFilePanel();
            else fSubPanel = getOpenDirOrAnyPanel();
        } else if (fc.getDialogType() == JFileChooser.CUSTOM_DIALOG) {
            if (fc.getFileSelectionMode() == JFileChooser.FILES_ONLY) fSubPanel = getCustomFilePanel();
            else fSubPanel = getCustomDirOrAnyPanel();
        }

        fSubPanel.installPanel(fc, true);
        updateApproveButton(fc);
        updateButtonState(fc);
        setDefaultButtonForMode(fc);
        setFocusForMode(fc);
        fc.invalidate();
    }

    // fTextfieldPanel and fDirectoryPanel both have NewFolder buttons; only one should be visible at a time
    JButton createNewFolderButton() {
        final JButton b = new JButton(newFolderButtonText);
        b.setToolTipText(newFolderToolTipText);
        b.getAccessibleContext().setAccessibleName(newFolderAccessibleName);
        b.setHorizontalTextPosition(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setAlignmentY(Component.CENTER_ALIGNMENT);
        b.addActionListener(getAction(kNewFolder));
        return b;
    }

    JButton createButton(final int which, String label) {
        if (label == null) label = UIManager.getString(sDataPrefix + sButtonKinds[which] + sButtonData[0]);
        final int mnemonic = UIManager.getInt(sDataPrefix + sButtonKinds[which] + sButtonData[1]);
        final String tipText = UIManager.getString(sDataPrefix + sButtonKinds[which] + sButtonData[2]);
        final JButton b = new JButton(label);
        b.setMnemonic(mnemonic);
        b.setToolTipText(tipText);
        b.addActionListener(getAction(which));
        return b;
    }

    AbstractAction getAction(final int which) {
        return fButtonActions[which];
    }

    public void uninstallComponents(final JFileChooser fc) {
        // AquaButtonUI install some listeners to all parents, which means that
        // we need to uninstall UI here to remove those listeners, because after
        // we remove them from FileChooser we lost the latest reference to them,
        // and our standard uninstallUI machinery will not call them.
        fApproveButton.getUI().uninstallUI(fApproveButton);
        fOpenButton.getUI().uninstallUI(fOpenButton);
        fNewFolderButton.getUI().uninstallUI(fNewFolderButton);
        fCancelButton.getUI().uninstallUI(fCancelButton);
        directoryComboBox.getUI().uninstallUI(directoryComboBox);
        filterComboBox.getUI().uninstallUI(filterComboBox);
    }

    // Consistent with the AppKit NSSavePanel, clicks on a file (not a directory) should populate the text field
    // with that file's display name.
    protected class FileListMouseListener extends MouseAdapter {
        public void mouseClicked(final MouseEvent e) {
            final Point p = e.getPoint();
            final int row = fFileList.rowAtPoint(p);
            final int column = fFileList.columnAtPoint(p);

            // The autoscroller can generate drag events outside the Table's range.
            if ((column == -1) || (row == -1)) { return; }

            final File clickedFile = (File)(fFileList.getValueAt(row, 0));

            // rdar://problem/3734130 -- don't populate the text field if this file isn't selectable in this mode.
            if (isSelectableForMode(getFileChooser(), clickedFile)) {
                // [3188387] Populate the file name field with the selected file name
                // [3484163] It should also use the display name, not the actual name.
                setFileName(fileView.getName(clickedFile));
            }
        }
    }

    protected JPanel createList(final JFileChooser fc) {
        // The first part is similar to MetalFileChooserUI.createList - same kind of listeners
        final JPanel p = new JPanel(new BorderLayout());
        fFileList = new JTableExtension();
        fFileList.setToolTipText(null); // Workaround for 2487689
        fFileList.addMouseListener(new FileListMouseListener());
        model = new AquaFileSystemModel(fc, fFileList, fColumnNames);
        final MacListSelectionModel listSelectionModel = new MacListSelectionModel(model);

        if (getFileChooser().isMultiSelectionEnabled()) {
            listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        fFileList.setModel(model);
        fFileList.setSelectionModel(listSelectionModel);
        fFileList.getSelectionModel().addListSelectionListener(createListSelectionListener(fc));

        // Now we're different, because we're a table, not a list
        fc.addPropertyChangeListener(model);
        fFileList.addFocusListener(new SaveTextFocusListener());
        final JTableHeader th = new JSortingTableHeader(fFileList.getColumnModel());
        fFileList.setTableHeader(th);
        fFileList.setRowMargin(0);
        fFileList.setIntercellSpacing(new Dimension(0, 1));
        fFileList.setShowVerticalLines(false);
        fFileList.setShowHorizontalLines(false);
        final Font f = fFileList.getFont(); //ThemeFont.GetThemeFont(AppearanceConstants.kThemeViewsFont);
        //fc.setFont(f);
        //fFileList.setFont(f);
        fFileList.setDefaultRenderer(File.class, new FileRenderer(f));
        fFileList.setDefaultRenderer(Date.class, new DateRenderer(f));
        final FontMetrics fm = fFileList.getFontMetrics(f);

        // Row height isn't based on the renderers.  It defaults to 16 so we have to set it
        fFileList.setRowHeight(Math.max(fm.getHeight(), fileIcon.getIconHeight() + 2));

        // Add a binding for the file list that triggers return and escape
        fFileList.registerKeyboardAction(new CancelSelectionAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
        // Add a binding for the file list that triggers the default button (see DefaultButtonAction)
        fFileList.registerKeyboardAction(new DefaultButtonAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        fFileList.setDropTarget(dragAndDropTarget);

        final JScrollPane scrollpane = new JScrollPane(fFileList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollpane.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        scrollpane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, new ScrollPaneCornerPanel());
        p.add(scrollpane, BorderLayout.CENTER);
        return p;
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class ScrollPaneCornerPanel extends JPanel {
        final Border border = UIManager.getBorder("TableHeader.cellBorder");

        protected void paintComponent(final Graphics g) {
            border.paintBorder(this, g, 0, 0, getWidth() + 1, getHeight());
        }
    }

    JComboBox<File> directoryComboBox;
    DirectoryComboBoxModel fDirectoryComboBoxModel;
    private final Action directoryComboBoxAction = new DirectoryComboBoxAction();

    JTextField filenameTextField;

    JTableExtension fFileList;

    private FilterComboBoxModel filterComboBoxModel;
    JComboBox<FileFilter> filterComboBox;
    private final Action filterComboBoxAction = new FilterComboBoxAction();

    private static final Dimension hstrut10 = new Dimension(10, 1);
    private static final Dimension vstrut10 = new Dimension(1, 10);

    private static final int PREF_WIDTH = 550;
    private static final int PREF_HEIGHT = 400;
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 250;
    private static final int LIST_MIN_WIDTH = 400;
    private static final int LIST_MIN_HEIGHT = 100;
    private static final Dimension LIST_MIN_SIZE = new Dimension(LIST_MIN_WIDTH, LIST_MIN_HEIGHT);

    static String fileNameLabelText = null;
    JLabel fTextFieldLabel = null;

    private static String filesOfTypeLabelText = null;

    private static String newFolderToolTipText = null;
    static String newFolderAccessibleName = null;

    private static final String[] fColumnNames = new String[2];

    JPanel fTextfieldPanel; // Filename textfield for Save or Custom
    private JPanel fDirectoryPanel; // NewFolder/OpenFolder/Cancel/Approve buttons
    private Component fDirectoryPanelSpacer;
    private JPanel fBottomPanel; // The panel that holds fDirectoryPanel and filterComboBox

    private FCSubpanel fSaveFilePanel = null;
    private FCSubpanel fOpenFilePanel = null;
    private FCSubpanel fOpenDirOrAnyPanel = null;
    private FCSubpanel fCustomFilePanel = null;
    private FCSubpanel fCustomDirOrAnyPanel = null;

    FCSubpanel fSubPanel = null; // Current FCSubpanel

    JButton fApproveButton; // mode-specific behavior is managed by FCSubpanel.approveSelection
    JButton fOpenButton; // for Directories
    JButton fNewFolderButton; // for fDirectoryPanel

    // ToolTip text varies with type of dialog
    private JButton fCancelButton;

    private final ApproveSelectionAction fApproveSelectionAction = new ApproveSelectionAction();
    protected int fSortColumn = 0;
    protected int fPackageIsTraversable = -1;
    protected int fApplicationIsTraversable = -1;

    protected static final int sGlobalPackageIsTraversable;
    protected static final int sGlobalApplicationIsTraversable;

    protected static final String PACKAGE_TRAVERSABLE_PROPERTY = "JFileChooser.packageIsTraversable";
    protected static final String APPLICATION_TRAVERSABLE_PROPERTY = "JFileChooser.appBundleIsTraversable";
    protected static final String[] sTraversableProperties = {"always", // Bundle is always traversable
            "never", // Bundle is never traversable
            "conditional"}; // Bundle is traversable on command click
    protected static final int kOpenAlways = 0, kOpenNever = 1, kOpenConditional = 2;

    AbstractAction[] fButtonActions = {fApproveSelectionAction, fApproveSelectionAction, new CancelSelectionAction(), new OpenSelectionAction(), null, new NewFolderAction()};

    static int parseTraversableProperty(final String s) {
        if (s == null) return -1;
        for (int i = 0; i < sTraversableProperties.length; i++) {
            if (s.equals(sTraversableProperties[i])) return i;
        }
        return -1;
    }

    static {
        Object o = UIManager.get(PACKAGE_TRAVERSABLE_PROPERTY);
        if (o != null && o instanceof String) sGlobalPackageIsTraversable = parseTraversableProperty((String)o);
        else sGlobalPackageIsTraversable = kOpenConditional;

        o = UIManager.get(APPLICATION_TRAVERSABLE_PROPERTY);
        if (o != null && o instanceof String) sGlobalApplicationIsTraversable = parseTraversableProperty((String)o);
        else sGlobalApplicationIsTraversable = kOpenConditional;
    }
    static final String sDataPrefix = "FileChooser.";
    static final String[] sButtonKinds = {"openButton", "saveButton", "cancelButton", "openDirectoryButton", "helpButton", "newFolderButton"};
    static final String[] sButtonData = {"Text", "Mnemonic", "ToolTipText"};
    static final int kOpen = 0, kSave = 1, kCancel = 2, kOpenDirectory = 3, kHelp = 4, kNewFolder = 5;

    /*-------

     Possible states: Save, {Open, Custom}x{Files, File and Directory, Directory}
     --------- */

    // This class returns the values for the Custom type, to avoid duplicating code in the two Custom subclasses
    abstract class FCSubpanel {
        // Install the appropriate panels for this mode
        abstract void installPanel(JFileChooser fc, boolean controlButtonsAreShown);

        abstract void updateButtonState(JFileChooser fc, File f);

        // Can this item be selected?
        // if not, it's disabled in the list
        boolean isSelectableInList(final JFileChooser fc, final File f) {
            if (f == null) return false;
            if (fc.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) return fc.isTraversable(f);
            return fc.accept(f);
        }

        void approveSelection(final JFileChooser fc) {
            fc.approveSelection();
        }

        JButton getDefaultButton(final JFileChooser fc) {
            return fApproveButton;
        }

        // Default to the textfield, panels without one should subclass
        JComponent getFocusComponent(final JFileChooser fc) {
            return filenameTextField;
        }

        String getApproveButtonText(final JFileChooser fc) {
            // Fallback to "choose"
            return this.getApproveButtonText(fc, chooseButtonText);
        }

        // Try to get the custom text.  If none, use the fallback
        String getApproveButtonText(final JFileChooser fc, final String fallbackText) {
            final String buttonText = fc.getApproveButtonText();
            return buttonText != null
                    ? buttonText
                    : fallbackText;
        }

        int getApproveButtonMnemonic(final JFileChooser fc) {
            // Don't use a default
            return fc.getApproveButtonMnemonic();
        }

        // No fallback
        String getApproveButtonToolTipText(final JFileChooser fc) {
            return getApproveButtonToolTipText(fc, null);
        }

        String getApproveButtonToolTipText(final JFileChooser fc, final String fallbackText) {
            final String tooltipText = fc.getApproveButtonToolTipText();
            return tooltipText != null
                    ? tooltipText
                    : fallbackText;
        }

        String getCancelButtonToolTipText(final JFileChooser fc) {
            return cancelChooseButtonToolTipText;
        }
    }

    // Custom FILES_ONLY dialog
    /*
     NavServices Save appearance with Open behavior
     Approve button label = Open when list has focus and a directory is selected, Custom otherwise
     No OpenDirectory button - Approve button is overloaded
     Default button / double click = Approve
     Has text field
     List - everything is enabled
     */
    class CustomFilePanel extends FCSubpanel {
        void installPanel(final JFileChooser fc, final boolean controlButtonsAreShown) {
            fTextfieldPanel.setVisible(true); // do we really want one in multi-select?  It's confusing
            fOpenButton.setVisible(false);
            fNewFolderButton.setVisible(true);
        }

        // If the list has focus, the mode depends on the selection
        // - directory = open, file = approve
        // If something else has focus and we have text, it's approve
        // otherwise, it depends on selection again.
        boolean inOpenDirectoryMode(final JFileChooser fc, final File f) {
            final boolean selectionIsDirectory = (f != null && fc.isTraversable(f));
            if (fFileList.hasFocus()) return selectionIsDirectory;
            else if (textfieldIsValid()) return false;
            return selectionIsDirectory;
        }

        // The approve button is overloaded to mean OpenDirectory or Save
        void approveSelection(final JFileChooser fc) {
            File f = getFirstSelectedItem();
            if (inOpenDirectoryMode(fc, f)) {
                openDirectory(f);
            } else {
                f = makeFile(fc, getFileName());
                if (f != null) {
                    selectionInProgress = true;
                    getFileChooser().setSelectedFile(f);
                    selectionInProgress = false;
                }
                getFileChooser().approveSelection();
            }
        }

        // The approve button should be enabled
        // - if something in the list can be opened
        // - if the textfield has something in it
        void updateButtonState(final JFileChooser fc, final File f) {
            boolean enabled = true;
            if (!inOpenDirectoryMode(fc, f)) {
                enabled = (f != null) || textfieldIsValid();
            }
            getApproveButton(fc).setEnabled(enabled);

            // The OpenDirectory button should be disabled if there's no directory selected
            fOpenButton.setEnabled(f != null && fc.isTraversable(f));

            // Update the default button, since we may have disabled the current default.
            setDefaultButtonForMode(fc);
        }

        // everything's enabled, because we don't know what they're doing with them
        boolean isSelectableInList(final JFileChooser fc, final File f) {
            if (f == null) return false;
            return fc.accept(f);
        }

        String getApproveButtonToolTipText(final JFileChooser fc) {
            // The approve Button should have openDirectoryButtonToolTipText when the selection is a folder...
            if (inOpenDirectoryMode(fc, getFirstSelectedItem())) return openDirectoryButtonToolTipText;
            return super.getApproveButtonToolTipText(fc);
        }
    }

    // All Save dialogs
    /*
     NavServices Save
     Approve button label = Open when list has focus and a directory is selected, Save otherwise
     No OpenDirectory button - Approve button is overloaded
     Default button / double click = Approve
     Has text field
     Has NewFolder button (by text field)
     List - only traversables are enabled
     List is always SINGLE_SELECT
     */
    // Subclasses CustomFilePanel because they look alike and have some common behavior
    class SaveFilePanel extends CustomFilePanel {
        void installPanel(final JFileChooser fc, final boolean controlButtonsAreShown) {
            fTextfieldPanel.setVisible(true);
            fOpenButton.setVisible(false);
            fNewFolderButton.setVisible(true);
        }

        // only traversables are enabled, regardless of mode
        // because all you can do is select the next folder to open
        boolean isSelectableInList(final JFileChooser fc, final File f) {
            return fc.accept(f) && fc.isTraversable(f);
        }

        // The approve button means 'approve the file name in the text field.'
        void approveSelection(final JFileChooser fc) {
            final File f = makeFile(fc, getFileName());
            if (f != null) {
                selectionInProgress = true;
                getFileChooser().setSelectedFile(f);
                selectionInProgress = false;
                getFileChooser().approveSelection();
            }
        }

        // The approve button should be enabled if the textfield has something in it
        void updateButtonState(final JFileChooser fc, final File f) {
            final boolean enabled = textfieldIsValid();
            getApproveButton(fc).setEnabled(enabled);
        }

        String getApproveButtonText(final JFileChooser fc) {
            // Get the custom text, or fallback to "Save"
            return this.getApproveButtonText(fc, saveButtonText);
        }

        int getApproveButtonMnemonic(final JFileChooser fc) {
            return saveButtonMnemonic;
        }

        String getApproveButtonToolTipText(final JFileChooser fc) {
            // The approve Button should have openDirectoryButtonToolTipText when the selection is a folder...
            if (inOpenDirectoryMode(fc, getFirstSelectedItem())) return openDirectoryButtonToolTipText;
            return this.getApproveButtonToolTipText(fc, saveButtonToolTipText);
        }

        String getCancelButtonToolTipText(final JFileChooser fc) {
            return cancelSaveButtonToolTipText;
        }
    }

    // Open FILES_ONLY
    /*
     NSOpenPanel-style
     Approve button label = Open
     Default button / double click = Approve
     No text field
     No NewFolder button
     List - all items are enabled
     */
    class OpenFilePanel extends FCSubpanel {
        void installPanel(final JFileChooser fc, final boolean controlButtonsAreShown) {
            fTextfieldPanel.setVisible(false);
            fOpenButton.setVisible(false);
            fNewFolderButton.setVisible(false);
            setDefaultButtonForMode(fc);
        }

        boolean inOpenDirectoryMode(final JFileChooser fc, final File f) {
            return (f != null && fc.isTraversable(f));
        }

        // Default to the list
        JComponent getFocusComponent(final JFileChooser fc) {
            return fFileList;
        }

        void updateButtonState(final JFileChooser fc, final File f) {
            // Button is disabled if there's nothing selected
            final boolean enabled = (f != null) && !fc.isTraversable(f);
            getApproveButton(fc).setEnabled(enabled);
        }

        // all items are enabled
        boolean isSelectableInList(final JFileChooser fc, final File f) {
            return f != null && fc.accept(f);
        }

        String getApproveButtonText(final JFileChooser fc) {
            // Get the custom text, or fallback to "Open"
            return this.getApproveButtonText(fc, openButtonText);
        }

        int getApproveButtonMnemonic(final JFileChooser fc) {
            return openButtonMnemonic;
        }

        String getApproveButtonToolTipText(final JFileChooser fc) {
            return this.getApproveButtonToolTipText(fc, openButtonToolTipText);
        }

        String getCancelButtonToolTipText(final JFileChooser fc) {
            return cancelOpenButtonToolTipText;
        }
    }

    // used by open and custom panels for Directory only or files and directories
    abstract class DirOrAnyPanel extends FCSubpanel {
        void installPanel(final JFileChooser fc, final boolean controlButtonsAreShown) {
            fOpenButton.setVisible(false);
        }

        JButton getDefaultButton(final JFileChooser fc) {
            return getApproveButton(fc);
        }

        void updateButtonState(final JFileChooser fc, final File f) {
            // Button is disabled if there's nothing selected
            // Approve button is handled by the subclasses
            // getApproveButton(fc).setEnabled(f != null);

            // The OpenDirectory button should be disabled if there's no directory selected
            // - we only check the first item

            fOpenButton.setEnabled(false);
            setDefaultButtonForMode(fc);
        }
    }

    // Open FILES_AND_DIRECTORIES or DIRECTORIES_ONLY
    /*
     NavServices Choose
     Approve button label = Choose/Custom
     Has OpenDirectory button
     Default button / double click = OpenDirectory
     No text field
     List - files are disabled in DIRECTORIES_ONLY
     */
    class OpenDirOrAnyPanel extends DirOrAnyPanel {
        void installPanel(final JFileChooser fc, final boolean controlButtonsAreShown) {
            super.installPanel(fc, controlButtonsAreShown);
            fTextfieldPanel.setVisible(false);
            fNewFolderButton.setVisible(false);
        }

        // Default to the list
        JComponent getFocusComponent(final JFileChooser fc) {
            return fFileList;
        }

        int getApproveButtonMnemonic(final JFileChooser fc) {
            return chooseButtonMnemonic;
        }

        String getApproveButtonToolTipText(final JFileChooser fc) {
            String fallbackText;
            if (fc.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) fallbackText = chooseFolderButtonToolTipText;
            else fallbackText = chooseItemButtonToolTipText;
            return this.getApproveButtonToolTipText(fc, fallbackText);
        }

        void updateButtonState(final JFileChooser fc, final File f) {
            // Button is disabled if there's nothing selected
            getApproveButton(fc).setEnabled(f != null);
            super.updateButtonState(fc, f);
        }
    }

    // Custom FILES_AND_DIRECTORIES or DIRECTORIES_ONLY
    /*
     No NavServices equivalent
     Approve button label = user defined or Choose
     Has OpenDirectory button
     Default button / double click = OpenDirectory
     Has text field
     Has NewFolder button (by text field)
     List - files are disabled in DIRECTORIES_ONLY
     */
    class CustomDirOrAnyPanel extends DirOrAnyPanel {
        void installPanel(final JFileChooser fc, final boolean controlButtonsAreShown) {
            super.installPanel(fc, controlButtonsAreShown);
            fTextfieldPanel.setVisible(true);
            fNewFolderButton.setVisible(true);
        }

        // If there's text, make a file and select it
        void approveSelection(final JFileChooser fc) {
            final File f = makeFile(fc, getFileName());
            if (f != null) {
                selectionInProgress = true;
                getFileChooser().setSelectedFile(f);
                selectionInProgress = false;
            }
            getFileChooser().approveSelection();
        }

        void updateButtonState(final JFileChooser fc, final File f) {
            // Button is disabled if there's nothing selected
            getApproveButton(fc).setEnabled(f != null || textfieldIsValid());
            super.updateButtonState(fc, f);
        }
    }

    // See FileRenderer - documents in Save dialogs draw disabled, so they shouldn't be selected
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class MacListSelectionModel extends DefaultListSelectionModel {
        AquaFileSystemModel fModel;

        MacListSelectionModel(final AquaFileSystemModel model) {
            fModel = model;
        }

        // Can the file be selected in this mode?
        // (files are visible even if they can't be selected)
        boolean isSelectableInListIndex(final int index) {
            final File file = (File)fModel.getValueAt(index, 0);
            return (file != null && isSelectableInList(file));
        }

        // Make sure everything in the selection interval is valid
        void verifySelectionInterval(int index0, int index1, boolean isSetSelection) {
            if (index0 > index1) {
                final int tmp = index1;
                index1 = index0;
                index0 = tmp;
            }
            int start = index0;
            int end;
            do {
                // Find the first selectable file in the range
                for (; start <= index1; start++) {
                    if (isSelectableInListIndex(start)) break;
                }
                end = -1;
                // Find the last selectable file in the range
                for (int i = start; i <= index1; i++) {
                    if (!isSelectableInListIndex(i)) {
                        break;
                    }
                    end = i;
                }
                // Select the range
                if (end >= 0) {
                    // If setting the selection, do "set" the first time to clear the old one
                    // after that do "add" to extend it
                    if (isSetSelection) {
                        super.setSelectionInterval(start, end);
                        isSetSelection = false;
                    } else {
                        super.addSelectionInterval(start, end);
                    }
                    start = end + 1;
                } else {
                    break;
                }
            } while (start <= index1);
        }

        public void setAnchorSelectionIndex(final int anchorIndex) {
            if (isSelectableInListIndex(anchorIndex)) super.setAnchorSelectionIndex(anchorIndex);
        }

        public void setLeadSelectionIndex(final int leadIndex) {
            if (isSelectableInListIndex(leadIndex)) super.setLeadSelectionIndex(leadIndex);
        }

        public void setSelectionInterval(final int index0, final int index1) {
            if (index0 == -1 || index1 == -1) { return; }

            if ((getSelectionMode() == SINGLE_SELECTION) || (index0 == index1)) {
                if (isSelectableInListIndex(index1)) super.setSelectionInterval(index1, index1);
            } else {
                verifySelectionInterval(index0, index1, true);
            }
        }

        public void addSelectionInterval(final int index0, final int index1) {
            if (index0 == -1 || index1 == -1) { return; }

            if (index0 == index1) {
                if (isSelectableInListIndex(index1)) super.addSelectionInterval(index1, index1);
                return;
            }

            if (getSelectionMode() != MULTIPLE_INTERVAL_SELECTION) {
                setSelectionInterval(index0, index1);
                return;
            }

            verifySelectionInterval(index0, index1, false);
        }
    }

    // Convenience, to translate from the JList directory view to the Mac-style JTable
    //   & minimize diffs between this and BasicFileChooserUI
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class JTableExtension extends JTable {
        public void setSelectedIndex(final int index) {
            getSelectionModel().setSelectionInterval(index, index);
        }

        public void removeSelectedIndex(final int index) {
            getSelectionModel().removeSelectionInterval(index, index);
        }

        public void ensureIndexIsVisible(final int index) {
            final Rectangle cellBounds = getCellRect(index, 0, false);
            if (cellBounds != null) {
                scrollRectToVisible(cellBounds);
            }
        }

        public int locationToIndex(final Point location) {
            return rowAtPoint(location);
        }
    }
}
