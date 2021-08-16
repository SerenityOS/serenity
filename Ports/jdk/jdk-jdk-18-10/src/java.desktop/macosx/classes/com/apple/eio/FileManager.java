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

package com.apple.eio;

import java.io.*;

/**
 * Provides functionality to query and modify Mac-specific file attributes. The methods in this class are based on Finder
 * attributes. These attributes in turn are dependent on HFS and HFS+ file systems. As such, it is important to recognize
 * their limitation when writing code that must function well across multiple platforms.<p>
 *
 * In addition to file name suffixes, Mac OS X can use Finder attributes like file {@code type} and {@code creator} codes to
 * identify and handle files. These codes are unique 4-byte identifiers. The file {@code type} is a string that describes the
 * contents of a file. For example, the file type {@code APPL} identifies the file as an application and therefore
 * executable. A file type of {@code TEXT}  means that the file contains raw text. Any application that can read raw
 * text can open a file of type {@code TEXT}. Applications that use proprietary file types might assign their files a proprietary
 * file {@code type} code.
 * <p>
 * To identify the application that can handle a document, the Finder can look at the {@code creator}. For example, if a user
 * double-clicks on a document with the {@code ttxt creator}, it opens up in Text Edit, the application registered
 * with the {@code ttxt creator} code. Note that the {@code creator}
 * code can be set to any application, not necessarily the application that created it. For example, if you
 * use an editor to create an HTML document, you might want to assign a browser's {@code creator} code for the file rather than
 * the HTML editor's {@code creator} code. Double-clicking on the document then opens the appropriate browser rather than the
 *HTML editor.
 *<p>
 * If you plan to publicly distribute your application, you must register its creator and any proprietary file types with the Apple
 * Developer Connection to avoid collisions with codes used by other developers. You can register a codes online at the
 * <a target=_blank href=http://developer.apple.com/dev/cftype/>Creator Code Registration</a> site.
 *
 * @since 1.4
 */
public class FileManager {
    static {
        loadOSXLibrary();
    }

    @SuppressWarnings("removal")
    private static void loadOSXLibrary() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("osx");
                    return null;
                }
            });
    }

    /**
     * The default
     * @since Java for Mac OS X 10.5 - 1.5
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
     */
    public static final short kOnAppropriateDisk = -32767;
    /**
     * Read-only system hierarchy.
     * @since Java for Mac OS X 10.5 - 1.5
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
     */
    public static final short kSystemDomain = -32766;
    /**
     * All users of a single machine have access to these resources.
     * @since Java for Mac OS X 10.5 - 1.5
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
     */
    public static final short kLocalDomain = -32765;
    /**
     * All users configured to use a common network server has access to these resources.
     * @since Java for Mac OS X 10.5 - 1.5
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
     */
    public static final short kNetworkDomain = -32764;
    /**
     * Read/write. Resources that are private to the user.
     * @since Java for Mac OS X 10.5 - 1.5
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
     */
    public static final short kUserDomain = -32763;


        /**
         * Converts an OSType (e.g. "macs"
         * from {@literal <CarbonCore/Folders.h>}) into an int.
         *
         * @param type the 4 character type to convert.
         * @return an int representing the 4 character value
         *
         * @since Java for Mac OS X 10.5 - 1.5
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
         */
    @SuppressWarnings("deprecation")
        public static int OSTypeToInt(String type) {
        int result = 0;

                byte[] b = { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
                int len = type.length();
                if (len > 0) {
                        if (len > 4) len = 4;
                        type.getBytes(0, len, b, 4 - len);
                }

                for (int i = 0;  i < len;  i++)  {
                        if (i > 0) result <<= 8;
                        result |= (b[i] & 0xff);
                }

        return result;
    }

    /**
         * Sets the file {@code type} and {@code creator} codes for a file or folder.
         *
         * @since 1.4
         */
    public static void setFileTypeAndCreator(String filename, int type, int creator) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkWrite(filename);
        }
        _setFileTypeAndCreator(filename, type, creator);
    }
        private static native void _setFileTypeAndCreator(String filename, int type, int creator) throws IOException;

    /**
         * Sets the file {@code type} code for a file or folder.
         *
         * @since 1.4
         */
    public static void setFileType(String filename, int type) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkWrite(filename);
        }
        _setFileType(filename, type);
        }
    private static native void _setFileType(String filename, int type) throws IOException;

    /**
         * Sets the file {@code creator} code for a file or folder.
         *
         * @since 1.4
         */
    public static void setFileCreator(String filename, int creator) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkWrite(filename);
        }
        _setFileCreator(filename, creator);
    }
    private static native void _setFileCreator(String filename, int creator) throws IOException;

    /**
         * Obtains the file {@code type} code for a file or folder.
         *
         * @since 1.4
         */
    public static int getFileType(String filename) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(filename);
        }
        return _getFileType(filename);
    }
    private static native int _getFileType(String filename) throws IOException;

    /**
         * Obtains the file {@code creator} code for a file or folder.
         *
         * @since 1.4
         */
    public static int getFileCreator(String filename) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(filename);
        }
        return _getFileCreator(filename);
    }
    private static native int _getFileCreator(String filename) throws IOException;


    /**
         * Locates a folder of a particular type. Mac OS X recognizes certain specific folders that have distinct purposes.
         * For example, the user's desktop or temporary folder. These folders have corresponding codes. Given one of these codes,
         * this method returns the path to that particular folder. Certain folders of a given type may appear in more than
         * one domain. For example, although there is only one {@code root} folder, there are multiple {@code pref}
         * folders. If this method is called to find the {@code pref} folder, it will return the first one it finds,
         * the user's preferences folder in {@code ~/Library/Preferences}. To explicitly locate a folder in a certain
         * domain use {@code findFolder(short domain, int folderType)} or
         * {@code findFolder(short domain, int folderType, boolean createIfNeeded)}.
         *
         * @return the path to the folder searched for
         *
         * @since 1.4
         */
        public static String findFolder(int folderType) throws FileNotFoundException {
                return findFolder(kOnAppropriateDisk, folderType);
        }

    /**
         * Locates a folder of a particular type, within a given domain. Similar to {@code findFolder(int folderType)}
         * except that the domain to look in can be specified. Valid values for {@code domain} include:
         * <dl>
         * <dt>user</dt>
         * <dd>The User domain contains resources specific to the user who is currently logged in</dd>
         * <dt>local</dt>
         * <dd>The Local domain contains resources shared by all users of the system but are not needed for the system
         * itself to run.</dd>
         * <dt>network</dt>
         * <dd>The Network domain contains resources shared by users of a local area network.</dd>
         * <dt>system</dt>
         * <dd>The System domain contains the operating system resources installed by Apple.</dd>
         * </dl>
         *
         * @return the path to the folder searched for
         *
         * @since 1.4
         */
        public static String findFolder(short domain, int folderType) throws FileNotFoundException {
                return findFolder(domain, folderType, false);
        }

    /**
         * Locates a folder of a particular type within a given domain and optionally creating the folder if it does
         * not exist. The behavior is similar to {@code findFolder(int folderType)} and
         * {@code findFolder(short domain, int folderType)} except that it can create the folder if it does not already exist.
         *
         * @param createIfNeeded
         *            set to {@code true}, by setting to {@code false} the behavior will be the
         *            same as {@code findFolder(short domain, int folderType, boolean createIfNeeded)}
         * @return the path to the folder searched for
         *
         * @since 1.4
         */
    public static String findFolder(short domain, int folderType, boolean createIfNeeded) throws FileNotFoundException {
        @SuppressWarnings("removal")
        final SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("canExamineFileSystem"));
        }

        final String foundFolder = _findFolder(domain, folderType, createIfNeeded);
        if (foundFolder == null) throw new FileNotFoundException("Can't find folder: " + Integer.toHexString(folderType));
        return foundFolder;
    }
    private static native String _findFolder(short domain, int folderType, boolean createIfNeeded);


    /**
         * Opens the path specified by a URL in the appropriate application for that URL. HTTP URL's ({@code http://})
         * open in the default browser as set in the Internet pane of System Preferences. File ({@code file://}) and
         * FTP URL's ({@code ftp://}) open in the Finder. Note that opening an FTP URL will prompt the user for where
         * they want to save the downloaded file(s).
         *
         * @param url
         *            the URL for the file you want to open, it can either be an HTTP, FTP, or file url
         *
         * @deprecated this functionality has been superseded by java.awt.Desktop.browse() and java.awt.Desktop.open()
         *
         * @since 1.4
         */
    @Deprecated
    public static void openURL(String url) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("canOpenURLs"));
        }
        _openURL(url);
    }
    private static native void _openURL(String url) throws IOException;

    /**
         * @return full pathname for the resource identified by a given name.
         *
         * @since 1.4
         */
        public static String getResource(String resourceName) throws FileNotFoundException {
                return getResourceFromBundle(resourceName, null, null);
        }

        /**
         * @return full pathname for the resource identified by a given name and located in the specified bundle subdirectory.
         *
         * @since 1.4
         */
        public static String getResource(String resourceName, String subDirName) throws FileNotFoundException {
                return getResourceFromBundle(resourceName, subDirName, null);
        }

        /**
         * Returns the full pathname for the resource identified by the given name and file extension
         * and located in the specified bundle subdirectory.
         *
         * If extension is an empty string or null, the returned pathname is the first one encountered where the
         * file name exactly matches name.
         *
         * If subpath is null, this method searches the top-level nonlocalized resource directory (typically Resources)
         * and the top-level of any language-specific directories. For example, suppose you have a modern bundle and
         * specify "Documentation" for the subpath parameter. This method would first look in the
         * Contents/Resources/Documentation directory of the bundle, followed by the Documentation subdirectories of
         * each language-specific .lproj directory. (The search order for the language-specific directories
         * corresponds to the user's preferences.) This method does not recurse through any other subdirectories at
         * any of these locations. For more details see the AppKit NSBundle documentation.
         *
         * @return full pathname for the resource identified by the given name and file extension and located in the specified bundle subdirectory.
         *
         * @since 1.4
         */
        public static String getResource(String resourceName, String subDirName, String type) throws FileNotFoundException {
                return getResourceFromBundle(resourceName, subDirName, type);
        }

        private static native String getNativeResourceFromBundle(String resourceName, String subDirName, String type) throws FileNotFoundException;
        private static String getResourceFromBundle(String resourceName, String subDirName, String type) throws FileNotFoundException {
                @SuppressWarnings("removal")
                final SecurityManager security = System.getSecurityManager();
                if (security != null) security.checkPermission(new RuntimePermission("canReadBundle"));

                final String resourceFromBundle = getNativeResourceFromBundle(resourceName, subDirName, type);
                if (resourceFromBundle == null) throw new FileNotFoundException(resourceName);
                return resourceFromBundle;
        }

        /**
         * Obtains the path to the current application's NSBundle, may not
         * return a valid path if Java was launched from the command line.
         *
         * @return full pathname of the NSBundle of the current application executable.
         *
         * @since Java for Mac OS X 10.5 Update 1 - 1.6
         * @since Java for Mac OS X 10.5 Update 2 - 1.5
         */
        public static String getPathToApplicationBundle() {
                @SuppressWarnings("removal")
                SecurityManager security = System.getSecurityManager();
                if (security != null) security.checkPermission(new RuntimePermission("canReadBundle"));
                return getNativePathToApplicationBundle();
        }

        private static native String getNativePathToApplicationBundle();

        /**
         * Moves the specified file to the Trash
         *
         * @param file the file
         * @return returns true if the NSFileManager successfully moved the file to the Trash.
         * @throws FileNotFoundException
         *
         * @since Java for Mac OS X 10.6 Update 1 - 1.6
         * @since Java for Mac OS X 10.5 Update 6 - 1.6, 1.5
         */
        public static boolean moveToTrash(final File file) throws FileNotFoundException {
                if (file == null) throw new FileNotFoundException();
                final String fileName = file.getAbsolutePath();

                @SuppressWarnings("removal")
                final SecurityManager security = System.getSecurityManager();
                if (security != null) security.checkDelete(fileName);

                return _moveToTrash(fileName);
        }

        private static native boolean _moveToTrash(String fileName);

        /**
         * Reveals the specified file in the Finder
         *
         * @param file
         *            the file to reveal
         * @return returns true if the NSFileManager successfully revealed the file in the Finder.
         * @throws FileNotFoundException
         *
         * @since Java for Mac OS X 10.6 Update 1 - 1.6
         * @since Java for Mac OS X 10.5 Update 6 - 1.6, 1.5
         */
        public static boolean revealInFinder(final File file) throws FileNotFoundException {
                if (file == null || !file.exists()) throw new FileNotFoundException();
                final String fileName = file.getAbsolutePath();

                @SuppressWarnings("removal")
                final SecurityManager security = System.getSecurityManager();
                if (security != null) security.checkRead(fileName);

                return _revealInFinder(fileName);
        }

        private static native boolean _revealInFinder(String fileName);
}
