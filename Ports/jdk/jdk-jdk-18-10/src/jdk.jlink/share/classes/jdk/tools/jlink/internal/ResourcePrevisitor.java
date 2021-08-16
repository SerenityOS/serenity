/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import jdk.tools.jlink.plugin.ResourcePool;

/**
 * Plugin wishing to pre-visit the resources must implement this interface.
 * Pre-visit can be useful when some activities are required prior to the actual
 * Resource visit.
 * The StringTable plays a special role during previsit. The passed Strings are NOT
 * added to the jimage file. The string usage is tracked in order to build an efficient
 * string storage.
 */
public interface ResourcePrevisitor {

    /**
     * Previsit the collection of resources.
     *
     * @param resources Read only resources.
     * @param strings StringTable instance. Add string to the StringTable to track string
     * usage.
     */
    public void previsit(ResourcePool resources, StringTable strings);
}
