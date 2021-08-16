/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * Interface for managing the underlying file system
 */

public interface FtpFileSystemHandler {
    /*
     * Change current directory to.
     * Returns <code>true</code> if operation was successful.
     *
     * @param path the path of the directory to change to, in either relative
     * or absolute form.
     * @return <code>true</code> if the operation was successful.
     */
    public boolean cd(String path);
    /*
     * Change current directory to parent.
     * @return <code>true</code> if the operation was successful.
     */
    public boolean cdUp();
    /*
     * Print Working Directory. I.E. returns a string containing the current
     * working directory full path.
     */
    public String pwd();
    /*
     * Tests if a specified file exists. Returns <code>true</code> if the file
     * does exist.
     * @param name can be either a relative pathname or an absolute pathname.
     * @return <code>true</code> if the file exists.
     */
    public boolean fileExists(String name);
    /*
     * Get the content of a file. Returns an InputStream pointing to the
     * content of the file whose name was passed as an argument.
     * Returns <code>null</code> if the operation failed.
     */
    public java.io.InputStream getFile(String name);
    /*
     * Returns the size, in bytes, of the specified file.
     *
     * @param name the pathname, which can be either relative or absolute,
     *             of the file.
     * @return the size in bytes of the file.
     */
    public long getFileSize(String name);
    /*
     * Get the content of the current directory. Returns an InputStream
     * pointing to the content (in text form) of the current directory.
     * Returns <code>null</code> if the operation failed.
     */
    public java.io.InputStream listCurrentDir();
    /*
     * Open a file for writing on the server and provides an OutputStream
     * pointing to it.
     * Returns <code>null</code> if the operation failed.
     */
    public java.io.OutputStream putFile(String name);
    /*
     * Remove the specified file on the server. Returns <code>true</code> if
     * the operation was successful.
     * @return <code>true</code> if the operation was successful.
     */
    public boolean removeFile(String name);
    /*
     * Creates a directory on the server. Returns <code>true</code> if the
     * operation was successful.
     *
     * @param name the path of the directory to create, which can be
     * either in relative or absolute for.
     * @return <code>true</code> if the operation was successful.
     */
    public boolean mkdir(String name);
    /*
     * Rename a file in the current working directory.
     * Returns <code>true</code> if the operation was successful.
     *
     * @param from the name of the file to rename.
     * @param to the new name.
     * @return <code>true</code> if the operation was successful.
     */

    public boolean rename(String from, String to);
}
