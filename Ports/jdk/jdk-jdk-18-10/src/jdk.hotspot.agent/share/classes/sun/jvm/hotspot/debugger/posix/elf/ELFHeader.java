/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileInputStream;

/**
 * This is a Java class that represents a ELF file header.
 *
 * @author Joshua W. Outwater
 */
public interface ELFHeader {
    /** No file type. */
    public static final int FT_NONE = 0;
    /** Relocatable file type. */
    public static final int FT_REL = 1;
    /** Executable file type. */
    public static final int FT_EXEC = 2;
    /** Shared object file type. */
    public static final int FT_DYN = 3;
    /** Core file file type. */
    public static final int FT_CORE = 4;
    /** Processor specific. */
    public static final int FT_LOCPROC = 0xff00;
    /** Processor specific. */
    public static final int FT_HICPROC = 0xffff;

    /** No architecture type. */
    public static final int ARCH_NONE = 0;
    /** AT&T architecture type. */
    public static final int ARCH_ATT = 1;
    /** SPARC architecture type. */
    public static final int ARCH_SPARC = 2;
    /** Intel 386 architecture type. */
    public static final int ARCH_i386 = 3;
    /** Motorolla 68000 architecture type. */
    public static final int ARCH_68k = 4;
    /** Motorolla 88000 architecture type. */
    public static final int ARCH_88k = 5;
    /** Intel 860 architecture type. */
    public static final int ARCH_i860 = 7;
    /** MIPS architecture type. */
    public static final int ARCH_MIPS = 8;

    /** Returns a file type which is defined by the file type constants. */
    public short getFileType();
    /** Returns one of the architecture constants. */
    public short getArch();
    /** Returns the size of a section header. */
    public short getSectionHeaderSize();
    /** Returns the number of section headers. */
    public short getNumberOfSectionHeaders();
    /** Returns the section header at the specified index.  The section header
     * at index 0 is the undefined section header. */
    public ELFSectionHeader getSectionHeader(int index);
    /** Returns the section header string table associated with this ELF
     * file. */
    public ELFStringTable getSectionHeaderStringTable();
    /** Returns the string table associated with this ELF file. */
    public ELFStringTable getStringTable();
    /** Returns the dynamic string table associated with this ELF file, or null
     * if one does not exist. */
    public ELFStringTable getDynamicStringTable();
    /** Returns the hash table associated with this ELF file, or null if one
     * does not exist.  NOTE: Currently the ELFHashTable does not work so this
     * method will always return null. */
    public ELFHashTable getHashTable();
    /** Returns the symbol table associated with this ELF file, or null if one
     * does not exist. */
    public ELFSectionHeader getSymbolTableSection();
    /** Returns the dynamic symbol table associated with this ELF file, or null
     * if one does not exist. */
    public ELFSectionHeader getDynamicSymbolTableSection();
    /** Returns the elf symbol with the specified name or null if one is not
     * found. */
    public ELFSymbol getELFSymbol(String name);
    /** Returns the elf symbol with the specified address or null if one is not
     * found. 'address' is relative to base of shared object for .so's. */
    public ELFSymbol getELFSymbol(long address);
    /** Returns the size of a program header. */
    //public short getProgramHeaderSize();
    /** Returns the number of program headers. */
    //public short getNumberOfProgramHeaders();
    /** Returns the program header at the specified index. */
    //public ProgramHeader getProgramHeader(int index);
}
