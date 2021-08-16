/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.IOException;

import com.sun.tools.classfile.ConstantPool.CONSTANT_Module_info;

/**
 * See Jigsaw.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Module_attribute extends Attribute {
    public static final int ACC_TRANSITIVE      =   0x20;
    public static final int ACC_STATIC_PHASE    =   0x40;
    public static final int ACC_OPEN            =   0x20;
    public static final int ACC_SYNTHETIC       = 0x1000;
    public static final int ACC_MANDATED        = 0x8000;

    Module_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);

        module_name = cr.readUnsignedShort();
        module_flags = cr.readUnsignedShort();

        module_version_index = cr.readUnsignedShort();

        requires_count = cr.readUnsignedShort();
        requires = new RequiresEntry[requires_count];
        for (int i = 0; i < requires_count; i++)
            requires[i] = new RequiresEntry(cr);

        exports_count = cr.readUnsignedShort();
        exports = new ExportsEntry[exports_count];
        for (int i = 0; i < exports_count; i++)
            exports[i] = new ExportsEntry(cr);

        opens_count = cr.readUnsignedShort();
        opens = new OpensEntry[opens_count];
        for (int i = 0; i < opens_count; i++)
            opens[i] = new OpensEntry(cr);

        uses_count = cr.readUnsignedShort();
        uses_index = new int[uses_count];
        for (int i = 0; i < uses_count; i++)
            uses_index[i] = cr.readUnsignedShort();

        provides_count = cr.readUnsignedShort();
        provides = new ProvidesEntry[provides_count];
        for (int i = 0; i < provides_count; i++)
            provides[i] = new ProvidesEntry(cr);
    }

    public Module_attribute(int name_index,
            int module_name,
            int module_flags,
            int module_version_index,
            RequiresEntry[] requires,
            ExportsEntry[] exports,
            OpensEntry[] opens,
            int[] uses,
            ProvidesEntry[] provides) {
        super(name_index, 2);
        this.module_name = module_name;
        this.module_flags = module_flags;
        this.module_version_index = module_version_index;
        requires_count = requires.length;
        this.requires = requires;
        exports_count = exports.length;
        this.exports = exports;
        opens_count = opens.length;
        this.opens = opens;
        uses_count = uses.length;
        this.uses_index = uses;
        provides_count = provides.length;
        this.provides = provides;
    }

    public String getUses(int index, ConstantPool constant_pool) throws ConstantPoolException {
        int i = uses_index[index];
        return constant_pool.getClassInfo(i).getName();
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitModule(this, data);
    }

    public final int module_name;
    public final int module_flags;
    public final int module_version_index;
    public final int requires_count;
    public final RequiresEntry[] requires;
    public final int exports_count;
    public final ExportsEntry[] exports;
    public final int opens_count;
    public final OpensEntry[] opens;
    public final int uses_count;
    public final int[] uses_index;
    public final int provides_count;
    public final ProvidesEntry[] provides;

    public static class RequiresEntry {
        RequiresEntry(ClassReader cr) throws IOException {
            requires_index = cr.readUnsignedShort();
            requires_flags = cr.readUnsignedShort();
            requires_version_index = cr.readUnsignedShort();
        }

        public RequiresEntry(int index, int flags, int version_index) {
            this.requires_index = index;
            this.requires_flags = flags;
            this.requires_version_index = version_index;
        }

        public String getRequires(ConstantPool constant_pool) throws ConstantPoolException {
            CONSTANT_Module_info info = constant_pool.getModuleInfo(requires_index);
            return info.getName();
        }

        public static final int length = 4;

        public final int requires_index;
        public final int requires_flags;
        public final int requires_version_index;
    }

    public static class ExportsEntry {
        ExportsEntry(ClassReader cr) throws IOException {
            exports_index = cr.readUnsignedShort();
            exports_flags = cr.readUnsignedShort();
            exports_to_count = cr.readUnsignedShort();
            exports_to_index = new int[exports_to_count];
            for (int i = 0; i < exports_to_count; i++)
                exports_to_index[i] = cr.readUnsignedShort();
        }

        public ExportsEntry(int index, int flags, int[] to) {
            this.exports_index = index;
            this.exports_flags = flags;
            this.exports_to_count = to.length;
            this.exports_to_index = to;
        }

        public int length() {
            return 4 + 2 * exports_to_index.length;
        }

        public final int exports_index;
        public final int exports_flags;
        public final int exports_to_count;
        public final int[] exports_to_index;
    }

    public static class OpensEntry {
        OpensEntry(ClassReader cr) throws IOException {
            opens_index = cr.readUnsignedShort();
            opens_flags = cr.readUnsignedShort();
            opens_to_count = cr.readUnsignedShort();
            opens_to_index = new int[opens_to_count];
            for (int i = 0; i < opens_to_count; i++)
                opens_to_index[i] = cr.readUnsignedShort();
        }

        public OpensEntry(int index, int flags, int[] to) {
            this.opens_index = index;
            this.opens_flags = flags;
            this.opens_to_count = to.length;
            this.opens_to_index = to;
        }

        public int length() {
            return 4 + 2 * opens_to_index.length;
        }

        public final int opens_index;
        public final int opens_flags;
        public final int opens_to_count;
        public final int[] opens_to_index;
    }

    public static class ProvidesEntry {
        ProvidesEntry(ClassReader cr) throws IOException {
            provides_index = cr.readUnsignedShort();
            with_count = cr.readUnsignedShort();
            with_index = new int[with_count];
            for (int i = 0; i < with_count; i++)
                with_index[i] = cr.readUnsignedShort();
        }

        public ProvidesEntry(int provides, int[] with) {
            this.provides_index = provides;
            this.with_count = with.length;
            this.with_index = with;
        }

        public static final int length = 4;

        public final int provides_index;
        public final int with_count;
        public final int[] with_index;
    }
}
