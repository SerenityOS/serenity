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

/**
 * Base builder for attribute containing class file entities.
 *
 * @param <S> the type of the symbol representation
 * @param <T> the type of type descriptors representation
 * @param <E> the type of pool entries
 * @param <D> the type of this builder
 */
 public class AttributeBuilder<S, T, E, D extends AttributeBuilder<S, T, E, D>>
        extends AbstractBuilder<S, T, E, D> {

     /**
      * The number of attributes.
      */
    protected int nattrs;

    /**
     * The attributes represented as bytes.
     */
    protected GrowableByteBuffer attributes = new GrowableByteBuffer();

    /**
     * Create an attribute builder.
     *
     * @param poolHelper the helper to build the constant pool
     * @param typeHelper the helper to use to manipulate type descriptors
     */
    public AttributeBuilder(PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
    }

    /**
     * Add a class file Attribute.  Defined as:
     * <pre>
     * {@code   attribute_info {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *     u1 info[attribute_length];
     *   }}
     * </pre>
     *
     * @param name the attribute name
     * @param bytes the bytes of the attribute info
     * @return this builder, for chained calls
     */
    public D withAttribute(CharSequence name, byte[] bytes) {
        attributes.writeChar(poolHelper.putUtf8(name));
        attributes.writeInt(bytes.length);
        attributes.writeBytes(bytes);
        nattrs++;
        return thisBuilder();
    }

    /**
     * Add a class file Attribute, using a writer.  Defined as:
     * <pre>
     * {@code   attribute_info {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *     u1 info[attribute_length];
     *   }}
     * </pre>
     *
     * @param <Z> the type of the object representing the attribute
     * @param name the attribute name
     * @param attr the representation of the attribute
     * @param attrWriter the writer which transform the attribute representation into bytes
     * @return this builder, for chained calls
     */
    public <Z> D withAttribute(CharSequence name, Z attr, AttributeWriter<S, T, E, Z> attrWriter) {
        attributes.writeChar(poolHelper.putUtf8(name));
        int offset = attributes.offset;
        attributes.writeInt(0);
        attrWriter.write(attr, poolHelper, attributes);
        int len = attributes.offset - offset - 4;
        attributes.withOffset(offset, buf -> buf.writeInt(len));
        nattrs++;
        return thisBuilder();
    }

     /**
      * Writer for transforming attribute representations to bytes
      *
      * @param <S> the type of symbol representation
      * @param <T> the type of type descriptors representation
      * @param <E> the type of pool entries
      * @param <A> the type of the object representing the attribute
      */
    public interface AttributeWriter<S, T, E, A> {

        /**
         * Write an attribute representation into a byte buffer.
         *
         * @param attr the representation of the attribute
         * @param poolHelper the constant pool helper
         * @param buf the buffer to collect the bytes
         */
        void write(A attr, PoolHelper<S, T, E> poolHelper, GrowableByteBuffer buf);
    }
}
