/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

/**
 * Defines the flags for used by the flags component of an ACL {@link AclEntry
 * entry}.
 *
 * <p> In this release, this class does not define flags related to {@link
 * AclEntryType#AUDIT} and {@link AclEntryType#ALARM} entry types.
 *
 * @since 1.7
 */

public enum AclEntryFlag {

    /**
     * Can be placed on a directory and indicates that the ACL entry should be
     * added to each new non-directory file created.
     */
    FILE_INHERIT,

    /**
     * Can be placed on a directory and indicates that the ACL entry should be
     * added to each new directory created.
     */
    DIRECTORY_INHERIT,

    /**
     * Can be placed on a directory to indicate that the ACL entry should not
     * be placed on the newly created directory which is inheritable by
     * subdirectories of the created directory.
     */
    NO_PROPAGATE_INHERIT,

    /**
     * Can be placed on a directory but does not apply to the directory,
     * only to newly created files/directories as specified by the
     * {@link #FILE_INHERIT} and {@link #DIRECTORY_INHERIT} flags.
     */
    INHERIT_ONLY;
}
