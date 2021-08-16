/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

/**
 * Base plugin to update a static field in java.lang.VersionProps
 *
 * Fields to be updated must not be final such that values are not constant
 * replaced at compile time and initialization code is generated.
 * We assume that the initialization code only has ldcs, method calls and
 * field instructions.
 */
abstract class VersionPropsPlugin extends AbstractPlugin {

    private static final String VERSION_PROPS_CLASS
        = "/java.base/java/lang/VersionProps.class";

    private final String field;
    private String value;

    /**
     * @param field The name of the java.lang.VersionProps field to be redefined
     * @param option The option name
     */
    protected VersionPropsPlugin(String field, String option) {
        super(option);
        this.field = field;
    }

    /**
     * Shorthand constructor for when the option name can be derived from the
     * name of the field.
     *
     * @param field The name of the java.lang.VersionProps field to be redefined
     */
    protected VersionPropsPlugin(String field) {
        this(field, field.toLowerCase().replace('_', '-'));
    }

    @Override
    public Category getType() {
        return Category.TRANSFORMER;
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasRawArgument() {
        return true;
    }

    @Override
    public void configure(Map<String, String> config) {
        var v = config.get(getName());
        if (v == null)
            throw new AssertionError();
        value = v;
    }

    private boolean redefined = false;

    private byte[] redefine(byte[] classFile) {

        var cr = new ClassReader(classFile);
        var cw = new ClassWriter(0);

        cr.accept(new ClassVisitor(Opcodes.ASM7, cw) {

                @Override
                public MethodVisitor visitMethod(int access,
                                                 String name,
                                                 String desc,
                                                 String sig,
                                                 String[] xs)
                {
                    if (name.equals("<clinit>"))
                        return new MethodVisitor(Opcodes.ASM7,
                                                 super.visitMethod(access,
                                                                   name,
                                                                   desc,
                                                                   sig,
                                                                   xs))
                            {
                                private Object pendingLDC = null;

                                private void flushPendingLDC() {
                                    if (pendingLDC != null) {
                                        super.visitLdcInsn(pendingLDC);
                                        pendingLDC = null;
                                    }
                                }

                                @Override
                                public void visitLdcInsn(Object value) {
                                    flushPendingLDC();
                                    pendingLDC = value;
                                }

                                @Override
                                public void visitMethodInsn(int opcode,
                                                            String owner,
                                                            String name,
                                                            String descriptor,
                                                            boolean isInterface) {
                                    flushPendingLDC();
                                    super.visitMethodInsn(opcode, owner, name,
                                                          descriptor, isInterface);
                                }

                                @Override
                                public void visitFieldInsn(int opcode,
                                                           String owner,
                                                           String name,
                                                           String desc)
                                {
                                    if (opcode == Opcodes.PUTSTATIC
                                        && name.equals(field))
                                    {
                                        // assert that there is a pending ldc
                                        // for the old value
                                        if (pendingLDC == null) {
                                            throw new AssertionError("No load " +
                                                "instruction found for field " + field +
                                                " in static initializer of " +
                                                VERSION_PROPS_CLASS);
                                        }
                                        // forget about it
                                        pendingLDC = null;
                                        // and add an ldc for the new value
                                        super.visitLdcInsn(value);
                                        redefined = true;
                                    } else {
                                        flushPendingLDC();
                                    }
                                    super.visitFieldInsn(opcode, owner,
                                                         name, desc);
                                }

                        };
                    else
                        return super.visitMethod(access, name, desc, sig, xs);
                }

            }, 0);

        return cw.toByteArray();

    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        in.transformAndCopy(res -> {
                if (res.type().equals(ResourcePoolEntry.Type.CLASS_OR_RESOURCE)) {
                    if (res.path().equals(VERSION_PROPS_CLASS)) {
                        return res.copyWithContent(redefine(res.contentBytes()));
                    }
                }
                return res;
            }, out);
        if (!redefined)
            throw new AssertionError(field);
        return out.build();
    }

}
