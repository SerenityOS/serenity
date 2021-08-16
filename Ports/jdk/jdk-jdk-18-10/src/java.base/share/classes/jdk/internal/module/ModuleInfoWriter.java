/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.commons.ModuleResolutionAttribute;
import jdk.internal.org.objectweb.asm.commons.ModuleTargetAttribute;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * Utility class to write a ModuleDescriptor as a module-info.class.
 */

public final class ModuleInfoWriter {

    private static final Map<ModuleDescriptor.Modifier, Integer>
        MODULE_MODS_TO_FLAGS = Map.of(
            ModuleDescriptor.Modifier.OPEN, ACC_OPEN,
            ModuleDescriptor.Modifier.SYNTHETIC, ACC_SYNTHETIC,
            ModuleDescriptor.Modifier.MANDATED, ACC_MANDATED
        );

    private static final Map<ModuleDescriptor.Requires.Modifier, Integer>
        REQUIRES_MODS_TO_FLAGS = Map.of(
            ModuleDescriptor.Requires.Modifier.TRANSITIVE, ACC_TRANSITIVE,
            ModuleDescriptor.Requires.Modifier.STATIC, ACC_STATIC_PHASE,
            ModuleDescriptor.Requires.Modifier.SYNTHETIC, ACC_SYNTHETIC,
            ModuleDescriptor.Requires.Modifier.MANDATED, ACC_MANDATED
        );

    private static final Map<ModuleDescriptor.Exports.Modifier, Integer>
        EXPORTS_MODS_TO_FLAGS = Map.of(
            ModuleDescriptor.Exports.Modifier.SYNTHETIC, ACC_SYNTHETIC,
            ModuleDescriptor.Exports.Modifier.MANDATED, ACC_MANDATED
        );

    private static final Map<ModuleDescriptor.Opens.Modifier, Integer>
        OPENS_MODS_TO_FLAGS = Map.of(
            ModuleDescriptor.Opens.Modifier.SYNTHETIC, ACC_SYNTHETIC,
            ModuleDescriptor.Opens.Modifier.MANDATED, ACC_MANDATED
        );

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private ModuleInfoWriter() { }

    /**
     * Writes the given module descriptor to a module-info.class file,
     * returning it in a byte array.
     */
    private static byte[] toModuleInfo(ModuleDescriptor md,
                                       ModuleResolution mres,
                                       ModuleTarget target) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V10, ACC_MODULE, "module-info", null, null, null);

        int moduleFlags = md.modifiers().stream()
                .map(MODULE_MODS_TO_FLAGS::get)
                .reduce(0, (x, y) -> (x | y));
        String vs = md.rawVersion().orElse(null);
        ModuleVisitor mv = cw.visitModule(md.name(), moduleFlags, vs);

        // requires
        for (ModuleDescriptor.Requires r : md.requires()) {
            int flags = r.modifiers().stream()
                    .map(REQUIRES_MODS_TO_FLAGS::get)
                    .reduce(0, (x, y) -> (x | y));
            vs = r.rawCompiledVersion().orElse(null);
            mv.visitRequire(r.name(), flags, vs);
        }

        // exports
        for (ModuleDescriptor.Exports e : md.exports()) {
            int flags = e.modifiers().stream()
                    .map(EXPORTS_MODS_TO_FLAGS::get)
                    .reduce(0, (x, y) -> (x | y));
            String[] targets = e.targets().toArray(EMPTY_STRING_ARRAY);
            mv.visitExport(e.source().replace('.', '/'), flags, targets);
        }

        // opens
        for (ModuleDescriptor.Opens opens : md.opens()) {
            int flags = opens.modifiers().stream()
                    .map(OPENS_MODS_TO_FLAGS::get)
                    .reduce(0, (x, y) -> (x | y));
            String[] targets = opens.targets().toArray(EMPTY_STRING_ARRAY);
            mv.visitOpen(opens.source().replace('.', '/'), flags, targets);
        }

        // uses
        md.uses().stream().map(sn -> sn.replace('.', '/')).forEach(mv::visitUse);

        // provides
        for (ModuleDescriptor.Provides p : md.provides()) {
            mv.visitProvide(p.service().replace('.', '/'),
                            p.providers()
                                .stream()
                                .map(pn -> pn.replace('.', '/'))
                                .toArray(String[]::new));
        }

        // add the ModulePackages attribute when there are packages that aren't
        // exported or open
        Stream<String> exported = md.exports().stream()
                .map(ModuleDescriptor.Exports::source);
        Stream<String> open = md.opens().stream()
                .map(ModuleDescriptor.Opens::source);
        long exportedOrOpen = Stream.concat(exported, open).distinct().count();
        if (md.packages().size() > exportedOrOpen) {
            md.packages().stream()
                    .map(pn -> pn.replace('.', '/'))
                    .forEach(mv::visitPackage);
        }

        // ModuleMainClass attribute
        md.mainClass()
            .map(mc -> mc.replace('.', '/'))
            .ifPresent(mv::visitMainClass);

        mv.visitEnd();

        // write ModuleResolution attribute if specified
        if (mres != null) {
            cw.visitAttribute(new ModuleResolutionAttribute(mres.value()));
        }

        // write ModuleTarget attribute if there is a target platform
        if (target != null && target.targetPlatform().length() > 0) {
            cw.visitAttribute(new ModuleTargetAttribute(target.targetPlatform()));
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Writes a module descriptor to the given output stream as a
     * module-info.class.
     */
    public static void write(ModuleDescriptor descriptor,
                             ModuleResolution mres,
                             ModuleTarget target,
                             OutputStream out)
        throws IOException
    {
        byte[] bytes = toModuleInfo(descriptor, mres, target);
        out.write(bytes);
    }

    /**
     * Writes a module descriptor to the given output stream as a
     * module-info.class.
     */
    public static void write(ModuleDescriptor descriptor,
                             ModuleResolution mres,
                             OutputStream out)
        throws IOException
    {
        write(descriptor, mres, null, out);
    }

    /**
     * Writes a module descriptor to the given output stream as a
     * module-info.class.
     */
    public static void write(ModuleDescriptor descriptor,
                             ModuleTarget target,
                             OutputStream out)
        throws IOException
    {
        write(descriptor, null, target, out);
    }

    /**
     * Writes a module descriptor to the given output stream as a
     * module-info.class.
     */
    public static void write(ModuleDescriptor descriptor, OutputStream out)
        throws IOException
    {
        write(descriptor, null, null, out);
    }

    /**
     * Returns a byte array containing the given module descriptor in
     * module-info.class format.
     */
    public static byte[] toBytes(ModuleDescriptor descriptor) {
        return toModuleInfo(descriptor, null, null);
    }

    /**
     * Returns a {@code ByteBuffer} containing the given module descriptor
     * in module-info.class format.
     */
    public static ByteBuffer toByteBuffer(ModuleDescriptor descriptor) {
        byte[] bytes = toModuleInfo(descriptor, null, null);
        return ByteBuffer.wrap(bytes);
    }
}
