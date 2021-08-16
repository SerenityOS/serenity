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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.RecordComponentVisitor;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.TypePath;
import jdk.internal.org.objectweb.asm.TypeReference;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.Frame;
import jdk.internal.org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * A {@link ClassVisitor} that checks that its methods are properly used. More precisely this class
 * adapter checks each method call individually, based <i>only</i> on its arguments, but does
 * <i>not</i> check the <i>sequence</i> of method calls. For example, the invalid sequence {@code
 * visitField(ACC_PUBLIC, "i", "I", null)} {@code visitField(ACC_PUBLIC, "i", "D", null)} will
 * <i>not</i> be detected by this class adapter.
 *
 * <p><code>CheckClassAdapter</code> can be also used to verify bytecode transformations in order to
 * make sure that the transformed bytecode is sane. For example:
 *
 * <pre>
 * InputStream inputStream = ...; // get bytes for the source class
 * ClassReader classReader = new ClassReader(inputStream);
 * ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
 * ClassVisitor classVisitor = new <b>MyClassAdapter</b>(new CheckClassAdapter(classWriter, true));
 * classReader.accept(classVisitor, 0);
 *
 * StringWriter stringWriter = new StringWriter();
 * PrintWriter printWriter = new PrintWriter(stringWriter);
 * CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), false, printWriter);
 * assertTrue(stringWriter.toString().isEmpty());
 * </pre>
 *
 * <p>The above code pass the transformed bytecode through a <code>CheckClassAdapter</code>, with
 * data flow checks enabled. These checks are not exactly the same as the JVM verification, but
 * provide some basic type checking for each method instruction. If the bytecode has errors, the
 * output text shows the erroneous instruction number, and a dump of the failed method with
 * information about the type of the local variables and of the operand stack slots for each
 * instruction. For example (format is - insnNumber locals : stack):
 *
 * <pre>
 * jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException: Error at instruction 71: Expected I, but found .
 *   at jdk.internal.org.objectweb.asm.tree.analysis.Analyzer.analyze(Analyzer.java:...)
 *   at jdk.internal.org.objectweb.asm.util.CheckClassAdapter.verify(CheckClassAdapter.java:...)
 * ...
 * remove()V
 * 00000 LinkedBlockingQueue$Itr . . . . . . . .  : ICONST_0
 * 00001 LinkedBlockingQueue$Itr . . . . . . . .  : I ISTORE 2
 * 00001 LinkedBlockingQueue$Itr <b>.</b> I . . . . . .  :
 * ...
 * 00071 LinkedBlockingQueue$Itr <b>.</b> I . . . . . .  : ILOAD 1
 * 00072 <b>?</b> INVOKESPECIAL java/lang/Integer.&lt;init&gt; (I)V
 * ...
 * </pre>
 *
 * <p>The above output shows that the local variable 1, loaded by the <code>ILOAD 1</code>
 * instruction at position <code>00071</code> is not initialized, whereas the local variable 2 is
 * initialized and contains an int value.
 *
 * @author Eric Bruneton
 */
public class CheckClassAdapter extends ClassVisitor {

    /** The help message shown when command line arguments are incorrect. */
    private static final String USAGE =
            "Verifies the given class.\n"
                    + "Usage: CheckClassAdapter <fully qualified class name or class file name>";

    private static final String ERROR_AT = ": error at index ";

    /** Whether the bytecode must be checked with a BasicVerifier. */
    private boolean checkDataFlow;

    /** The class version number. */
    private int version;

    /** Whether the {@link #visit} method has been called. */
    private boolean visitCalled;

    /** Whether the {@link #visitModule} method has been called. */
    private boolean visitModuleCalled;

    /** Whether the {@link #visitSource} method has been called. */
    private boolean visitSourceCalled;

    /** Whether the {@link #visitOuterClass} method has been called. */
    private boolean visitOuterClassCalled;

    /** Whether the {@link #visitNestHost} method has been called. */
    private boolean visitNestHostCalled;

    /**
      * The common package of all the nest members. Not {@literal null} if the visitNestMember method
      * has been called.
      */
    private String nestMemberPackageName;

    /** Whether the {@link #visitEnd} method has been called. */
    private boolean visitEndCalled;

    /** The index of the instruction designated by each visited label so far. */
    private Map<Label, Integer> labelInsnIndices;

    // -----------------------------------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------------------------------

    /**
      * Constructs a new {@link CheckClassAdapter}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #CheckClassAdapter(int, ClassVisitor, boolean)} version.
      *
      * @param classVisitor the class visitor to which this adapter must delegate calls.
      */
    public CheckClassAdapter(final ClassVisitor classVisitor) {
        this(classVisitor, true);
    }

    /**
      * Constructs a new {@link CheckClassAdapter}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #CheckClassAdapter(int, ClassVisitor, boolean)} version.
      *
      * @param classVisitor the class visitor to which this adapter must delegate calls.
      * @param checkDataFlow whether to perform basic data flow checks. This option requires valid
      *     maxLocals and maxStack values.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public CheckClassAdapter(final ClassVisitor classVisitor, final boolean checkDataFlow) {
        this(/* latest api = */ Opcodes.ASM8, classVisitor, checkDataFlow);
        if (getClass() != CheckClassAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link CheckClassAdapter}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param classVisitor the class visitor to which this adapter must delegate calls.
      * @param checkDataFlow {@literal true} to perform basic data flow checks, or {@literal false} to
      *     not perform any data flow check (see {@link CheckMethodAdapter}). This option requires
      *     valid maxLocals and maxStack values.
      */
    protected CheckClassAdapter(
            final int api, final ClassVisitor classVisitor, final boolean checkDataFlow) {
        super(api, classVisitor);
        this.labelInsnIndices = new HashMap<>();
        this.checkDataFlow = checkDataFlow;
    }

    // -----------------------------------------------------------------------------------------------
    // Implementation of the ClassVisitor interface
    // -----------------------------------------------------------------------------------------------

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        if (visitCalled) {
            throw new IllegalStateException("visit must be called only once");
        }
        visitCalled = true;
        checkState();
        checkAccess(
                access,
                Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_FINAL
                        | Opcodes.ACC_SUPER
                        | Opcodes.ACC_INTERFACE
                        | Opcodes.ACC_ABSTRACT
                        | Opcodes.ACC_SYNTHETIC
                        | Opcodes.ACC_ANNOTATION
                        | Opcodes.ACC_ENUM
                        | Opcodes.ACC_DEPRECATED
                        | Opcodes.ACC_RECORD
                        | Opcodes.ACC_MODULE);
        if (name == null) {
            throw new IllegalArgumentException("Illegal class name (null)");
        }
        if (!name.endsWith("package-info") && !name.endsWith("module-info")) {
            CheckMethodAdapter.checkInternalName(version, name, "class name");
        }
        if ("java/lang/Object".equals(name)) {
            if (superName != null) {
                throw new IllegalArgumentException(
                        "The super class name of the Object class must be 'null'");
            }
        } else if (name.endsWith("module-info")) {
            if (superName != null) {
                throw new IllegalArgumentException(
                        "The super class name of a module-info class must be 'null'");
            }
        } else {
            CheckMethodAdapter.checkInternalName(version, superName, "super class name");
        }
        if (signature != null) {
            checkClassSignature(signature);
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0 && !"java/lang/Object".equals(superName)) {
            throw new IllegalArgumentException(
                    "The super class name of interfaces must be 'java/lang/Object'");
        }
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; ++i) {
                CheckMethodAdapter.checkInternalName(
                        version, interfaces[i], "interface name at index " + i);
            }
        }
        this.version = version;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(final String file, final String debug) {
        checkState();
        if (visitSourceCalled) {
            throw new IllegalStateException("visitSource can be called only once.");
        }
        visitSourceCalled = true;
        super.visitSource(file, debug);
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
        checkState();
        if (visitModuleCalled) {
            throw new IllegalStateException("visitModule can be called only once.");
        }
        visitModuleCalled = true;
        checkFullyQualifiedName(this.version, name, "module name");
        checkAccess(access, Opcodes.ACC_OPEN | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_MANDATED);
        CheckModuleAdapter checkModuleAdapter =
                new CheckModuleAdapter(
                        api, super.visitModule(name, access, version), (access & Opcodes.ACC_OPEN) != 0);
        checkModuleAdapter.classVersion = this.version;
        return checkModuleAdapter;
    }

    @Override
    public void visitNestHost(final String nestHost) {
        checkState();
        CheckMethodAdapter.checkInternalName(version, nestHost, "nestHost");
        if (visitNestHostCalled) {
            throw new IllegalStateException("visitNestHost can be called only once.");
        }
        if (nestMemberPackageName != null) {
            throw new IllegalStateException("visitNestHost and visitNestMember are mutually exclusive.");
        }
        visitNestHostCalled = true;
        super.visitNestHost(nestHost);
    }

    @Override
    public void visitNestMember(final String nestMember) {
        checkState();
        CheckMethodAdapter.checkInternalName(version, nestMember, "nestMember");
        if (visitNestHostCalled) {
            throw new IllegalStateException(
                    "visitMemberOfNest and visitNestHost are mutually exclusive.");
        }
        String packageName = packageName(nestMember);
        if (nestMemberPackageName == null) {
            nestMemberPackageName = packageName;
        } else if (!nestMemberPackageName.equals(packageName)) {
            throw new IllegalStateException(
                    "nest member " + nestMember + " should be in the package " + nestMemberPackageName);
        }
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
        checkState();
        CheckMethodAdapter.checkInternalName(version, permittedSubclass, "permittedSubclass");
        super.visitPermittedSubclassExperimental(permittedSubclass);
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        checkState();
        if (visitOuterClassCalled) {
            throw new IllegalStateException("visitOuterClass can be called only once.");
        }
        visitOuterClassCalled = true;
        if (owner == null) {
            throw new IllegalArgumentException("Illegal outer class owner");
        }
        if (descriptor != null) {
            CheckMethodAdapter.checkMethodDescriptor(version, descriptor);
        }
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
        checkState();
        CheckMethodAdapter.checkInternalName(version, name, "class name");
        if (outerName != null) {
            CheckMethodAdapter.checkInternalName(version, outerName, "outer class name");
        }
        if (innerName != null) {
            int startIndex = 0;
            while (startIndex < innerName.length() && Character.isDigit(innerName.charAt(startIndex))) {
                startIndex++;
            }
            if (startIndex == 0 || startIndex < innerName.length()) {
                CheckMethodAdapter.checkIdentifier(version, innerName, startIndex, -1, "inner class name");
            }
        }
        checkAccess(
                access,
                Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_PRIVATE
                        | Opcodes.ACC_PROTECTED
                        | Opcodes.ACC_STATIC
                        | Opcodes.ACC_FINAL
                        | Opcodes.ACC_INTERFACE
                        | Opcodes.ACC_ABSTRACT
                        | Opcodes.ACC_SYNTHETIC
                        | Opcodes.ACC_ANNOTATION
                        | Opcodes.ACC_ENUM);
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            final String name, final String descriptor, final String signature) {
        checkState();
        CheckMethodAdapter.checkUnqualifiedName(version, name, "record component name");
        CheckMethodAdapter.checkDescriptor(version, descriptor, /* canBeVoid = */ false);
        if (signature != null) {
            checkFieldSignature(signature);
        }
        return new CheckRecordComponentAdapter(
                api, super.visitRecordComponent(name, descriptor, signature));
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        checkState();
        checkAccess(
                access,
                Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_PRIVATE
                        | Opcodes.ACC_PROTECTED
                        | Opcodes.ACC_STATIC
                        | Opcodes.ACC_FINAL
                        | Opcodes.ACC_VOLATILE
                        | Opcodes.ACC_TRANSIENT
                        | Opcodes.ACC_SYNTHETIC
                        | Opcodes.ACC_ENUM
                        | Opcodes.ACC_MANDATED
                        | Opcodes.ACC_DEPRECATED);
        CheckMethodAdapter.checkUnqualifiedName(version, name, "field name");
        CheckMethodAdapter.checkDescriptor(version, descriptor, /* canBeVoid = */ false);
        if (signature != null) {
            checkFieldSignature(signature);
        }
        if (value != null) {
            CheckMethodAdapter.checkConstant(value);
        }
        return new CheckFieldAdapter(api, super.visitField(access, name, descriptor, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        checkState();
        checkAccess(
                access,
                Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_PRIVATE
                        | Opcodes.ACC_PROTECTED
                        | Opcodes.ACC_STATIC
                        | Opcodes.ACC_FINAL
                        | Opcodes.ACC_SYNCHRONIZED
                        | Opcodes.ACC_BRIDGE
                        | Opcodes.ACC_VARARGS
                        | Opcodes.ACC_NATIVE
                        | Opcodes.ACC_ABSTRACT
                        | Opcodes.ACC_STRICT
                        | Opcodes.ACC_SYNTHETIC
                        | Opcodes.ACC_MANDATED
                        | Opcodes.ACC_DEPRECATED);
        if (!"<init>".equals(name) && !"<clinit>".equals(name)) {
            CheckMethodAdapter.checkMethodIdentifier(version, name, "method name");
        }
        CheckMethodAdapter.checkMethodDescriptor(version, descriptor);
        if (signature != null) {
            checkMethodSignature(signature);
        }
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length; ++i) {
                CheckMethodAdapter.checkInternalName(
                        version, exceptions[i], "exception name at index " + i);
            }
        }
        CheckMethodAdapter checkMethodAdapter;
        if (checkDataFlow) {
            checkMethodAdapter =
                    new CheckMethodAdapter(
                            api,
                            access,
                            name,
                            descriptor,
                            super.visitMethod(access, name, descriptor, signature, exceptions),
                            labelInsnIndices);
        } else {
            checkMethodAdapter =
                    new CheckMethodAdapter(
                            api,
                            super.visitMethod(access, name, descriptor, signature, exceptions),
                            labelInsnIndices);
        }
        checkMethodAdapter.version = version;
        return checkMethodAdapter;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        checkState();
        CheckMethodAdapter.checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(super.visitAnnotation(descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        checkState();
        int sort = new TypeReference(typeRef).getSort();
        if (sort != TypeReference.CLASS_TYPE_PARAMETER
                && sort != TypeReference.CLASS_TYPE_PARAMETER_BOUND
                && sort != TypeReference.CLASS_EXTENDS) {
            throw new IllegalArgumentException(
                    "Invalid type reference sort 0x" + Integer.toHexString(sort));
        }
        checkTypeRef(typeRef);
        CheckMethodAdapter.checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(
                super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        checkState();
        if (attribute == null) {
            throw new IllegalArgumentException("Invalid attribute (must not be null)");
        }
        super.visitAttribute(attribute);
    }

    @Override
    public void visitEnd() {
        checkState();
        visitEndCalled = true;
        super.visitEnd();
    }

    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    /** Checks that the visit method has been called and that visitEnd has not been called. */
    private void checkState() {
        if (!visitCalled) {
            throw new IllegalStateException("Cannot visit member before visit has been called.");
        }
        if (visitEndCalled) {
            throw new IllegalStateException("Cannot visit member after visitEnd has been called.");
        }
    }

    /**
      * Checks that the given access flags do not contain invalid flags. This method also checks that
      * mutually incompatible flags are not set simultaneously.
      *
      * @param access the access flags to be checked.
      * @param possibleAccess the valid access flags.
      */
    static void checkAccess(final int access, final int possibleAccess) {
        if ((access & ~possibleAccess) != 0) {
            throw new IllegalArgumentException("Invalid access flags: " + access);
        }
        int publicProtectedPrivate = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE;
        if (Integer.bitCount(access & publicProtectedPrivate) > 1) {
            throw new IllegalArgumentException(
                    "public, protected and private are mutually exclusive: " + access);
        }
        if (Integer.bitCount(access & (Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT)) > 1) {
            throw new IllegalArgumentException("final and abstract are mutually exclusive: " + access);
        }
    }

    /**
      * Checks that the given name is a fully qualified name, using dots.
      *
      * @param version the class version.
      * @param name the name to be checked.
      * @param source the source of 'name' (e.g 'module' for a module name).
      */
    static void checkFullyQualifiedName(final int version, final String name, final String source) {
        try {
            int startIndex = 0;
            int dotIndex;
            while ((dotIndex = name.indexOf('.', startIndex + 1)) != -1) {
                CheckMethodAdapter.checkIdentifier(version, name, startIndex, dotIndex, null);
                startIndex = dotIndex + 1;
            }
            CheckMethodAdapter.checkIdentifier(version, name, startIndex, name.length(), null);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid " + source + " (must be a fully qualified name): " + name, e);
        }
    }

    /**
      * Checks a class signature.
      *
      * @param signature a string containing the signature that must be checked.
      */
    public static void checkClassSignature(final String signature) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // ClassSignature:
        //   [TypeParameters] SuperclassSignature SuperinterfaceSignature*
        // SuperclassSignature:
        //   ClassTypeSignature
        // SuperinterfaceSignature:
        //   ClassTypeSignature
        int pos = 0;
        if (getChar(signature, 0) == '<') {
            pos = checkTypeParameters(signature, pos);
        }
        pos = checkClassTypeSignature(signature, pos);
        while (getChar(signature, pos) == 'L') {
            pos = checkClassTypeSignature(signature, pos);
        }
        if (pos != signature.length()) {
            throw new IllegalArgumentException(signature + ERROR_AT + pos);
        }
    }

    /**
      * Checks a method signature.
      *
      * @param signature a string containing the signature that must be checked.
      */
    public static void checkMethodSignature(final String signature) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // MethodSignature:
        //   [TypeParameters] ( JavaTypeSignature* ) Result ThrowsSignature*
        // Result:
        //   JavaTypeSignature
        //   VoidDescriptor
        // ThrowsSignature:
        //   ^ ClassTypeSignature
        //   ^ TypeVariableSignature
        int pos = 0;
        if (getChar(signature, 0) == '<') {
            pos = checkTypeParameters(signature, pos);
        }
        pos = checkChar('(', signature, pos);
        while ("ZCBSIFJDL[T".indexOf(getChar(signature, pos)) != -1) {
            pos = checkJavaTypeSignature(signature, pos);
        }
        pos = checkChar(')', signature, pos);
        if (getChar(signature, pos) == 'V') {
            ++pos;
        } else {
            pos = checkJavaTypeSignature(signature, pos);
        }
        while (getChar(signature, pos) == '^') {
            ++pos;
            if (getChar(signature, pos) == 'L') {
                pos = checkClassTypeSignature(signature, pos);
            } else {
                pos = checkTypeVariableSignature(signature, pos);
            }
        }
        if (pos != signature.length()) {
            throw new IllegalArgumentException(signature + ERROR_AT + pos);
        }
    }

    /**
      * Checks a field signature.
      *
      * @param signature a string containing the signature that must be checked.
      */
    public static void checkFieldSignature(final String signature) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // FieldSignature:
        //   ReferenceTypeSignature
        int pos = checkReferenceTypeSignature(signature, 0);
        if (pos != signature.length()) {
            throw new IllegalArgumentException(signature + ERROR_AT + pos);
        }
    }

    /**
      * Checks the type parameters of a class or method signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkTypeParameters(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // TypeParameters:
        //   < TypeParameter TypeParameter* >
        int pos = startPos;
        pos = checkChar('<', signature, pos);
        pos = checkTypeParameter(signature, pos);
        while (getChar(signature, pos) != '>') {
            pos = checkTypeParameter(signature, pos);
        }
        return pos + 1;
    }

    /**
      * Checks a type parameter of a class or method signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkTypeParameter(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // TypeParameter:
        //   Identifier ClassBound InterfaceBound*
        // ClassBound:
        //   : [ReferenceTypeSignature]
        // InterfaceBound:
        //   : ReferenceTypeSignature
        int pos = startPos;
        pos = checkSignatureIdentifier(signature, pos);
        pos = checkChar(':', signature, pos);
        if ("L[T".indexOf(getChar(signature, pos)) != -1) {
            pos = checkReferenceTypeSignature(signature, pos);
        }
        while (getChar(signature, pos) == ':') {
            pos = checkReferenceTypeSignature(signature, pos + 1);
        }
        return pos;
    }

    /**
      * Checks a reference type signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param pos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkReferenceTypeSignature(final String signature, final int pos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // ReferenceTypeSignature:
        //   ClassTypeSignature
        //   TypeVariableSignature
        //   ArrayTypeSignature
        // ArrayTypeSignature:
        //   [ JavaTypeSignature
        switch (getChar(signature, pos)) {
            case 'L':
                return checkClassTypeSignature(signature, pos);
            case '[':
                return checkJavaTypeSignature(signature, pos + 1);
            default:
                return checkTypeVariableSignature(signature, pos);
        }
    }

    /**
      * Checks a class type signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkClassTypeSignature(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // ClassTypeSignature:
        //   L [PackageSpecifier] SimpleClassTypeSignature ClassTypeSignatureSuffix* ;
        // PackageSpecifier:
        //   Identifier / PackageSpecifier*
        // SimpleClassTypeSignature:
        //   Identifier [TypeArguments]
        // ClassTypeSignatureSuffix:
        //   . SimpleClassTypeSignature
        int pos = startPos;
        pos = checkChar('L', signature, pos);
        pos = checkSignatureIdentifier(signature, pos);
        while (getChar(signature, pos) == '/') {
            pos = checkSignatureIdentifier(signature, pos + 1);
        }
        if (getChar(signature, pos) == '<') {
            pos = checkTypeArguments(signature, pos);
        }
        while (getChar(signature, pos) == '.') {
            pos = checkSignatureIdentifier(signature, pos + 1);
            if (getChar(signature, pos) == '<') {
                pos = checkTypeArguments(signature, pos);
            }
        }
        return checkChar(';', signature, pos);
    }

    /**
      * Checks the type arguments in a class type signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkTypeArguments(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // TypeArguments:
        //   < TypeArgument TypeArgument* >
        int pos = startPos;
        pos = checkChar('<', signature, pos);
        pos = checkTypeArgument(signature, pos);
        while (getChar(signature, pos) != '>') {
            pos = checkTypeArgument(signature, pos);
        }
        return pos + 1;
    }

    /**
      * Checks a type argument in a class type signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkTypeArgument(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // TypeArgument:
        //   [WildcardIndicator] ReferenceTypeSignature
        //   *
        // WildcardIndicator:
        //   +
        //   -
        int pos = startPos;
        char c = getChar(signature, pos);
        if (c == '*') {
            return pos + 1;
        } else if (c == '+' || c == '-') {
            pos++;
        }
        return checkReferenceTypeSignature(signature, pos);
    }

    /**
      * Checks a type variable signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkTypeVariableSignature(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // TypeVariableSignature:
        //  T Identifier ;
        int pos = startPos;
        pos = checkChar('T', signature, pos);
        pos = checkSignatureIdentifier(signature, pos);
        return checkChar(';', signature, pos);
    }

    /**
      * Checks a Java type signature.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkJavaTypeSignature(final String signature, final int startPos) {
        // From https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1:
        // JavaTypeSignature:
        //   ReferenceTypeSignature
        //   BaseType
        // BaseType:
        //   (one of)
        //   B C D F I J S Z
        int pos = startPos;
        switch (getChar(signature, pos)) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
                return pos + 1;
            default:
                return checkReferenceTypeSignature(signature, pos);
        }
    }

    /**
      * Checks an identifier.
      *
      * @param signature a string containing the signature that must be checked.
      * @param startPos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkSignatureIdentifier(final String signature, final int startPos) {
        int pos = startPos;
        while (pos < signature.length() && ".;[/<>:".indexOf(signature.codePointAt(pos)) == -1) {
            pos = signature.offsetByCodePoints(pos, 1);
        }
        if (pos == startPos) {
            throw new IllegalArgumentException(signature + ": identifier expected at index " + startPos);
        }
        return pos;
    }

    /**
      * Checks a single character.
      *
      * @param c a character.
      * @param signature a string containing the signature that must be checked.
      * @param pos index of first character to be checked.
      * @return the index of the first character after the checked part.
      */
    private static int checkChar(final char c, final String signature, final int pos) {
        if (getChar(signature, pos) == c) {
            return pos + 1;
        }
        throw new IllegalArgumentException(signature + ": '" + c + "' expected at index " + pos);
    }

    /**
      * Returns the string character at the given index, or 0.
      *
      * @param string a string.
      * @param pos an index in 'string'.
      * @return the character at the given index, or 0 if there is no such character.
      */
    private static char getChar(final String string, final int pos) {
        return pos < string.length() ? string.charAt(pos) : (char) 0;
    }

    /**
      * Checks the reference to a type in a type annotation.
      *
      * @param typeRef a reference to an annotated type.
      */
    static void checkTypeRef(final int typeRef) {
        int mask = 0;
        switch (typeRef >>> 24) {
            case TypeReference.CLASS_TYPE_PARAMETER:
            case TypeReference.METHOD_TYPE_PARAMETER:
            case TypeReference.METHOD_FORMAL_PARAMETER:
                mask = 0xFFFF0000;
                break;
            case TypeReference.FIELD:
            case TypeReference.METHOD_RETURN:
            case TypeReference.METHOD_RECEIVER:
            case TypeReference.LOCAL_VARIABLE:
            case TypeReference.RESOURCE_VARIABLE:
            case TypeReference.INSTANCEOF:
            case TypeReference.NEW:
            case TypeReference.CONSTRUCTOR_REFERENCE:
            case TypeReference.METHOD_REFERENCE:
                mask = 0xFF000000;
                break;
            case TypeReference.CLASS_EXTENDS:
            case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
            case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
            case TypeReference.THROWS:
            case TypeReference.EXCEPTION_PARAMETER:
                mask = 0xFFFFFF00;
                break;
            case TypeReference.CAST:
            case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
            case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
            case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
                mask = 0xFF0000FF;
                break;
            default:
                throw new AssertionError();
        }
        if ((typeRef & ~mask) != 0) {
            throw new IllegalArgumentException(
                    "Invalid type reference 0x" + Integer.toHexString(typeRef));
        }
    }

    /**
      * Returns the package name of an internal name.
      *
      * @param name an internal name.
      * @return the package name or "" if there is no package.
      */
    private static String packageName(final String name) {
        int index = name.lastIndexOf('/');
        if (index == -1) {
            return "";
        }
        return name.substring(0, index);
    }

    // -----------------------------------------------------------------------------------------------
    // Static verification methods
    // -----------------------------------------------------------------------------------------------

    /**
      * Checks the given class.
      *
      * <p>Usage: CheckClassAdapter &lt;binary class name or class file name&gt;
      *
      * @param args the command line arguments.
      * @throws IOException if the class cannot be found, or if an IO exception occurs.
      */
    public static void main(final String[] args) throws IOException {
        main(args, new PrintWriter(System.err, true));
    }

    /**
      * Checks the given class.
      *
      * @param args the command line arguments.
      * @param logger where to log errors.
      * @throws IOException if the class cannot be found, or if an IO exception occurs.
      */
    static void main(final String[] args, final PrintWriter logger) throws IOException {
        if (args.length != 1) {
            logger.println(USAGE);
            return;
        }

        ClassReader classReader;
        if (args[0].endsWith(".class")) {
            InputStream inputStream =
                    new FileInputStream(args[0]); // NOPMD(AvoidFileStream): can't fix for 1.5 compatibility
            classReader = new ClassReader(inputStream);
        } else {
            classReader = new ClassReader(args[0]);
        }

        verify(classReader, false, logger);
    }

    /**
      * Checks the given class.
      *
      * @param classReader the class to be checked.
      * @param printResults whether to print the results of the bytecode verification.
      * @param printWriter where the results (or the stack trace in case of error) must be printed.
      */
    public static void verify(
            final ClassReader classReader, final boolean printResults, final PrintWriter printWriter) {
        verify(classReader, null, printResults, printWriter);
    }

    /**
      * Checks the given class.
      *
      * @param classReader the class to be checked.
      * @param loader a <code>ClassLoader</code> which will be used to load referenced classes. May be
      *     {@literal null}.
      * @param printResults whether to print the results of the bytecode verification.
      * @param printWriter where the results (or the stack trace in case of error) must be printed.
      */
    @SuppressWarnings("deprecation")
    public static void verify(
            final ClassReader classReader,
            final ClassLoader loader,
            final boolean printResults,
            final PrintWriter printWriter) {
        ClassNode classNode = new ClassNode();
        classReader.accept(
                new CheckClassAdapter(Opcodes.ASM9_EXPERIMENTAL, classNode, false) {},
                ClassReader.SKIP_DEBUG);

        Type syperType = classNode.superName == null ? null : Type.getObjectType(classNode.superName);
        List<MethodNode> methods = classNode.methods;

        List<Type> interfaces = new ArrayList<>();
        for (String interfaceName : classNode.interfaces) {
            interfaces.add(Type.getObjectType(interfaceName));
        }

        for (MethodNode method : methods) {
            SimpleVerifier verifier =
                    new SimpleVerifier(
                            Type.getObjectType(classNode.name),
                            syperType,
                            interfaces,
                            (classNode.access & Opcodes.ACC_INTERFACE) != 0);
            Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
            if (loader != null) {
                verifier.setClassLoader(loader);
            }
            try {
                analyzer.analyze(classNode.name, method);
            } catch (AnalyzerException e) {
                e.printStackTrace(printWriter);
            }
            if (printResults) {
                printAnalyzerResult(method, analyzer, printWriter);
            }
        }
        printWriter.flush();
    }

    static void printAnalyzerResult(
            final MethodNode method, final Analyzer<BasicValue> analyzer, final PrintWriter printWriter) {
        Textifier textifier = new Textifier();
        TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(textifier);

        printWriter.println(method.name + method.desc);
        for (int i = 0; i < method.instructions.size(); ++i) {
            method.instructions.get(i).accept(traceMethodVisitor);

            StringBuilder stringBuilder = new StringBuilder();
            Frame<BasicValue> frame = analyzer.getFrames()[i];
            if (frame == null) {
                stringBuilder.append('?');
            } else {
                for (int j = 0; j < frame.getLocals(); ++j) {
                    stringBuilder.append(getUnqualifiedName(frame.getLocal(j).toString())).append(' ');
                }
                stringBuilder.append(" : ");
                for (int j = 0; j < frame.getStackSize(); ++j) {
                    stringBuilder.append(getUnqualifiedName(frame.getStack(j).toString())).append(' ');
                }
            }
            while (stringBuilder.length() < method.maxStack + method.maxLocals + 1) {
                stringBuilder.append(' ');
            }
            printWriter.print(Integer.toString(i + 100000).substring(1));
            printWriter.print(
                    " " + stringBuilder + " : " + textifier.text.get(textifier.text.size() - 1));
        }
        for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
            tryCatchBlock.accept(traceMethodVisitor);
            printWriter.print(" " + textifier.text.get(textifier.text.size() - 1));
        }
        printWriter.println();
    }

    private static String getUnqualifiedName(final String name) {
        int lastSlashIndex = name.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return name;
        } else {
            int endIndex = name.length();
            if (name.charAt(endIndex - 1) == ';') {
                endIndex--;
            }
            return name.substring(lastSlashIndex + 1, endIndex);
        }
    }
}
