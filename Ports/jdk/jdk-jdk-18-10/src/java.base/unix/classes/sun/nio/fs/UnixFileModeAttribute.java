/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.attribute.*;
import java.util.*;

class UnixFileModeAttribute {
    static final int ALL_PERMISSIONS =
        UnixConstants.S_IRUSR | UnixConstants.S_IWUSR | UnixConstants.S_IXUSR |
        UnixConstants.S_IRGRP | UnixConstants.S_IWGRP | UnixConstants.S_IXGRP |
        UnixConstants.S_IROTH | UnixConstants.S_IWOTH | UnixConstants. S_IXOTH;

    static final int ALL_READWRITE =
        UnixConstants.S_IRUSR | UnixConstants.S_IWUSR |
        UnixConstants.S_IRGRP | UnixConstants.S_IWGRP |
        UnixConstants.S_IROTH | UnixConstants.S_IWOTH;

    static final int TEMPFILE_PERMISSIONS =
        UnixConstants.S_IRUSR | UnixConstants.S_IWUSR | UnixConstants.S_IXUSR;

    private UnixFileModeAttribute() {
    }

    static int toUnixMode(Set<PosixFilePermission> perms) {
        int mode = 0;
        for (PosixFilePermission perm: perms) {
            if (perm == null)
                throw new NullPointerException();
            switch (perm) {
                case OWNER_READ :     mode |= UnixConstants.S_IRUSR; break;
                case OWNER_WRITE :    mode |= UnixConstants.S_IWUSR; break;
                case OWNER_EXECUTE :  mode |= UnixConstants.S_IXUSR; break;
                case GROUP_READ :     mode |= UnixConstants.S_IRGRP; break;
                case GROUP_WRITE :    mode |= UnixConstants.S_IWGRP; break;
                case GROUP_EXECUTE :  mode |= UnixConstants.S_IXGRP; break;
                case OTHERS_READ :    mode |= UnixConstants.S_IROTH; break;
                case OTHERS_WRITE :   mode |= UnixConstants.S_IWOTH; break;
                case OTHERS_EXECUTE : mode |= UnixConstants.S_IXOTH; break;
            }
        }
        return mode;
    }

    @SuppressWarnings("unchecked")
    static int toUnixMode(int defaultMode, FileAttribute<?>... attrs) {
        int mode = defaultMode;
        for (FileAttribute<?> attr: attrs) {
            String name = attr.name();
            if (!name.equals("posix:permissions") && !name.equals("unix:permissions")) {
                throw new UnsupportedOperationException("'" + attr.name() +
                   "' not supported as initial attribute");
            }
            mode = toUnixMode((Set<PosixFilePermission>)attr.value());
        }
        return mode;
    }
}
