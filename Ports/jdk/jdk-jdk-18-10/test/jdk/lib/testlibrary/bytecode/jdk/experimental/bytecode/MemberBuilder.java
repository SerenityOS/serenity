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
 * Class member builder.
 *
 * @param <S> the type of symbol representation
 * @param <T> the type of type descriptors representation
 * @param <E> the type of pool entries
 * @param <M> the type of this builder
 */
public class MemberBuilder<S, T, E, M extends MemberBuilder<S, T, E, M>> extends DeclBuilder<S, T, E, M> {

    CharSequence name;
    T desc;

    /**
     * Create a member builder.
     *
     * @param name the name of the class member
     * @param type the type descriptor of the class member
     * @param poolHelper the helper to build the constant pool
     * @param typeHelper the helper to use to manipulate type descriptors
     */
    MemberBuilder(CharSequence name, T type, PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
        this.name = name;
        this.desc = type;
    }

    /**
     * Build the member.
     *
     * @param buf the {@code GrowableByteBuffer} to build the member into
     */
    protected void build(GrowableByteBuffer buf) {
        addAnnotations();
        buf.writeChar(flags);
        buf.writeChar(poolHelper.putUtf8(name));
        buf.writeChar(poolHelper.putType(desc));
        buf.writeChar(nattrs);
        buf.writeBytes(attributes);
    }

    /**
     * Build the member.
     *
     * @return a byte array representation of the member
     */
    protected byte[] build() {
        GrowableByteBuffer buf = new GrowableByteBuffer();
        addAnnotations();
        build(buf);
        return buf.bytes();
    }
}
