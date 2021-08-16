/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package selectionresolution;

import java.io.File;
import java.io.FileOutputStream;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;

public abstract class ClassConstruct {
    private final ClassWriter cw;
    private final String name;
    private final boolean isInterface;
    private final int index;

    /**
     * Base constructor for building a Class or Interface
     * @param name Name of Class/Interface, including package name
     * @param extending Name of extending Class if any
     * @param access Access for Class/Interface
     * @param classFileVersion Class file version
     * @param interfaces Interface implemented
     */
    public ClassConstruct(String name,
                          String extending,
                          int access,
                          int classFileVersion,
                          int index,
                          String... interfaces) {
        this.name = name;
        isInterface = (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(classFileVersion, access, name, null, extending, interfaces == null ?  new String[] { } : interfaces);
        this.index = index;
    }

    /**
     * Get full Class/Interface name including package name, as it
     * should appear in a classfile.
     *
     * @return The full Class/Interface name including package name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the class, including package as it would appear
     * in Java source.
     *
     * @return The name of the class as it would appear in Java source.
     */
    public String getDottedName() {
        return name.replace("/", ".");
    }

    public String getPackageName() {
        final int idx = name.lastIndexOf('/');
        if (idx != -1) {
            return name.substring(0, name.indexOf('/'));
        } else {
            return null;
        }
    }

    public String getClassName() {
        final int idx = name.lastIndexOf('/');
        if (idx != -1) {
            return name.substring(name.indexOf('/'));
        } else {
            return name;
        }
    }

    /**
     * Add a method, no code associated with it yet
     * @param name Name of method
     * @param descriptor Descriptor for method
     * @param access Access for the method
     * @return Method object that can be used for constructing a method body
     */
    public Method addMethod(String name,
                            String descriptor,
                            int access) {
        return addMethod(name, descriptor, access, null);
    }

    /**
     * Add a method, no code associated with it yet
     * @param name Name of method
     * @param descriptor Descriptor for method
     * @param access Access for the method
     * @param execMode The execution mode for the method.
     * @return Method object that can be used for constructing a method body
     */
    public Method addMethod(String name,
                            String descriptor,
                            int access,
                            ClassBuilder.ExecutionMode execMode) {
        return new Method(this, cw, name, descriptor, access, execMode);
    }

    /**
     * Adds a m()LTestObject; method which returns null unless the method is abstract
     * @param access Access for the method
     */
    public void addTestMethod(int access) {
        Method m = new Method(this, cw, Method.defaultMethodName, Method.defaultMethodDescriptor, access, null);
        if ((access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT) {
            m.makeDefaultMethod();
        }
    }

    /**
     * Construct the class to a byte[]
     * @return byte[] with class file
     */
    public byte[] generateBytes() {
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Write out a class to a file in the specified directory.
     *
     * @param dir Directory to which to write out the file.
     */
    public void writeClass(final File dir) throws Exception {
        final String pkgname = getPackageName();
        final File pkgdir = pkgname != null ? new File(dir, getPackageName()) : dir;
        pkgdir.mkdirs();
        final File out = new File(pkgdir, getClassName() + ".class");
        out.createNewFile();
        try (final FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(generateBytes());
        }
    }

    public boolean isInterface() {
        return isInterface;
    }

    public Integer getIndex() {
        return index;
    }
}
