/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.commons;

import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * A {@link ClassVisitor} that merges &lt;clinit&gt; methods into a single one. All the existing
 * &lt;clinit&gt; methods are renamed, and a new one is created, which calls all the renamed
 * methods.
 *
 * @author Eric Bruneton
 */
public class StaticInitMerger extends ClassVisitor {

    /** The internal name of the visited class. */
    private String owner;

    /** The prefix to use to rename the existing &lt;clinit&gt; methods. */
    private final String renamedClinitMethodPrefix;

    /** The number of &lt;clinit&gt; methods visited so far. */
    private int numClinitMethods;

    /** The MethodVisitor for the merged &lt;clinit&gt; method. */
    private MethodVisitor mergedClinitVisitor;

    /**
      * Constructs a new {@link StaticInitMerger}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #StaticInitMerger(int, String, ClassVisitor)} version.
      *
      * @param prefix the prefix to use to rename the existing &lt;clinit&gt; methods.
      * @param classVisitor the class visitor to which this visitor must delegate method calls. May be
      *     null.
      */
    public StaticInitMerger(final String prefix, final ClassVisitor classVisitor) {
        this(/* latest api = */ Opcodes.ASM8, prefix, classVisitor);
    }

    /**
      * Constructs a new {@link StaticInitMerger}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param prefix the prefix to use to rename the existing &lt;clinit&gt; methods.
      * @param classVisitor the class visitor to which this visitor must delegate method calls. May be
      *     null.
      */
    protected StaticInitMerger(final int api, final String prefix, final ClassVisitor classVisitor) {
        super(api, classVisitor);
        this.renamedClinitMethodPrefix = prefix;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.owner = name;
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        MethodVisitor methodVisitor;
        if ("<clinit>".equals(name)) {
            int newAccess = Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC;
            String newName = renamedClinitMethodPrefix + numClinitMethods++;
            methodVisitor = super.visitMethod(newAccess, newName, descriptor, signature, exceptions);

            if (mergedClinitVisitor == null) {
                mergedClinitVisitor = super.visitMethod(newAccess, name, descriptor, null, null);
            }
            mergedClinitVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, newName, descriptor, false);
        } else {
            methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        if (mergedClinitVisitor != null) {
            mergedClinitVisitor.visitInsn(Opcodes.RETURN);
            mergedClinitVisitor.visitMaxs(0, 0);
        }
        super.visitEnd();
    }
}
