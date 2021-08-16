/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

package jdk.internal.misc;

/**
 * A class used to expose details of the underlying hardware that
 * configure the operation of class Unsafe.  This class is
 * package-private as the only intended client is class Unsafe.
 * All fields in this class must be static final constants.
 *
 * @since 13
 *
 * @implNote
 *
 * The JVM injects hardware-specific values into all the static fields
 * of this class during JVM initialization. The static initialization
 * block is executed when the class is initialized then JVM injection
 * updates the fields with the correct constants. The static block
 * is required to prevent the fields from being considered constant
 * variables, so the field values will be not be compiled directly into
 * any class that uses them.
 */

final class UnsafeConstants {

    /**
     * This constructor is private because the class is not meant to
     * be instantiated.
     */
    private UnsafeConstants() {}

    /**
     * The size in bytes of a native pointer, as stored via {@link
     * #putAddress}.  This value will be either 4 or 8.  Note that the
     * sizes of other primitive types (as stored in native memory
     * blocks) is determined fully by their information content.
     *
     * @implNote
     * The actual value for this field is injected by the JVM.
     */

    static final int ADDRESS_SIZE0;

    /**
     * The size in bytes of a native memory page (whatever that is).
     * This value will always be a power of two.
     *
     * @implNote
     * The actual value for this field is injected by the JVM.
     */

    static final int PAGE_SIZE;

    /**
     * Flag whose value is true if and only if the native endianness
     * of this platform is big.
     *
     * @implNote
     * The actual value for this field is injected by the JVM.
     */

    static final boolean BIG_ENDIAN;

    /**
     * Flag whose value is true if and only if the platform can
     * perform unaligned accesses
     *
     * @implNote
     * The actual value for this field is injected by the JVM.
     */

    static final boolean UNALIGNED_ACCESS;

    /**
     * The size of an L1 data cache line which will be either a power
     * of two or zero.
     *
     * <p>A non-zero value indicates that writeback to memory is
     * enabled for the current processor. The value defines the
     * natural alignment and size of any data cache line committed to
     * memory by a single writeback operation. If data cache line
     * writeback is not enabled for the current hardware the field
     * will have value 0.
     *
     * @implNote
     * The actual value for this field is injected by the JVM.
     */

    static final int DATA_CACHE_LINE_FLUSH_SIZE;

    static {
        ADDRESS_SIZE0 = 0;
        PAGE_SIZE = 0;
        BIG_ENDIAN = false;
        UNALIGNED_ACCESS = false;
        DATA_CACHE_LINE_FLUSH_SIZE = 0;
    }
}
