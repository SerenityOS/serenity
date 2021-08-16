/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.X11;

import java.awt.FileDialog;
import java.awt.peer.FileDialogPeer;
import java.io.File;
import java.io.FilenameFilter;
import sun.awt.AWTAccessor;

/**
 * FileDialogPeer for the GtkFileChooser.
 *
 * @author Costantino Cerbo (c.cerbo@gmail.com)
 */
final class GtkFileDialogPeer extends XDialogPeer implements FileDialogPeer {

    private final FileDialog fd;

    // A pointer to the native GTK FileChooser widget
    private volatile long widget = 0L;
    private long standaloneWindow;
    private volatile boolean quit;

    GtkFileDialogPeer(FileDialog fd) {
        super(fd);
        this.fd = fd;
    }

    private static native void initIDs();
    static {
        initIDs();
    }

    private native void run(String title, int mode, String dir, String file,
                            FilenameFilter filter, boolean isMultipleMode, int x, int y);
    private native void quit();

    @Override
    public native void toFront();

    @Override
    public native void setBounds(int x, int y, int width, int height, int op);

    /**
     * Called exclusively by the native C code.
     */
    private void setFileInternal(String directory, String[] filenames) {
        AWTAccessor.FileDialogAccessor accessor = AWTAccessor
                .getFileDialogAccessor();

        if (filenames == null) {
            accessor.setDirectory(fd, null);
            accessor.setFile(fd, null);
            accessor.setFiles(fd, null);
        } else {
            // Fix 6987233: add the trailing slash if it's absent
            String with_separator = directory;
            if (directory != null) {
                with_separator = directory.endsWith(File.separator) ?
                        directory : (directory + File.separator);
            }
            accessor.setDirectory(fd, with_separator);
            accessor.setFile(fd, filenames[0]);

            int filesNumber = (filenames != null) ? filenames.length : 0;
            File[] files = new File[filesNumber];
            for (int i = 0; i < filesNumber; i++) {
                files[i] = new File(directory, filenames[i]);
            }
            accessor.setFiles(fd, files);
        }
    }

    /**
     * Called exclusively by the native C code.
     */
    private boolean filenameFilterCallback(String fullname) {
        if (fd.getFilenameFilter() == null) {
            // no filter, accept all.
            return true;
        }

        File filen = new File(fullname);
        return fd.getFilenameFilter().accept(new File(filen.getParent()),
                filen.getName());
    }

    @Override
    public void setVisible(boolean b) {
        XToolkit.awtLock();
        try {
            quit = !b;
            if (b) {
                Runnable task = () -> {
                    showNativeDialog();
                    standaloneWindow = 0;
                    fd.setVisible(false);
                };
                new Thread(null, task, "ShowDialog", 0, false).start();
            } else {
                quit();
                fd.setVisible(false);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    @Override
    public void dispose() {
        XToolkit.awtLock();
        try {
            quit = true;
            quit();
        }
        finally {
            XToolkit.awtUnlock();
        }
        super.dispose();
    }

    @Override
    public void setDirectory(String dir) {
        // We do not implement this method because we
        // have delegated to FileDialog#setDirectory
    }

    @Override
    public void setFile(String file) {
        // We do not implement this method because we
        // have delegated to FileDialog#setFile
    }

    protected void requestXFocus(long time, boolean timeProvided) {
        if(standaloneWindow == 0) {
            super.requestXFocus(time, timeProvided);
            return;
        }
        XNETProtocol net_protocol = XWM.getWM().getNETProtocol();
        if (net_protocol != null) {
            net_protocol.setActiveWindow(standaloneWindow);
        }
    }

    @Override
    public void setFilenameFilter(FilenameFilter filter) {
        // We do not implement this method because we
        // have delegated to FileDialog#setFilenameFilter
    }

    private void showNativeDialog() {
        String dirname = fd.getDirectory();
        // File path has a priority against directory path.
        String filename = fd.getFile();
        if (filename != null) {
            final File file = new File(filename);
            if (fd.getMode() == FileDialog.LOAD
                && dirname != null
                && file.getParent() == null) {
                // File path for gtk_file_chooser_set_filename.
                filename = dirname + (dirname.endsWith(File.separator) ? "" :
                                              File.separator) + filename;
            }
            if (fd.getMode() == FileDialog.SAVE && file.getParent() != null) {
                // Filename for gtk_file_chooser_set_current_name.
                filename = file.getName();
                // Directory path for gtk_file_chooser_set_current_folder.
                dirname = file.getParent();
            }
        }
        if (!quit) {
            run(fd.getTitle(), fd.getMode(), dirname, filename,
                    fd.getFilenameFilter(), fd.isMultipleMode(), fd.getX(), fd.getY());
        }
    }

    /**
     * Called by native code when GTK dialog is created.
     */
    boolean setWindow(long xid) {
        if (!quit && widget != 0) {
            standaloneWindow = xid;
            requestXFocus();
            return true;
        }
        return false;
    }
}
