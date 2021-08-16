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

package javax.swing.plaf;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import java.io.File;

/**
 * Pluggable look and feel interface for <code>JFileChooser</code>.
 *
 * @author Jeff Dinkins
 */

public abstract class FileChooserUI extends ComponentUI
{
    /**
     * Constructor for subclasses to call.
     */
    protected FileChooserUI() {}

    /**
     * Returns an accept-all file filter.
     * @param fc the file chooser
     * @return an accept-all file filter
     */
    public abstract FileFilter getAcceptAllFileFilter(JFileChooser fc);
    /**
     * Returns a file view.
     * @param fc the file chooser
     * @return a file view
     */
    public abstract FileView getFileView(JFileChooser fc);

    /**
     * Returns approve button text.
     * @param fc the file chooser
     * @return approve button text.
     */
    public abstract String getApproveButtonText(JFileChooser fc);
    /**
     * Returns the dialog title.
     * @param fc the file chooser
     * @return the dialog title.
     */
    public abstract String getDialogTitle(JFileChooser fc);

    /**
     * Rescan the current directory.
     * @param fc the file chooser
     */
    public abstract void rescanCurrentDirectory(JFileChooser fc);
    /**
     * Ensure the file in question is visible.
     * @param fc the file chooser
     * @param f the file
     */
    public abstract void ensureFileIsVisible(JFileChooser fc, File f);

    /**
     * Returns default button for current <code>LookAndFeel</code>.
     * <code>JFileChooser</code> will use this button as default button
     * for dialog windows.
     *
     * @param fc the {@code JFileChooser} whose default button is requested
     * @return the default JButton for current look and feel
     * @since 1.7
     */
    public JButton getDefaultButton(JFileChooser fc) {
        return null;
    }
}
