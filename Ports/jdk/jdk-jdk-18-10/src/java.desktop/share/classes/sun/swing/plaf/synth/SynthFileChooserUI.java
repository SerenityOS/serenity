/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.plaf.synth.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicFileChooserUI;

/**
 * Synth FileChooserUI.
 *
 * Note: This class is abstract. It does not actually create the file chooser GUI.
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
public abstract class SynthFileChooserUI extends BasicFileChooserUI implements
                           SynthUI {
    private JButton approveButton, cancelButton;

    private SynthStyle style;

    // Some generic FileChooser functions
    private Action fileNameCompletionAction = new FileNameCompletionAction();

    private FileFilter actualFileFilter = null;
    private GlobFilter globFilter = null;

    public static ComponentUI createUI(JComponent c) {
        return new SynthFileChooserUIImpl((JFileChooser)c);
    }

    public SynthFileChooserUI(JFileChooser b) {
        super(b);
    }

    public SynthContext getContext(JComponent c) {
        return new SynthContext(c, Region.FILE_CHOOSER, style,
                                getComponentState(c));
    }

    protected SynthContext getContext(JComponent c, int state) {
        Region region = SynthLookAndFeel.getRegion(c);
        return new SynthContext(c, Region.FILE_CHOOSER, style, state);
    }

    private Region getRegion(JComponent c) {
        return SynthLookAndFeel.getRegion(c);
    }

    private int getComponentState(JComponent c) {
        if (c.isEnabled()) {
            if (c.isFocusOwner()) {
                return ENABLED | FOCUSED;
            }
            return ENABLED;
        }
        return DISABLED;
    }

    private void updateStyle(JComponent c) {
        SynthStyle newStyle = SynthLookAndFeel.getStyleFactory().getStyle(c,
                                               Region.FILE_CHOOSER);
        if (newStyle != style) {
            if (style != null) {
                style.uninstallDefaults(getContext(c, ENABLED));
            }
            style = newStyle;
            SynthContext context = getContext(c, ENABLED);
            style.installDefaults(context);
            Border border = c.getBorder();
            if (border == null || border instanceof UIResource) {
                c.setBorder(new UIBorder(style.getInsets(context, null)));
            }

            directoryIcon = style.getIcon(context, "FileView.directoryIcon");
            fileIcon = style.getIcon(context, "FileView.fileIcon");
            computerIcon = style.getIcon(context, "FileView.computerIcon");
            hardDriveIcon = style.getIcon(context, "FileView.hardDriveIcon");
            floppyDriveIcon = style.getIcon(context, "FileView.floppyDriveIcon");

            newFolderIcon    = style.getIcon(context, "FileChooser.newFolderIcon");
            upFolderIcon     = style.getIcon(context, "FileChooser.upFolderIcon");
            homeFolderIcon   = style.getIcon(context, "FileChooser.homeFolderIcon");
            detailsViewIcon  = style.getIcon(context, "FileChooser.detailsViewIcon");
            listViewIcon     = style.getIcon(context, "FileChooser.listViewIcon");
        }
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        SwingUtilities.replaceUIActionMap(c, createActionMap());
    }

    public void installComponents(JFileChooser fc) {
        SynthContext context = getContext(fc, ENABLED);

        cancelButton = new JButton(cancelButtonText);
        cancelButton.setName("SynthFileChooser.cancelButton");
        cancelButton.setIcon(context.getStyle().getIcon(context, "FileChooser.cancelIcon"));
        cancelButton.setMnemonic(cancelButtonMnemonic);
        cancelButton.setToolTipText(cancelButtonToolTipText);
        cancelButton.addActionListener(getCancelSelectionAction());

        approveButton = new JButton(getApproveButtonText(fc));
        approveButton.setName("SynthFileChooser.approveButton");
        approveButton.setIcon(context.getStyle().getIcon(context, "FileChooser.okIcon"));
        approveButton.setMnemonic(getApproveButtonMnemonic(fc));
        approveButton.setToolTipText(getApproveButtonToolTipText(fc));
        approveButton.addActionListener(getApproveSelectionAction());

    }

    public void uninstallComponents(JFileChooser fc) {
        fc.removeAll();
    }

    protected void installListeners(JFileChooser fc) {
        super.installListeners(fc);

        getModel().addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                // Update the selection after JList has been updated
                new DelayedSelectionUpdater();
            }
            public void intervalAdded(ListDataEvent e) {
                new DelayedSelectionUpdater();
            }
            public void intervalRemoved(ListDataEvent e) {
            }
        });

    }

    private class DelayedSelectionUpdater implements Runnable {
        DelayedSelectionUpdater() {
            SwingUtilities.invokeLater(this);
        }

        public void run() {
            updateFileNameCompletion();
        }
    }

    protected abstract ActionMap createActionMap();


    protected void installDefaults(JFileChooser fc) {
        super.installDefaults(fc);
        updateStyle(fc);
    }

    protected void uninstallDefaults(JFileChooser fc) {
        super.uninstallDefaults(fc);

        SynthContext context = getContext(getFileChooser(), ENABLED);
        style.uninstallDefaults(context);
        style = null;
    }

    protected void installIcons(JFileChooser fc) {
        // The icons are installed in updateStyle, not here
    }

    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        if (c.isOpaque()) {
            g.setColor(style.getColor(context, ColorType.BACKGROUND));
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }

        style.getPainter(context).paintFileChooserBackground(context,
                                    g, 0, 0, c.getWidth(), c.getHeight());
        paint(context, g);
    }

    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    public void paint(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        paint(context, g);
    }

    protected void paint(SynthContext context, Graphics g) {
    }

    public abstract void setFileName(String fileName);
    public abstract String getFileName();

    protected void doSelectedFileChanged(PropertyChangeEvent e) {
    }

    protected void doSelectedFilesChanged(PropertyChangeEvent e) {
    }

    protected void doDirectoryChanged(PropertyChangeEvent e) {
    }

    protected void doAccessoryChanged(PropertyChangeEvent e) {
    }

    protected void doFileSelectionModeChanged(PropertyChangeEvent e) {
    }

    protected void doMultiSelectionChanged(PropertyChangeEvent e) {
        if (!getFileChooser().isMultiSelectionEnabled()) {
            getFileChooser().setSelectedFiles(null);
        }
    }

    protected void doControlButtonsChanged(PropertyChangeEvent e) {
        if (getFileChooser().getControlButtonsAreShown()) {
            approveButton.setText(getApproveButtonText(getFileChooser()));
            approveButton.setToolTipText(getApproveButtonToolTipText(getFileChooser()));
            approveButton.setMnemonic(getApproveButtonMnemonic(getFileChooser()));
        }
    }

    protected void doAncestorChanged(PropertyChangeEvent e) {
    }

    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return new SynthFCPropertyChangeListener();
    }

    private class SynthFCPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (prop.equals(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
                doFileSelectionModeChanged(e);
            } else if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                doSelectedFileChanged(e);
            } else if (prop.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
                doSelectedFilesChanged(e);
            } else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                doDirectoryChanged(e);
            } else if (prop == JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY) {
                doMultiSelectionChanged(e);
            } else if (prop == JFileChooser.ACCESSORY_CHANGED_PROPERTY) {
                doAccessoryChanged(e);
            } else if (prop == JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY ||
                       prop == JFileChooser.APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY ||
                       prop == JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY ||
                       prop == JFileChooser.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY) {
                doControlButtonsChanged(e);
            } else if (prop.equals("componentOrientation")) {
                ComponentOrientation o = (ComponentOrientation)e.getNewValue();
                JFileChooser cc = (JFileChooser)e.getSource();
                if (o != (ComponentOrientation)e.getOldValue()) {
                    cc.applyComponentOrientation(o);
                }
            } else if (prop.equals("ancestor")) {
                doAncestorChanged(e);
            }
        }
    }


    /**
     * Responds to a File Name completion request (e.g. Tab)
     */
    @SuppressWarnings("serial") // JDK-implementation class
    private class FileNameCompletionAction extends AbstractAction {
        protected FileNameCompletionAction() {
            super("fileNameCompletion");
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = getFileChooser();

            String fileName = getFileName();

            if (fileName != null) {
                // Remove whitespace from beginning and end of filename
                fileName = fileName.trim();
            }

            resetGlobFilter();

            if (fileName == null || fileName.isEmpty() ||
                    (chooser.isMultiSelectionEnabled() && fileName.startsWith("\""))) {
                return;
            }

            FileFilter currentFilter = chooser.getFileFilter();
            if (globFilter == null) {
                globFilter = new GlobFilter();
            }
            try {
                globFilter.setPattern(!isGlobPattern(fileName) ? fileName + "*" : fileName);
                if (!(currentFilter instanceof GlobFilter)) {
                    actualFileFilter = currentFilter;
                }
                chooser.setFileFilter(null);
                chooser.setFileFilter(globFilter);
                fileNameCompletionString = fileName;
            } catch (PatternSyntaxException pse) {
                // Not a valid glob pattern. Abandon filter.
            }
        }
    }

    private String fileNameCompletionString;

    private void updateFileNameCompletion() {
        if (fileNameCompletionString != null) {
            if (fileNameCompletionString.equals(getFileName())) {
                File[] files = getModel().getFiles().toArray(new File[0]);
                String str = getCommonStartString(files);
                if (str != null && str.startsWith(fileNameCompletionString)) {
                    setFileName(str);
                }
                fileNameCompletionString = null;
            }
        }
    }

    private String getCommonStartString(File[] files) {
        String str = null;
        String str2 = null;
        int i = 0;
        if (files.length == 0) {
            return null;
        }
        while (true) {
            for (int f = 0; f < files.length; f++) {
                String name = files[f].getName();
                if (f == 0) {
                    if (name.length() == i) {
                        return str;
                    }
                    str2 = name.substring(0, i+1);
                }
                if (!name.startsWith(str2)) {
                    return str;
                }
            }
            str = str2;
            i++;
        }
    }

    private void resetGlobFilter() {
        if (actualFileFilter != null) {
            JFileChooser chooser = getFileChooser();
            FileFilter currentFilter = chooser.getFileFilter();
            if (currentFilter != null && currentFilter.equals(globFilter)) {
                chooser.setFileFilter(actualFileFilter);
                chooser.removeChoosableFileFilter(globFilter);
            }
            actualFileFilter = null;
        }
    }

    private static boolean isGlobPattern(String fileName) {
        return ((File.separatorChar == '\\' && fileName.indexOf('*') >= 0)
                || (File.separatorChar == '/' && (fileName.indexOf('*') >= 0
                                                  || fileName.indexOf('?') >= 0
                                                  || fileName.indexOf('[') >= 0)));
    }


    /* A file filter which accepts file patterns containing
     * the special wildcard '*' on windows, plus '?', and '[ ]' on Unix.
     */
    class GlobFilter extends FileFilter {
        Pattern pattern;
        String globPattern;

        public void setPattern(String globPattern) {
            char[] gPat = globPattern.toCharArray();
            char[] rPat = new char[gPat.length * 2];
            boolean isWin32 = (File.separatorChar == '\\');
            boolean inBrackets = false;
            int j = 0;

            this.globPattern = globPattern;

            if (isWin32) {
                // On windows, a pattern ending with *.* is equal to ending with *
                int len = gPat.length;
                if (globPattern.endsWith("*.*")) {
                    len -= 2;
                }
                for (int i = 0; i < len; i++) {
                    if (gPat[i] == '*') {
                        rPat[j++] = '.';
                    }
                    rPat[j++] = gPat[i];
                }
            } else {
                for (int i = 0; i < gPat.length; i++) {
                    switch(gPat[i]) {
                      case '*':
                        if (!inBrackets) {
                            rPat[j++] = '.';
                        }
                        rPat[j++] = '*';
                        break;

                      case '?':
                        rPat[j++] = inBrackets ? '?' : '.';
                        break;

                      case '[':
                        inBrackets = true;
                        rPat[j++] = gPat[i];

                        if (i < gPat.length - 1) {
                            switch (gPat[i+1]) {
                              case '!':
                              case '^':
                                rPat[j++] = '^';
                                i++;
                                break;

                              case ']':
                                rPat[j++] = gPat[++i];
                                break;
                            }
                        }
                        break;

                      case ']':
                        rPat[j++] = gPat[i];
                        inBrackets = false;
                        break;

                      case '\\':
                        if (i == 0 && gPat.length > 1 && gPat[1] == '~') {
                            rPat[j++] = gPat[++i];
                        } else {
                            rPat[j++] = '\\';
                            if (i < gPat.length - 1 && "*?[]".indexOf(gPat[i+1]) >= 0) {
                                rPat[j++] = gPat[++i];
                            } else {
                                rPat[j++] = '\\';
                            }
                        }
                        break;

                      default:
                        //if ("+()|^$.{}<>".indexOf(gPat[i]) >= 0) {
                        if (!Character.isLetterOrDigit(gPat[i])) {
                            rPat[j++] = '\\';
                        }
                        rPat[j++] = gPat[i];
                        break;
                    }
                }
            }
            this.pattern = Pattern.compile(new String(rPat, 0, j), Pattern.CASE_INSENSITIVE);
        }

        public boolean accept(File f) {
            if (f == null) {
                return false;
            }
            if (f.isDirectory()) {
                return true;
            }
            return pattern.matcher(f.getName()).matches();
        }

        public String getDescription() {
            return globPattern;
        }
    }


    // *******************************************************
    // ************ FileChooser UI PLAF methods **************
    // *******************************************************


    // *****************************
    // ***** Directory Actions *****
    // *****************************

    public Action getFileNameCompletionAction() {
        return fileNameCompletionAction;
    }


    protected JButton getApproveButton(JFileChooser fc) {
        return approveButton;
    }

    protected JButton getCancelButton(JFileChooser fc) {
        return cancelButton;
    }


    // Overload to do nothing.   We don't have and icon cache.
    public void clearIconCache() { }

    // Copied as SynthBorder is package private in synth
    @SuppressWarnings("serial") // JDK-implementation clas
    private class UIBorder extends AbstractBorder implements UIResource {
        private Insets _insets;
        UIBorder(Insets insets) {
            if (insets != null) {
                _insets = new Insets(insets.top, insets.left, insets.bottom,
                                     insets.right);
            }
            else {
                _insets = null;
            }
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            if (!(c instanceof JComponent)) {
                return;
            }
            JComponent jc = (JComponent)c;
            SynthContext context = getContext(jc);
            SynthStyle style = context.getStyle();
            if (style != null) {
                style.getPainter(context).paintFileChooserBorder(
                      context, g, x, y, width, height);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            if (insets == null) {
                insets = new Insets(0, 0, 0, 0);
            }
            if (_insets != null) {
                insets.top = _insets.top;
                insets.bottom = _insets.bottom;
                insets.left = _insets.left;
                insets.right = _insets.right;
            }
            else {
                insets.top = insets.bottom = insets.right = insets.left = 0;
            }
            return insets;
        }
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
