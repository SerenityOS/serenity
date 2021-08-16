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

package jdk.internal.module;

import java.lang.module.ModuleReference;
import static jdk.internal.module.ClassFileConstants.*;

/**
 * Represents the Module Resolution flags.
 */
public final class ModuleResolution {

    final int value;

    ModuleResolution(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static ModuleResolution empty() {
        return new ModuleResolution(0);
    }

    public boolean doNotResolveByDefault() {
        return (value & DO_NOT_RESOLVE_BY_DEFAULT) != 0;
    }

    public boolean hasDeprecatedWarning() {
        return (value & WARN_DEPRECATED) != 0;
    }

    public boolean hasDeprecatedForRemovalWarning() {
        return (value & WARN_DEPRECATED_FOR_REMOVAL) != 0;
    }

    public boolean hasIncubatingWarning() {
        return (value & WARN_INCUBATING) != 0;
    }

    public ModuleResolution withDoNotResolveByDefault() {
        return new ModuleResolution(value | DO_NOT_RESOLVE_BY_DEFAULT);
    }

    public ModuleResolution withDeprecated() {
        if ((value & (WARN_DEPRECATED_FOR_REMOVAL | WARN_INCUBATING)) != 0)
            throw new InternalError("cannot add deprecated to " + value);
        return new ModuleResolution(value | WARN_DEPRECATED);
    }

    public ModuleResolution withDeprecatedForRemoval() {
        if ((value & (WARN_DEPRECATED | WARN_INCUBATING)) != 0)
            throw new InternalError("cannot add deprecated for removal to " + value);
        return new ModuleResolution(value | WARN_DEPRECATED_FOR_REMOVAL);
    }

    public ModuleResolution withIncubating() {
        if ((value & (WARN_DEPRECATED | WARN_DEPRECATED_FOR_REMOVAL)) != 0)
            throw new InternalError("cannot add incubating to " + value);
        return new ModuleResolution(value | WARN_INCUBATING);
    }

    public static boolean doNotResolveByDefault(ModuleReference mref) {
        // get the DO_NOT_RESOLVE_BY_DEFAULT flag, if any
        if (mref instanceof ModuleReferenceImpl) {
            ModuleResolution mres = ((ModuleReferenceImpl) mref).moduleResolution();
            if (mres != null)
                return mres.doNotResolveByDefault();
        }

        return false;
    }

    public static boolean hasIncubatingWarning(ModuleReference mref) {
        if (mref instanceof ModuleReferenceImpl) {
            ModuleResolution mres = ((ModuleReferenceImpl) mref).moduleResolution();
            if (mres != null)
                return mres.hasIncubatingWarning();
        }

        return false;
    }

    @Override
    public String toString() {
        return super.toString() + "[value=" + value + "]";
    }
}
