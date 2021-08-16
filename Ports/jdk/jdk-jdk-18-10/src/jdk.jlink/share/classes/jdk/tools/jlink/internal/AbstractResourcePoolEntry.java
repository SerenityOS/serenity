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

import java.util.Objects;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

/**
 * A LinkResourcePoolEntry is the elementary unit of data inside an image. It is
 * generally a file. e.g.: a java class file, a resource file, a shared library,
 * ...
 * <br>
 * A LinkResourcePoolEntry is identified by a path of the form:
 * <ul>
 * <li>For jimage content: /{module name}/{package1}/.../{packageN}/{file
 * name}</li>
 * <li>For other files (shared lib, launchers, config, ...):/{module name}/
 * {@literal bin|conf|lib}/{dir1}>/.../{dirN}/{file name}</li>
 * </ul>
 */
abstract class AbstractResourcePoolEntry implements ResourcePoolEntry {
    private final String path;
    private final String module;
    private final Type type;

    /**
     * Create a new AbstractResourcePoolEntry.
     *
     * @param module The module name.
     * @param path The data path identifier.
     * @param type The data type.
     */
    AbstractResourcePoolEntry(String module, String path, Type type) {
        this.module = Objects.requireNonNull(module);
        this.path = Objects.requireNonNull(path);
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public final String moduleName() {
        return module;
    }

    @Override
    public final String path() {
        return path;
    }

    @Override
    public final Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.path);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbstractResourcePoolEntry)) {
            return false;
        }
        AbstractResourcePoolEntry f = (AbstractResourcePoolEntry) other;
        return f.path.equals(path);
    }

    @Override
    public String toString() {
        return path();
    }
}
