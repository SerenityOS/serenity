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
package jdk.internal.org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;
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
 * A node that represents a class.
 *
 * @author Eric Bruneton
 */
public class ClassNode extends ClassVisitor {

    /**
      * The class version. The minor version is stored in the 16 most significant bits, and the major
      * version in the 16 least significant bits.
      */
    public int version;

    /**
      * The class's access flags (see {@link jdk.internal.org.objectweb.asm.Opcodes}). This field also indicates if
      * the class is deprecated {@link Opcodes#ACC_DEPRECATED} or a record {@link Opcodes#ACC_RECORD}.
      */
    public int access;

    /** The internal name of this class (see {@link jdk.internal.org.objectweb.asm.Type#getInternalName}). */
    public String name;

    /** The signature of this class. May be {@literal null}. */
    public String signature;

    /**
      * The internal of name of the super class (see {@link jdk.internal.org.objectweb.asm.Type#getInternalName}).
      * For interfaces, the super class is {@link Object}. May be {@literal null}, but only for the
      * {@link Object} class.
      */
    public String superName;

    /**
      * The internal names of the interfaces directly implemented by this class (see {@link
      * jdk.internal.org.objectweb.asm.Type#getInternalName}).
      */
    public List<String> interfaces;

    /** The name of the source file from which this class was compiled. May be {@literal null}. */
    public String sourceFile;

    /**
      * The correspondence between source and compiled elements of this class. May be {@literal null}.
      */
    public String sourceDebug;

    /** The module stored in this class. May be {@literal null}. */
    public ModuleNode module;

    /** The internal name of the enclosing class of this class. May be {@literal null}. */
    public String outerClass;

    /**
      * The name of the method that contains this class, or {@literal null} if this class is not
      * enclosed in a method.
      */
    public String outerMethod;

    /**
      * The descriptor of the method that contains this class, or {@literal null} if this class is not
      * enclosed in a method.
      */
    public String outerMethodDesc;

    /** The runtime visible annotations of this class. May be {@literal null}. */
    public List<AnnotationNode> visibleAnnotations;

    /** The runtime invisible annotations of this class. May be {@literal null}. */
    public List<AnnotationNode> invisibleAnnotations;

    /** The runtime visible type annotations of this class. May be {@literal null}. */
    public List<TypeAnnotationNode> visibleTypeAnnotations;

    /** The runtime invisible type annotations of this class. May be {@literal null}. */
    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    /** The non standard attributes of this class. May be {@literal null}. */
    public List<Attribute> attrs;

    /** The inner classes of this class. */
    public List<InnerClassNode> innerClasses;

    /** The internal name of the nest host class of this class. May be {@literal null}. */
    public String nestHostClass;

    /** The internal names of the nest members of this class. May be {@literal null}. */
    public List<String> nestMembers;

    /**
      * <b>Experimental, use at your own risk. This method will be renamed when it becomes stable, this
      * will break existing code using it</b>. The internal names of the permitted subclasses of this
      * class. May be {@literal null}.
      *
      * @deprecated this API is experimental.
      */
    @Deprecated public List<String> permittedSubclassesExperimental;

    /** The record components of this class. May be {@literal null}. */
    public List<RecordComponentNode> recordComponents;

    /** The fields of this class. */
    public List<FieldNode> fields;

    /** The methods of this class. */
    public List<MethodNode> methods;

    /**
      * Constructs a new {@link ClassNode}. <i>Subclasses must not use this constructor</i>. Instead,
      * they must use the {@link #ClassNode(int)} version.
      *
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public ClassNode() {
        this(Opcodes.ASM8);
        if (getClass() != ClassNode.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link ClassNode}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      */
    public ClassNode(final int api) {
        super(api);
        this.interfaces = new ArrayList<>();
        this.innerClasses = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }

    // -----------------------------------------------------------------------------------------------
    // Implementation of the ClassVisitor abstract class
    // -----------------------------------------------------------------------------------------------

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        this.version = version;
        this.access = access;
        this.name = name;
        this.signature = signature;
        this.superName = superName;
        this.interfaces = Util.asArrayList(interfaces);
    }

    @Override
    public void visitSource(final String file, final String debug) {
        sourceFile = file;
        sourceDebug = debug;
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
        module = new ModuleNode(name, access, version);
        return module;
    }

    @Override
    public void visitNestHost(final String nestHost) {
        this.nestHostClass = nestHost;
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        outerClass = owner;
        outerMethod = name;
        outerMethodDesc = descriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        AnnotationNode annotation = new AnnotationNode(descriptor);
        if (visible) {
            visibleAnnotations = Util.add(visibleAnnotations, annotation);
        } else {
            invisibleAnnotations = Util.add(invisibleAnnotations, annotation);
        }
        return annotation;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
        if (visible) {
            visibleTypeAnnotations = Util.add(visibleTypeAnnotations, typeAnnotation);
        } else {
            invisibleTypeAnnotations = Util.add(invisibleTypeAnnotations, typeAnnotation);
        }
        return typeAnnotation;
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        attrs = Util.add(attrs, attribute);
    }

    @Override
    public void visitNestMember(final String nestMember) {
        nestMembers = Util.add(nestMembers, nestMember);
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
        permittedSubclassesExperimental = Util.add(permittedSubclassesExperimental, permittedSubclass);
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
        InnerClassNode innerClass = new InnerClassNode(name, outerName, innerName, access);
        innerClasses.add(innerClass);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            final String name, final String descriptor, final String signature) {
        RecordComponentNode recordComponent = new RecordComponentNode(name, descriptor, signature);
        recordComponents = Util.add(recordComponents, recordComponent);
        return recordComponent;
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        FieldNode field = new FieldNode(access, name, descriptor, signature, value);
        fields.add(field);
        return field;
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        MethodNode method = new MethodNode(access, name, descriptor, signature, exceptions);
        methods.add(method);
        return method;
    }

    @Override
    public void visitEnd() {
        // Nothing to do.
    }

    // -----------------------------------------------------------------------------------------------
    // Accept method
    // -----------------------------------------------------------------------------------------------

    /**
      * Checks that this class node is compatible with the given ASM API version. This method checks
      * that this node, and all its children recursively, do not contain elements that were introduced
      * in more recent versions of the ASM API than the given version.
      *
      * @param api an ASM API version. Must be one of {@link Opcodes#ASM4}, {@link Opcodes#ASM5},
      *     {@link Opcodes#ASM6}, {@link Opcodes#ASM7}. or {@link Opcodes#ASM8}.
      */
    @SuppressWarnings("deprecation")
    public void check(final int api) {
        if (api != Opcodes.ASM9_EXPERIMENTAL && permittedSubclassesExperimental != null) {
            throw new UnsupportedClassVersionException();
        }
        if (api < Opcodes.ASM8 && ((access & Opcodes.ACC_RECORD) != 0 || recordComponents != null)) {
            throw new UnsupportedClassVersionException();
        }
        if (api < Opcodes.ASM7 && (nestHostClass != null || nestMembers != null)) {
            throw new UnsupportedClassVersionException();
        }
        if (api < Opcodes.ASM6 && module != null) {
            throw new UnsupportedClassVersionException();
        }
        if (api < Opcodes.ASM5) {
            if (visibleTypeAnnotations != null && !visibleTypeAnnotations.isEmpty()) {
                throw new UnsupportedClassVersionException();
            }
            if (invisibleTypeAnnotations != null && !invisibleTypeAnnotations.isEmpty()) {
                throw new UnsupportedClassVersionException();
            }
        }
        // Check the annotations.
        if (visibleAnnotations != null) {
            for (int i = visibleAnnotations.size() - 1; i >= 0; --i) {
                visibleAnnotations.get(i).check(api);
            }
        }
        if (invisibleAnnotations != null) {
            for (int i = invisibleAnnotations.size() - 1; i >= 0; --i) {
                invisibleAnnotations.get(i).check(api);
            }
        }
        if (visibleTypeAnnotations != null) {
            for (int i = visibleTypeAnnotations.size() - 1; i >= 0; --i) {
                visibleTypeAnnotations.get(i).check(api);
            }
        }
        if (invisibleTypeAnnotations != null) {
            for (int i = invisibleTypeAnnotations.size() - 1; i >= 0; --i) {
                invisibleTypeAnnotations.get(i).check(api);
            }
        }
        if (recordComponents != null) {
            for (int i = recordComponents.size() - 1; i >= 0; --i) {
                recordComponents.get(i).check(api);
            }
        }
        for (int i = fields.size() - 1; i >= 0; --i) {
            fields.get(i).check(api);
        }
        for (int i = methods.size() - 1; i >= 0; --i) {
            methods.get(i).check(api);
        }
    }

    /**
      * Makes the given class visitor visit this class.
      *
      * @param classVisitor a class visitor.
      */
    @SuppressWarnings("deprecation")
    public void accept(final ClassVisitor classVisitor) {
        // Visit the header.
        String[] interfacesArray = new String[this.interfaces.size()];
        this.interfaces.toArray(interfacesArray);
        classVisitor.visit(version, access, name, signature, superName, interfacesArray);
        // Visit the source.
        if (sourceFile != null || sourceDebug != null) {
            classVisitor.visitSource(sourceFile, sourceDebug);
        }
        // Visit the module.
        if (module != null) {
            module.accept(classVisitor);
        }
        // Visit the nest host class.
        if (nestHostClass != null) {
            classVisitor.visitNestHost(nestHostClass);
        }
        // Visit the outer class.
        if (outerClass != null) {
            classVisitor.visitOuterClass(outerClass, outerMethod, outerMethodDesc);
        }
        // Visit the annotations.
        if (visibleAnnotations != null) {
            for (int i = 0, n = visibleAnnotations.size(); i < n; ++i) {
                AnnotationNode annotation = visibleAnnotations.get(i);
                annotation.accept(classVisitor.visitAnnotation(annotation.desc, true));
            }
        }
        if (invisibleAnnotations != null) {
            for (int i = 0, n = invisibleAnnotations.size(); i < n; ++i) {
                AnnotationNode annotation = invisibleAnnotations.get(i);
                annotation.accept(classVisitor.visitAnnotation(annotation.desc, false));
            }
        }
        if (visibleTypeAnnotations != null) {
            for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = visibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        classVisitor.visitTypeAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
            }
        }
        if (invisibleTypeAnnotations != null) {
            for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = invisibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        classVisitor.visitTypeAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
            }
        }
        // Visit the non standard attributes.
        if (attrs != null) {
            for (int i = 0, n = attrs.size(); i < n; ++i) {
                classVisitor.visitAttribute(attrs.get(i));
            }
        }
        // Visit the nest members.
        if (nestMembers != null) {
            for (int i = 0, n = nestMembers.size(); i < n; ++i) {
                classVisitor.visitNestMember(nestMembers.get(i));
            }
        }
        // Visit the permitted subclass.
        if (permittedSubclassesExperimental != null) {
            for (int i = 0, n = permittedSubclassesExperimental.size(); i < n; ++i) {
                classVisitor.visitPermittedSubclassExperimental(permittedSubclassesExperimental.get(i));
            }
        }
        // Visit the inner classes.
        for (int i = 0, n = innerClasses.size(); i < n; ++i) {
            innerClasses.get(i).accept(classVisitor);
        }
        // Visit the record components.
        if (recordComponents != null) {
            for (int i = 0, n = recordComponents.size(); i < n; ++i) {
                recordComponents.get(i).accept(classVisitor);
            }
        }
        // Visit the fields.
        for (int i = 0, n = fields.size(); i < n; ++i) {
            fields.get(i).accept(classVisitor);
        }
        // Visit the methods.
        for (int i = 0, n = methods.size(); i < n; ++i) {
            methods.get(i).accept(classVisitor);
        }
        classVisitor.visitEnd();
    }
}
