/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.shell;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

import javax.swing.SwingConstants;

/**
 * @author Michael Martak
 * @since 1.4
 */
@SuppressWarnings("serial") // JDK-implementation class
public abstract class ShellFolder extends File {
    public static final String COLUMN_NAME = "FileChooser.fileNameHeaderText";
    public static final String COLUMN_SIZE = "FileChooser.fileSizeHeaderText";
    public static final String COLUMN_DATE = "FileChooser.fileDateHeaderText";

    protected ShellFolder parent;

    /**
     * Create a file system shell folder from a file
     */
    ShellFolder(ShellFolder parent, String pathname) {
        super((pathname != null) ? pathname : "ShellFolder");
        this.parent = parent;
    }

    /**
     * @return Whether this is a file system shell folder
     */
    public boolean isFileSystem() {
        return (!getPath().startsWith("ShellFolder"));
    }

    /**
     * This method must be implemented to make sure that no instances
     * of {@code ShellFolder} are ever serialized. If {@code isFileSystem()} returns
     * {@code true}, then the object should be representable with an instance of
     * {@code java.io.File} instead. If not, then the object is most likely
     * depending on some internal (native) state and cannot be serialized.
     *
     * @return a java.io.File replacement object, or null
     *         if no suitable replacement can be found.
     */
    @Serial
    protected abstract Object writeReplace() throws java.io.ObjectStreamException;

    /**
     * Returns the path for this object's parent,
     * or {@code null} if this object does not name a parent
     * folder.
     *
     * @return  the path as a String for this object's parent,
     * or {@code null} if this object does not name a parent
     * folder
     *
     * @see java.io.File#getParent()
     * @since 1.4
     */
    public String getParent() {
        if (parent == null && isFileSystem()) {
            return super.getParent();
        }
        if (parent != null) {
            return (parent.getPath());
        } else {
            return null;
        }
    }

    /**
     * Returns a File object representing this object's parent,
     * or {@code null} if this object does not name a parent
     * folder.
     *
     * @return  a File object representing this object's parent,
     * or {@code null} if this object does not name a parent
     * folder
     *
     * @see java.io.File#getParentFile()
     * @since 1.4
     */
    public File getParentFile() {
        if (parent != null) {
            return parent;
        } else if (isFileSystem()) {
            return super.getParentFile();
        } else {
            return null;
        }
    }

    public File[] listFiles() {
        return listFiles(true);
    }

    public File[] listFiles(boolean includeHiddenFiles) {
        File[] files = super.listFiles();

        if (!includeHiddenFiles) {
            Vector<File> v = new Vector<>();
            int nameCount = (files == null) ? 0 : files.length;
            for (int i = 0; i < nameCount; i++) {
                if (!files[i].isHidden()) {
                    v.addElement(files[i]);
                }
            }
            files = v.toArray(new File[v.size()]);
        }

        return files;
    }


    /**
     * @return Whether this shell folder is a link
     */
    public abstract boolean isLink();

    /**
     * @return The shell folder linked to by this shell folder, or null
     * if this shell folder is not a link
     */
    public abstract ShellFolder getLinkLocation() throws FileNotFoundException;

    /**
     * @return The name used to display this shell folder
     */
    public abstract String getDisplayName();

    /**
     * @return The type of shell folder as a string
     */
    public abstract String getFolderType();

    /**
     * @return The executable type as a string
     */
    public abstract String getExecutableType();

    /**
     * Compares this ShellFolder with the specified ShellFolder for order.
     *
     * @see #compareTo(Object)
     */
    public int compareTo(File file2) {
        if (file2 == null || !(file2 instanceof ShellFolder)
            || ((file2 instanceof ShellFolder) && ((ShellFolder)file2).isFileSystem())) {

            if (isFileSystem()) {
                return super.compareTo(file2);
            } else {
                return -1;
            }
        } else {
            if (isFileSystem()) {
                return 1;
            } else {
                return getName().compareTo(file2.getName());
            }
        }
    }

    /**
     * @param getLargeIcon whether to return large icon (ignored in base implementation)
     * @return The icon used to display this shell folder
     */
    public Image getIcon(boolean getLargeIcon) {
        return null;
    }

    /**
     * Returns the icon of the specified size used to display this shell folder.
     *
     * @param width width of the icon > 0
     * @param height height of the icon > 0
     * @return The icon of the specified size used to display this shell folder
     */
    public Image getIcon(int width, int height) {
        return null;
    }

    // Static

    private static final ShellFolderManager shellFolderManager;

    private static final Invoker invoker;

    static {
        String managerClassName = (String)Toolkit.getDefaultToolkit().
                                      getDesktopProperty("Shell.shellFolderManager");
        Class<?> managerClass = null;
        try {
            managerClass = Class.forName(managerClassName, false, null);
            if (!ShellFolderManager.class.isAssignableFrom(managerClass)) {
                managerClass = null;
            }
        // swallow the exceptions below and use default shell folder
        } catch(ClassNotFoundException e) {
        } catch(NullPointerException e) {
        } catch(SecurityException e) {
        }

        if (managerClass == null) {
            managerClass = ShellFolderManager.class;
        }
        try {
            shellFolderManager =
                (ShellFolderManager)managerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new Error("Could not instantiate Shell Folder Manager: "
            + managerClass.getName());
        }

        invoker = shellFolderManager.createInvoker();
    }

    /**
     * Return a shell folder from a file object
     * @exception FileNotFoundException if file does not exist
     */
    public static ShellFolder getShellFolder(File file) throws FileNotFoundException {
        if (file instanceof ShellFolder) {
            return (ShellFolder)file;
        }

        if (!Files.exists(Paths.get(file.getPath()), LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException();
        }
        return shellFolderManager.createShellFolder(file);
    }

    /**
     * @param key a {@code String}
     * @return An Object matching the string {@code key}.
     * @see ShellFolderManager#get(String)
     */
    public static Object get(String key) {
        return shellFolderManager.get(key);
    }

    /**
     * Does {@code dir} represent a "computer" such as a node on the network, or
     * "My Computer" on the desktop.
     */
    public static boolean isComputerNode(File dir) {
        return shellFolderManager.isComputerNode(dir);
    }

    /**
     * @return Whether this is a file system root directory
     */
    public static boolean isFileSystemRoot(File dir) {
        return shellFolderManager.isFileSystemRoot(dir);
    }

    /**
     * Canonicalizes files that don't have symbolic links in their path.
     * Normalizes files that do, preserving symbolic links from being resolved.
     */
    public static File getNormalizedFile(File f) throws IOException {
        File canonical = f.getCanonicalFile();
        if (f.equals(canonical)) {
            // path of f doesn't contain symbolic links
            return canonical;
        }

        // preserve symbolic links from being resolved
        return new File(f.toURI().normalize());
    }

    // Override File methods

    public static void sort(final List<? extends File> files) {
        if (files == null || files.size() <= 1) {
            return;
        }

        // To avoid loads of synchronizations with Invoker and improve performance we
        // synchronize the whole code of the sort method once
        invoke(new Callable<Void>() {
            public Void call() {
                // Check that we can use the ShellFolder.sortChildren() method:
                //   1. All files have the same non-null parent
                //   2. All files is ShellFolders
                File commonParent = null;

                for (File file : files) {
                    File parent = file.getParentFile();

                    if (parent == null || !(file instanceof ShellFolder)) {
                        commonParent = null;

                        break;
                    }

                    if (commonParent == null) {
                        commonParent = parent;
                    } else {
                        if (commonParent != parent && !commonParent.equals(parent)) {
                            commonParent = null;

                            break;
                        }
                    }
                }

                if (commonParent instanceof ShellFolder) {
                    ((ShellFolder) commonParent).sortChildren(files);
                } else {
                    Collections.sort(files, FILE_COMPARATOR);
                }

                return null;
            }
        });
    }

    public void sortChildren(final List<? extends File> files) {
        // To avoid loads of synchronizations with Invoker and improve performance we
        // synchronize the whole code of the sort method once
        invoke(new Callable<Void>() {
            public Void call() {
                Collections.sort(files, FILE_COMPARATOR);

                return null;
            }
        });
    }

    public boolean isAbsolute() {
        return (!isFileSystem() || super.isAbsolute());
    }

    public File getAbsoluteFile() {
        return (isFileSystem() ? super.getAbsoluteFile() : this);
    }

    public boolean canRead() {
        return (isFileSystem() ? super.canRead() : true);       // ((Fix?))
    }

    /**
     * Returns true if folder allows creation of children.
     * True for the "Desktop" folder, but false for the "My Computer"
     * folder.
     */
    public boolean canWrite() {
        return (isFileSystem() ? super.canWrite() : false);     // ((Fix?))
    }

    public boolean exists() {
        // Assume top-level drives exist, because state is uncertain for
        // removable drives.
        return (!isFileSystem() || isFileSystemRoot(this) || super.exists()) ;
    }

    public boolean isDirectory() {
        return (isFileSystem() ? super.isDirectory() : true);   // ((Fix?))
    }

    public boolean isFile() {
        return (isFileSystem() ? super.isFile() : !isDirectory());      // ((Fix?))
    }

    public long lastModified() {
        return (isFileSystem() ? super.lastModified() : 0L);    // ((Fix?))
    }

    public long length() {
        return (isFileSystem() ? super.length() : 0L);  // ((Fix?))
    }

    public boolean createNewFile() throws IOException {
        return (isFileSystem() ? super.createNewFile() : false);
    }

    public boolean delete() {
        return (isFileSystem() ? super.delete() : false);       // ((Fix?))
    }

    public void deleteOnExit() {
        if (isFileSystem()) {
            super.deleteOnExit();
        } else {
            // Do nothing       // ((Fix?))
        }
    }

    public boolean mkdir() {
        return (isFileSystem() ? super.mkdir() : false);
    }

    public boolean mkdirs() {
        return (isFileSystem() ? super.mkdirs() : false);
    }

    public boolean renameTo(File dest) {
        return (isFileSystem() ? super.renameTo(dest) : false); // ((Fix?))
    }

    public boolean setLastModified(long time) {
        return (isFileSystem() ? super.setLastModified(time) : false); // ((Fix?))
    }

    public boolean setReadOnly() {
        return (isFileSystem() ? super.setReadOnly() : false); // ((Fix?))
    }

    public String toString() {
        return (isFileSystem() ? super.toString() : getDisplayName());
    }

    public static ShellFolderColumnInfo[] getFolderColumns(File dir) {
        ShellFolderColumnInfo[] columns = null;

        if (dir instanceof ShellFolder) {
            columns = ((ShellFolder) dir).getFolderColumns();
        }

        if (columns == null) {
            columns = new ShellFolderColumnInfo[]{
                    new ShellFolderColumnInfo(COLUMN_NAME, 150,
                            SwingConstants.LEADING, true, null,
                            FILE_COMPARATOR),
                    new ShellFolderColumnInfo(COLUMN_SIZE, 75,
                            SwingConstants.RIGHT, true, null,
                            DEFAULT_COMPARATOR, true),
                    new ShellFolderColumnInfo(COLUMN_DATE, 130,
                            SwingConstants.LEADING, true, null,
                            DEFAULT_COMPARATOR, true)
            };
        }

        return columns;
    }

    public ShellFolderColumnInfo[] getFolderColumns() {
        return null;
    }

    public static Object getFolderColumnValue(File file, int column) {
        if (file instanceof ShellFolder) {
            Object value = ((ShellFolder)file).getFolderColumnValue(column);
            if (value != null) {
                return value;
            }
        }

        if (file == null || !file.exists()) {
            return null;
        }

        switch (column) {
            case 0:
                // By default, file name will be rendered using getSystemDisplayName()
                return file;

            case 1: // size
                return file.isDirectory() ? null : Long.valueOf(file.length());

            case 2: // date
                if (isFileSystemRoot(file)) {
                    return null;
                }
                long time = file.lastModified();
                return (time == 0L) ? null : new Date(time);

            default:
                return null;
        }
    }

    public Object getFolderColumnValue(int column) {
        return null;
    }

    /**
     * Invokes the {@code task} which doesn't throw checked exceptions
     * from its {@code call} method. If invokation is interrupted then Thread.currentThread().isInterrupted() will
     * be set and result will be {@code null}
     */
    public static <T> T invoke(Callable<T> task) {
        try {
            return invoke(task, RuntimeException.class);
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * Invokes the {@code task} which throws checked exceptions from its {@code call} method.
     * If invokation is interrupted then Thread.currentThread().isInterrupted() will
     * be set and InterruptedException will be thrown as well.
     */
    public static <T, E extends Throwable> T invoke(Callable<T> task, Class<E> exceptionClass)
            throws InterruptedException, E {
        try {
            return invoker.invoke(task);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                // Rethrow unchecked exceptions
                throw (RuntimeException) e;
            }

            if (e instanceof InterruptedException) {
                // Set isInterrupted flag for current thread
                Thread.currentThread().interrupt();

                // Rethrow InterruptedException
                throw (InterruptedException) e;
            }

            if (exceptionClass.isInstance(e)) {
                throw exceptionClass.cast(e);
            }

            throw new RuntimeException("Unexpected error", e);
        }
    }

    /**
     * Interface allowing to invoke tasks in different environments on different platforms.
     */
    public static interface Invoker {
        /**
         * Invokes a callable task.
         *
         * @param task a task to invoke
         * @throws Exception {@code InterruptedException} or an exception that was thrown from the {@code task}
         * @return the result of {@code task}'s invokation
         */
        <T> T invoke(Callable<T> task) throws Exception;
    }

    /**
     * Provides a default comparator for the default column set
     */
    private static final Comparator<Object> DEFAULT_COMPARATOR = new Comparator<Object>() {
        public int compare(Object o1, Object o2) {
            int gt;

            if (o1 == null && o2 == null) {
                gt = 0;
            } else if (o1 != null && o2 == null) {
                gt = 1;
            } else if (o1 == null && o2 != null) {
                gt = -1;
            } else if (o1 instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> o = (Comparable<Object>) o1;
                gt = o.compareTo(o2);
            } else {
                gt = 0;
            }

            return gt;
        }
    };

    private static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
        public int compare(File f1, File f2) {
            ShellFolder sf1 = null;
            ShellFolder sf2 = null;

            if (f1 instanceof ShellFolder) {
                sf1 = (ShellFolder) f1;
                if (sf1.isFileSystem()) {
                    sf1 = null;
                }
            }
            if (f2 instanceof ShellFolder) {
                sf2 = (ShellFolder) f2;
                if (sf2.isFileSystem()) {
                    sf2 = null;
                }
            }

            if (sf1 != null && sf2 != null) {
                return sf1.compareTo(sf2);
            } else if (sf1 != null) {
                // Non-file shellfolders sort before files
                return -1;
            } else if (sf2 != null) {
                return 1;
            } else {
                String name1 = f1.getName();
                String name2 = f2.getName();

                // First ignore case when comparing
                int diff = name1.compareToIgnoreCase(name2);
                if (diff != 0) {
                    return diff;
                } else {
                    // May differ in case (e.g. "mail" vs. "Mail")
                    // We need this test for consistent sorting
                    return name1.compareTo(name2);
                }
            }
        }
    };
}
