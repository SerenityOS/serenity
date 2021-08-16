/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

/**
 * Win32 APIs constants.
 */

class WindowsConstants {
    private WindowsConstants() { }

    // general
    public static final long INVALID_HANDLE_VALUE = -1L;

    // generic rights
    public static final int GENERIC_READ        = 0x80000000;
    public static final int GENERIC_WRITE       = 0x40000000;

    // share modes
    public static final int FILE_SHARE_READ     = 0x00000001;
    public static final int FILE_SHARE_WRITE    = 0x00000002;
    public static final int FILE_SHARE_DELETE   = 0x00000004;

    // creation modes
    public static final int CREATE_NEW          = 1;
    public static final int CREATE_ALWAYS       = 2;
    public static final int OPEN_EXISTING       = 3;
    public static final int OPEN_ALWAYS         = 4;
    public static final int TRUNCATE_EXISTING   = 5;

    // attributes and flags
    public static final int FILE_ATTRIBUTE_READONLY         = 0x00000001;
    public static final int FILE_ATTRIBUTE_HIDDEN           = 0x00000002;
    public static final int FILE_ATTRIBUTE_SYSTEM           = 0x00000004;
    public static final int FILE_ATTRIBUTE_DIRECTORY        = 0x00000010;
    public static final int FILE_ATTRIBUTE_ARCHIVE          = 0x00000020;
    public static final int FILE_ATTRIBUTE_DEVICE           = 0x00000040;
    public static final int FILE_ATTRIBUTE_NORMAL           = 0x00000080;
    public static final int FILE_ATTRIBUTE_REPARSE_POINT    = 0x400;
    public static final int FILE_FLAG_NO_BUFFERING          = 0x20000000;
    public static final int FILE_FLAG_OVERLAPPED            = 0x40000000;
    public static final int FILE_FLAG_WRITE_THROUGH         = 0x80000000;
    public static final int FILE_FLAG_BACKUP_SEMANTICS      = 0x02000000;
    public static final int FILE_FLAG_DELETE_ON_CLOSE       = 0x04000000;
    public static final int FILE_FLAG_OPEN_REPARSE_POINT    = 0x00200000;

    // stream ids
    public static final int BACKUP_ALTERNATE_DATA           = 0x00000004;
    public static final int BACKUP_SPARSE_BLOCK             = 0x00000009;

    // reparse point/symbolic link related constants
    public static final int IO_REPARSE_TAG_SYMLINK              = 0xA000000C;
    public static final int IO_REPARSE_TAG_AF_UNIX              = 0x80000023;
    public static final int MAXIMUM_REPARSE_DATA_BUFFER_SIZE    = 16 * 1024;
    public static final int SYMBOLIC_LINK_FLAG_DIRECTORY        = 0x1;
    public static final int SYMBOLIC_LINK_FLAG_ALLOW_UNPRIVILEGED_CREATE = 0x2;

    // volume flags
    public static final int FILE_CASE_SENSITIVE_SEARCH      = 0x00000001;
    public static final int FILE_CASE_PRESERVED_NAMES       = 0x00000002;
    public static final int FILE_PERSISTENT_ACLS            = 0x00000008;
    public static final int FILE_VOLUME_IS_COMPRESSED       = 0x00008000;
    public static final int FILE_NAMED_STREAMS              = 0x00040000;
    public static final int FILE_READ_ONLY_VOLUME           = 0x00080000;

    // error codes
    public static final int ERROR_FILE_NOT_FOUND        = 2;
    public static final int ERROR_PATH_NOT_FOUND        = 3;
    public static final int ERROR_ACCESS_DENIED         = 5;
    public static final int ERROR_INVALID_HANDLE        = 6;
    public static final int ERROR_INVALID_DATA          = 13;
    public static final int ERROR_NOT_SAME_DEVICE       = 17;
    public static final int ERROR_NOT_READY             = 21;
    public static final int ERROR_SHARING_VIOLATION     = 32;
    public static final int ERROR_FILE_EXISTS           = 80;
    public static final int ERROR_INVALID_PARAMETER     = 87;
    public static final int ERROR_DISK_FULL             = 112;
    public static final int ERROR_INSUFFICIENT_BUFFER   = 122;
    public static final int ERROR_INVALID_LEVEL         = 124;
    public static final int ERROR_DIR_NOT_ROOT          = 144;
    public static final int ERROR_DIR_NOT_EMPTY         = 145;
    public static final int ERROR_ALREADY_EXISTS        = 183;
    public static final int ERROR_MORE_DATA             = 234;
    public static final int ERROR_DIRECTORY             = 267;
    public static final int ERROR_NOTIFY_ENUM_DIR       = 1022;
    public static final int ERROR_PRIVILEGE_NOT_HELD    = 1314;
    public static final int ERROR_NONE_MAPPED           = 1332;
    public static final int ERROR_CANT_ACCESS_FILE      = 1920;
    public static final int ERROR_NOT_A_REPARSE_POINT   = 4390;
    public static final int ERROR_INVALID_REPARSE_DATA  = 4392;

    // notify filters
    public static final int FILE_NOTIFY_CHANGE_FILE_NAME   = 0x00000001;
    public static final int FILE_NOTIFY_CHANGE_DIR_NAME    = 0x00000002;
    public static final int FILE_NOTIFY_CHANGE_ATTRIBUTES  = 0x00000004;
    public static final int FILE_NOTIFY_CHANGE_SIZE        = 0x00000008;
    public static final int FILE_NOTIFY_CHANGE_LAST_WRITE  = 0x00000010;
    public static final int FILE_NOTIFY_CHANGE_LAST_ACCESS = 0x00000020;
    public static final int FILE_NOTIFY_CHANGE_CREATION    = 0x00000040;
    public static final int FILE_NOTIFY_CHANGE_SECURITY    = 0x00000100;

    // notify actions
    public static final int FILE_ACTION_ADDED              = 0x00000001;
    public static final int FILE_ACTION_REMOVED            = 0x00000002;
    public static final int FILE_ACTION_MODIFIED           = 0x00000003;
    public static final int FILE_ACTION_RENAMED_OLD_NAME   = 0x00000004;
    public static final int FILE_ACTION_RENAMED_NEW_NAME   = 0x00000005;

    // copy flags
    public static final int COPY_FILE_FAIL_IF_EXISTS       = 0x00000001;
    public static final int COPY_FILE_COPY_SYMLINK         = 0x00000800;

    // move flags
    public static final int MOVEFILE_REPLACE_EXISTING       = 0x00000001;
    public static final int MOVEFILE_COPY_ALLOWED           = 0x00000002;

    // drive types
    public static final int DRIVE_UNKNOWN                   = 0;
    public static final int DRIVE_NO_ROOT_DIR               = 1;
    public static final int DRIVE_REMOVABLE                 = 2;
    public static final int DRIVE_FIXED                     = 3;
    public static final int DRIVE_REMOTE                    = 4;
    public static final int DRIVE_CDROM                     = 5;
    public static final int DRIVE_RAMDISK                   = 6;

    // file security
    public static final int OWNER_SECURITY_INFORMATION      = 0x00000001;
    public static final int GROUP_SECURITY_INFORMATION      = 0x00000002;
    public static final int DACL_SECURITY_INFORMATION       = 0x00000004;
    public static final int SACL_SECURITY_INFORMATION       = 0x00000008;

    public static final int SidTypeUser = 1;
    public static final int SidTypeGroup = 2;
    public static final int SidTypeDomain = 3;
    public static final int SidTypeAlias = 4;
    public static final int SidTypeWellKnownGroup = 5;
    public static final int SidTypeDeletedAccount = 6;
    public static final int SidTypeInvalid = 7;
    public static final int SidTypeUnknown = 8;
    public static final int SidTypeComputer= 9;

    public static final byte ACCESS_ALLOWED_ACE_TYPE         = 0x0;
    public static final byte ACCESS_DENIED_ACE_TYPE          = 0x1;

    public static final byte OBJECT_INHERIT_ACE              = 0x1;
    public static final byte CONTAINER_INHERIT_ACE           = 0x2;
    public static final byte NO_PROPAGATE_INHERIT_ACE        = 0x4;
    public static final byte INHERIT_ONLY_ACE                = 0x8;

    public static final int DELETE                      = 0x00010000;
    public static final int READ_CONTROL                = 0x00020000;
    public static final int WRITE_DAC                   = 0x00040000;
    public static final int WRITE_OWNER                 = 0x00080000;
    public static final int SYNCHRONIZE                 = 0x00100000;

    public static final int FILE_LIST_DIRECTORY         = 0x0001;
    public static final int FILE_READ_DATA              = 0x0001;
    public static final int FILE_WRITE_DATA             = 0x0002;
    public static final int FILE_APPEND_DATA            = 0x0004;
    public static final int FILE_READ_EA                = 0x0008;
    public static final int FILE_WRITE_EA               = 0x0010;
    public static final int FILE_EXECUTE                = 0x0020;
    public static final int FILE_DELETE_CHILD           = 0x0040;
    public static final int FILE_READ_ATTRIBUTES        = 0x0080;
    public static final int FILE_WRITE_ATTRIBUTES       = 0x0100;

    public static final int FILE_GENERIC_READ           = 0x00120089;
    public static final int FILE_GENERIC_WRITE          = 0x00120116;
    public static final int FILE_GENERIC_EXECUTE        = 0x001200a0;
    public static final int FILE_ALL_ACCESS             = 0x001f01ff;

    // operating system security
    public static final int TOKEN_DUPLICATE             = 0x0002;
    public static final int TOKEN_IMPERSONATE           = 0x0004;
    public static final int TOKEN_QUERY                 = 0x0008;
    public static final int TOKEN_ADJUST_PRIVILEGES     = 0x0020;

    public static final int SE_PRIVILEGE_ENABLED        = 0x00000002;

    public static final int TokenUser                   = 1;
    public static final int PROCESS_QUERY_INFORMATION   = 0x0400;
}
