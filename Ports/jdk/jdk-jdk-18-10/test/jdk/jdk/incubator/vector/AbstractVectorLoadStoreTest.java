/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

public class AbstractVectorLoadStoreTest extends AbstractVectorTest {

    static final Collection<ByteOrder> BYTE_ORDER_VALUES = Set.of(
            ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN);

    static final List<IntFunction<ByteBuffer>> BYTE_BUFFER_GENERATORS = List.of(
            withToString("HB:RW:NE", (int s) -> {
                return ByteBuffer.allocate(s)
                        .order(ByteOrder.nativeOrder());
            }),
            withToString("DB:RW:NE", (int s) -> {
                return ByteBuffer.allocateDirect(s)
                        .order(ByteOrder.nativeOrder());
            }),
            withToString("MS:RW:NE", (int s) -> {
                return MemorySegment.allocateNative(s, ResourceScope.newImplicitScope())
                        .asByteBuffer()
                        .order(ByteOrder.nativeOrder());
            })
    );
}
