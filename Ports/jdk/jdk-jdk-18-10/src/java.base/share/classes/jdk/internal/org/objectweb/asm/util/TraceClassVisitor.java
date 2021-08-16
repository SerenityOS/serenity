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
package jdk.internal.org.objectweb.asm.util;

import java.io.PrintWriter;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.RecordComponentVisitor;
import jdk.internal.org.objectweb.asm.TypePath;

/**
 * A {@link ClassVisitor} that prints the classes it visits with a {@link Printer}. This class
 * visitor can be used in the middle of a class visitor chain to trace the class that is visited at
 * a given point in this chain. This may be useful for debugging purposes.
 *
 * <p>When used with a {@link Textifier}, the trace printed when visiting the {@code Hello} class is
 * the following:
 *
 * <pre>
 * // class version 49.0 (49) // access flags 0x21 public class Hello {
 *
 * // compiled from: Hello.java
 *
 * // access flags 0x1
 * public &lt;init&gt; ()V
 * ALOAD 0
 * INVOKESPECIAL java/lang/Object &lt;init&gt; ()V
 * RETURN
 * MAXSTACK = 1 MAXLOCALS = 1
 *
 * // access flags 0x9
 * public static main ([Ljava/lang/String;)V
 * GETSTATIC java/lang/System out Ljava/io/PrintStream;
 * LDC &quot;hello&quot;
 * INVOKEVIRTUAL java/io/PrintStream println (Ljava/lang/String;)V
 * RETURN
 * MAXSTACK = 2 MAXLOCALS = 1
 * }
 * </pre>
 *
 * <p>where {@code Hello} is defined by:
 *
 * <pre>
 * public class Hello {
 *
 *   public static void main(String[] args) {
 *     System.out.println(&quot;hello&quot;);
 *   }
 * }
 * </pre>
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public final class TraceClassVisitor extends ClassVisitor {

    /** The print writer to be used to print the class. May be {@literal null}. */
    private final PrintWriter printWriter;

    /** The printer to convert the visited class into text. */
    // DontCheck(MemberName): can't be renamed (for backward binary compatibility).
    public final Printer p;

    /**
      * Constructs a new {@link TraceClassVisitor}.
      *
      * @param printWriter the print writer to be used to print the class. May be {@literal null}.
      */
    public TraceClassVisitor(final PrintWriter printWriter) {
        this(null, printWriter);
    }

    /**
      * Constructs a new {@link TraceClassVisitor}.
      *
      * @param classVisitor the class visitor to which to delegate calls. May be {@literal null}.
      * @param printWriter the print writer to be used to print the class. May be {@literal null}.
      */
    public TraceClassVisitor(final ClassVisitor classVisitor, final PrintWriter printWriter) {
        this(classVisitor, new Textifier(), printWriter);
    }

    /**
      * Constructs a new {@link TraceClassVisitor}.
      *
      * @param classVisitor the class visitor to which to delegate calls. May be {@literal null}.
      * @param printer the printer to convert the visited class into text.
      * @param printWriter the print writer to be used to print the class. May be {@literal null}.
      */
    @SuppressWarnings("deprecation")
    public TraceClassVisitor(
            final ClassVisitor classVisitor, final Printer printer, final PrintWriter printWriter) {
        super(/* latest api = */ Opcodes.ASM9_EXPERIMENTAL, classVisitor);
        this.printWriter = printWriter;
        this.p = printer;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        p.visit(version, access, name, signature, superName, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(final String file, final String debug) {
        p.visitSource(file, debug);
        super.visitSource(file, debug);
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int flags, final String version) {
        Printer modulePrinter = p.visitModule(name, flags, version);
        return new TraceModuleVisitor(super.visitModule(name, flags, version), modulePrinter);
    }

    @Override
    public void visitNestHost(final String nestHost) {
        p.visitNestHost(nestHost);
        super.visitNestHost(nestHost);
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        p.visitOuterClass(owner, name, descriptor);
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        Printer annotationPrinter = p.visitClassAnnotation(descriptor, visible);
        return new TraceAnnotationVisitor(
                super.visitAnnotation(descriptor, visible), annotationPrinter);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        Printer annotationPrinter = p.visitClassTypeAnnotation(typeRef, typePath, descriptor, visible);
        return new TraceAnnotationVisitor(
                super.visitTypeAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        p.visitClassAttribute(attribute);
        super.visitAttribute(attribute);
    }

    @Override
    public void visitNestMember(final String nestMember) {
        p.visitNestMember(nestMember);
        super.visitNestMember(nestMember);
    }

    /**
      * <b>Experimental, use at your own risk.</b>.
      *
      * @param permittedSubclass the internal name of a permitted subclass.
      * @deprecated this API is experimental.
      */
    @Override
    @Deprecated
    public void visitPermittedSubclassExperimental(final String permittedSubclass) {
        p.visitPermittedSubclassExperimental(permittedSubclass);
        super.visitPermittedSubclassExperimental(permittedSubclass);
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
        p.visitInnerClass(name, outerName, innerName, access);
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            final String name, final String descriptor, final String signature) {
        Printer recordComponentPrinter = p.visitRecordComponent(name, descriptor, signature);
        return new TraceRecordComponentVisitor(
                super.visitRecordComponent(name, descriptor, signature), recordComponentPrinter);
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        Printer fieldPrinter = p.visitField(access, name, descriptor, signature, value);
        return new TraceFieldVisitor(
                super.visitField(access, name, descriptor, signature, value), fieldPrinter);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        Printer methodPrinter = p.visitMethod(access, name, descriptor, signature, exceptions);
        return new TraceMethodVisitor(
                super.visitMethod(access, name, descriptor, signature, exceptions), methodPrinter);
    }

    @Override
    public void visitEnd() {
        p.visitClassEnd();
        if (printWriter != null) {
            p.print(printWriter);
            printWriter.flush();
        }
        super.visitEnd();
    }
}
