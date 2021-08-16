/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
package sun.swing;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Image;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JToolBar;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.ButtonGroup;
import javax.swing.UIManager;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Box;

import javax.swing.border.EmptyBorder;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileSystemView;

import sun.awt.shell.ShellFolder;
import sun.awt.OSInfo;

/**
 * <b>WARNING:</b> This class is an implementation detail and is only
 * public so that it can be used by two packages. You should NOT consider
 * this public API.
 *
 * @author Leif Samuelsson
 */
@SuppressWarnings("serial") // JDK-implementation class
public class WindowsPlacesBar extends JToolBar
                              implements ActionListener, PropertyChangeListener {
    JFileChooser fc;
    JToggleButton[] buttons;
    ButtonGroup buttonGroup;
    File[] files;
    final Dimension buttonSize;

    public WindowsPlacesBar(JFileChooser fc, boolean isXPStyle) {
        super(JToolBar.VERTICAL);
        this.fc = fc;
        setFloatable(false);
        putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        boolean isXPPlatform = (OSInfo.getOSType() == OSInfo.OSType.WINDOWS &&
                OSInfo.getWindowsVersion().compareTo(OSInfo.WINDOWS_XP) >= 0);

        if (isXPStyle) {
            buttonSize = new Dimension(83, 69);
            putClientProperty("XPStyle.subAppName", "placesbar");
            setBorder(new EmptyBorder(1, 1, 1, 1));
        } else {
            // The button size almost matches the XP style when in Classic style on XP
            buttonSize = new Dimension(83, isXPPlatform ? 65 : 54);
            setBorder(new BevelBorder(BevelBorder.LOWERED,
                                      UIManager.getColor("ToolBar.highlight"),
                                      UIManager.getColor("ToolBar.background"),
                                      UIManager.getColor("ToolBar.darkShadow"),
                                      UIManager.getColor("ToolBar.shadow")));
        }
        Color bgColor = new Color(UIManager.getColor("ToolBar.shadow").getRGB());
        setBackground(bgColor);
        FileSystemView fsv = fc.getFileSystemView();

        files = fsv.getChooserShortcutPanelFiles();

        buttons = new JToggleButton[files.length];
        buttonGroup = new ButtonGroup();
        for (int i = 0; i < files.length; i++) {
            if (fsv.isFileSystemRoot(files[i])) {
                // Create special File wrapper for drive path
                files[i] = fsv.createFileObject(files[i].getAbsolutePath());
            }

            String folderName = fsv.getSystemDisplayName(files[i]);
            int index = folderName.lastIndexOf(File.separatorChar);
            if (index >= 0 && index < folderName.length() - 1) {
                folderName = folderName.substring(index + 1);
            }
            Icon icon;
            if (files[i] instanceof ShellFolder) {
                // We want a large icon, fsv only gives us a small.
                ShellFolder sf = (ShellFolder)files[i];
                Image image = sf.getIcon(true);

                if (image == null) {
                    // Get default image
                    image = (Image) ShellFolder.get("shell32LargeIcon 1");
                }

                icon = image == null ? null : new ImageIcon(image, sf.getFolderType());
            } else {
                icon = fsv.getSystemIcon(files[i]);
            }
            buttons[i] = new JToggleButton(folderName, icon);
            if (isXPStyle) {
                buttons[i].putClientProperty("XPStyle.subAppName", "placesbar");
            } else {
                Color fgColor = new Color(UIManager.getColor("List.selectionForeground").getRGB());
                buttons[i].setContentAreaFilled(false);
                buttons[i].setForeground(fgColor);
            }
            buttons[i].setMargin(new Insets(3, 2, 1, 2));
            buttons[i].setFocusPainted(false);
            buttons[i].setIconTextGap(0);
            buttons[i].setHorizontalTextPosition(JToggleButton.CENTER);
            buttons[i].setVerticalTextPosition(JToggleButton.BOTTOM);
            buttons[i].setAlignmentX(JComponent.CENTER_ALIGNMENT);
            buttons[i].setPreferredSize(buttonSize);
            buttons[i].setMaximumSize(buttonSize);
            buttons[i].addActionListener(this);
            add(buttons[i]);
            if (i < files.length-1 && isXPStyle) {
                add(Box.createRigidArea(new Dimension(1, 1)));
            }
            buttonGroup.add(buttons[i]);
        }
        doDirectoryChanged(fc.getCurrentDirectory());
    }

    protected void doDirectoryChanged(File f) {
        for (int i=0; i<buttons.length; i++) {
            JToggleButton b = buttons[i];
            if (files[i].equals(f)) {
                b.setSelected(true);
                break;
            } else if (b.isSelected()) {
                // Remove temporarily from group because it doesn't
                // allow for no button to be selected.
                buttonGroup.remove(b);
                b.setSelected(false);
                buttonGroup.add(b);
            }
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop == JFileChooser.DIRECTORY_CHANGED_PROPERTY) {
            doDirectoryChanged(fc.getCurrentDirectory());
        }
    }

    public void actionPerformed(ActionEvent e) {
        JToggleButton b = (JToggleButton)e.getSource();
        for (int i=0; i<buttons.length; i++) {
            if (b == buttons[i]) {
                fc.setCurrentDirectory(files[i]);
                break;
            }
        }
    }

    public Dimension getPreferredSize() {
        Dimension min  = super.getMinimumSize();
        Dimension pref = super.getPreferredSize();
        int h = min.height;
        if (buttons != null && buttons.length > 0 && buttons.length < 5) {
            JToggleButton b = buttons[0];
            if (b != null) {
                int bh = 5 * (b.getPreferredSize().height + 1);
                if (bh > h) {
                    h = bh;
                }
            }
        }
        if (h > pref.height) {
            pref = new Dimension(pref.width, h);
        }
        return pref;
    }
}
