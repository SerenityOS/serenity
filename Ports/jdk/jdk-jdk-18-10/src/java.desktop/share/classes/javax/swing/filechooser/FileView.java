/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.filechooser;

import java.io.File;
import javax.swing.*;

/**
 * <code>FileView</code> defines an abstract class that can be implemented
 * to provide the filechooser with UI information for a <code>File</code>.
 * Each L&amp;F <code>JFileChooserUI</code> object implements this
 * class to pass back the correct icons and type descriptions specific to
 * that L&amp;F. For example, the Microsoft Windows L&amp;F returns the
 * generic Windows icons for directories and generic files.
 * Additionally, you may want to provide your own <code>FileView</code> to
 * <code>JFileChooser</code> to return different icons or additional
 * information using {@link javax.swing.JFileChooser#setFileView}.
 *
 * <p>
 *
 * <code>JFileChooser</code> first looks to see if there is a user defined
 * <code>FileView</code>, if there is, it gets type information from
 * there first. If <code>FileView</code> returns <code>null</code> for
 * any method, <code>JFileChooser</code> then uses the L&amp;F specific
 * view to get the information.
 * So, for example, if you provide a <code>FileView</code> class that
 * returns an <code>Icon</code> for JPG files, and returns <code>null</code>
 * icons for all other files, the UI's <code>FileView</code> will provide
 * default icons for all other files.
 *
 * <p>
 *
 * For an example implementation of a simple file view, see
 * <code><i>yourJDK</i>/demo/jfc/FileChooserDemo/ExampleFileView.java</code>.
 * For more information and examples see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html">How to Use File Choosers</a>,
 * a section in <em>The Java Tutorial</em>.
 *
 * @see javax.swing.JFileChooser
 *
 * @author Jeff Dinkins
 *
 */
public abstract class FileView {
    /**
     * Constructor for subclasses to call.
     */
    protected FileView() {}

    /**
     * The name of the file. Normally this would be simply
     * <code>f.getName()</code>.
     *
     * @param f a {@code File} object
     * @return a {@code String} representing the name of the file
     */
    public String getName(File f) {
        return null;
    };

    /**
     * A human readable description of the file. For example,
     * a file named <i>jag.jpg</i> might have a description that read:
     * "A JPEG image file of James Gosling's face".
     *
     * @param f a {@code File} object
     * @return a {@code String} containing a description of the file or
     *         {@code null} if it is not available.
     *
     */
    public String getDescription(File f) {
        return null;
    }

    /**
     * A human readable description of the type of the file. For
     * example, a <code>jpg</code> file might have a type description of:
     * "A JPEG Compressed Image File"
     *
     * @param f a {@code File} object
     * @return a {@code String} containing a description of the type of the file
     *         or {@code null} if it is not available   .
     */
    public String getTypeDescription(File f) {
        return null;
    }

    /**
     * The icon that represents this file in the <code>JFileChooser</code>.
     *
     * @param f a {@code File} object
     * @return an {@code Icon} which represents the specified {@code File} or
     *         {@code null} if it is not available.
     */
    public Icon getIcon(File f) {
        return null;
    }

    /**
     * Whether the directory is traversable or not. This might be
     * useful, for example, if you want a directory to represent
     * a compound document and don't want the user to descend into it.
     *
     * @param f a {@code File} object representing a directory
     * @return {@code true} if the directory is traversable,
     *         {@code false} if it is not, and {@code null} if the
     *         file system should be checked.
     * @see FileSystemView#isTraversable
     */
    public Boolean isTraversable(File f) {
        return null;
    }

}
