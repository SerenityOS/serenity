/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio.zipfs;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;

/**
 * The zip file system attribute view with POSIX support.
 */
class ZipPosixFileAttributeView extends ZipFileAttributeView implements PosixFileAttributeView {
    private final boolean isOwnerView;

    ZipPosixFileAttributeView(ZipPath path, boolean owner) {
        super(path, true);
        this.isOwnerView = owner;
    }

    @Override
    public String name() {
        return isOwnerView ? "owner" : "posix";
    }

    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        return (PosixFileAttributes)path.readAttributes();
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        return readAttributes().owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        path.setOwner(owner);
    }

    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
        path.setGroup(group);
    }

    @Override
    Object attribute(AttrID id, ZipFileAttributes zfas) {
        PosixFileAttributes pzfas = (PosixFileAttributes)zfas;
        switch (id) {
        case owner:
            return pzfas.owner();
        case group:
            return pzfas.group();
        case permissions:
            if (!isOwnerView) {
                return pzfas.permissions();
            } else {
                return super.attribute(id, zfas);
            }
        default:
            return super.attribute(id, zfas);
        }
    }
}
