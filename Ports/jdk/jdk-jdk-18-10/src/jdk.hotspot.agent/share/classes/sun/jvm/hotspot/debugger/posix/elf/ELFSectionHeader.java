/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

public interface ELFSectionHeader {
    /** Undefined section header index. */
    public static final int NDX_UNDEFINED = 0;
    /** Lower bound section header index. */
    public static final int NDX_LORESERVE = 0xff00;
    /** Lower bound section header index reserved for processor specific
     * semantics. */
    public static final int NDX_LOPROC = 0xff00;
    /** Upper bound section header index reserved for processor specific
     * semantics. */
    public static final int NDX_HIPROC = 0xff1f;
    /** Absolute values for the corresponding reference.  Symbols defined
     * relative to section number NDX_ABS have absolute values and are not
     * affected by relocation. */
    public static final int NDX_ABS = 0xfff1;
    /** Symbols defined relative to this section are common symbols, such
     * as FORTRAN, COMMON or unallocated C external variables. */
    public static final int NDX_COMMON = 0xfff2;
    /** Upper bound section header index. */
    public static final int NDX_HIRESERVE = 0xffff;

    /** Section is inactive. */
    public static final int TYPE_NULL = 0;
    /** Section holds information defined by the program. */
    public static final int TYPE_PROGBITS = 1;
    /** Section holds symbol table information for link editing.  It may also
     * be used to store symbols for dynamic linking. */
    public static final int TYPE_SYMTBL = 2;
    /** Section holds string table information. */
    public static final int TYPE_STRTBL = 3;
    /** Section holds relocation entries with explicit addends. */
    public static final int TYPE_RELO_EXPLICIT = 4;
    /** Section holds symbol hash table. */
    public static final int TYPE_HASH = 5;
    /** Section holds information for dynamic linking. */
    public static final int TYPE_DYNAMIC = 6;
    /** Section holds information that marks the file. */
    public static final int TYPE_NOTE = 7;
    /** Section occupies no space but resembles TYPE_PROGBITS. */
    public static final int TYPE_NOBITS = 8;
    /** Section holds relocation entries without explicit addends. */
    public static final int TYPE_RELO = 9;
    /** Section is reserved but has unspecified semantics. */
    public static final int TYPE_SHLIB = 10;
    /** Section holds a minimum set of dynamic linking symbols. */
    public static final int TYPE_DYNSYM = 11;
    /** Lower bound section type that contains processor specific semantics. */
    public static final int TYPE_LOPROC = 0x70000000;
    /** Upper bound section type that contains processor specific semantics. */
    public static final int TYPE_HIPROC = 0x7fffffff;
    /** Lower bound of the range of indexes reserved for application
     * programs. */
    public static final int TYPE_LOUSER = 0x80000000;
    /** Upper bound of the range of indexes reserved for application
     * programs. */
    public static final int TYPE_HIUSER = 0xffffffff;

    /** Flag informing that this section contains data that should be writable
     * during process execution. */
    public static final int FLAG_WRITE = 0x1;
    /** Flag informing that section occupies memory during process
     * execution. */
    public static final int FLAG_ALLOC = 0x2;
    /** Flag informaing that section contains executable machine
     * instructions. */
    public static final int FLAG_EXEC_INSTR = 0x4;
    /** Flag informing that all the bits in the mask are reserved for processor
     * specific semantics. */
    public static final int FLAG_MASK = 0xf0000000;

    /** Section header name identifying the section as a string table. */
    public static final String STRING_TABLE_NAME = ".strtab";
    /** Section header name identifying the section as a dynamic string
     * table. */
    public static final String DYNAMIC_STRING_TABLE_NAME = ".dynstr";
    /** Returns the type of section header. */
    public int getType();
    /** Returns the number of symbols in this section or 0 if none. */
    public int getNumberOfSymbols();
    /** Returns the symbol at the specified index.  The ELF symbol at index 0
     * is the undefined symbol. */
    public ELFSymbol getELFSymbol(int index);
    /** Returns the string table for this section or null if one does not
     * exist. */
    public ELFStringTable getStringTable();
    /** Returns the hash table for this section or null if one does not
     * exist.  NOTE: currently the ELFHashTable does not work and this method
     * will always return null. */
    public ELFHashTable getHashTable();
    public int getLink();
    /** Returns the name of the section or null if the section has no name. */
    public String getName();
    /** Returns the offset in bytes to the beginning of the section. */
    public long getOffset();
}
