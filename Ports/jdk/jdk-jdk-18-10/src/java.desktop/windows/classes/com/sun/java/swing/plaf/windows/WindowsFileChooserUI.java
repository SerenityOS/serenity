/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.windows;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicDirectoryModel;
import javax.swing.plaf.basic.BasicFileChooserUI;

import sun.awt.shell.ShellFolder;
import sun.swing.FilePane;
import sun.swing.SwingUtilities2;
import sun.swing.WindowsPlacesBar;

/**
 * Windows {@literal L&F} implementation of a FileChooser.
 *
 * @author Jeff Dinkins
 */
public class WindowsFileChooserUI extends BasicFileChooserUI {

    // The following are private because the implementation of the
    // Windows FileChooser L&F is not complete yet.

    private JPanel centerPanel;

    private JLabel lookInLabel;
    private JComboBox<File> directoryComboBox;
    private DirectoryComboBoxModel directoryComboBoxModel;
    private ActionListener directoryComboBoxAction = new DirectoryComboBoxAction();

    private FilterComboBoxModel filterComboBoxModel;

    private JTextField filenameTextField;
    private FilePane filePane;
    private WindowsPlacesBar placesBar;

    private JButton approveButton;
    private JButton cancelButton;

    private JPanel buttonPanel;
    private JPanel bottomPanel;

    private JComboBox<FileFilter> filterComboBox;

    private static final Dimension hstrut10 = new Dimension(10, 1);

    private static final Dimension vstrut4  = new Dimension(1, 4);
    private static final Dimension vstrut6  = new Dimension(1, 6);
    private static final Dimension vstrut8  = new Dimension(1, 8);

    private static final Insets shrinkwrap = new Insets(0,0,0,0);

    // Preferred and Minimum sizes for the dialog box
    private static int PREF_WIDTH = 425;
    private static int PREF_HEIGHT = 245;
    private static Dimension PREF_SIZE = new Dimension(PREF_WIDTH, PREF_HEIGHT);

    private static int MIN_WIDTH = 425;
    private static int MIN_HEIGHT = 245;

    private static int LIST_PREF_WIDTH = 444;
    private static int LIST_PREF_HEIGHT = 138;
    private static Dimension LIST_PREF_SIZE = new Dimension(LIST_PREF_WIDTH, LIST_PREF_HEIGHT);

    // Labels, mnemonics, and tooltips (oh my!)
    private int    lookInLabelMnemonic = 0;
    private String lookInLabelText = null;
    private String saveInLabelText = null;

    private int    fileNameLabelMnemonic = 0;
    private String fileNameLabelText = null;
    private int    folderNameLabelMnemonic = 0;
    private String folderNameLabelText = null;

    private int    filesOfTypeLabelMnemonic = 0;
    private String filesOfTypeLabelText = null;

    private String upFolderToolTipText = null;
    private String upFolderAccessibleName = null;

    private String newFolderToolTipText = null;
    private String newFolderAccessibleName = null;

    private String viewMenuButtonToolTipText = null;
    private String viewMenuButtonAccessibleName = null;

    private BasicFileView fileView = new WindowsFileView();

    private JLabel fileNameLabel;

    private void populateFileNameLabel() {
        if (getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
            fileNameLabel.setText(folderNameLabelText);
            fileNameLabel.setDisplayedMnemonic(folderNameLabelMnemonic);
        } else {
            fileNameLabel.setText(fileNameLabelText);
            fileNameLabel.setDisplayedMnemonic(fileNameLabelMnemonic);
        }
    }

    //
    // ComponentUI Interface Implementation methods
    //
    public static ComponentUI createUI(JComponent c) {
        return new WindowsFileChooserUI((JFileChooser) c);
    }

    public WindowsFileChooserUI(JFileChooser filechooser) {
        super(filechooser);
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallComponents(JFileChooser fc) {
        fc.removeAll();
    }

    private class WindowsFileChooserUIAccessor implements FilePane.FileChooserUIAccessor {
        public JFileChooser getFileChooser() {
            return WindowsFileChooserUI.this.getFileChooser();
        }

        public BasicDirectoryModel getModel() {
            return WindowsFileChooserUI.this.getModel();
        }

        public JPanel createList() {
            return WindowsFileChooserUI.this.createList(getFileChooser());
        }

        public JPanel createDetailsView() {
            return WindowsFileChooserUI.this.createDetailsView(getFileChooser());
        }

        public boolean isDirectorySelected() {
            return WindowsFileChooserUI.this.isDirectorySelected();
        }

        public File getDirectory() {
            return WindowsFileChooserUI.this.getDirectory();
        }

        public Action getChangeToParentDirectoryAction() {
            return WindowsFileChooserUI.this.getChangeToParentDirectoryAction();
        }

        public Action getApproveSelectionAction() {
            return WindowsFileChooserUI.this.getApproveSelectionAction();
        }

        public Action getNewFolderAction() {
            return WindowsFileChooserUI.this.getNewFolderAction();
        }

        public MouseListener createDoubleClickListener(JList<?> list) {
            return WindowsFileChooserUI.this.createDoubleClickListener(getFileChooser(),
                                                                       list);
        }

        public ListSelectionListener createListSelectionListener() {
            return WindowsFileChooserUI.this.createListSelectionListener(getFileChooser());
        }
    }

    public void installComponents(JFileChooser fc) {
        filePane = new FilePane(new WindowsFileChooserUIAccessor());
        fc.addPropertyChangeListener(filePane);

        FileSystemView fsv = fc.getFileSystemView();

        fc.setBorder(new EmptyBorder(4, 10, 10, 10));
        fc.setLayout(new BorderLayout(8, 8));

        updateUseShellFolder();

        // ********************************* //
        // **** Construct the top panel **** //
        // ********************************* //

        // Directory manipulation buttons
        JToolBar topPanel = new JToolBar();
        topPanel.setFloatable(false);
        topPanel.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        // Add the top panel to the fileChooser
        fc.add(topPanel, BorderLayout.NORTH);

        // ComboBox Label
        @SuppressWarnings("serial") // anonymous class
        JLabel tmp1 = new JLabel(lookInLabelText, JLabel.TRAILING) {
            public Dimension getPreferredSize() {
                return getMinimumSize();
            }

            public Dimension getMinimumSize() {
                Dimension d = super.getPreferredSize();
                if (placesBar != null) {
                    d.width = Math.max(d.width, placesBar.getWidth());
                }
                return d;
            }
        };
        lookInLabel = tmp1;
        lookInLabel.setDisplayedMnemonic(lookInLabelMnemonic);
        lookInLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lookInLabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        topPanel.add(lookInLabel);
        topPanel.add(Box.createRigidArea(new Dimension(8,0)));

        // CurrentDir ComboBox
        @SuppressWarnings("serial") // anonymous class
        JComboBox<File> tmp2 = new JComboBox<File>() {
            public Dimension getMinimumSize() {
                Dimension d = super.getMinimumSize();
                d.width = 60;
                return d;
            }

            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                // Must be small enough to not affect total width.
                d.width = 150;
                return d;
            }
        };
        directoryComboBox = tmp2;
        directoryComboBox.putClientProperty( "JComboBox.lightweightKeyboardNavigation", "Lightweight" );
        directoryComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        lookInLabel.setLabelFor(directoryComboBox);
        directoryComboBoxModel = createDirectoryComboBoxModel(fc);
        directoryComboBox.setModel(directoryComboBoxModel);
        directoryComboBox.addActionListener(directoryComboBoxAction);
        directoryComboBox.setRenderer(createDirectoryComboBoxRenderer(fc));
        directoryComboBox.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        directoryComboBox.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        directoryComboBox.setMaximumRowCount(8);

        topPanel.add(directoryComboBox);
        topPanel.add(Box.createRigidArea(hstrut10));

        // Up Button
        JButton upFolderButton = createToolButton(getChangeToParentDirectoryAction(), upFolderIcon,
            upFolderToolTipText, upFolderAccessibleName);
        topPanel.add(upFolderButton);

        // New Directory Button
        if (!UIManager.getBoolean("FileChooser.readOnly")) {
            JButton newFolderButton = createToolButton(filePane.getNewFolderAction(), newFolderIcon,
                newFolderToolTipText, newFolderAccessibleName);
            topPanel.add(newFolderButton);
        }

        // View button group
        ButtonGroup viewButtonGroup = new ButtonGroup();

        // Popup Menu
        final JPopupMenu viewTypePopupMenu = new JPopupMenu();

        final JRadioButtonMenuItem listViewMenuItem = new JRadioButtonMenuItem(
                filePane.getViewTypeAction(FilePane.VIEWTYPE_LIST));
        listViewMenuItem.setSelected(filePane.getViewType() == FilePane.VIEWTYPE_LIST);
        viewTypePopupMenu.add(listViewMenuItem);
        viewButtonGroup.add(listViewMenuItem);

        final JRadioButtonMenuItem detailsViewMenuItem = new JRadioButtonMenuItem(
                filePane.getViewTypeAction(FilePane.VIEWTYPE_DETAILS));
        detailsViewMenuItem.setSelected(filePane.getViewType() == FilePane.VIEWTYPE_DETAILS);
        viewTypePopupMenu.add(detailsViewMenuItem);
        viewButtonGroup.add(detailsViewMenuItem);

        // Create icon for viewMenuButton
        BufferedImage image = new BufferedImage(viewMenuIcon.getIconWidth() + 7, viewMenuIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        viewMenuIcon.paintIcon(filePane, graphics, 0, 0);
        int x = image.getWidth() - 5;
        int y = image.getHeight() / 2 - 1;
        graphics.setColor(Color.BLACK);
        graphics.fillPolygon(new int[]{x, x + 5, x + 2}, new int[]{y, y, y + 3}, 3);

        // Details Button
        final JButton viewMenuButton = createToolButton(null, new ImageIcon(image), viewMenuButtonToolTipText,
                viewMenuButtonAccessibleName);

        viewMenuButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !viewMenuButton.isSelected()) {
                    viewMenuButton.setSelected(true);

                    viewTypePopupMenu.show(viewMenuButton, 0, viewMenuButton.getHeight());
                }
            }
        });
        viewMenuButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                // Forbid keyboard actions if the button is not in rollover state
                if (e.getKeyCode() == KeyEvent.VK_SPACE && viewMenuButton.getModel().isRollover()) {
                    viewMenuButton.setSelected(true);

                    viewTypePopupMenu.show(viewMenuButton, 0, viewMenuButton.getHeight());
                }
            }
        });
        viewTypePopupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        viewMenuButton.setSelected(false);
                    }
                });
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        topPanel.add(viewMenuButton);

        topPanel.add(Box.createRigidArea(new Dimension(80, 0)));

        filePane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if ("viewType".equals(e.getPropertyName())) {
                    switch (filePane.getViewType()) {
                        case FilePane.VIEWTYPE_LIST:
                            listViewMenuItem.setSelected(true);
                            break;

                        case FilePane.VIEWTYPE_DETAILS:
                            detailsViewMenuItem.setSelected(true);
                            break;
                    }
                }
            }
        });

        // ************************************** //
        // ******* Add the directory pane ******* //
        // ************************************** //
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(getAccessoryPanel(), BorderLayout.AFTER_LINE_ENDS);
        JComponent accessory = fc.getAccessory();
        if(accessory != null) {
            getAccessoryPanel().add(accessory);
        }
        filePane.setPreferredSize(LIST_PREF_SIZE);
        centerPanel.add(filePane, BorderLayout.CENTER);
        fc.add(centerPanel, BorderLayout.CENTER);

        // ********************************** //
        // **** Construct the bottom panel ** //
        // ********************************** //
        getBottomPanel().setLayout(new BoxLayout(getBottomPanel(), BoxLayout.LINE_AXIS));

        // Add the bottom panel to file chooser
        centerPanel.add(getBottomPanel(), BorderLayout.SOUTH);

        // labels
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.PAGE_AXIS));
        labelPanel.add(Box.createRigidArea(vstrut4));

        fileNameLabel = new JLabel();
        populateFileNameLabel();
        fileNameLabel.setAlignmentY(0);
        labelPanel.add(fileNameLabel);

        labelPanel.add(Box.createRigidArea(new Dimension(1,12)));

        JLabel ftl = new JLabel(filesOfTypeLabelText);
        ftl.setDisplayedMnemonic(filesOfTypeLabelMnemonic);
        labelPanel.add(ftl);

        getBottomPanel().add(labelPanel);
        getBottomPanel().add(Box.createRigidArea(new Dimension(15, 0)));

        // file entry and filters
        JPanel fileAndFilterPanel = new JPanel();
        fileAndFilterPanel.add(Box.createRigidArea(vstrut8));
        fileAndFilterPanel.setLayout(new BoxLayout(fileAndFilterPanel, BoxLayout.Y_AXIS));

        @SuppressWarnings("serial") // anonymous class
        JTextField tmp3 = new JTextField(35) {
            public Dimension getMaximumSize() {
                return new Dimension(Short.MAX_VALUE, super.getPreferredSize().height);
            }
        };
        filenameTextField = tmp3;

        fileNameLabel.setLabelFor(filenameTextField);
        filenameTextField.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (!getFileChooser().isMultiSelectionEnabled()) {
                        filePane.clearSelection();
                    }
                }
            }
        );

        if (fc.isMultiSelectionEnabled()) {
            setFileName(fileNameString(fc.getSelectedFiles()));
        } else {
            setFileName(fileNameString(fc.getSelectedFile()));
        }

        fileAndFilterPanel.add(filenameTextField);
        fileAndFilterPanel.add(Box.createRigidArea(vstrut8));

        filterComboBoxModel = createFilterComboBoxModel();
        fc.addPropertyChangeListener(filterComboBoxModel);
        filterComboBox = new JComboBox<FileFilter>(filterComboBoxModel);
        ftl.setLabelFor(filterComboBox);
        filterComboBox.setRenderer(createFilterComboBoxRenderer());
        fileAndFilterPanel.add(filterComboBox);

        getBottomPanel().add(fileAndFilterPanel);
        getBottomPanel().add(Box.createRigidArea(new Dimension(30, 0)));

        // buttons
        getButtonPanel().setLayout(new BoxLayout(getButtonPanel(), BoxLayout.Y_AXIS));

        @SuppressWarnings("serial") // anonymous class
        JButton tmp4 = new JButton(getApproveButtonText(fc)) {
            public Dimension getMaximumSize() {
                return approveButton.getPreferredSize().width > cancelButton.getPreferredSize().width ?
                       approveButton.getPreferredSize() : cancelButton.getPreferredSize();
            }
        };
        approveButton = tmp4;
        Insets buttonMargin = approveButton.getMargin();
        buttonMargin = new InsetsUIResource(buttonMargin.top,    buttonMargin.left  + 5,
                                            buttonMargin.bottom, buttonMargin.right + 5);
        approveButton.setMargin(buttonMargin);
        approveButton.setMnemonic(getApproveButtonMnemonic(fc));
        approveButton.addActionListener(getApproveSelectionAction());
        approveButton.setToolTipText(getApproveButtonToolTipText(fc));
        getButtonPanel().add(Box.createRigidArea(vstrut6));
        getButtonPanel().add(approveButton);
        getButtonPanel().add(Box.createRigidArea(vstrut4));

        @SuppressWarnings("serial") // anonymous class
        JButton tmp5 = new JButton(cancelButtonText) {
            public Dimension getMaximumSize() {
                return approveButton.getPreferredSize().width > cancelButton.getPreferredSize().width ?
                       approveButton.getPreferredSize() : cancelButton.getPreferredSize();
            }
        };
        cancelButton = tmp5;
        cancelButton.setMargin(buttonMargin);
        cancelButton.setToolTipText(cancelButtonToolTipText);
        cancelButton.addActionListener(getCancelSelectionAction());
        getButtonPanel().add(cancelButton);

        if(fc.getControlButtonsAreShown()) {
            addControlButtons();
        }
    }

    private void updateUseShellFolder() {
        // Decide whether to use the ShellFolder class to populate shortcut
        // panel and combobox.
        JFileChooser fc = getFileChooser();

        if (FilePane.usesShellFolder(fc)) {
            if (placesBar == null && !UIManager.getBoolean("FileChooser.noPlacesBar")) {
                placesBar = new WindowsPlacesBar(fc, XPStyle.getXP() != null);
                fc.add(placesBar, BorderLayout.BEFORE_LINE_BEGINS);
                fc.addPropertyChangeListener(placesBar);
            }
        } else {
            if (placesBar != null) {
                fc.remove(placesBar);
                fc.removePropertyChangeListener(placesBar);
                placesBar = null;
            }
        }
    }

    protected JPanel getButtonPanel() {
        if(buttonPanel == null) {
            buttonPanel = new JPanel();
        }
        return buttonPanel;
    }

    protected JPanel getBottomPanel() {
        if(bottomPanel == null) {
            bottomPanel = new JPanel();
        }
        return bottomPanel;
    }

    protected void installStrings(JFileChooser fc) {
        super.installStrings(fc);

        Locale l = fc.getLocale();

        lookInLabelMnemonic = getMnemonic("FileChooser.lookInLabelMnemonic", l);
        lookInLabelText = UIManager.getString("FileChooser.lookInLabelText",l);
        saveInLabelText = UIManager.getString("FileChooser.saveInLabelText",l);

        fileNameLabelMnemonic = getMnemonic("FileChooser.fileNameLabelMnemonic", l);
        fileNameLabelText = UIManager.getString("FileChooser.fileNameLabelText",l);
        folderNameLabelMnemonic = getMnemonic("FileChooser.folderNameLabelMnemonic", l);
        folderNameLabelText = UIManager.getString("FileChooser.folderNameLabelText",l);

        filesOfTypeLabelMnemonic = getMnemonic("FileChooser.filesOfTypeLabelMnemonic", l);
        filesOfTypeLabelText = UIManager.getString("FileChooser.filesOfTypeLabelText",l);

        upFolderToolTipText =  UIManager.getString("FileChooser.upFolderToolTipText",l);
        upFolderAccessibleName = UIManager.getString("FileChooser.upFolderAccessibleName",l);

        newFolderToolTipText = UIManager.getString("FileChooser.newFolderToolTipText",l);
        newFolderAccessibleName = UIManager.getString("FileChooser.newFolderAccessibleName",l);

        viewMenuButtonToolTipText = UIManager.getString("FileChooser.viewMenuButtonToolTipText",l);
        viewMenuButtonAccessibleName = UIManager.getString("FileChooser.viewMenuButtonAccessibleName",l);
    }

    private Integer getMnemonic(String key, Locale l) {
        return SwingUtilities2.getUIDefaultsInt(key, l);
    }

    protected void installListeners(JFileChooser fc) {
        super.installListeners(fc);
        ActionMap actionMap = getActionMap();
        SwingUtilities.replaceUIActionMap(fc, actionMap);
    }

    protected ActionMap getActionMap() {
        return createActionMap();
    }

    protected ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        FilePane.addActionsToMap(map, filePane.getActions());
        return map;
    }

    protected JPanel createList(JFileChooser fc) {
        return filePane.createList();
    }

    protected JPanel createDetailsView(JFileChooser fc) {
        return filePane.createDetailsView();
    }

    /**
     * Creates a selection listener for the list of files and directories.
     *
     * @param fc a <code>JFileChooser</code>
     * @return a <code>ListSelectionListener</code>
     */
    public ListSelectionListener createListSelectionListener(JFileChooser fc) {
        return super.createListSelectionListener(fc);
    }

    public void uninstallUI(JComponent c) {
        // Remove listeners
        c.removePropertyChangeListener(filterComboBoxModel);
        c.removePropertyChangeListener(filePane);
        if (placesBar != null) {
            c.removePropertyChangeListener(placesBar);
        }
        cancelButton.removeActionListener(getCancelSelectionAction());
        approveButton.removeActionListener(getApproveSelectionAction());
        filenameTextField.removeActionListener(getApproveSelectionAction());

        if (filePane != null) {
            filePane.uninstallUI();
            filePane = null;
        }

        super.uninstallUI(c);
    }

    /**
     * Returns the preferred size of the specified
     * <code>JFileChooser</code>.
     * The preferred size is at least as large,
     * in both height and width,
     * as the preferred size recommended
     * by the file chooser's layout manager.
     *
     * @param c  a <code>JFileChooser</code>
     * @return   a <code>Dimension</code> specifying the preferred
     *           width and height of the file chooser
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        int prefWidth = PREF_SIZE.width;
        Dimension d = c.getLayout().preferredLayoutSize(c);
        if (d != null) {
            return new Dimension(d.width < prefWidth ? prefWidth : d.width,
                                 d.height < PREF_SIZE.height ? PREF_SIZE.height : d.height);
        } else {
            return new Dimension(prefWidth, PREF_SIZE.height);
        }
    }

    /**
     * Returns the minimum size of the <code>JFileChooser</code>.
     *
     * @param c  a <code>JFileChooser</code>
     * @return   a <code>Dimension</code> specifying the minimum
     *           width and height of the file chooser
     */
    @Override
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(MIN_WIDTH, MIN_HEIGHT);
    }

    /**
     * Returns the maximum size of the <code>JFileChooser</code>.
     *
     * @param c  a <code>JFileChooser</code>
     * @return   a <code>Dimension</code> specifying the maximum
     *           width and height of the file chooser
     */
    @Override
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private String fileNameString(File file) {
        if (file == null) {
            return null;
        } else {
            JFileChooser fc = getFileChooser();
            if ((fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) ||
                (fc.isDirectorySelectionEnabled() && fc.isFileSelectionEnabled() && fc.getFileSystemView().isFileSystemRoot(file))){
                return file.getPath();
            } else {
                return file.getName();
            }
        }
    }

    private String fileNameString(File[] files) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; files != null && i < files.length; i++) {
            if (i > 0) {
                buf.append(" ");
            }
            if (files.length > 1) {
                buf.append("\"");
            }
            buf.append(fileNameString(files[i]));
            if (files.length > 1) {
                buf.append("\"");
            }
        }
        return buf.toString();
    }

    /* The following methods are used by the PropertyChange Listener */

    private void doSelectedFileChanged(PropertyChangeEvent e) {
        File f = (File) e.getNewValue();
        JFileChooser fc = getFileChooser();
        if (f != null
            && ((fc.isFileSelectionEnabled() && !f.isDirectory())
                || (f.isDirectory() && fc.isDirectorySelectionEnabled()))) {

            setFileName(fileNameString(f));
        }
    }

    private void doSelectedFilesChanged(PropertyChangeEvent e) {
        File[] files = (File[]) e.getNewValue();
        JFileChooser fc = getFileChooser();
        if (files != null
            && files.length > 0
            && (files.length > 1 || fc.isDirectorySelectionEnabled() || !files[0].isDirectory())) {
            setFileName(fileNameString(files));
        }
    }

    private void doDirectoryChanged(PropertyChangeEvent e) {
        JFileChooser fc = getFileChooser();
        FileSystemView fsv = fc.getFileSystemView();

        clearIconCache();
        File currentDirectory = fc.getCurrentDirectory();
        if(currentDirectory != null) {
            directoryComboBoxModel.addItem(currentDirectory);

            if (fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) {
                if (fsv.isFileSystem(currentDirectory)) {
                    setFileName(currentDirectory.getPath());
                } else {
                    setFileName(null);
                }
            }
        }
    }

    private void doFilterChanged(PropertyChangeEvent e) {
        clearIconCache();
    }

    private void doFileSelectionModeChanged(PropertyChangeEvent e) {
        if (fileNameLabel != null) {
            populateFileNameLabel();
        }
        clearIconCache();

        JFileChooser fc = getFileChooser();
        File currentDirectory = fc.getCurrentDirectory();
        if (currentDirectory != null
            && fc.isDirectorySelectionEnabled()
            && !fc.isFileSelectionEnabled()
            && fc.getFileSystemView().isFileSystem(currentDirectory)) {

            setFileName(currentDirectory.getPath());
        } else {
            setFileName(null);
        }
    }

    private void doAccessoryChanged(PropertyChangeEvent e) {
        if(getAccessoryPanel() != null) {
            if(e.getOldValue() != null) {
                getAccessoryPanel().remove((JComponent) e.getOldValue());
            }
            JComponent accessory = (JComponent) e.getNewValue();
            if(accessory != null) {
                getAccessoryPanel().add(accessory, BorderLayout.CENTER);
            }
        }
    }

    private void doApproveButtonTextChanged(PropertyChangeEvent e) {
        JFileChooser chooser = getFileChooser();
        approveButton.setText(getApproveButtonText(chooser));
        approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
        approveButton.setMnemonic(getApproveButtonMnemonic(chooser));
    }

    private void doDialogTypeChanged(PropertyChangeEvent e) {
        JFileChooser chooser = getFileChooser();
        approveButton.setText(getApproveButtonText(chooser));
        approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
        approveButton.setMnemonic(getApproveButtonMnemonic(chooser));
        if (chooser.getDialogType() == JFileChooser.SAVE_DIALOG) {
            lookInLabel.setText(saveInLabelText);
        } else {
            lookInLabel.setText(lookInLabelText);
        }
    }

    private void doApproveButtonMnemonicChanged(PropertyChangeEvent e) {
        approveButton.setMnemonic(getApproveButtonMnemonic(getFileChooser()));
    }

    private void doControlButtonsChanged(PropertyChangeEvent e) {
        if(getFileChooser().getControlButtonsAreShown()) {
            addControlButtons();
        } else {
            removeControlButtons();
        }
    }

    /*
     * Listen for filechooser property changes, such as
     * the selected file changing, or the type of the dialog changing.
     */
    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String s = e.getPropertyName();
                if(s.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    doSelectedFileChanged(e);
                } else if (s.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
                    doSelectedFilesChanged(e);
                } else if(s.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                    doDirectoryChanged(e);
                } else if(s.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
                    doFilterChanged(e);
                } else if(s.equals(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
                    doFileSelectionModeChanged(e);
                } else if(s.equals(JFileChooser.ACCESSORY_CHANGED_PROPERTY)) {
                    doAccessoryChanged(e);
                } else if (s.equals(JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY) ||
                           s.equals(JFileChooser.APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY)) {
                    doApproveButtonTextChanged(e);
                } else if(s.equals(JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY)) {
                    doDialogTypeChanged(e);
                } else if(s.equals(JFileChooser.APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY)) {
                    doApproveButtonMnemonicChanged(e);
                } else if(s.equals(JFileChooser.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY)) {
                    doControlButtonsChanged(e);
                } else if (s == "FileChooser.useShellFolder") {
                    updateUseShellFolder();
                    doDirectoryChanged(e);
                } else if (s.equals("componentOrientation")) {
                    ComponentOrientation o = (ComponentOrientation)e.getNewValue();
                    JFileChooser cc = (JFileChooser)e.getSource();
                    if (o != e.getOldValue()) {
                        cc.applyComponentOrientation(o);
                    }
                } else if (s.equals("ancestor")) {
                    if (e.getOldValue() == null && e.getNewValue() != null) {
                        // Ancestor was added, set initial focus
                        filenameTextField.selectAll();
                        filenameTextField.requestFocus();
                    }
                }
            }
        };
    }


    protected void removeControlButtons() {
        getBottomPanel().remove(getButtonPanel());
    }

    protected void addControlButtons() {
        getBottomPanel().add(getButtonPanel());
    }

    public void ensureFileIsVisible(JFileChooser fc, File f) {
        filePane.ensureFileIsVisible(fc, f);
    }

    public void rescanCurrentDirectory(JFileChooser fc) {
        filePane.rescanCurrentDirectory();
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

    /**
     * Property to remember whether a directory is currently selected in the UI.
     * This is normally called by the UI on a selection event.
     *
     * @param directorySelected if a directory is currently selected.
     * @since 1.4
     */
    protected void setDirectorySelected(boolean directorySelected) {
        super.setDirectorySelected(directorySelected);
        JFileChooser chooser = getFileChooser();
        if(directorySelected) {
            approveButton.setText(directoryOpenButtonText);
            approveButton.setToolTipText(directoryOpenButtonToolTipText);
            approveButton.setMnemonic(directoryOpenButtonMnemonic);
        } else {
            approveButton.setText(getApproveButtonText(chooser));
            approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
            approveButton.setMnemonic(getApproveButtonMnemonic(chooser));
        }
    }

    public String getDirectoryName() {
        // PENDING(jeff) - get the name from the directory combobox
        return null;
    }

    public void setDirectoryName(String dirname) {
        // PENDING(jeff) - set the name in the directory combobox
    }

    protected DirectoryComboBoxRenderer createDirectoryComboBoxRenderer(JFileChooser fc) {
        return new DirectoryComboBoxRenderer();
    }

    @SuppressWarnings("serial") // anonymous class
    private static JButton createToolButton(Action a, Icon defaultIcon, String toolTipText, String accessibleName) {
        final JButton result = new JButton(a);

        result.setText(null);
        result.setIcon(defaultIcon);
        result.setToolTipText(toolTipText);
        result.setRequestFocusEnabled(false);
        result.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, accessibleName);
        result.putClientProperty(WindowsLookAndFeel.HI_RES_DISABLED_ICON_CLIENT_KEY, Boolean.TRUE);
        result.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        result.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        result.setMargin(shrinkwrap);
        result.setFocusPainted(false);

        result.setModel(new DefaultButtonModel() {
            public void setPressed(boolean b) {
                // Forbid keyboard actions if the button is not in rollover state
                if (!b || isRollover()) {
                    super.setPressed(b);
                }
            }

            public void setRollover(boolean b) {
                if (b && !isRollover()) {
                    // Reset other buttons
                    for (Component component : result.getParent().getComponents()) {
                        if (component instanceof JButton && component != result) {
                            ((JButton) component).getModel().setRollover(false);
                        }
                    }
                }

                super.setRollover(b);
            }

            public void setSelected(boolean b) {
                super.setSelected(b);

                if (b) {
                    stateMask |= PRESSED | ARMED;
                } else {
                    stateMask &= ~(PRESSED | ARMED);
                }
            }
        });

        result.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                result.getModel().setRollover(true);
            }

            public void focusLost(FocusEvent e) {
                result.getModel().setRollover(false);
            }
        });

        return result;
    }

    //
    // Renderer for DirectoryComboBox
    //
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class DirectoryComboBoxRenderer extends DefaultListCellRenderer  {
        IndentIcon ii = new IndentIcon();
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value == null) {
                setText("");
                return this;
            }
            File directory = (File)value;
            setText(getFileChooser().getName(directory));
            Icon icon = getFileChooser().getIcon(directory);
            ii.icon = icon;
            ii.depth = directoryComboBoxModel.getDepth(index);
            setIcon(ii);

            return this;
        }
    }

    static final int space = 10;
    class IndentIcon implements Icon {

        Icon icon = null;
        int depth = 0;

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (c.getComponentOrientation().isLeftToRight()) {
                icon.paintIcon(c, g, x+depth*space, y);
            } else {
                icon.paintIcon(c, g, x, y);
            }
        }

        public int getIconWidth() {
            return icon.getIconWidth() + depth*space;
        }

        public int getIconHeight() {
            return icon.getIconHeight();
        }

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
        int[] depths = null;
        File selectedDirectory = null;
        JFileChooser chooser = getFileChooser();
        FileSystemView fsv = chooser.getFileSystemView();

        public DirectoryComboBoxModel() {
            // Add the current directory to the model, and make it the
            // selectedDirectory
            File dir = getFileChooser().getCurrentDirectory();
            if(dir != null) {
                addItem(dir);
            }
        }

        /**
         * Adds the directory to the model and sets it to be selected,
         * additionally clears out the previous selected directory and
         * the paths leading up to it, if any.
         */
        private void addItem(File directory) {

            if(directory == null) {
                return;
            }

            boolean useShellFolder = FilePane.usesShellFolder(chooser);

            directories.clear();

            File[] baseFolders = (useShellFolder)
                    ? (File[]) ShellFolder.get("fileChooserComboBoxFolders")
                    : fsv.getRoots();
            directories.addAll(Arrays.asList(baseFolders));

            // Get the canonical (full) path. This has the side
            // benefit of removing extraneous chars from the path,
            // for example /foo/bar/ becomes /foo/bar
            File canonical;
            try {
                canonical = directory.getCanonicalFile();
            } catch (IOException e) {
                // Maybe drive is not ready. Can't abort here.
                canonical = directory;
            }

            // create File instances of each directory leading up to the top
            try {
                File sf = useShellFolder ? ShellFolder.getShellFolder(canonical)
                                         : canonical;
                File f = sf;
                Vector<File> path = new Vector<File>(10);
                do {
                    path.addElement(f);
                } while ((f = f.getParentFile()) != null);

                int pathCount = path.size();
                // Insert chain at appropriate place in vector
                for (int i = 0; i < pathCount; i++) {
                    f = path.get(i);
                    if (directories.contains(f)) {
                        int topIndex = directories.indexOf(f);
                        for (int j = i-1; j >= 0; j--) {
                            directories.insertElementAt(path.get(j), topIndex+i-j);
                        }
                        break;
                    }
                }
                calculateDepths();
                setSelectedItem(sf);
            } catch (FileNotFoundException ex) {
                calculateDepths();
            }
        }

        private void calculateDepths() {
            depths = new int[directories.size()];
            for (int i = 0; i < depths.length; i++) {
                File dir = directories.get(i);
                File parent = dir.getParentFile();
                depths[i] = 0;
                if (parent != null) {
                    for (int j = i-1; j >= 0; j--) {
                        if (parent.equals(directories.get(j))) {
                            depths[i] = depths[j] + 1;
                            break;
                        }
                    }
                }
            }
        }

        public int getDepth(int i) {
            return (depths != null && i >= 0 && i < depths.length) ? depths[i] : 0;
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

        public File getElementAt(int index) {
            return directories.elementAt(index);
        }
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

    public void valueChanged(ListSelectionEvent e) {
        JFileChooser fc = getFileChooser();
        File f = fc.getSelectedFile();
        if (!e.getValueIsAdjusting() && f != null && !getFileChooser().isTraversable(f)) {
            setFileName(fileNameString(f));
        }
    }

    /**
     * Acts when DirectoryComboBox has changed the selected item.
     */
    protected class DirectoryComboBoxAction implements ActionListener {




        public void actionPerformed(ActionEvent e) {
            File f = (File)directoryComboBox.getSelectedItem();
            getFileChooser().setCurrentDirectory(f);
        }
    }

    protected JButton getApproveButton(JFileChooser fc) {
        return approveButton;
    }

    public FileView getFileView(JFileChooser fc) {
        return fileView;
    }

    // ***********************
    // * FileView operations *
    // ***********************
    protected class WindowsFileView extends BasicFileView {
        /* FileView type descriptions */

        public Icon getIcon(File f) {
            Icon icon = getCachedIcon(f);
            if (icon != null) {
                return icon;
            }
            if (f != null) {
                icon = getFileChooser().getFileSystemView().getSystemIcon(f);
            }
            if (icon == null) {
                icon = super.getIcon(f);
            }
            cacheIcon(f, icon);
            return icon;
        }
    }
}
