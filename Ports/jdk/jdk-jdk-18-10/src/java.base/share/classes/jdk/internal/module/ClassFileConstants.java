/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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


// Constants in module-info.class files

public class ClassFileConstants {

    private ClassFileConstants() { }

    // Attribute names
    public static final String MODULE             = "Module";
    public static final String SOURCE_FILE        = "SourceFile";
    public static final String SDE                = "SourceDebugExtension";

    public static final String MODULE_PACKAGES    = "ModulePackages";
    public static final String MODULE_MAIN_CLASS  = "ModuleMainClass";
    public static final String MODULE_TARGET      = "ModuleTarget";
    public static final String MODULE_HASHES      = "ModuleHashes";
    public static final String MODULE_RESOLUTION  = "ModuleResolution";

    // access, requires, exports, and opens flags
    public static final int ACC_MODULE        = 0x8000;
    public static final int ACC_OPEN          = 0x0020;
    public static final int ACC_TRANSITIVE    = 0x0020;
    public static final int ACC_STATIC_PHASE  = 0x0040;
    public static final int ACC_SYNTHETIC     = 0x1000;
    public static final int ACC_MANDATED      = 0x8000;

    // ModuleResolution_attribute resolution flags
    public static final int DO_NOT_RESOLVE_BY_DEFAULT   = 0x0001;
    public static final int WARN_DEPRECATED             = 0x0002;
    public static final int WARN_DEPRECATED_FOR_REMOVAL = 0x0004;
    public static final int WARN_INCUBATING             = 0x0008;

}
