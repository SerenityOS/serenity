/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.filechooser.FileView;

import com.apple.laf.AquaUtils.RecyclableSingleton;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("serial") // JDK implementation class
class AquaFileView extends FileView {
    private static final boolean DEBUG = false;

    private static final int UNINITALIZED_LS_INFO = -1;

    // Constants from LaunchServices.h
    static final int kLSItemInfoIsPlainFile        = 0x00000001; /* Not a directory, volume, or symlink*/
    static final int kLSItemInfoIsPackage          = 0x00000002; /* Packaged directory*/
    static final int kLSItemInfoIsApplication      = 0x00000004; /* Single-file or packaged application*/
    static final int kLSItemInfoIsContainer        = 0x00000008; /* Directory (includes packages) or volume*/
    static final int kLSItemInfoIsAliasFile        = 0x00000010; /* Alias file (includes sym links)*/
    static final int kLSItemInfoIsSymlink          = 0x00000020; /* UNIX sym link*/
    static final int kLSItemInfoIsInvisible        = 0x00000040; /* Invisible by any known mechanism*/
    static final int kLSItemInfoIsNativeApp        = 0x00000080; /* Carbon or Cocoa native app*/
    static final int kLSItemInfoIsClassicApp       = 0x00000100; /* CFM/68K Classic app*/
    static final int kLSItemInfoAppPrefersNative   = 0x00000200; /* Carbon app that prefers to be launched natively*/
    static final int kLSItemInfoAppPrefersClassic  = 0x00000400; /* Carbon app that prefers to be launched in Classic*/
    static final int kLSItemInfoAppIsScriptable    = 0x00000800; /* App can be scripted*/
    static final int kLSItemInfoIsVolume           = 0x00001000; /* Item is a volume*/
    static final int kLSItemInfoExtensionIsHidden  = 0x00100000; /* Item has a hidden extension*/

    static {
        loadOSXUILibrary();
    }

    @SuppressWarnings("removal")
    private static void loadOSXUILibrary() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("osxui");
                    return null;
                }
            });
    }

    // TODO: Un-comment this out when the native version exists
    //private static native String getNativePathToRunningJDKBundle();
    private static native String getNativePathToSharedJDKBundle();

    private static native String getNativeMachineName();
    private static native String getNativeDisplayName(final byte[] pathBytes, final boolean isDirectory);
    private static native int getNativeLSInfo(final byte[] pathBytes, final boolean isDirectory);
    private static native String getNativePathForResolvedAlias(final byte[] absolutePath, final boolean isDirectory);

    private static final RecyclableSingleton<String> machineName = new RecyclableSingleton<String>() {
        @Override
        protected String getInstance() {
            return getNativeMachineName();
        }
    };
    private static String getMachineName() {
        return machineName.get();
    }

    protected static String getPathToRunningJDKBundle() {
        // TODO: Return empty string for now
        return "";//getNativePathToRunningJDKBundle();
    }

    protected static String getPathToSharedJDKBundle() {
        return getNativePathToSharedJDKBundle();
    }

    static class FileInfo {
        final boolean isDirectory;
        final String absolutePath;
        byte[] pathBytes;

        String displayName;
        Icon icon;
        int launchServicesInfo = UNINITALIZED_LS_INFO;

        FileInfo(final File file){
            isDirectory = file.isDirectory();
            absolutePath = file.getAbsolutePath();
            pathBytes = absolutePath.getBytes(UTF_8);
        }
    }

    final int MAX_CACHED_ENTRIES = 256;
    protected final Map<File, FileInfo> cache = new LinkedHashMap<File, FileInfo>(){
        protected boolean removeEldestEntry(final Entry<File, FileInfo> eldest) {
            return size() > MAX_CACHED_ENTRIES;
        }
    };

    FileInfo getFileInfoFor(final File file) {
        final FileInfo info = cache.get(file);
        if (info != null) return info;
        final FileInfo newInfo = new FileInfo(file);
        cache.put(file, newInfo);
        return newInfo;
    }


    final AquaFileChooserUI fFileChooserUI;
    public AquaFileView(final AquaFileChooserUI fileChooserUI) {
        fFileChooserUI = fileChooserUI;
    }

    String _directoryDescriptionText() {
        return fFileChooserUI.directoryDescriptionText;
    }

    String _fileDescriptionText() {
        return fFileChooserUI.fileDescriptionText;
    }

    boolean _packageIsTraversable() {
        return fFileChooserUI.fPackageIsTraversable == AquaFileChooserUI.kOpenAlways;
    }

    boolean _applicationIsTraversable() {
        return fFileChooserUI.fApplicationIsTraversable == AquaFileChooserUI.kOpenAlways;
    }

    public String getName(final File f) {
        final FileInfo info = getFileInfoFor(f);
        if (info.displayName != null) return info.displayName;

        final String nativeDisplayName = getNativeDisplayName(info.pathBytes, info.isDirectory);
        if (nativeDisplayName != null) {
            info.displayName = nativeDisplayName;
            return nativeDisplayName;
        }

        final String displayName = f.getName();
        if (f.isDirectory() && fFileChooserUI.getFileChooser().getFileSystemView().isRoot(f)) {
            final String localMachineName = getMachineName();
            info.displayName = localMachineName;
            return localMachineName;
        }

        info.displayName = displayName;
        return displayName;
    }

    public String getDescription(final File f) {
        return f.getName();
    }

    public String getTypeDescription(final File f) {
        if (f.isDirectory()) return _directoryDescriptionText();
        return _fileDescriptionText();
    }

    public Icon getIcon(final File f) {
        final FileInfo info = getFileInfoFor(f);
        if (info.icon != null) return info.icon;

        if (f == null) {
            info.icon = AquaIcon.SystemIcon.getDocumentIconUIResource();
        } else {
            // Look for the document's icon
            final AquaIcon.FileIcon fileIcon = new AquaIcon.FileIcon(f);
            info.icon = fileIcon;
            if (!fileIcon.hasIconRef()) {
                // Fall back on the default icons
                if (f.isDirectory()) {
                    if (fFileChooserUI.getFileChooser().getFileSystemView().isRoot(f)) {
                        info.icon = AquaIcon.SystemIcon.getComputerIconUIResource();
                    } else if (f.getParent() == null || f.getParent().equals("/")) {
                        info.icon = AquaIcon.SystemIcon.getHardDriveIconUIResource();
                    } else {
                        info.icon = AquaIcon.SystemIcon.getFolderIconUIResource();
                    }
                } else {
                    info.icon = AquaIcon.SystemIcon.getDocumentIconUIResource();
                }
            }
        }

        return info.icon;
    }

    // aliases are traversable though they aren't directories
    public Boolean isTraversable(final File f) {
        if (f.isDirectory()) {
            // Doesn't matter if it's a package or app, because they're traversable
            if (_packageIsTraversable() && _applicationIsTraversable()) {
                return Boolean.TRUE;
            } else if (!_packageIsTraversable() && !_applicationIsTraversable()) {
                if (isPackage(f) || isApplication(f)) return Boolean.FALSE;
            } else if (!_applicationIsTraversable()) {
                if (isApplication(f)) return Boolean.FALSE;
            } else if (!_packageIsTraversable()) {
                // [3101730] All applications are packages, but not all packages are applications.
                if (isPackage(f) && !isApplication(f)) return Boolean.FALSE;
            }

            // We're allowed to traverse it
            return Boolean.TRUE;
        }

        if (isAlias(f)) {
            final File realFile = resolveAlias(f);
            return realFile.isDirectory() ? Boolean.TRUE : Boolean.FALSE;
        }

        return Boolean.FALSE;
    }

    int getLSInfoFor(final File f) {
        final FileInfo info = getFileInfoFor(f);

        if (info.launchServicesInfo == UNINITALIZED_LS_INFO) {
            info.launchServicesInfo = getNativeLSInfo(info.pathBytes, info.isDirectory);
        }

        return info.launchServicesInfo;
    }

    boolean isAlias(final File f) {
        final int lsInfo = getLSInfoFor(f);
        return ((lsInfo & kLSItemInfoIsAliasFile) != 0) && ((lsInfo & kLSItemInfoIsSymlink) == 0);
    }

    boolean isApplication(final File f) {
        return (getLSInfoFor(f) & kLSItemInfoIsApplication) != 0;
    }

    boolean isPackage(final File f) {
        return (getLSInfoFor(f) & kLSItemInfoIsPackage) != 0;
    }

    /**
     * Things that need to be handled:
     * -Change getFSRef to use CFURLRef instead of FSPathMakeRef
     * -Use the HFS-style path from CFURLRef in resolveAlias() to avoid
     *      path length limitations
     * -In resolveAlias(), simply resolve immediately if this is an alias
     */

    /**
     * Returns the actual file represented by this object.  This will
     * resolve any aliases in the path, including this file if it is an
     * alias.  No alias resolution requiring user interaction (e.g.
     * mounting servers) will occur.  Note that aliases to servers may
     * take a significant amount of time to resolve.  This method
     * currently does not have any provisions for a more fine-grained
     * timeout for alias resolution beyond that used by the system.
     *
     * In the event of a path that does not contain any aliases, or if the file
     *  does not exist, this method will return the file that was passed in.
     *    @return    The canonical path to the file
     *    @throws    IOException    If an I/O error occurs while attempting to
     *                            construct the path
     */
    File resolveAlias(final File mFile) {
        // If the file exists and is not an alias, there aren't
        // any aliases along its path, so the standard version
        // of getCanonicalPath() will work.
        if (mFile.exists() && !isAlias(mFile)) {
            if (DEBUG) System.out.println("not an alias");
            return mFile;
        }

        // If it doesn't exist, either there's an alias in the
        // path or this is an alias.  Traverse the path and
        // resolve all aliases in it.
        final LinkedList<String> components = getPathComponents(mFile);
        if (components == null) {
            if (DEBUG) System.out.println("getPathComponents is null ");
            return mFile;
        }

        File file = new File("/");
        for (final String nextComponent : components) {
            file = new File(file, nextComponent);
            final FileInfo info = getFileInfoFor(file);

            // If any point along the way doesn't exist,
            // just return the file.
            if (!file.exists()) { return mFile; }

            if (isAlias(file)) {
                // Resolve it!
                final String path = getNativePathForResolvedAlias(info.pathBytes, info.isDirectory);

                // <rdar://problem/3582601> If the alias doesn't resolve (on a non-existent volume, for example)
                // just return the file.
                if (path == null) return mFile;

                file = new File(path);
            }
        }

        return file;
    }

    /**
     * Returns a linked list of Strings consisting of the components of
     * the path of this file, in order, including the filename as the
     * last element.  The first element in the list will be the first
     * directory in the path, or "".
     *    @return A linked list of the components of this file's path
     */
    private static LinkedList<String> getPathComponents(final File mFile) {
        final LinkedList<String> componentList = new LinkedList<String>();
        String parent;

        File file = new File(mFile.getAbsolutePath());
        componentList.add(0, file.getName());
        while ((parent = file.getParent()) != null) {
            file = new File(parent);
            componentList.add(0, file.getName());
        }
        return componentList;
    }
}
