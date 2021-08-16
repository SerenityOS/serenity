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

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * A {@link ClassVisitor} that adds a serial version unique identifier to a class if missing. A
 * typical usage of this class is:
 *
 * <pre>
 *   ClassWriter classWriter = new ClassWriter(...);
 *   ClassVisitor svuidAdder = new SerialVersionUIDAdder(classWriter);
 *   ClassVisitor classVisitor = new MyClassAdapter(svuidAdder);
 *   new ClassReader(orginalClass).accept(classVisitor, 0);
 * </pre>
 *
 * <p>The SVUID algorithm can be found at <a href=
 * "https://docs.oracle.com/javase/10/docs/specs/serialization/class.html#stream-unique-identifiers"
 * >https://docs.oracle.com/javase/10/docs/specs/serialization/class.html#stream-unique-identifiers</a>:
 *
 * <p>The serialVersionUID is computed using the signature of a stream of bytes that reflect the
 * class definition. The National Institute of Standards and Technology (NIST) Secure Hash Algorithm
 * (SHA-1) is used to compute a signature for the stream. The first two 32-bit quantities are used
 * to form a 64-bit hash. A java.lang.DataOutputStream is used to convert primitive data types to a
 * sequence of bytes. The values input to the stream are defined by the Java Virtual Machine (VM)
 * specification for classes.
 *
 * <p>The sequence of items in the stream is as follows:
 *
 * <ol>
 *   <li>The class name written using UTF encoding.
 *   <li>The class modifiers written as a 32-bit integer.
 *   <li>The name of each interface sorted by name written using UTF encoding.
 *   <li>For each field of the class sorted by field name (except private static and private
 *       transient fields):
 *       <ol>
 *         <li>The name of the field in UTF encoding.
 *         <li>The modifiers of the field written as a 32-bit integer.
 *         <li>The descriptor of the field in UTF encoding
 *       </ol>
 *   <li>If a class initializer exists, write out the following:
 *       <ol>
 *         <li>The name of the method, &lt;clinit&gt;, in UTF encoding.
 *         <li>The modifier of the method, STATIC, written as a 32-bit integer.
 *         <li>The descriptor of the method, ()V, in UTF encoding.
 *       </ol>
 *   <li>For each non-private constructor sorted by method name and signature:
 *       <ol>
 *         <li>The name of the method, &lt;init&gt;, in UTF encoding.
 *         <li>The modifiers of the method written as a 32-bit integer.
 *         <li>The descriptor of the method in UTF encoding.
 *       </ol>
 *   <li>For each non-private method sorted by method name and signature:
 *       <ol>
 *         <li>The name of the method in UTF encoding.
 *         <li>The modifiers of the method written as a 32-bit integer.
 *         <li>The descriptor of the method in UTF encoding.
 *       </ol>
 *   <li>The SHA-1 algorithm is executed on the stream of bytes produced by DataOutputStream and
 *       produces five 32-bit values sha[0..4].
 *   <li>The hash value is assembled from the first and second 32-bit values of the SHA-1 message
 *       digest. If the result of the message digest, the five 32-bit words H0 H1 H2 H3 H4, is in an
 *       array of five int values named sha, the hash value would be computed as follows: long hash
 *       = ((sha[0] &gt;&gt;&gt; 24) &amp; 0xFF) | ((sha[0] &gt;&gt;&gt; 16) &amp; 0xFF) &lt;&lt; 8
 *       | ((sha[0] &gt;&gt;&gt; 8) &amp; 0xFF) &lt;&lt; 16 | ((sha[0] &gt;&gt;&gt; 0) &amp; 0xFF)
 *       &lt;&lt; 24 | ((sha[1] &gt;&gt;&gt; 24) &amp; 0xFF) &lt;&lt; 32 | ((sha[1] &gt;&gt;&gt; 16)
 *       &amp; 0xFF) &lt;&lt; 40 | ((sha[1] &gt;&gt;&gt; 8) &amp; 0xFF) &lt;&lt; 48 | ((sha[1]
 *       &gt;&gt;&gt; 0) &amp; 0xFF) &lt;&lt; 56;
 * </ol>
 *
 * @author Rajendra Inamdar, Vishal Vishnoi
 */
// DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
public class SerialVersionUIDAdder extends ClassVisitor {

    /** The JVM name of static initializer methods. */
    private static final String CLINIT = "<clinit>";

    /** A flag that indicates if we need to compute SVUID. */
    private boolean computeSvuid;

    /** Whether the class already has a SVUID. */
    private boolean hasSvuid;

    /** The class access flags. */
    private int access;

    /** The internal name of the class. */
    private String name;

    /** The interfaces implemented by the class. */
    private String[] interfaces;

    /** The fields of the class that are needed to compute the SVUID. */
    private Collection<Item> svuidFields;

    /** Whether the class has a static initializer. */
    private boolean hasStaticInitializer;

    /** The constructors of the class that are needed to compute the SVUID. */
    private Collection<Item> svuidConstructors;

    /** The methods of the class that are needed to compute the SVUID. */
    private Collection<Item> svuidMethods;

    /**
      * Constructs a new {@link SerialVersionUIDAdder}. <i>Subclasses must not use this
      * constructor</i>. Instead, they must use the {@link #SerialVersionUIDAdder(int, ClassVisitor)}
      * version.
      *
      * @param classVisitor a {@link ClassVisitor} to which this visitor will delegate calls.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public SerialVersionUIDAdder(final ClassVisitor classVisitor) {
        this(/* latest api = */ Opcodes.ASM8, classVisitor);
        if (getClass() != SerialVersionUIDAdder.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link SerialVersionUIDAdder}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param classVisitor a {@link ClassVisitor} to which this visitor will delegate calls.
      */
    protected SerialVersionUIDAdder(final int api, final ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    // -----------------------------------------------------------------------------------------------
    // Overridden methods
    // -----------------------------------------------------------------------------------------------

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        // Get the class name, access flags, and interfaces information (step 1, 2 and 3) for SVUID
        // computation.
        computeSvuid = (access & Opcodes.ACC_ENUM) == 0;

        if (computeSvuid) {
            this.name = name;
            this.access = access;
            this.interfaces = interfaces.clone();
            this.svuidFields = new ArrayList<>();
            this.svuidConstructors = new ArrayList<>();
            this.svuidMethods = new ArrayList<>();
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        // Get constructor and method information (step 5 and 7). Also determine if there is a class
        // initializer (step 6).
        if (computeSvuid) {
            if (CLINIT.equals(name)) {
                hasStaticInitializer = true;
            }
            // Collect the non private constructors and methods. Only the ACC_PUBLIC, ACC_PRIVATE,
            // ACC_PROTECTED, ACC_STATIC, ACC_FINAL, ACC_SYNCHRONIZED, ACC_NATIVE, ACC_ABSTRACT and
            // ACC_STRICT flags are used.
            int mods =
                    access
                            & (Opcodes.ACC_PUBLIC
                                    | Opcodes.ACC_PRIVATE
                                    | Opcodes.ACC_PROTECTED
                                    | Opcodes.ACC_STATIC
                                    | Opcodes.ACC_FINAL
                                    | Opcodes.ACC_SYNCHRONIZED
                                    | Opcodes.ACC_NATIVE
                                    | Opcodes.ACC_ABSTRACT
                                    | Opcodes.ACC_STRICT);

            if ((access & Opcodes.ACC_PRIVATE) == 0) {
                if ("<init>".equals(name)) {
                    svuidConstructors.add(new Item(name, mods, descriptor));
                } else if (!CLINIT.equals(name)) {
                    svuidMethods.add(new Item(name, mods, descriptor));
                }
            }
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final Object value) {
        // Get the class field information for step 4 of the algorithm. Also determine if the class
        // already has a SVUID.
        if (computeSvuid) {
            if ("serialVersionUID".equals(name)) {
                // Since the class already has SVUID, we won't be computing it.
                computeSvuid = false;
                hasSvuid = true;
            }
            // Collect the non private fields. Only the ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED,
            // ACC_STATIC, ACC_FINAL, ACC_VOLATILE, and ACC_TRANSIENT flags are used when computing
            // serialVersionUID values.
            if ((access & Opcodes.ACC_PRIVATE) == 0
                    || (access & (Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT)) == 0) {
                int mods =
                        access
                                & (Opcodes.ACC_PUBLIC
                                        | Opcodes.ACC_PRIVATE
                                        | Opcodes.ACC_PROTECTED
                                        | Opcodes.ACC_STATIC
                                        | Opcodes.ACC_FINAL
                                        | Opcodes.ACC_VOLATILE
                                        | Opcodes.ACC_TRANSIENT);
                svuidFields.add(new Item(name, mods, desc));
            }
        }

        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitInnerClass(
            final String innerClassName,
            final String outerName,
            final String innerName,
            final int innerClassAccess) {
        // Handles a bizarre special case. Nested classes (static classes declared inside another class)
        // that are protected have their access bit set to public in their class files to deal with some
        // odd reflection situation. Our SVUID computation must do as the JVM does and ignore access
        // bits in the class file in favor of the access bits of the InnerClass attribute.
        if ((name != null) && name.equals(innerClassName)) {
            this.access = innerClassAccess;
        }
        super.visitInnerClass(innerClassName, outerName, innerName, innerClassAccess);
    }

    @Override
    public void visitEnd() {
        // Add the SVUID field to the class if it doesn't have one.
        if (computeSvuid && !hasSvuid) {
            try {
                addSVUID(computeSVUID());
            } catch (IOException e) {
                throw new IllegalStateException("Error while computing SVUID for " + name, e);
            }
        }

        super.visitEnd();
    }

    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    /**
      * Returns true if the class already has a SVUID field. The result of this method is only valid
      * when visitEnd has been called.
      *
      * @return true if the class already has a SVUID field.
      */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
    public boolean hasSVUID() {
        return hasSvuid;
    }

    /**
      * Adds a static final serialVersionUID field to the class, with the given value.
      *
      * @param svuid the serialVersionUID field value.
      */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
    protected void addSVUID(final long svuid) {
        FieldVisitor fieldVisitor =
                super.visitField(
                        Opcodes.ACC_FINAL + Opcodes.ACC_STATIC, "serialVersionUID", "J", null, svuid);
        if (fieldVisitor != null) {
            fieldVisitor.visitEnd();
        }
    }

    /**
      * Computes and returns the value of SVUID.
      *
      * @return the serial version UID.
      * @throws IOException if an I/O error occurs.
      */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
    protected long computeSVUID() throws IOException {
        long svuid = 0;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {

            // 1. The class name written using UTF encoding.
            dataOutputStream.writeUTF(name.replace('/', '.'));

            // 2. The class modifiers written as a 32-bit integer.
            int mods = access;
            if ((mods & Opcodes.ACC_INTERFACE) != 0) {
                mods =
                        svuidMethods.isEmpty() ? (mods & ~Opcodes.ACC_ABSTRACT) : (mods | Opcodes.ACC_ABSTRACT);
            }
            dataOutputStream.writeInt(
                    mods
                            & (Opcodes.ACC_PUBLIC
                                    | Opcodes.ACC_FINAL
                                    | Opcodes.ACC_INTERFACE
                                    | Opcodes.ACC_ABSTRACT));

            // 3. The name of each interface sorted by name written using UTF encoding.
            Arrays.sort(interfaces);
            for (String interfaceName : interfaces) {
                dataOutputStream.writeUTF(interfaceName.replace('/', '.'));
            }

            // 4. For each field of the class sorted by field name (except private static and private
            // transient fields):
            //   1. The name of the field in UTF encoding.
            //   2. The modifiers of the field written as a 32-bit integer.
            //   3. The descriptor of the field in UTF encoding.
            // Note that field signatures are not dot separated. Method and constructor signatures are dot
            // separated. Go figure...
            writeItems(svuidFields, dataOutputStream, false);

            // 5. If a class initializer exists, write out the following:
            //   1. The name of the method, <clinit>, in UTF encoding.
            //   2. The modifier of the method, ACC_STATIC, written as a 32-bit integer.
            //   3. The descriptor of the method, ()V, in UTF encoding.
            if (hasStaticInitializer) {
                dataOutputStream.writeUTF(CLINIT);
                dataOutputStream.writeInt(Opcodes.ACC_STATIC);
                dataOutputStream.writeUTF("()V");
            }

            // 6. For each non-private constructor sorted by method name and signature:
            //   1. The name of the method, <init>, in UTF encoding.
            //   2. The modifiers of the method written as a 32-bit integer.
            //   3. The descriptor of the method in UTF encoding.
            writeItems(svuidConstructors, dataOutputStream, true);

            // 7. For each non-private method sorted by method name and signature:
            //   1. The name of the method in UTF encoding.
            //   2. The modifiers of the method written as a 32-bit integer.
            //   3. The descriptor of the method in UTF encoding.
            writeItems(svuidMethods, dataOutputStream, true);

            dataOutputStream.flush();

            // 8. The SHA-1 algorithm is executed on the stream of bytes produced by DataOutputStream and
            // produces five 32-bit values sha[0..4].
            byte[] hashBytes = computeSHAdigest(byteArrayOutputStream.toByteArray());

            // 9. The hash value is assembled from the first and second 32-bit values of the SHA-1 message
            // digest. If the result of the message digest, the five 32-bit words H0 H1 H2 H3 H4, is in an
            // array of five int values named sha, the hash value would be computed as follows:
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                svuid = (svuid << 8) | (hashBytes[i] & 0xFF);
            }
        }

        return svuid;
    }

    /**
      * Returns the SHA-1 message digest of the given value.
      *
      * @param value the value whose SHA message digest must be computed.
      * @return the SHA-1 message digest of the given value.
      */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
    protected byte[] computeSHAdigest(final byte[] value) {
        try {
            return MessageDigest.getInstance("SHA").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
      * Sorts the items in the collection and writes it to the given output stream.
      *
      * @param itemCollection a collection of items.
      * @param dataOutputStream where the items must be written.
      * @param dotted whether package names must use dots, instead of slashes.
      * @exception IOException if an error occurs.
      */
    private static void writeItems(
            final Collection<Item> itemCollection,
            final DataOutput dataOutputStream,
            final boolean dotted)
            throws IOException {
        Item[] items = itemCollection.toArray(new Item[0]);
        Arrays.sort(items);
        for (Item item : items) {
            dataOutputStream.writeUTF(item.name);
            dataOutputStream.writeInt(item.access);
            dataOutputStream.writeUTF(dotted ? item.descriptor.replace('/', '.') : item.descriptor);
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Inner classes
    // -----------------------------------------------------------------------------------------------

    private static final class Item implements Comparable<Item> {

        final String name;
        final int access;
        final String descriptor;

        Item(final String name, final int access, final String descriptor) {
            this.name = name;
            this.access = access;
            this.descriptor = descriptor;
        }

        @Override
        public int compareTo(final Item item) {
            int result = name.compareTo(item.name);
            if (result == 0) {
                result = descriptor.compareTo(item.descriptor);
            }
            return result;
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof Item) {
                return compareTo((Item) other) == 0;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ descriptor.hashCode();
        }
    }
}
