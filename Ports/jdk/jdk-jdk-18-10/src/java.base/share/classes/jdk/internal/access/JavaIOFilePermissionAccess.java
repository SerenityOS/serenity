/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.access;

import java.io.FilePermission;

public interface JavaIOFilePermissionAccess {

    /**
     * Returns a new FilePermission plus an alternative path.
     *
     * @param input the input
     * @return the new FilePermission plus the alt path (as npath2)
     *         or the input itself if no alt path is available.
     */
    FilePermission newPermPlusAltPath(FilePermission input);

    /**
     * Returns a new FilePermission using an alternative path.
     *
     * @param input the input
     * @return the new FilePermission using the alt path (as npath)
     *         or null if no alt path is available
     */
    FilePermission newPermUsingAltPath(FilePermission input);
}
