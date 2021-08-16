/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
package sun.swing.plaf.synth;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.synth.*;
import javax.swing.plaf.ActionMapUIResource;

import sun.awt.shell.ShellFolder;
import sun.swing.*;

/**
 * Synth FileChooserUI implementation.
 * <p>
 * Note that the classes in the com.sun.java.swing.plaf.synth
 * package are not
 * part of the core Java APIs. They are a part of Sun's JDK and JRE
 * distributions. Although other licensees may choose to distribute
 * these classes, developers cannot depend on their availability in
 * non-Sun implementations. Additionally this API may change in
 * incompatible ways between releases. While this class is public, it
 * shoud be considered an implementation detail, and subject to change.
 *
 * @author Leif Samuelsson
 * @author Jeff Dinkins
 */
public class SynthFileChooserUIImpl extends SynthFileChooserUI {
    private JLabel lookInLabel;
    private JComboBox<File> directoryComboBox;
    private DirectoryComboBoxModel directoryComboBoxModel;
    private Action directoryComboBoxAction = new DirectoryComboBoxAction();

    private FilterComboBoxModel filterComboBoxModel;

    private JTextField fileNameTextField;

    private FilePane filePane;
    private JToggleButton listViewButton;
    private JToggleButton detailsViewButton;

    private boolean readOnly;

    private JPanel buttonPanel;
    private JPanel bottomPanel;

    private JComboBox<FileFilter> filterComboBox;

    private static final Dimension hstrut5 = new Dimension(5, 1);

    private static final Insets shrinkwrap = new Insets(0,0,0,0);

    // Preferred and Minimum sizes for the dialog box
    private static Dimension LIST_PREF_SIZE = new Dimension(405, 135);

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

    private String homeFolderToolTipText = null;
    private String homeFolderAccessibleName = null;

    private String newFolderToolTipText = null;
    private String newFolderAccessibleName = null;

    private String listViewButtonToolTipText = null;
    private String listViewButtonAccessibleName = null;

    private String detailsViewButtonToolTipText = null;
    private String detailsViewButtonAccessibleName = null;

    private AlignedLabel fileNameLabel;
    private final PropertyChangeListener modeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
            if (fileNameLabel != null) {
                populateFileNameLabel();
            }
        }
    };

    private void populateFileNameLabel() {
        if (getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
            fileNameLabel.setText(folderNameLabelText);
            fileNameLabel.setDisplayedMnemonic(folderNameLabelMnemonic);
        } else {
            fileNameLabel.setText(fileNameLabelText);
            fileNameLabel.setDisplayedMnemonic(fileNameLabelMnemonic);
        }
    }

    public SynthFileChooserUIImpl(JFileChooser b) {
        super(b);
    }


    private class SynthFileChooserUIAccessor implements FilePane.FileChooserUIAccessor {
        public JFileChooser getFileChooser() {
            return SynthFileChooserUIImpl.this.getFileChooser();
        }

        public BasicDirectoryModel getModel() {
            return SynthFileChooserUIImpl.this.getModel();
        }

        public JPanel createList() {
            return null;
        }

        public JPanel createDetailsView() {
            return null;
        }

        public boolean isDirectorySelected() {
            return SynthFileChooserUIImpl.this.isDirectorySelected();
        }

        public File getDirectory() {
            return SynthFileChooserUIImpl.this.getDirectory();
        }

        public Action getChangeToParentDirectoryAction() {
            return SynthFileChooserUIImpl.this.getChangeToParentDirectoryAction();
        }

        public Action getApproveSelectionAction() {
            return SynthFileChooserUIImpl.this.getApproveSelectionAction();
        }

        public Action getNewFolderAction() {
            return SynthFileChooserUIImpl.this.getNewFolderAction();
        }

        public MouseListener createDoubleClickListener(JList<?> list) {
            return SynthFileChooserUIImpl.this.createDoubleClickListener(getFileChooser(),
                                                                     list);
        }

        public ListSelectionListener createListSelectionListener() {
            return SynthFileChooserUIImpl.this.createListSelectionListener(getFileChooser());
        }
    }

    protected void installDefaults(JFileChooser fc) {
        super.installDefaults(fc);
        readOnly = UIManager.getBoolean("FileChooser.readOnly");
    }

    @SuppressWarnings("serial") // anonymous classes inside
    public void installComponents(JFileChooser fc) {
        super.installComponents(fc);

        SynthContext context = getContext(fc, ENABLED);

        fc.setLayout(new BorderLayout(0, 11));

        // ********************************* //
        // **** Construct the top panel **** //
        // ********************************* //

        // Directory manipulation buttons
        JPanel topPanel = new JPanel(new BorderLayout(11, 0));
    JPanel topButtonPanel = new JPanel();
    topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.LINE_AXIS));
    topPanel.add(topButtonPanel, BorderLayout.AFTER_LINE_ENDS);

        // Add the top panel to the fileChooser
        fc.add(topPanel, BorderLayout.NORTH);

        // ComboBox Label
        lookInLabel = new JLabel(lookInLabelText);
        lookInLabel.setDisplayedMnemonic(lookInLabelMnemonic);
        topPanel.add(lookInLabel, BorderLayout.BEFORE_LINE_BEGINS);

        // CurrentDir ComboBox
        directoryComboBox = new JComboBox<File>();
        directoryComboBox.getAccessibleContext().setAccessibleDescription(lookInLabelText);
        directoryComboBox.putClientProperty( "JComboBox.isTableCellEditor", Boolean.TRUE );
        lookInLabel.setLabelFor(directoryComboBox);
        directoryComboBoxModel = createDirectoryComboBoxModel(fc);
        directoryComboBox.setModel(directoryComboBoxModel);
        directoryComboBox.addActionListener(directoryComboBoxAction);
        directoryComboBox.setRenderer(createDirectoryComboBoxRenderer(fc));
        directoryComboBox.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        directoryComboBox.setAlignmentY(JComponent.TOP_ALIGNMENT);
        directoryComboBox.setMaximumRowCount(8);
        topPanel.add(directoryComboBox, BorderLayout.CENTER);

        filePane = new FilePane(new SynthFileChooserUIAccessor());
        fc.addPropertyChangeListener(filePane);

        // Add 'Go Up' to context menu, plus 'Go Home' if on Unix
        JPopupMenu contextMenu = filePane.getComponentPopupMenu();
        if (contextMenu != null) {
            contextMenu.insert(getChangeToParentDirectoryAction(), 0);
            if (File.separatorChar == '/') {
                contextMenu.insert(getGoHomeAction(), 1);
            }
        }

    FileSystemView fsv = fc.getFileSystemView();

    // Up Button
    JButton upFolderButton = new JButton(getChangeToParentDirectoryAction());
    upFolderButton.setText(null);
    upFolderButton.setIcon(upFolderIcon);
    upFolderButton.setToolTipText(upFolderToolTipText);
    upFolderButton.getAccessibleContext().setAccessibleName(upFolderAccessibleName);
    upFolderButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    upFolderButton.setAlignmentY(JComponent.CENTER_ALIGNMENT);
    upFolderButton.setMargin(shrinkwrap);

    topButtonPanel.add(upFolderButton);
    topButtonPanel.add(Box.createRigidArea(hstrut5));

    // Home Button
    File homeDir = fsv.getHomeDirectory();
    String toolTipText = homeFolderToolTipText;

    JButton b = new JButton(homeFolderIcon);
    b.setToolTipText(toolTipText);
    b.getAccessibleContext().setAccessibleName(homeFolderAccessibleName);
    b.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    b.setAlignmentY(JComponent.CENTER_ALIGNMENT);
    b.setMargin(shrinkwrap);

    b.addActionListener(getGoHomeAction());
    topButtonPanel.add(b);
    topButtonPanel.add(Box.createRigidArea(hstrut5));

    // New Directory Button
    if (!readOnly) {
        b = new JButton(filePane.getNewFolderAction());
        b.setText(null);
        b.setIcon(newFolderIcon);
        b.setToolTipText(newFolderToolTipText);
        b.getAccessibleContext().setAccessibleName(newFolderAccessibleName);
        b.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        b.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        b.setMargin(shrinkwrap);
        topButtonPanel.add(b);
        topButtonPanel.add(Box.createRigidArea(hstrut5));
    }

    // View button group
    ButtonGroup viewButtonGroup = new ButtonGroup();

    // List Button
    listViewButton = new JToggleButton(listViewIcon);
    listViewButton.setToolTipText(listViewButtonToolTipText);
    listViewButton.getAccessibleContext().setAccessibleName(listViewButtonAccessibleName);
    listViewButton.setSelected(true);
    listViewButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    listViewButton.setAlignmentY(JComponent.CENTER_ALIGNMENT);
    listViewButton.setMargin(shrinkwrap);
    listViewButton.addActionListener(filePane.getViewTypeAction(FilePane.VIEWTYPE_LIST));
    topButtonPanel.add(listViewButton);
    viewButtonGroup.add(listViewButton);

    // Details Button
    detailsViewButton = new JToggleButton(detailsViewIcon);
    detailsViewButton.setToolTipText(detailsViewButtonToolTipText);
    detailsViewButton.getAccessibleContext().setAccessibleName(detailsViewButtonAccessibleName);
    detailsViewButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    detailsViewButton.setAlignmentY(JComponent.CENTER_ALIGNMENT);
    detailsViewButton.setMargin(shrinkwrap);
    detailsViewButton.addActionListener(filePane.getViewTypeAction(FilePane.VIEWTYPE_DETAILS));
    topButtonPanel.add(detailsViewButton);
    viewButtonGroup.add(detailsViewButton);

    filePane.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
            if ("viewType".equals(e.getPropertyName())) {
                int viewType = filePane.getViewType();
                switch (viewType) {
                    case FilePane.VIEWTYPE_LIST:
                        listViewButton.setSelected(true);
                        break;
                    case FilePane.VIEWTYPE_DETAILS:
                        detailsViewButton.setSelected(true);
                        break;
                }
            }
        }
    });

        // ************************************** //
        // ******* Add the directory pane ******* //
        // ************************************** //
        fc.add(getAccessoryPanel(), BorderLayout.AFTER_LINE_ENDS);
        JComponent accessory = fc.getAccessory();
        if (accessory != null) {
            getAccessoryPanel().add(accessory);
        }
        filePane.setPreferredSize(LIST_PREF_SIZE);
        fc.add(filePane, BorderLayout.CENTER);


        // ********************************** //
        // **** Construct the bottom panel ** //
        // ********************************** //
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        fc.add(bottomPanel, BorderLayout.SOUTH);

        // FileName label and textfield
        JPanel fileNamePanel = new JPanel();
        fileNamePanel.setLayout(new BoxLayout(fileNamePanel, BoxLayout.LINE_AXIS));
        bottomPanel.add(fileNamePanel);
        bottomPanel.add(Box.createRigidArea(new Dimension(1, 5)));

        fileNameLabel = new AlignedLabel();
        populateFileNameLabel();
        fileNamePanel.add(fileNameLabel);

        fileNameTextField = new JTextField(35) {
            public Dimension getMaximumSize() {
                return new Dimension(Short.MAX_VALUE, super.getPreferredSize().height);
            }
        };
        fileNamePanel.add(fileNameTextField);
        fileNameLabel.setLabelFor(fileNameTextField);
        fileNameTextField.addFocusListener(
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


        // Filetype label and combobox
        JPanel filesOfTypePanel = new JPanel();
        filesOfTypePanel.setLayout(new BoxLayout(filesOfTypePanel, BoxLayout.LINE_AXIS));
        bottomPanel.add(filesOfTypePanel);

        AlignedLabel filesOfTypeLabel = new AlignedLabel(filesOfTypeLabelText);
        filesOfTypeLabel.setDisplayedMnemonic(filesOfTypeLabelMnemonic);
        filesOfTypePanel.add(filesOfTypeLabel);

        filterComboBoxModel = createFilterComboBoxModel();
        fc.addPropertyChangeListener(filterComboBoxModel);
        filterComboBox = new JComboBox<FileFilter>(filterComboBoxModel);
        filterComboBox.getAccessibleContext().setAccessibleDescription(filesOfTypeLabelText);
        filesOfTypeLabel.setLabelFor(filterComboBox);
        filterComboBox.setRenderer(createFilterComboBoxRenderer());
        filesOfTypePanel.add(filterComboBox);


        // buttons
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new ButtonAreaLayout());

        buttonPanel.add(getApproveButton(fc));
        buttonPanel.add(getCancelButton(fc));

        if (fc.getControlButtonsAreShown()) {
            addControlButtons();
        }

        groupLabels(new AlignedLabel[] { fileNameLabel, filesOfTypeLabel });
    }

    protected void installListeners(JFileChooser fc) {
        super.installListeners(fc);
        fc.addPropertyChangeListener(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY, modeListener);
    }

    protected void uninstallListeners(JFileChooser fc) {
        fc.removePropertyChangeListener(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY, modeListener);
        super.uninstallListeners(fc);
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

    public void uninstallUI(JComponent c) {
        // Remove listeners
        c.removePropertyChangeListener(filterComboBoxModel);
        c.removePropertyChangeListener(filePane);

        if (filePane != null) {
            filePane.uninstallUI();
            filePane = null;
        }

        super.uninstallUI(c);
    }

    protected void installStrings(JFileChooser fc) {
        super.installStrings(fc);

        Locale l = fc.getLocale();

        lookInLabelMnemonic = getMnemonic("FileChooser.lookInLabelMnemonic", l);
        lookInLabelText = UIManager.getString("FileChooser.lookInLabelText", l);
        saveInLabelText = UIManager.getString("FileChooser.saveInLabelText", l);

        fileNameLabelMnemonic = getMnemonic("FileChooser.fileNameLabelMnemonic", l);
        fileNameLabelText = UIManager.getString("FileChooser.fileNameLabelText", l);
        folderNameLabelMnemonic = getMnemonic("FileChooser.folderNameLabelMnemonic", l);
        folderNameLabelText = UIManager.getString("FileChooser.folderNameLabelText", l);

        filesOfTypeLabelMnemonic = getMnemonic("FileChooser.filesOfTypeLabelMnemonic", l);
        filesOfTypeLabelText = UIManager.getString("FileChooser.filesOfTypeLabelText", l);

    upFolderToolTipText =  UIManager.getString("FileChooser.upFolderToolTipText",l);
    upFolderAccessibleName = UIManager.getString("FileChooser.upFolderAccessibleName",l);

    homeFolderToolTipText =  UIManager.getString("FileChooser.homeFolderToolTipText",l);
    homeFolderAccessibleName = UIManager.getString("FileChooser.homeFolderAccessibleName",l);

    newFolderToolTipText = UIManager.getString("FileChooser.newFolderToolTipText",l);
    newFolderAccessibleName = UIManager.getString("FileChooser.newFolderAccessibleName",l);

    listViewButtonToolTipText = UIManager.getString("FileChooser.listViewButtonToolTipText",l);
    listViewButtonAccessibleName = UIManager.getString("FileChooser.listViewButtonAccessibleName",l);

    detailsViewButtonToolTipText = UIManager.getString("FileChooser.detailsViewButtonToolTipText",l);
    detailsViewButtonAccessibleName = UIManager.getString("FileChooser.detailsViewButtonAccessibleName",l);
    }

    private int getMnemonic(String key, Locale l) {
        return SwingUtilities2.getUIDefaultsInt(key, l);
    }


    public String getFileName() {
        if (fileNameTextField != null) {
            return fileNameTextField.getText();
        } else {
            return null;
        }
    }

    public void setFileName(String fileName) {
        if (fileNameTextField != null) {
            fileNameTextField.setText(fileName);
        }
    }

    @Override public void rescanCurrentDirectory(JFileChooser fc) {
        filePane.rescanCurrentDirectory();
    }

    protected void doSelectedFileChanged(PropertyChangeEvent e) {
        super.doSelectedFileChanged(e);

        File f = (File) e.getNewValue();
        JFileChooser fc = getFileChooser();
        if (f != null
            && ((fc.isFileSelectionEnabled() && !f.isDirectory())
                || (f.isDirectory() && fc.isDirectorySelectionEnabled()))) {

            setFileName(fileNameString(f));
        }
    }

    protected void doSelectedFilesChanged(PropertyChangeEvent e) {
        super.doSelectedFilesChanged(e);

        File[] files = (File[]) e.getNewValue();
        JFileChooser fc = getFileChooser();
        if (files != null
            && files.length > 0
            && (files.length > 1 || fc.isDirectorySelectionEnabled() || !files[0].isDirectory())) {
            setFileName(fileNameString(files));
        }
    }

    protected void doDirectoryChanged(PropertyChangeEvent e) {
        super.doDirectoryChanged(e);

        JFileChooser fc = getFileChooser();
        FileSystemView fsv = fc.getFileSystemView();
        File currentDirectory = fc.getCurrentDirectory();

        if (!readOnly && currentDirectory != null) {
            getNewFolderAction().setEnabled(filePane.canWrite(currentDirectory));
        }

        if (currentDirectory != null) {
            JComponent cb = getDirectoryComboBox();
            if (cb instanceof JComboBox) {
                ComboBoxModel<?> model = ((JComboBox)cb).getModel();
                if (model instanceof DirectoryComboBoxModel) {
                    ((DirectoryComboBoxModel)model).addItem(currentDirectory);
                }
            }

            if (fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) {
                if (fsv.isFileSystem(currentDirectory)) {
                    setFileName(currentDirectory.getPath());
                } else {
                    setFileName(null);
                }
            }
        }
    }


    protected void doFileSelectionModeChanged(PropertyChangeEvent e) {
        super.doFileSelectionModeChanged(e);

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

    protected void doAccessoryChanged(PropertyChangeEvent e) {
        if (getAccessoryPanel() != null) {
            if (e.getOldValue() != null) {
                getAccessoryPanel().remove((JComponent)e.getOldValue());
            }
            JComponent accessory = (JComponent)e.getNewValue();
            if (accessory != null) {
                getAccessoryPanel().add(accessory, BorderLayout.CENTER);
            }
        }
    }

    protected void doControlButtonsChanged(PropertyChangeEvent e) {
        super.doControlButtonsChanged(e);

        if (getFileChooser().getControlButtonsAreShown()) {
            addControlButtons();
        } else {
            removeControlButtons();
        }
    }

    protected void addControlButtons() {
        if (bottomPanel != null) {
            bottomPanel.add(buttonPanel);
        }
    }

    protected void removeControlButtons() {
        if (bottomPanel != null) {
            bottomPanel.remove(buttonPanel);
        }
    }




    // *******************************************************
    // ************ FileChooser UI PLAF methods **************
    // *******************************************************

    protected ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        // add standard actions
        FilePane.addActionsToMap(map, filePane.getActions());
        // add synth only actions
        map.put("fileNameCompletion", getFileNameCompletionAction());
        return map;
    }

    // *****************************
    // ***** Directory Actions *****
    // *****************************

    protected JComponent getDirectoryComboBox() {
        return directoryComboBox;
    }

    protected Action getDirectoryComboBoxAction() {
        return directoryComboBoxAction;
    }

    protected DirectoryComboBoxRenderer createDirectoryComboBoxRenderer(JFileChooser fc) {
        return new DirectoryComboBoxRenderer(directoryComboBox.getRenderer());
    }

    //
    // Renderer for DirectoryComboBox
    //
    // Synth has some odd behavior with regards to renderers. Renderers are styled
    // in a specific manner by the SynthComboBoxUI. If we extend DefaultListCellRenderer
    // here, then we get none of those benefits or behaviors, leading to poor
    // looking combo boxes.
    // So what we do here is delegate most jobs to the "real" or original renderer,
    // and simply monkey with the icon and text of the renderer.
    private class DirectoryComboBoxRenderer implements ListCellRenderer<File> {
        private ListCellRenderer<? super File> delegate;
        IndentIcon ii = new IndentIcon();

        private DirectoryComboBoxRenderer(ListCellRenderer<? super File> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            assert c instanceof JLabel;
            JLabel label = (JLabel)c;
            if (value == null) {
                label.setText("");
                return label;
            }
            label.setText(getFileChooser().getName(value));
            Icon icon = getFileChooser().getIcon(value);
            ii.icon = icon;
            ii.depth = directoryComboBoxModel.getDepth(index);
            label.setIcon(ii);

            return label;
        }
    }

    static final int space = 10;
    class IndentIcon implements Icon {

        Icon icon = null;
        int depth = 0;

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (icon != null) {
                if (c.getComponentOrientation().isLeftToRight()) {
                    icon.paintIcon(c, g, x+depth*space, y);
                } else {
                    icon.paintIcon(c, g, x, y);
                }
            }
        }

        public int getIconWidth() {
            return ((icon != null) ? icon.getIconWidth() : 0) + depth*space;
        }

        public int getIconHeight() {
            return (icon != null) ? icon.getIconHeight() : 0;
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
    @SuppressWarnings("serial") // JDK-implementation class
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
            if (dir != null) {
                addItem(dir);
            }
        }

        /**
         * Adds the directory to the model and sets it to be selected,
         * additionally clears out the previous selected directory and
         * the paths leading up to it, if any.
         */
        public void addItem(File directory) {

            if (directory == null) {
                return;
            }

            boolean useShellFolder = FilePane.usesShellFolder(chooser);

            int oldSize = directories.size();
            directories.clear();
            if (oldSize > 0) {
                fireIntervalRemoved(this, 0, oldSize);
            }

            File[] baseFolders = (useShellFolder)
                    ? (File[]) ShellFolder.get("fileChooserComboBoxFolders")
                    : fsv.getRoots();
            directories.addAll(Arrays.asList(baseFolders));

            // Get the canonical (full) path. This has the side
            // benefit of removing extraneous chars from the path,
            // for example /foo/bar/ becomes /foo/bar
            File canonical;
            try {
                canonical = ShellFolder.getNormalizedFile(directory);
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

    /**
     * Acts when DirectoryComboBox has changed the selected item.
     */
    @SuppressWarnings("serial") // JDK-implementation class
    protected class DirectoryComboBoxAction extends AbstractAction {
        protected DirectoryComboBoxAction() {
            super("DirectoryComboBoxAction");
        }

        public void actionPerformed(ActionEvent e) {
            directoryComboBox.hidePopup();
            JComponent cb = getDirectoryComboBox();
            if (cb instanceof JComboBox) {
                File f = (File)((JComboBox)cb).getSelectedItem();
                getFileChooser().setCurrentDirectory(f);
            }
        }
    }

    //
    // Renderer for Types ComboBox
    //
    protected FilterComboBoxRenderer createFilterComboBoxRenderer() {
        return new FilterComboBoxRenderer(filterComboBox.getRenderer());
    }

    /**
     * Render different type sizes and styles.
     */
    public class FilterComboBoxRenderer implements ListCellRenderer<FileFilter> {
        private ListCellRenderer<? super FileFilter> delegate;
        private FilterComboBoxRenderer(ListCellRenderer<? super FileFilter> delegate) {
            this.delegate = delegate;
        }

        public Component getListCellRendererComponent(JList<? extends FileFilter> list, FileFilter value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            String text = null;
            if (value != null) {
                text = value.getDescription();
            }

            //this should always be true, since SynthComboBoxUI's SynthComboBoxRenderer
            //extends JLabel
            assert c instanceof JLabel;
            if (text != null) {
                ((JLabel)c).setText(text);
            }
            return c;
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
    @SuppressWarnings("serial") // JDK-implementation class
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



    /**
     * <code>ButtonAreaLayout</code> behaves in a similar manner to
     * <code>FlowLayout</code>. It lays out all components from left to
     * right, flushed right. The widths of all components will be set
     * to the largest preferred size width.
     */
    private static class ButtonAreaLayout implements LayoutManager {
        private int hGap = 5;
        private int topMargin = 17;

        public void addLayoutComponent(String string, Component comp) {
        }

        public void layoutContainer(Container container) {
            Component[] children = container.getComponents();

            if (children != null && children.length > 0) {
                int         numChildren = children.length;
                Dimension[] sizes = new Dimension[numChildren];
                Insets      insets = container.getInsets();
                int         yLocation = insets.top + topMargin;
                int         maxWidth = 0;

                for (int counter = 0; counter < numChildren; counter++) {
                    sizes[counter] = children[counter].getPreferredSize();
                    maxWidth = Math.max(maxWidth, sizes[counter].width);
                }
                int xLocation, xOffset;
                if (container.getComponentOrientation().isLeftToRight()) {
                    xLocation = container.getSize().width - insets.left - maxWidth;
                    xOffset = hGap + maxWidth;
                } else {
                    xLocation = insets.left;
                    xOffset = -(hGap + maxWidth);
                }
                for (int counter = numChildren - 1; counter >= 0; counter--) {
                    children[counter].setBounds(xLocation, yLocation,
                                                maxWidth, sizes[counter].height);
                    xLocation -= xOffset;
                }
            }
        }

        public Dimension minimumLayoutSize(Container c) {
            if (c != null) {
                Component[] children = c.getComponents();

                if (children != null && children.length > 0) {
                    int       numChildren = children.length;
                    int       height = 0;
                    Insets    cInsets = c.getInsets();
                    int       extraHeight = topMargin + cInsets.top + cInsets.bottom;
                    int       extraWidth = cInsets.left + cInsets.right;
                    int       maxWidth = 0;

                    for (int counter = 0; counter < numChildren; counter++) {
                        Dimension aSize = children[counter].getPreferredSize();
                        height = Math.max(height, aSize.height);
                        maxWidth = Math.max(maxWidth, aSize.width);
                    }
                    return new Dimension(extraWidth + numChildren * maxWidth +
                                         (numChildren - 1) * hGap,
                                         extraHeight + height);
                }
            }
            return new Dimension(0, 0);
        }

        public Dimension preferredLayoutSize(Container c) {
            return minimumLayoutSize(c);
        }

        public void removeLayoutComponent(Component c) { }
    }

    private static void groupLabels(AlignedLabel[] group) {
        for (int i = 0; i < group.length; i++) {
            group[i].group = group;
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    private class AlignedLabel extends JLabel {
        private AlignedLabel[] group;
        private int maxWidth = 0;

        AlignedLabel() {
            super();
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
        }

        AlignedLabel(String text) {
            super(text);
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
        }

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            // Align the width with all other labels in group.
            return new Dimension(getMaxWidth() + 11, d.height);
        }

        private int getMaxWidth() {
            if (maxWidth == 0 && group != null) {
                int max = 0;
                for (int i = 0; i < group.length; i++) {
                    max = Math.max(group[i].getSuperPreferredWidth(), max);
                }
                for (int i = 0; i < group.length; i++) {
                    group[i].maxWidth = max;
                }
            }
            return maxWidth;
        }

        private int getSuperPreferredWidth() {
            return super.getPreferredSize().width;
        }
    }
}
