/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.windows;

import java.awt.*;
import java.awt.event.FocusEvent.Cause;
import java.awt.dnd.DropTarget;
import java.awt.peer.*;
import java.io.File;
import java.io.FilenameFilter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Vector;
import sun.awt.AWTAccessor;

final class WFileDialogPeer extends WWindowPeer implements FileDialogPeer {

    static {
        initIDs();
    }

    private WComponentPeer parent;
    private FilenameFilter fileFilter;

    private Vector<WWindowPeer> blockedWindows = new Vector<>();

    //Needed to fix 4152317
    private static native void setFilterString(String allFilter);

    @Override
    public void setFilenameFilter(FilenameFilter filter) {
        this.fileFilter = filter;
    }

    boolean checkFilenameFilter(String filename) {
        FileDialog fileDialog = (FileDialog)target;
        if (fileFilter == null) {
            return true;
        }
        File file = new File(filename);
        return fileFilter.accept(new File(file.getParent()), file.getName());
    }

    // Toolkit & peer internals
    WFileDialogPeer(FileDialog target) {
        super(target);
    }

    @Override
    void create(WComponentPeer parent) {
        this.parent = parent;
    }

    // don't use checkCreation() from WComponentPeer to avoid hwnd check
    @Override
    protected void checkCreation() {
    }

    @Override
    void initialize() {
        setFilenameFilter(((FileDialog) target).getFilenameFilter());
    }

    private native void _dispose();
    @Override
    protected void disposeImpl() {
        WToolkit.targetDisposedPeer(target, this);
        _dispose();
    }

    private native void _show();
    private native void _hide();

    @Override
    public void show() {
        new Thread(null, this::_show, "FileDialog", 0, false).start();
    }

    @Override
    void hide() {
        _hide();
    }

    // called from native code when the dialog is shown or hidden
    void setHWnd(long hwnd) {
        if (this.hwnd == hwnd) {
            return;
        }
        this.hwnd = hwnd;
        for (WWindowPeer window : blockedWindows) {
            if (hwnd != 0) {
                window.modalDisable((Dialog)target, hwnd);
            } else {
                window.modalEnable((Dialog)target);
            }
        }
    }

    /*
     * The function converts the file names (the buffer parameter)
     * in the Windows format into the Java format and saves the results
     * into the FileDialog instance.
     *
     * If it's the multi-select mode, the buffer contains the current
     * directory followed by the short names of the files.
     * The directory and file name strings are NULL separated.
     * If it's the single-select mode, the buffer doesn't have the NULL
     * separator between the path and the file name.
     *
     * NOTE: This method is called by privileged threads.
     *       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
     */
    void handleSelected(final char[] buffer)
    {
        String[] wFiles = (new String(buffer)).split("\0"); // NULL is the delimiter
        boolean multiple = (wFiles.length > 1);

        String jDirectory = null;
        String jFile = null;
        File[] jFiles = null;

        if (multiple) {
            jDirectory = wFiles[0];
            int filesNumber = wFiles.length - 1;
            jFiles = new File[filesNumber];
            for (int i = 0; i < filesNumber; i++) {
                jFiles[i] = new File(jDirectory, wFiles[i + 1]);
        }
            jFile = wFiles[1]; // choose any file
        } else {
            int index = wFiles[0].lastIndexOf(java.io.File.separatorChar);
            if (index == -1) {
                jDirectory = "."+java.io.File.separator;
                jFile = wFiles[0];
            } else {
                jDirectory = wFiles[0].substring(0, index + 1);
                jFile = wFiles[0].substring(index + 1);
            }
            jFiles = new File[] { new File(jDirectory, jFile) };
        }

        final FileDialog fileDialog = (FileDialog)target;
        AWTAccessor.FileDialogAccessor fileDialogAccessor = AWTAccessor.getFileDialogAccessor();

        fileDialogAccessor.setDirectory(fileDialog, jDirectory);
        fileDialogAccessor.setFile(fileDialog, jFile);
        fileDialogAccessor.setFiles(fileDialog, jFiles);

        WToolkit.executeOnEventHandlerThread(fileDialog, new Runnable() {
             @Override
             public void run() {
                 fileDialog.setVisible(false);
             }
        });
    } // handleSelected()

    // NOTE: This method is called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    void handleCancel() {
        final FileDialog fileDialog = (FileDialog)target;

        AWTAccessor.getFileDialogAccessor().setFile(fileDialog, null);
        AWTAccessor.getFileDialogAccessor().setFiles(fileDialog, null);
        AWTAccessor.getFileDialogAccessor().setDirectory(fileDialog, null);

        WToolkit.executeOnEventHandlerThread(fileDialog, new Runnable() {
             @Override
             public void run() {
                 fileDialog.setVisible(false);
             }
        });
    } // handleCancel()

    //This whole static block is a part of 4152317 fix
    static {
        @SuppressWarnings("removal")
        String filterString = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                @Override
                public String run() {
                    try {
                        ResourceBundle rb = ResourceBundle.getBundle("sun.awt.windows.awtLocalization");
                        return rb.getString("allFiles");
                    } catch (MissingResourceException e) {
                        return "All Files";
                    }
                }
            });
        setFilterString(filterString);
    }

    void blockWindow(WWindowPeer window) {
        blockedWindows.add(window);
        // if this dialog hasn't got an HWND, notification is
        // postponed until setHWnd() is called
        if (hwnd != 0) {
            window.modalDisable((Dialog)target, hwnd);
        }
    }
    void unblockWindow(WWindowPeer window) {
        blockedWindows.remove(window);
        // if this dialog hasn't got an HWND or has been already
        // closed, don't send notification
        if (hwnd != 0) {
            window.modalEnable((Dialog)target);
        }
    }

    @Override
    public void blockWindows(java.util.List<Window> toBlock) {
        for (Window w : toBlock) {
            WWindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(w);
            if (wp != null) {
                blockWindow(wp);
            }
        }
    }

    @Override
    public native void toFront();
    @Override
    public native void toBack();

    // unused methods.  Overridden to disable this functionality as
    // it requires HWND which is not available for FileDialog
    @Override
    public void updateAlwaysOnTopState() {}
    @Override
    public void setDirectory(String dir) {}
    @Override
    public void setFile(String file) {}
    @Override
    public void setTitle(String title) {}

    @Override
    public void setResizable(boolean resizable) {}
    @Override
    void enable() {}
    @Override
    void disable() {}
    @Override
    public void reshape(int x, int y, int width, int height) {}
    @SuppressWarnings("deprecation")
    public boolean handleEvent(Event e) { return false; }
    @Override
    public void setForeground(Color c) {}
    @Override
    public void setBackground(Color c) {}
    @Override
    public void setFont(Font f) {}
    @Override
    public void updateMinimumSize() {}
    @Override
    public void updateIconImages() {}
    public boolean requestFocus(boolean temporary,
                                boolean focusedWindowChangeAllowed) {
        return false;
    }

    @Override
    public boolean requestFocus
         (Component lightweightChild, boolean temporary,
          boolean focusedWindowChangeAllowed, long time, Cause cause)
    {
        return false;
    }

    @Override
    void start() {}
    @Override
    public void beginValidate() {}
    @Override
    public void endValidate() {}
    void invalidate(int x, int y, int width, int height) {}
    @Override
    public void addDropTarget(DropTarget dt) {}
    @Override
    public void removeDropTarget(DropTarget dt) {}
    @Override
    public void updateFocusableWindowState() {}
    @Override
    public void setZOrder(ComponentPeer above) {}

    /**
     * Initialize JNI field and method ids
     */
    private static native void initIDs();

    // The effects are not supported for system dialogs.
    @Override
    public void applyShape(sun.java2d.pipe.Region shape) {}
    @Override
    public void setOpacity(float opacity) {}
    @Override
    public void setOpaque(boolean isOpaque) {}
    public void updateWindow(java.awt.image.BufferedImage backBuffer) {}

    // the file/print dialogs are native dialogs and
    // the native system does their own rendering
    @Override
    public void createScreenSurface(boolean isResize) {}
    @Override
    public void replaceSurfaceData() {}

    public boolean isMultipleMode() {
        FileDialog fileDialog = (FileDialog)target;
        return AWTAccessor.getFileDialogAccessor().isMultipleMode(fileDialog);
    }

    @Override
    public native Point getLocationOnScreen();
}
