/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.lang.module.ModuleDescriptor;
import java.util.Map;
import java.util.Set;

/**
 * A dummy SystemModules for use with exploded builds or testing.
 */

class ExplodedSystemModules implements SystemModules {
    @Override
    public boolean hasSplitPackages() {
        return true;  // not known
    }

    @Override
    public boolean hasIncubatorModules() {
        return true;  // not known
    }

    @Override
    public ModuleDescriptor[] moduleDescriptors() {
        throw new InternalError();
    }

    @Override
    public ModuleTarget[] moduleTargets() {
        throw new InternalError();
    }

    @Override
    public ModuleHashes[] moduleHashes() {
        throw new InternalError();
    }

    @Override
    public ModuleResolution[] moduleResolutions() {
        throw new InternalError();
    }

    @Override
    public Map<String, Set<String>> moduleReads() {
        throw new InternalError();
    }
}
