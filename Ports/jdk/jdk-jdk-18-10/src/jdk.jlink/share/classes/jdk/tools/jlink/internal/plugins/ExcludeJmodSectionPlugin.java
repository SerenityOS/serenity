/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.internal.plugins;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry.Type;

/**
 *
 * A plugin to exclude a JMOD section such as man pages or header files
 */
public final class ExcludeJmodSectionPlugin extends AbstractPlugin {

    public static final String MAN_PAGES = "man";
    public static final String INCLUDE_HEADER_FILES = "headers";

    private final Set<Type> filters = new HashSet<>();

    public ExcludeJmodSectionPlugin() {
        super("exclude-jmod-section");
    }

    @Override
    public void configure(Map<String, String> config) {
        String arg = config.get(getName());
        if (arg.isEmpty()) {
            throw new IllegalArgumentException("Section name must be specified");
        }

        switch (arg) {
            case MAN_PAGES:
                filters.add(Type.MAN_PAGE);
                break;
            case INCLUDE_HEADER_FILES:
                filters.add(Type.HEADER_FILE);
                break;
            default:
                throw new IllegalArgumentException("Invalid section name: " + arg);
        }
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        in.transformAndCopy(entry -> {
            // filter entries whose type corresponds to the specified JMOD section
            if (filters.contains(entry.type())) {
                entry = null;
            }
            return entry;
        }, out);
        return out.build();
    }

    @Override
    public Category getType() {
        return Category.FILTER;
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

}
