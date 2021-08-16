/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
package sun.net.ftp;

import java.util.Date;
import java.util.HashMap;

/**
 * A {@code FtpDirEntry} is a class agregating all the information that the FTP client
 * can gather from the server by doing a {@code LST} (or {@code NLST}) command and
 * parsing the output. It will typically contain the name, type, size, last modification
 * time, owner and group of the file, although some of these could be unavailable
 * due to specific FTP server limitations.
 *
 * @see sun.net.ftp.FtpDirParser
 * @since 1.7
 */
public class FtpDirEntry {

    public enum Type {

        FILE, DIR, PDIR, CDIR, LINK
    };

    public enum Permission {

        USER(0), GROUP(1), OTHERS(2);
        int value;

        Permission(int v) {
            value = v;
        }
    };
    private final String name;
    private String user = null;
    private String group = null;
    private long size = -1;
    private java.util.Date created = null;
    private java.util.Date lastModified = null;
    private Type type = Type.FILE;
    private boolean[][] permissions = null;
    private HashMap<String, String> facts = new HashMap<String, String>();

    private FtpDirEntry() {
        name = null;
    }

    /**
     * Creates an FtpDirEntry instance with only the name being set.
     *
     * @param name The name of the file
     */
    public FtpDirEntry(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the remote file.
     *
     * @return a {@code String} containing the name of the remote file.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user name of the owner of the file as returned by the FTP
     * server, if provided. This could be a name or a user id (number).
     *
     * @return a {@code String} containing the user name or
     *         {@code null} if that information is not available.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user name of the owner of the file. Intended mostly to be
     * used from inside a {@link java.net.FtpDirParser} implementation.
     *
     * @param user The user name of the owner of the file, or {@code null}
     * if that information is not available.
     * @return this FtpDirEntry
     */
    public FtpDirEntry setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * Returns the group name of the file as returned by the FTP
     * server, if provided. This could be a name or a group id (number).
     *
     * @return a {@code String} containing the group name or
     *         {@code null} if that information is not available.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the name of the group to which the file belong. Intended mostly to be
     * used from inside a {@link java.net.FtpDirParser} implementation.
     *
     * @param group The name of the group to which the file belong, or {@code null}
     * if that information is not available.
     * @return this FtpDirEntry
     */
    public FtpDirEntry setGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     * Returns the size of the remote file as it was returned by the FTP
     * server, if provided.
     *
     * @return the size of the file or -1 if that information is not available.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size of that file. Intended mostly to be used from inside an
     * {@link java.net.FtpDirParser} implementation.
     *
     * @param size The size, in bytes, of that file. or -1 if unknown.
     * @return this FtpDirEntry
     */
    public FtpDirEntry setSize(long size) {
        this.size = size;
        return this;
    }

    /**
     * Returns the type of the remote file as it was returned by the FTP
     * server, if provided.
     * It returns a FtpDirEntry.Type enum and the values can be:
     * - FtpDirEntry.Type.FILE for a normal file
     * - FtpDirEntry.Type.DIR for a directory
     * - FtpDirEntry.Type.LINK for a symbolic link
     *
     * @return a {@code FtpDirEntry.Type} describing the type of the file
     *         or {@code null} if that information is not available.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of the file. Intended mostly to be used from inside an
     * {@link java.net.FtpDirParser} implementation.
     *
     * @param type the type of this file or {@code null} if that information
     * is not available.
     * @return this FtpDirEntry
     */
    public FtpDirEntry setType(Type type) {
        this.type = type;
        return this;
    }

    /**
     * Returns the last modification time of the remote file as it was returned
     * by the FTP server, if provided, {@code null} otherwise.
     *
     * @return a <code>Date</code> representing the last time the file was
     *         modified on the server, or {@code null} if that
     *         information is not available.
     */
    public java.util.Date getLastModified() {
        return this.lastModified;
    }

    /**
     * Sets the last modification time of the file. Intended mostly to be used
     * from inside an {@link java.net.FtpDirParser} implementation.
     *
     * @param lastModified The Date representing the last modification time, or
     * {@code null} if that information is not available.
     * @return this FtpDirEntry
     */
    public FtpDirEntry setLastModified(Date lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    /**
     * Returns whether read access is granted for a specific permission.
     *
     * @param p the Permission (user, group, others) to check.
     * @return {@code true} if read access is granted.
     */
    public boolean canRead(Permission p) {
        if (permissions != null) {
            return permissions[p.value][0];
        }
        return false;
    }

    /**
     * Returns whether write access is granted for a specific permission.
     *
     * @param p the Permission (user, group, others) to check.
     * @return {@code true} if write access is granted.
     */
    public boolean canWrite(Permission p) {
        if (permissions != null) {
            return permissions[p.value][1];
        }
        return false;
    }

    /**
     * Returns whether execute access is granted for a specific permission.
     *
     * @param p the Permission (user, group, others) to check.
     * @return {@code true} if execute access is granted.
     */
    public boolean canExexcute(Permission p) {
        if (permissions != null) {
            return permissions[p.value][2];
        }
        return false;
    }

    /**
     * Sets the permissions for that file. Intended mostly to be used
     * from inside an {@link java.net.FtpDirParser} implementation.
     * The permissions array is a 3x3 {@code boolean} array, the first index being
     * the User, group or owner (0, 1 and 2 respectively) while the second
     * index is read, write or execute (0, 1 and 2 respectively again).
     * <p>E.G.: {@code permissions[1][2]} is the group/execute permission.</p>
     *
     * @param permissions a 3x3 {@code boolean} array
     * @return this {@code FtpDirEntry}
     */
    public FtpDirEntry setPermissions(boolean[][] permissions) {
        this.permissions = permissions;
        return this;
    }

    /**
     * Adds a 'fact', as defined in RFC 3659, to the list of facts of this file.
     * Intended mostly to be used from inside a {@link java.net.FtpDirParser}
     * implementation.
     *
     * @param fact the name of the fact (e.g. "Media-Type"). It is not case-sensitive.
     * @param value the value associated with this fact.
     * @return this {@code FtpDirEntry}
     */
    public FtpDirEntry addFact(String fact, String value) {
        facts.put(fact.toLowerCase(), value);
        return this;
    }

    /**
     * Returns the requested 'fact', as defined in RFC 3659, if available.
     *
     * @param fact The name of the fact *e.g. "Media-Type"). It is not case sensitive.
     * @return The value of the fact or, {@code null} if that fact wasn't
     * provided by the server.
     */
    public String getFact(String fact) {
        return facts.get(fact.toLowerCase());
    }

    /**
     * Returns the creation time of the file, when provided by the server.
     *
     * @return The Date representing the creation time, or {@code null}
     * if the server didn't provide that information.
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the creation time for that file. Intended mostly to be used from
     * inside a {@link java.net.FtpDirParser} implementation.
     *
     * @param created the Date representing the creation time for that file, or
     * {@code null} if that information is not available.
     * @return this FtpDirEntry
     */
    public FtpDirEntry setCreated(Date created) {
        this.created = created;
        return this;
    }

    /**
     * Returns a string representation of the object.
     * The {@code toString} method for class {@code FtpDirEntry}
     * returns a string consisting of the name of the file, followed by its
     * type between brackets, followed by the user and group between
     * parenthesis, then size between '{', and, finally, the lastModified of last
     * modification if it's available.
     *
     * @return  a string representation of the object.
     */
    @Override
    public String toString() {
        if (lastModified == null) {
            return name + " [" + type + "] (" + user + " / " + group + ") " + size;
        }
        return name + " [" + type + "] (" + user + " / " + group + ") {" + size + "} " + java.text.DateFormat.getDateInstance().format(lastModified);
    }
}
