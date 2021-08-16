/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jlink.internal;

import jdk.tools.jlink.plugin.ResourcePoolEntry;

import java.io.InputStream;
import java.util.Objects;

/**
 * A symbolic link ResourcePoolEntry.  It will be created in the image
 * as a symbolic link to the target when the target platform supports
 * symbolic links; otherwise, it will create a file containing the
 * path to the target file.
 */
public class SymLinkResourcePoolEntry extends AbstractResourcePoolEntry {
    private final ResourcePoolEntry target;

    /**
     * Create a new SymLinkResourcePoolEntry.
     *
     * @param module The module name.
     * @param path   The path for the resource content.
     * @param type   The data type.
     * @param target Target entry in the image
     */
    public SymLinkResourcePoolEntry(String module,
                                    String path,
                                    ResourcePoolEntry.Type type,
                                    ResourcePoolEntry target) {
        super(module, path, type);
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public long contentLength() {
        return target.contentLength();
    }

    @Override
    public InputStream content() {
        return target.content();
    }

    @Override
    public ResourcePoolEntry linkedTarget() {
        return target;
    }
}
