/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.debugger.posix.elf;

public interface ELFFile {
    /** ELF magic number. */
    public static final byte ELF_MAGIC_NUMBER[] = { 0x7f, 'E', 'L', 'F' };

    public static final byte CLASS_INVALID = 0;
    /** 32-bit objects. */
    public static final byte CLASS_32 = 1;
    /** 64-bit objects. */
    public static final byte CLASS_64 = 2;

    /** No data encoding. */
    public static final byte DATA_INVALID = 0;
    /** LSB data encoding. */
    public static final byte DATA_LSB = 1;
    /** MSB data encoding. */
    public static final byte DATA_MSB = 2;

    /** No ELF header version. */
    public static final byte VERSION_INVALID = 0;
    /** Current ELF header version. */
    public static final byte VERSION_CURRENT = 1;

    public static final byte NDX_MAGIC_0 = 0;
    public static final byte NDX_MAGIC_1 = 1;
    public static final byte NDX_MAGIC_2 = 2;
    public static final byte NDX_MAGIC_3 = 3;
    public static final byte NDX_OBJECT_SIZE = 4;
    public static final byte NDX_ENCODING = 5;
    public static final byte NDX_VERSION = 6;

    public ELFHeader getHeader();
    public void close();

    /** Returns the 4 byte magic number for this file.  This value should
     * match the values in ELF_MAGIC_NUMBER. */
    public byte[] getMagicNumber();
    /** Returns a byte identifying the size of objects used for this ELF
     * file.  The byte will be either CLASS_INVALID, CLASS_32 or CLASS_64. */
    public byte getObjectSize();
    /** Returns a byte identifying the data encoding of the processor specific
     * data.  This byte will be either DATA_INVALID, DATA_LSB or DATA_MSB. */
    public byte getEncoding();
    /** Returns one of the version constants. This should be VERSION_CURRENT. */
    public byte getVersion();
}
