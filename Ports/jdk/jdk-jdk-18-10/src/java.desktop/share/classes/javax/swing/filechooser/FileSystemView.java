/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

import jdk.internal.ref.CleanerFactory;
import sun.awt.shell.ShellFolder;

/**
 * FileSystemView is JFileChooser's gateway to the
 * file system. Since the JDK1.1 File API doesn't allow
 * access to such information as root partitions, file type
 * information, or hidden file bits, this class is designed
 * to intuit as much OS-specific file system information as
 * possible.
 *
 * <p>
 *
 * Java Licensees may want to provide a different implementation of
 * FileSystemView to better handle a given operating system.
 *
 * @author Jeff Dinkins
 */

// PENDING(jeff) - need to provide a specification for
// how Mac/OS2/BeOS/etc file systems can modify FileSystemView
// to handle their particular type of file system.

public abstract class FileSystemView {

    static FileSystemView windowsFileSystemView = null;
    static FileSystemView unixFileSystemView = null;
    //static FileSystemView macFileSystemView = null;
    static FileSystemView genericFileSystemView = null;

    private boolean useSystemExtensionHiding =
            UIManager.getDefaults().getBoolean("FileChooser.useSystemExtensionHiding");

    /**
     * Returns the file system view.
     * @return the file system view
     */
    public static FileSystemView getFileSystemView() {
        if(File.separatorChar == '\\') {
            if(windowsFileSystemView == null) {
                windowsFileSystemView = new WindowsFileSystemView();
            }
            return windowsFileSystemView;
        }

        if(File.separatorChar == '/') {
            if(unixFileSystemView == null) {
                unixFileSystemView = new UnixFileSystemView();
            }
            return unixFileSystemView;
        }

        // if(File.separatorChar == ':') {
        //    if(macFileSystemView == null) {
        //      macFileSystemView = new MacFileSystemView();
        //    }
        //    return macFileSystemView;
        //}

        if(genericFileSystemView == null) {
            genericFileSystemView = new GenericFileSystemView();
        }
        return genericFileSystemView;
    }

    /**
     * Constructs a FileSystemView.
     */
    public FileSystemView() {
        final WeakReference<FileSystemView> weakReference = new WeakReference<>(this);
        final PropertyChangeListener pcl = evt -> {
            final FileSystemView fsv = weakReference.get();
            if (fsv != null && evt.getPropertyName().equals("lookAndFeel")) {
                fsv.useSystemExtensionHiding =
                        UIManager.getDefaults().getBoolean(
                                "FileChooser.useSystemExtensionHiding");
            }
        };

        UIManager.addPropertyChangeListener(pcl);
        CleanerFactory.cleaner().register(this, () -> {
            UIManager.removePropertyChangeListener(pcl);
        });
    }

    /**
     * Determines if the given file is a root in the navigable tree(s).
     * Examples: Windows 98 has one root, the Desktop folder. DOS has one root
     * per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one root,
     * the <code>"/"</code> directory.
     *
     * The default implementation gets information from the <code>ShellFolder</code> class.
     *
     * @param f a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root in the navigable tree.
     * @see #isFileSystemRoot
     */
    public boolean isRoot(File f) {
        if (f == null || !f.isAbsolute()) {
            return false;
        }

        File[] roots = getRoots();
        for (File root : roots) {
            if (root.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the file (directory) can be visited.
     * Returns false if the directory cannot be traversed.
     *
     * @param f the <code>File</code>
     * @return <code>true</code> if the file/directory can be traversed, otherwise <code>false</code>
     * @see JFileChooser#isTraversable
     * @see FileView#isTraversable
     * @since 1.4
     */
    public Boolean isTraversable(File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    /**
     * Name of a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "M:\" directory
     * displays as "CD-ROM (M:)"
     *
     * The default implementation gets information from the ShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return the file name as it would be displayed by a native file chooser
     * @see JFileChooser#getName
     * @since 1.4
     */
    public String getSystemDisplayName(File f) {
        if (f == null) {
            return null;
        }

        String name = f.getName();

        if (!name.equals("..") && !name.equals(".") &&
                (useSystemExtensionHiding || !isFileSystem(f) || isFileSystemRoot(f)) &&
                (f instanceof ShellFolder || f.exists())) {

            try {
                name = getShellFolder(f).getDisplayName();
            } catch (FileNotFoundException e) {
                return null;
            }

            if (name == null || name.length() == 0) {
                name = f.getPath(); // e.g. "/"
            }
        }

        return name;
    }

    /**
     * Type description for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "Desktop" folder
     * is described as "Desktop".
     *
     * Override for platforms with native ShellFolder implementations.
     *
     * @param f a <code>File</code> object
     * @return the file type description as it would be displayed by a native file chooser
     * or null if no native information is available.
     * @see JFileChooser#getTypeDescription
     * @since 1.4
     */
    public String getSystemTypeDescription(File f) {
        return null;
    }

    /**
     * Icon for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "M:\" directory
     * displays a CD-ROM icon.
     * <p>
     * The default implementation gets information from the ShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return an icon as it would be displayed by a native file chooser
     * @see JFileChooser#getIcon
     * @since 1.4
     */
    public Icon getSystemIcon(File f) {
        if (f == null) {
            return null;
        }

        ShellFolder sf;

        try {
            sf = getShellFolder(f);
        } catch (FileNotFoundException e) {
            return null;
        }

        Image img = sf.getIcon(false);

        if (img != null) {
            return new ImageIcon(img, sf.getFolderType());
        } else {
            return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
        }
    }

   /**
    * Returns an icon for a file, directory, or folder as it would be displayed
    * in a system file browser for the requested size.
    * <p>
    * Example: <pre>
    *     FileSystemView fsv = FileSystemView.getFileSystemView();
    *     Icon icon = fsv.getSystemIcon(new File("application.exe"), 64, 64);
    *     JLabel label = new JLabel(icon);
    * </pre>
    *
    * @implSpec The available icons may be platform specific and so the
    * available sizes determined by the platform. Therefore an exact match
    * for the requested size may not be possible.
    *
    * The icon returned may be a multi-resolution icon image,
    * which allows better support for High DPI environments
    * with different scaling factors.
    *
    * @param f a {@code File} object for which the icon will be retrieved
    * @param width width of the icon in user coordinate system.
    * @param height height of the icon in user coordinate system.
    * @return an icon as it would be displayed by a native file chooser
    * or null for a non-existent or inaccessible file.
    * @throws IllegalArgumentException if an invalid parameter such
    * as a negative size or a null file reference is passed.
    * @see JFileChooser#getIcon
    * @see AbstractMultiResolutionImage
    * @see FileSystemView#getSystemIcon(File)
    * @since 17
    */
    public Icon getSystemIcon(File f, int width, int height) {
        if (height < 1 || width < 1) {
            throw new IllegalArgumentException("Icon size can not be below 1");
        }

        if (f == null) {
            throw new IllegalArgumentException("The file reference should not be null");
        }

        if(!f.exists()) {
            return null;
        }

        ShellFolder sf;

        try {
            sf = ShellFolder.getShellFolder(f);
        } catch (FileNotFoundException e) {
            return null;
        }

        Image img = sf.getIcon(width, height);

        if (img != null) {
            return new ImageIcon(img, sf.getFolderType());
        } else {
            return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon"
                    : "FileView.fileIcon");
        }
    }

    /**
     * On Windows, a file can appear in multiple folders, other than its
     * parent directory in the filesystem. Folder could for example be the
     * "Desktop" folder which is not the same as file.getParentFile().
     *
     * @param folder a <code>File</code> object representing a directory or special folder
     * @param file a <code>File</code> object
     * @return <code>true</code> if <code>folder</code> is a directory or special folder and contains <code>file</code>.
     * @since 1.4
     */
    public boolean isParent(File folder, File file) {
        if (folder == null || file == null) {
            return false;
        } else if (folder instanceof ShellFolder) {
                File parent = file.getParentFile();
                if (parent != null && parent.equals(folder)) {
                    return true;
                }
            File[] children = getFiles(folder, false);
            for (File child : children) {
                if (file.equals(child)) {
                    return true;
                }
            }
            return false;
        } else {
            return folder.equals(file.getParentFile());
        }
    }

    /**
     *
     * @param parent a <code>File</code> object representing a directory or special folder
     * @param fileName a name of a file or folder which exists in <code>parent</code>
     * @return a File object. This is normally constructed with <code>new
     * File(parent, fileName)</code> except when parent and child are both
     * special folders, in which case the <code>File</code> is a wrapper containing
     * a <code>ShellFolder</code> object.
     * @since 1.4
     */
    public File getChild(File parent, String fileName) {
        if (parent instanceof ShellFolder) {
            File[] children = getFiles(parent, false);
            for (File child : children) {
                if (child.getName().equals(fileName)) {
                    return child;
                }
            }
        }
        return createFileObject(parent, fileName);
    }


    /**
     * Checks if <code>f</code> represents a real directory or file as opposed to a
     * special folder such as <code>"Desktop"</code>. Used by UI classes to decide if
     * a folder is selectable when doing directory choosing.
     *
     * @param f a <code>File</code> object
     * @return <code>true</code> if <code>f</code> is a real file or directory.
     * @since 1.4
     */
    public boolean isFileSystem(File f) {
        if (f instanceof ShellFolder) {
            ShellFolder sf = (ShellFolder)f;
            // Shortcuts to directories are treated as not being file system objects,
            // so that they are never returned by JFileChooser.
            return sf.isFileSystem() && !(sf.isLink() && sf.isDirectory());
        } else {
            return true;
        }
    }

    /**
     * Creates a new folder with a default folder name.
     *
     * @param containingDir a {@code File} object denoting directory to contain the new folder
     * @return a {@code File} object denoting the newly created folder
     * @throws IOException if new folder could not be created
     */
    public abstract File createNewFolder(File containingDir) throws IOException;

    /**
     * Returns whether a file is hidden or not.
     *
     * @param f a {@code File} object
     * @return true if the given {@code File} denotes a hidden file
     */
    public boolean isHiddenFile(File f) {
        return f.isHidden();
    }


    /**
     * Is dir the root of a tree in the file system, such as a drive
     * or partition. Example: Returns true for "C:\" on Windows 98.
     *
     * @param dir a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root of a filesystem
     * @see #isRoot
     * @since 1.4
     */
    public boolean isFileSystemRoot(File dir) {
        return ShellFolder.isFileSystemRoot(dir);
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for drives or partitions, e.g. a "hard disk" icon.
     *
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    public boolean isDrive(File dir) {
        return false;
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for a floppy disk. Implies isDrive(dir).
     *
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for a computer node, e.g. "My Computer" or a network server.
     *
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    public boolean isComputerNode(File dir) {
        return ShellFolder.isComputerNode(dir);
    }


    /**
     * Returns all root partitions on this system. For example, on
     * Windows, this would be the "Desktop" folder, while on DOS this
     * would be the A: through Z: drives.
     *
     * @return an array of {@code File} objects representing all root partitions
     *         on this system
     */
    public File[] getRoots() {
        // Don't cache this array, because filesystem might change
        File[] roots = (File[])ShellFolder.get("roots");

        for (int i = 0; i < roots.length; i++) {
            if (isFileSystemRoot(roots[i])) {
                roots[i] = createFileSystemRoot(roots[i]);
            }
        }
        return roots;
    }


    // Providing default implementations for the remaining methods
    // because most OS file systems will likely be able to use this
    // code. If a given OS can't, override these methods in its
    // implementation.

    /**
     * Returns the home directory.
     * @return the home directory
     */
    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    /**
     * Return the user's default starting directory for the file chooser.
     *
     * @return a <code>File</code> object representing the default
     *         starting folder
     * @since 1.4
     */
    public File getDefaultDirectory() {
        File f = (File)ShellFolder.get("fileChooserDefaultFolder");
        if (isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }
        return f;
    }

    /**
     * Returns a File object constructed in dir from the given filename.
     *
     * @param dir an abstract pathname denoting a directory
     * @param filename a {@code String} representation of a pathname
     * @return a {@code File} object created from {@code dir} and {@code filename}
     */
    public File createFileObject(File dir, String filename) {
        if(dir == null) {
            return new File(filename);
        } else {
            return new File(dir, filename);
        }
    }

    /**
     * Returns a File object constructed from the given path string.
     *
     * @param path {@code String} representation of path
     * @return a {@code File} object created from the given {@code path}
     */
    public File createFileObject(String path) {
        File f = new File(path);
        if (isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }
        return f;
    }


    /**
     * Gets the list of shown (i.e. not hidden) files.
     *
     * @param dir the root directory of files to be returned
     * @param useFileHiding determine if hidden files are returned
     * @return an array of {@code File} objects representing files and
     *         directories in the given {@code dir}. It includes hidden
     *         files if {@code useFileHiding} is false.
     */
    public File[] getFiles(File dir, boolean useFileHiding) {
        List<File> files = new ArrayList<File>();

        // add all files in dir
        if (!(dir instanceof ShellFolder)) {
            try {
                dir = getShellFolder(dir);
            } catch (FileNotFoundException e) {
                return new File[0];
            }
        }

        File[] names = ((ShellFolder) dir).listFiles(!useFileHiding);

        if (names == null) {
            return new File[0];
        }

        for (File f : names) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            if (!(f instanceof ShellFolder)) {
                if (isFileSystemRoot(f)) {
                    f = createFileSystemRoot(f);
                }
                try {
                    f = ShellFolder.getShellFolder(f);
                } catch (FileNotFoundException e) {
                    // Not a valid file (wouldn't show in native file chooser)
                    // Example: C:\pagefile.sys
                    continue;
                } catch (InternalError e) {
                    // Not a valid file (wouldn't show in native file chooser)
                    // Example C:\Winnt\Profiles\joe\history\History.IE5
                    continue;
                }
            }
            if (!useFileHiding || !isHiddenFile(f)) {
                files.add(f);
            }
        }

        return files.toArray(new File[files.size()]);
    }



    /**
     * Returns the parent directory of <code>dir</code>.
     * @param dir the <code>File</code> being queried
     * @return the parent directory of <code>dir</code>, or
     *   <code>null</code> if <code>dir</code> is <code>null</code>
     */
    public File getParentDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return null;
        }

        ShellFolder sf;

        try {
            sf = getShellFolder(dir);
        } catch (FileNotFoundException e) {
            return null;
        }

        File psf = sf.getParentFile();

        if (psf == null) {
            return null;
        }

        if (isFileSystem(psf)) {
            File f = psf;
            if (!f.exists()) {
                // This could be a node under "Network Neighborhood".
                File ppsf = psf.getParentFile();
                if (ppsf == null || !isFileSystem(ppsf)) {
                    // We're mostly after the exists() override for windows below.
                    f = createFileSystemRoot(f);
                }
            }
            return f;
        } else {
            return psf;
        }
    }

    /**
     * Returns an array of files representing the values which will be shown
     * in the file chooser selector.
     *
     * @return an array of {@code File} objects. The array returned may be
     * possibly empty if there are no appropriate permissions.
     * @since 9
     */
    public File[] getChooserComboBoxFiles() {
        return (File[]) ShellFolder.get("fileChooserComboBoxFolders");
    }

    /**
     * Returns an array of files representing the values to show by default in
     * the file chooser shortcuts panel.
     *
     * @return an array of {@code File} objects. The array returned may be
     * possibly empty if there are no appropriate permissions.
     * @since 12
     */
    public final File[] getChooserShortcutPanelFiles() {
        return (File[]) ShellFolder.get("fileChooserShortcutPanelFolders");
    }

    /**
     * Returns whether the specified file denotes a shell interpreted link which
     * can be obtained by the {@link #getLinkLocation(File)}.
     *
     * @param file a file
     * @return whether this is a link
     * @throws NullPointerException if {@code file} equals {@code null}
     * @throws SecurityException if the caller does not have necessary
     *                           permissions
     * @see #getLinkLocation(File)
     * @since 9
     */
    public boolean isLink(File file) {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        try {
            return ShellFolder.getShellFolder(file).isLink();
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns the regular file referenced by the specified link file if
     * the specified file is a shell interpreted link.
     * Returns {@code null} if the specified file is not
     * a shell interpreted link.
     *
     * @param file a file
     * @return the linked file or {@code null}.
     * @throws FileNotFoundException if the linked file does not exist
     * @throws NullPointerException if {@code file} equals {@code null}
     * @throws SecurityException if the caller does not have necessary
     *                           permissions
     * @since 9
     */
    public File getLinkLocation(File file) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        ShellFolder shellFolder;
        try {
            shellFolder = ShellFolder.getShellFolder(file);
        } catch (FileNotFoundException e) {
            return null;
        }
        return shellFolder.isLink() ? shellFolder.getLinkLocation() : null;
    }

    /**
     * Throws {@code FileNotFoundException} if file not found or current thread was interrupted
     */
    ShellFolder getShellFolder(File f) throws FileNotFoundException {
        if (!(f instanceof ShellFolder) && !(f instanceof FileSystemRoot) && isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }

        try {
            return ShellFolder.getShellFolder(f);
        } catch (InternalError e) {
            System.err.println("FileSystemView.getShellFolder: f="+f);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct
     * behavior for a file system root directory.
     *
     * @param f a <code>File</code> object representing a file system root
     *          directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    protected File createFileSystemRoot(File f) {
        return new FileSystemRoot(f);
    }

    @SuppressWarnings("serial") // Same-version serialization only
    static class FileSystemRoot extends File {
        public FileSystemRoot(File f) {
            super(f,"");
        }

        public FileSystemRoot(String s) {
            super(s);
        }

        public boolean isDirectory() {
            return true;
        }

        public String getName() {
            return getPath();
        }
    }
}

/**
 * FileSystemView that handles some specific unix-isms.
 */
class UnixFileSystemView extends FileSystemView {

    private static final String newFolderString =
            UIManager.getString("FileChooser.other.newFolder");
    private static final String newFolderNextString  =
            UIManager.getString("FileChooser.other.newFolder.subsequent");

    /**
     * Creates a new folder with a default folder name.
     */
    public File createNewFolder(File containingDir) throws IOException {
        if(containingDir == null) {
            throw new IOException("Containing directory is null:");
        }
        File newFolder;
        // Unix - using OpenWindows' default folder name. Can't find one for Motif/CDE.
        newFolder = createFileObject(containingDir, newFolderString);
        int i = 1;
        while (newFolder.exists() && i < 100) {
            newFolder = createFileObject(containingDir, MessageFormat.format(
                    newFolderNextString, i));
            i++;
        }

        if(newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            if(!newFolder.mkdirs()) {
                throw new IOException(newFolder.getAbsolutePath());
            }
        }

        return newFolder;
    }

    public boolean isFileSystemRoot(File dir) {
        return dir != null && dir.getAbsolutePath().equals("/");
    }

    public boolean isDrive(File dir) {
        return isFloppyDrive(dir);
    }

    public boolean isFloppyDrive(File dir) {
        // Could be looking at the path for Solaris, but wouldn't be reliable.
        // For example:
        // return (dir != null && dir.getAbsolutePath().toLowerCase().startsWith("/floppy"));
        return false;
    }

    public boolean isComputerNode(File dir) {
        if (dir != null) {
            String parent = dir.getParent();
            if (parent != null && parent.equals("/net")) {
                return true;
            }
        }
        return false;
    }
}


/**
 * FileSystemView that handles some specific windows concepts.
 */
class WindowsFileSystemView extends FileSystemView {

    private static final String newFolderString =
            UIManager.getString("FileChooser.win32.newFolder");
    private static final String newFolderNextString  =
            UIManager.getString("FileChooser.win32.newFolder.subsequent");

    public Boolean isTraversable(File f) {
        return Boolean.valueOf(isFileSystemRoot(f) || isComputerNode(f) || f.isDirectory());
    }

    public File getChild(File parent, String fileName) {
        if (fileName.startsWith("\\")
            && !fileName.startsWith("\\\\")
            && isFileSystem(parent)) {

            //Path is relative to the root of parent's drive
            String path = parent.getAbsolutePath();
            if (path.length() >= 2
                && path.charAt(1) == ':'
                && Character.isLetter(path.charAt(0))) {

                return createFileObject(path.substring(0, 2) + fileName);
            }
        }
        return super.getChild(parent, fileName);
    }

    /**
     * Type description for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "Desktop" folder
     * is described as "Desktop".
     *
     * The Windows implementation gets information from the ShellFolder class.
     */
    public String getSystemTypeDescription(File f) {
        if (f == null) {
            return null;
        }

        try {
            return getShellFolder(f).getFolderType();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * @return the Desktop folder.
     */
    public File getHomeDirectory() {
        File[] roots = getRoots();
        return (roots.length == 0) ? null : roots[0];
    }

    /**
     * Creates a new folder with a default folder name.
     */
    public File createNewFolder(File containingDir) throws IOException {
        if(containingDir == null) {
            throw new IOException("Containing directory is null:");
        }
        // Using NT's default folder name
        File newFolder = createFileObject(containingDir, newFolderString);
        int i = 2;
        while (newFolder.exists() && i < 100) {
            newFolder = createFileObject(containingDir, MessageFormat.format(
                newFolderNextString, i));
            i++;
        }

        if(newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            if(!newFolder.mkdirs()) {
                throw new IOException(newFolder.getAbsolutePath());
            }
        }

        return newFolder;
    }

    public boolean isDrive(File dir) {
        return isFileSystemRoot(dir);
    }

    public boolean isFloppyDrive(final File dir) {
        @SuppressWarnings("removal")
        String path = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return dir.getAbsolutePath();
            }
        });

        return path != null && (path.equals("A:\\") || path.equals("B:\\"));
    }

    /**
     * Returns a File object constructed from the given path string.
     */
    public File createFileObject(String path) {
        // Check for missing backslash after drive letter such as "C:" or "C:filename"
        if (path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0))) {
            if (path.length() == 2) {
                path += "\\";
            } else if (path.charAt(2) != '\\') {
                path = path.substring(0, 2) + "\\" + path.substring(2);
            }
        }
        return super.createFileObject(path);
    }

    @SuppressWarnings("serial") // anonymous class
    protected File createFileSystemRoot(File f) {
        // Problem: Removable drives on Windows return false on f.exists()
        // Workaround: Override exists() to always return true.
        return new FileSystemRoot(f) {
            public boolean exists() {
                return true;
            }
        };
    }

}

/**
 * Fallthrough FileSystemView in case we can't determine the OS.
 */
class GenericFileSystemView extends FileSystemView {

    private static final String newFolderString =
            UIManager.getString("FileChooser.other.newFolder");

    /**
     * Creates a new folder with a default folder name.
     */
    public File createNewFolder(File containingDir) throws IOException {
        if(containingDir == null) {
            throw new IOException("Containing directory is null:");
        }
        // Using NT's default folder name
        File newFolder = createFileObject(containingDir, newFolderString);

        if(newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            if(!newFolder.mkdirs()) {
                throw new IOException(newFolder.getAbsolutePath());
            }
        }
        return newFolder;
    }

}
