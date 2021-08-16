/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.util.function.Consumer;

/**
 * Base class builder. The base of higher level class builders.
 *
 * @param <S> the type of symbol representation
 * @param <T> the type of type descriptors representation
 * @param <C> the type of this builder
 */
public class ClassBuilder<S, T, C extends ClassBuilder<S, T, C>>
        extends DeclBuilder<S, T, byte[], C> {

    /**
     * The helper to use to manipulate type descriptors.
     */
    protected TypeHelper<S, T> typeHelper;

    /**
     * The symbol for the class being built.
     */
    protected S thisClass;

    /**
     * The super-interfaces of the class being built..
     */
    protected GrowableByteBuffer interfaces = new GrowableByteBuffer();

    /**
     * The fields of the class being built.
     */
    protected GrowableByteBuffer fields = new GrowableByteBuffer();

    /**
     * The methods of the class being built.
     */
    protected GrowableByteBuffer methods = new GrowableByteBuffer();

    int majorVersion;
    int minorVersion;
    int flags;
    int superclass;
    int nmethods, nfields, ninterfaces;

    /**
     * Create a class builder.
     *
     * @param poolHelper the helper to build the constant pool
     * @param typeHelper the helper to use to manipulate type descriptors
     */
    public ClassBuilder(BytePoolHelper<S, T> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
        this.typeHelper = typeHelper;
    }

    /**
     * Set the minor class file version.
     *
     * @param minorVersion the minor version number
     * @return this builder, for chained calls
     */
    public C withMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
        return thisBuilder();
    }

    /**
     * Set the major class file version.
     *
     * @param majorVersion the major version number
     * @return this builder, for chained calls
     */
    public C withMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        return thisBuilder();
    }

    /**
     * Set the class symbol
     *
     * @param thisClass the class symbol
     * @return this builder, for chained calls
     */
    public C withThisClass(S thisClass) {
        this.thisClass = thisClass;
        return thisBuilder();
    }

    /**
     * Set the class access flags
     *
     * @param flags an array of {@code Flag}
     * @return this builder, for chained calls
     */
    @Override
    public C withFlags(Flag... flags) {
        for (Flag f : flags) {
            this.flags |= f.flag;
        }
        return thisBuilder();
    }

    /**
     * Set the superclass
     *
     * @param sup the superclass symbol
     * @return this builder, for chained calls
     */
    public C withSuperclass(S sup) {
        this.superclass = poolHelper.putClass(sup);
        return thisBuilder();
    }

    /**
     * Add a super interface.
     *
     * @param sup an interface symbol
     * @return this builder, for chained calls
     */
    public C withSuperinterface(S sup) {
        this.interfaces.writeChar(poolHelper.putClass(sup));
        ninterfaces++;
        return thisBuilder();
    }

    /**
     * Add a field.
     *
     * @param name the name of the field
     * @param type the type descriptor of the field
     * @return this builder, for chained calls
     */
    public final C withField(CharSequence name, T type) {
        return withField(name, type, FB -> {
        });
    }

    /**
     * Add a field.
     *
     * @param name the name of the field
     * @param type the type descriptor of the field
     * @param fieldConfig access to the {@code FieldBuilder} to allow clients to
     * adjust flags, annotations and bytecode attributes on the field declaration
     * @return this builder, for chained calls
     */
    public C withField(CharSequence name, T type, Consumer<? super FieldBuilder<S, T, byte[]>> fieldConfig) {
        FieldBuilder<S, T, byte[]> F = new FieldBuilder<>(name, type, poolHelper, typeHelper);
        fieldConfig.accept(F);
        F.build(fields);
        nfields++;
        return thisBuilder();
    }

    /**
     * Add a method
     *
     * @param name the name of the method
     * @param type the type descriptor of the method
     * @return this builder, for chained calls
     */
    public final C withMethod(CharSequence name, T type) {
        return withMethod(name, type, MB -> {
        });
    }

    /**
     * Add a method
     *
     * @param name the name of the method
     * @param type the type descriptor of the method
     * @param methodConfig access to the {@code MethodBuilder} to allow clients to
     * adjust flags, annotations and bytecode attributes on the method declaration
     * @return this builder, for chained calls
     */
    public C withMethod(CharSequence name, T type, Consumer<? super MethodBuilder<S, T, byte[]>> methodConfig) {
        MethodBuilder<S, T, byte[]> M = new MethodBuilder<>(thisClass, name, type, poolHelper, typeHelper);
        methodConfig.accept(M);
        M.build(methods);
        nmethods++;
        return thisBuilder();
    }

    /**
     * Build the constant pool into a byte array.
     *
     * @return a representation of this constant pool as a byte array
     */
    @SuppressWarnings("unchecked")
    public byte[] build() {
        ((BytePoolHelper<S, T>)poolHelper).addAttributes(this);
        addAnnotations();
        int thisClassIdx = poolHelper.putClass(thisClass);
        byte[] poolBytes = poolHelper.entries();
        GrowableByteBuffer buf = new GrowableByteBuffer();
        buf.writeInt(0xCAFEBABE);
        buf.writeChar(minorVersion);
        buf.writeChar(majorVersion);
        buf.writeChar(poolHelper.size() + 1);
        buf.writeBytes(poolBytes);
        buf.writeChar(flags);
        buf.writeChar(thisClassIdx);
        buf.writeChar(superclass);
        buf.writeChar(ninterfaces);
        if (ninterfaces > 0) {
            buf.writeBytes(interfaces);
        }
        buf.writeChar(nfields);
        buf.writeBytes(fields);
        buf.writeChar(nmethods);
        buf.writeBytes(methods);
        buf.writeChar(nattrs);
        buf.writeBytes(attributes);
        return buf.bytes();
    }
}
