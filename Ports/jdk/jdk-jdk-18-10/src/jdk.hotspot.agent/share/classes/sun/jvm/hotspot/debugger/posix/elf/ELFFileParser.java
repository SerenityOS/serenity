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

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.utilities.memo.*;
import sun.jvm.hotspot.debugger.DataSource;
import sun.jvm.hotspot.debugger.RandomAccessFileDataSource;

public class ELFFileParser {
    private static ELFFileParser elfParser;
    private static final String US_ASCII = "US-ASCII";

    public static ELFFileParser getParser() {
        if (elfParser == null) {
            elfParser = new ELFFileParser();
        }
        return elfParser;
    }

    /**
     * Parses the data in filename and returns the ELFFile representation.
     */
    public ELFFile parse(String filename) throws ELFException {
        try {
            RandomAccessFile file = new RandomAccessFile(filename, "r");
            return parse(new RandomAccessFileDataSource(file));
        } catch (FileNotFoundException e) {
            throw new ELFException(e);
        }
    }

    /**
     * Parses the data source and returns the ELFFile representation.
     */
    public ELFFile parse(DataSource source) throws ELFException {
        return new ELFFileImpl(source);
    }

    /**
     * Implementation of the ELFFile interface.
     */
    class ELFFileImpl implements ELFFile {
        private DataSource file;
        private ELFHeader header;
        private byte ident[] = new byte[16];

        ELFFileImpl(DataSource file) throws ELFException {
            this.file = file;
            int bytesRead = readBytes(ident);
            if (bytesRead != ident.length) {
                throw new ELFException("Error reading elf header (read " +
                            bytesRead + "bytes, expected to " +
                            "read " + ident.length + "bytes).");
            }

            // Check the magic number before we continue reading the file.
            if (!Arrays.equals(getMagicNumber(), ELF_MAGIC_NUMBER)) {
                    throw new ELFException("Bad magic number for file.");
            }

            header = new ELFHeaderImpl();
        }

        public ELFHeader getHeader()     { return header; }

        public byte[] getMagicNumber() {
            byte magicNumber[] = new byte[4];
            magicNumber[0] = ident[NDX_MAGIC_0];
            magicNumber[1] = ident[NDX_MAGIC_1];
            magicNumber[2] = ident[NDX_MAGIC_2];
            magicNumber[3] = ident[NDX_MAGIC_3];
            return magicNumber;
        }

        public byte getObjectSize()         { return ident[NDX_OBJECT_SIZE]; }
        public byte getEncoding()           { return ident[NDX_ENCODING]; }
        public byte getVersion()            { return ident[NDX_VERSION]; }


        /**
         * Implementation of the ELFHeader interface.
         */
        class ELFHeaderImpl implements ELFHeader {
            /** Marks the file as an object file and provide machine-independent
             * data so the contents may be decoded and interpreted. */
            private byte ident[] = new byte[16];        // unsigned char
            /** Identifies the object file type. */
            private short file_type;                    // Elf32_Half
            /** The required architecture. */
            private short arch;                         // Elf32_Half
            /** Version */
            private int version;                        // Elf32_Word
            /** Virtual address to which the system first transfers control.
             * If there is no entry point for the file the value is 0. */
            private long entry_point;                    // Elf32_Addr
            /** Program header table offset in bytes.  If there is no program
             * header table the value is 0. */
            private long ph_offset;                      // Elf32_Off
            /** Section header table offset in bytes.  If there is no section
             * header table the value is 0. */
            private long sh_offset;                      // Elf32_Off
            /** Processor specific flags. */
            private int flags;                          // Elf32_Word
            /** ELF header size in bytes. */
            private short eh_size;                      // Elf32_Half
            /** Size of one entry in the file's program header table in bytes.
             * All entries are the same size. */
            private short ph_entry_size;                // Elf32_Half
            /** Number of entries in the program header table, 0 if no
             * entries. */
            private short num_ph;                       // Elf32_Half
            /** Section header entry size in bytes. */
            private short sh_entry_size;                // Elf32_Half
            /** Number of entries in the section header table, 0 if no
             * entries. */
            private short num_sh;                       // Elf32_Half
            /** Index into the section header table associated with the section
             * name string table.  SH_UNDEF if there is no section name string
             * table. */
            private short sh_string_ndx;                // Elf32_Half

            /** MemoizedObject array of section headers associated with this
             * ELF file. */
            private MemoizedObject[] sectionHeaders;
            /** MemoizedObject array of program headers associated with this
             * ELF file. */
            private MemoizedObject[] programHeaders;

            /** Used to cache symbol table lookup. */
            private ELFSectionHeader symbolTableSection;
            /** Used to cache dynamic symbol table lookup. */
            private ELFSectionHeader dynamicSymbolTableSection;
            /** Used to cache hash table lookup. */
            private ELFHashTable hashTable;

            /**
             * Reads the ELF header and sets up the section and program headers
             * in memoized arrays.
             */
            ELFHeaderImpl() throws ELFException {
                file_type = readShort();
                arch = readShort();
                version = readInt();
                entry_point = readWord();
                ph_offset = readWord();
                sh_offset = readWord();
                flags = readInt();
                eh_size = readShort();
                ph_entry_size = readShort();
                num_ph = readShort();
                sh_entry_size = readShort();
                num_sh = readShort();
                sh_string_ndx = readShort();

                // Set up the section headers
                sectionHeaders = new MemoizedObject[num_sh];
                for (int i = 0; i < num_sh; i++) {
                    final long sectionHeaderOffset =
                            (long)(sh_offset + (i * sh_entry_size));
                    sectionHeaders[i] = new MemoizedObject() {
                        public Object computeValue() {
                            return new ELFSectionHeaderImpl(sectionHeaderOffset);
                        }
                    };
                }

//                // Set up the program headers
//                programHeaders = new MemoizedObject[num_sh];
//                for (int i = 0; i < num_sh; i++) {
//                    final long programHeaderOffset =
//                            (long)(ph_offset + (i * ph_entry_size));
//                    programHeaders[i] = new MemoizedObject() {
//                        public Object computeValue() {
//                            return new ProgramHeaderImpl(programHeaderOffset);
//                        }
//                    };
//                }
            }

            public short getFileType()                 { return file_type; }
            public short getArch()                     { return arch; }
            public short getSectionHeaderSize()        { return sh_entry_size; }
            public short getNumberOfSectionHeaders()   { return num_sh; }

//            public short getProgramHeaderSize()      { return ph_entry_size; }
//            public short getNumberOfProgramHeaders() { return num_ph; }


            /**
             * Returns the section header at the specified index.  The section
             * header at index 0 is defined as being a undefined section. */
            public ELFSectionHeader getSectionHeader(int index) {
                return (ELFSectionHeader)sectionHeaders[index].getValue();
            }

            public ELFStringTable getSectionHeaderStringTable() {
                return getSectionHeader(sh_string_ndx).getStringTable();
            }

            public ELFStringTable getStringTable() {
                return findStringTableWithName(ELFSectionHeader.STRING_TABLE_NAME);
            }

            public ELFStringTable getDynamicStringTable() {
                return findStringTableWithName(
                        ELFSectionHeader.DYNAMIC_STRING_TABLE_NAME);
            }

            private ELFStringTable findStringTableWithName(String tableName) {
                // Loop through the section header and look for a section
                // header with the name "tableName".  We can ignore entry 0
                // since it is defined as being undefined.
                ELFSectionHeader sh = null;
                for (int i = 1; i < getNumberOfSectionHeaders(); i++) {
                    sh = getSectionHeader(i);
                    if (tableName.equals(sh.getName())) {
                        return sh.getStringTable();
                    }
                }
                return null;
            }

            /**
             * The ELFHashTable does not currently work.  This method will
             * always return null. */
            public ELFHashTable getHashTable() {
//                if (hashTable != null) {
//                    return hashTable;
//                }
//
//                ELFHashTable ht = null;
//                for (int i = 1; i < getNumberOfSectionHeaders(); i++) {
//                    if ((ht = getSectionHeader(i).getHashTable()) != null) {
//                        hashTable = ht;
//                        return hashTable;
//                    }
//                }
                return null;
            }

            public ELFSectionHeader getSymbolTableSection() {
                if (symbolTableSection != null) {
                    return symbolTableSection;
                }

                symbolTableSection =
                        getSymbolTableSection(ELFSectionHeader.TYPE_SYMTBL);
                return symbolTableSection;
            }

            public ELFSectionHeader getDynamicSymbolTableSection() {
                if (dynamicSymbolTableSection != null) {
                    return dynamicSymbolTableSection;
                }

                dynamicSymbolTableSection =
                        getSymbolTableSection(ELFSectionHeader.TYPE_DYNSYM);
                return dynamicSymbolTableSection;
            }

            private ELFSectionHeader getSymbolTableSection(int type) {
                ELFSectionHeader sh = null;
                for (int i = 1; i < getNumberOfSectionHeaders(); i++) {
                    sh = getSectionHeader(i);
                    if (sh.getType() == type) {
                        dynamicSymbolTableSection = sh;
                        return sh;
                    }
                }
                return null;
            }

            public ELFSymbol getELFSymbol(String symbolName) {
                if (symbolName == null) {
                    return null;
                }

                // Check dynamic symbol table for symbol name.
                ELFSymbol symbol = null;
                int numSymbols = 0;
                ELFSectionHeader sh = getDynamicSymbolTableSection();
                if (sh != null) {
                    numSymbols = sh.getNumberOfSymbols();
                    for (int i = 0; i < Math.ceil(numSymbols / 2); i++) {
                        if (symbolName.equals(
                                (symbol = sh.getELFSymbol(i)).getName())) {
                            return symbol;
                        } else if (symbolName.equals(
                                (symbol = sh.getELFSymbol(
                                        numSymbols - 1 - i)).getName())) {
                            return symbol;
                        }
                    }
                }

                // Check symbol table for symbol name.
                sh = getSymbolTableSection();
                if (sh != null) {
                    numSymbols = sh.getNumberOfSymbols();
                    for (int i = 0; i < Math.ceil(numSymbols / 2); i++) {
                        if (symbolName.equals(
                                (symbol = sh.getELFSymbol(i)).getName())) {
                            return symbol;
                        } else if (symbolName.equals(
                                (symbol = sh.getELFSymbol(
                                        numSymbols - 1 - i)).getName())) {
                            return symbol;
                        }
                    }
                }
                return null;
            }

            public ELFSymbol getELFSymbol(long address) {
                // Check dynamic symbol table for address.
                ELFSymbol symbol = null;
                int numSymbols = 0;
                long value = 0L;

                ELFSectionHeader sh = getDynamicSymbolTableSection();
                if (sh != null) {
                    numSymbols = sh.getNumberOfSymbols();
                    for (int i = 0; i < numSymbols; i++) {
                        symbol = sh.getELFSymbol(i);
                        value = symbol.getValue();
                        if (address >= value && address < value + symbol.getSize()) {
                           return symbol;
                        }
                    }
                }

                // Check symbol table for symbol name.
                sh = getSymbolTableSection();
                if (sh != null) {
                    numSymbols = sh.getNumberOfSymbols();
                    for (int i = 0; i < numSymbols; i++) {
                        symbol = sh.getELFSymbol(i);
                        value = symbol.getValue();
                        if (address >= value && address < value + symbol.getSize()) {
                           return symbol;
                        }
                    }
                }
                return null;
            }

//            public ProgramHeader getProgramHeader(int index) {
//                return (ProgramHeader)programHeaders[index].getValue();
//            }
        }


        /**
         * Implementation of the ELFSectionHeader interface.
         */
        class ELFSectionHeaderImpl implements ELFSectionHeader {
            /** Index into the section header string table which gives the
             * name of the section. */
            private int name_ndx;                     // Elf32_Word
            /** Section content and semantics. */
            private int type;                         // Elf32_Word
            /** Flags. */
            private long flags;                        // Elf32_Word
            /** If the section will be in the memory image of a process this
             * will be the address at which the first byte of section will be
             * loaded.  Otherwise, this value is 0. */
            private long address;                      // Elf32_Addr
            /** Offset from beginning of file to first byte of the section. */
            private long section_offset;               // Elf32_Off
            /** Size in bytes of the section.  TYPE_NOBITS is a special case. */
            private long size;                         // Elf32_Word
            /** Section header table index link. */
            private int link;                         // Elf32_Word
            /** Extra information determined by the section type. */
            private int info;                         // Elf32_Word
            /** Address alignment constraints for the section. */
            private long address_alignment;            // Elf32_Word
            /** Size of a fixed-size entry, 0 if none. */
            private long entry_size;                   // Elf32_Word

            /** Memoized symbol table.  */
            private MemoizedObject[] symbols;
            /** Memoized string table. */
            private MemoizedObject stringTable;
            /** Memoized hash table. */
            private MemoizedObject hashTable;

            /**
             * Reads the section header information located at offset.
             */
            ELFSectionHeaderImpl(long offset) throws ELFException {
                seek(offset);
                name_ndx = readInt();
                type = readInt();
                flags = readWord();
                address = readWord();
                section_offset = readWord();
                size = readWord();
                link = readInt();
                info = readInt();
                address_alignment = readWord();
                entry_size = readWord();

                switch (type) {
                    case ELFSectionHeader.TYPE_NULL:
                        break;
                    case ELFSectionHeader.TYPE_PROGBITS:
                        break;
                    case ELFSectionHeader.TYPE_SYMTBL:
                    case ELFSectionHeader.TYPE_DYNSYM:
                        // Setup the symbol table.
                        int num_entries = (int)(size / entry_size);
                        symbols = new MemoizedObject[num_entries];
                        for (int i = 0; i < num_entries; i++) {
                            final long symbolOffset = section_offset +
                                    (i * entry_size);
                            symbols[i] = new MemoizedObject() {
                                public Object computeValue() {
                                    return new ELFSymbolImpl(symbolOffset,type);
                                }
                            };
                        }
                        break;
                    case ELFSectionHeader.TYPE_STRTBL:
                        // Setup the string table.
                        final long strTableOffset = section_offset;
                        final long strTableSize = size;
                        assert32bitLong(strTableSize);  // must fit in 32-bits
                        stringTable = new MemoizedObject() {
                            public Object computeValue() {
                                return new ELFStringTableImpl(strTableOffset,
                                                              (int)strTableSize);
                            }
                        };
                        break;
                    case ELFSectionHeader.TYPE_RELO_EXPLICIT:
                        break;
                    case ELFSectionHeader.TYPE_HASH:
                        final long hashTableOffset = section_offset;
                        final long hashTableSize = size;
                        assert32bitLong(hashTableSize);  // must fit in 32-bits
                        hashTable = new MemoizedObject() {
                            public Object computeValue() {
                                return new ELFHashTableImpl(hashTableOffset,
                                                            (int)hashTableSize);
                            }
                        };
                        break;
                    case ELFSectionHeader.TYPE_DYNAMIC:
                        break;
                    case ELFSectionHeader.TYPE_NOTE:
                        break;
                    case ELFSectionHeader.TYPE_NOBITS:
                        break;
                    case ELFSectionHeader.TYPE_RELO:
                        break;
                    case ELFSectionHeader.TYPE_SHLIB:
                        break;
                    default:
                        break;
                }
            }

            public int getType() {
                return type;
            }

            public int getNumberOfSymbols() {
                if (symbols != null) {
                    return symbols.length;
                }
                return 0;
            }

            /**
             * Returns the ELFSymbol at the specified index.  Index 0 is
             * reserved for the undefined ELF symbol. */
            public ELFSymbol getELFSymbol(int index) {
                return (ELFSymbol)symbols[index].getValue();
            }

            public ELFStringTable getStringTable() {
                if (stringTable != null) {
                    return (ELFStringTable)stringTable.getValue();
                }
                return null;
            }

            /**
             * The ELFHashTable does not currently work.  This method will
             * always return null. */
            public ELFHashTable getHashTable() {
                if (hashTable != null) {
                    return (ELFHashTable)hashTable.getValue();
                }
                return null;
            }

            public String getName() {
                if (name_ndx == 0) {
                    return null;
                }

                ELFStringTable tbl = getHeader().getSectionHeaderStringTable();
                return tbl.get(name_ndx);
            }

            public int getLink() {
                return link;
            }

            public long getOffset() {
                return section_offset;
            }
        }


//        class ProgramHeaderImpl implements ProgramHeader {
//            /** Defines the kind of segment this element describes. */
//            private int type;                           // Elf32_Word
//            /** Offset from the beginning of the file. */
//            private int offset;                         // Elf32_Off
//            /** Virtual address at which the first byte of the segment
//             * resides in memory. */
//            private int virtual_address;                // Elf32_Addr
//            /** Reserved for the physical address of the segment on systems
//             * where physical addressinf is relevant. */
//            private int physical_address;               // Elf32_addr
//            /** File image size of segment in bytes, may be 0. */
//            private int file_size;                      // Elf32_Word
//            /** Memory image size of segment in bytes, may be 0. */
//            private int mem_size;                       // Elf32_Word
//            /** Flags relevant to this segment. Values for flags are defined
//             * in ELFSectionHeader. */
//            private int flags;                          // Elf32_Word
//            private int alignment;                      // Elf32_Word
//
//            private MemoizedObject[] symbols;
//
//            ProgramHeaderImpl(long offset) throws ELFException {
//                seek(offset);
//                type = readInt();
//                this.offset = readInt();
//                virtual_address = readInt();
//                physical_address = readInt();
//                file_size = readInt();
//                mem_size = readInt();
//                flags = readInt();
//                alignment = readInt();
//
//                switch (type) {
//                    case ELFSectionHeader.TYPE_NULL:
//                        break;
//                    case ELFSectionHeader.TYPE_PROGBITS:
//                        break;
//                    case ELFSectionHeader.TYPE_SYMTBL:
//                    case ELFSectionHeader.TYPE_DYNSYM:
//                        break;
//                    case ELFSectionHeader.TYPE_STRTBL:
//                        // Setup the string table.
//                        final int strTableOffset = section_offset;
//                        final int strTableSize = size;
//                        stringTable = new MemoizedObject() {
//                            public Object computeValue() {
//                                return new ELFStringTableImpl(strTableOffset,
//                                                           strTableSize);
//                            }
//                        };
//                        new ELFStringTableImpl(offset, file_size);
//                        break;
//                    case ELFSectionHeader.TYPE_RELO_EXPLICIT:
//                        break;
//                    case ELFSectionHeader.TYPE_HASH:
//                        break;
//                    case ELFSectionHeader.TYPE_DYNAMIC:
//                        break;
//                    case ELFSectionHeader.TYPE_NOTE:
//                        break;
//                    case ELFSectionHeader.TYPE_NOBITS:
//                        break;
//                    case ELFSectionHeader.TYPE_RELO:
//                        break;
//                    case ELFSectionHeader.TYPE_SHLIB:
//                        break;
//                    default:
//                        break;
//                }
//            }
//
//            public int getType() {
//                return type;
//            }
//        }


        /**
         * Implementation of the ELFSymbol interface.
         */
        class ELFSymbolImpl implements ELFSymbol {
            /** Index into the symbol string table that holds the character
             * representation of the symbols.  0 means the symbol has no
             * character name. */
            private int name_ndx;                       // Elf32_Word
            /** Value of the associated symbol.  This may be an address or
             * an absolute value. */
            private long value;                          // Elf32_Addr
            /** Size of the symbol.  0 if the symbol has no size or the size
             * is unknown. */
            private long size;                           // Elf32_Word
            /** Specifies the symbol type and beinding attributes. */
            private byte info;                          // unsigned char
            /** Currently holds the value of 0 and has no meaning. */
            private byte other;                         // unsigned char
            /** Index to the associated section header.   This value will need
             * to be read as an unsigned short if we compare it to
             * ELFSectionHeader.NDX_LORESERVE and ELFSectionHeader.NDX_HIRESERVE. */
            private short section_header_ndx;             // Elf32_Half

            private int section_type;

            /** Offset from the beginning of the file to this symbol. */
            private long offset;

            ELFSymbolImpl(long offset, int section_type) throws ELFException {
                seek(offset);
                this.offset = offset;
                switch (getObjectSize()) {
                    case CLASS_32: {
                        name_ndx = readInt();
                        value = readInt();
                        size = readInt();
                        info = readByte();
                        other = readByte();
                        section_header_ndx = readShort();
                        break;
                    }
                    case CLASS_64: {
                        name_ndx = readInt();
                        info = readByte();
                        other = readByte();
                        section_header_ndx = readShort();
                        value = readWord();
                        size = readWord();
                        break;
                    }
                    default:
                        throw new ELFException("Invalid Object Size.");
                }

                this.section_type = section_type;

                switch (getType()) {
                    case TYPE_NOOBJECT:
                        break;
                    case TYPE_OBJECT:
                        break;
                    case TYPE_FUNCTION:
                        break;
                    case TYPE_SECTION:
                        break;
                    case TYPE_FILE:
                        break;
                    case TYPE_LOPROC:
                        break;
                    case TYPE_HIPROC:
                        break;
                    default:
                        break;
                }
            }

            public int getBinding()             { return info >> 4; }
            public int getType()                { return info & 0x0F; }
            public long getOffset()             { return offset; }

            public String getName() {
                // Check to make sure this symbol has a name.
                if (name_ndx == 0) {
                    return null;
                }

                // Retrieve the name of the symbol from the correct string
                // table.
                String symbol_name = null;
                if (section_type == ELFSectionHeader.TYPE_SYMTBL) {
                    symbol_name = getHeader().getStringTable().get(name_ndx);
                } else if (section_type == ELFSectionHeader.TYPE_DYNSYM) {
                    symbol_name =
                            getHeader().getDynamicStringTable().get(name_ndx);
                }
                return symbol_name;
            }

            public long getValue() {
                return value;
            }

            public long getSize() {
                return size;
            }
        }

        /**
         * Implementation of the ELFStringTable interface.
         */
        class ELFStringTableImpl implements ELFStringTable {
            /** The string table data. */
            private byte data[];
            private int numStrings;

            /**
             * Reads all the strings from [offset, length].
             */
            ELFStringTableImpl(long offset, int length) throws ELFException {
                seek(offset);
                data = new byte[length];
                int bytesRead = readBytes(data);
                if (bytesRead != length) {
                    throw new ELFException("Error reading string table (read " +
                                           bytesRead + "bytes, expected to " +
                                           "read " + data.length + "bytes).");
                }

                // Count the strings.
                numStrings = 0;
                for (int ptr = 0; ptr < data.length; ptr++) {
                    if (data[ptr] == '\0') {
                        numStrings++;
                    }
                }
            }

            public String get(int index) {
                int startPtr = index;
                int endPtr = index;
                while (data[endPtr] != '\0') {
                    endPtr++;
                }
                return new String(data, startPtr, endPtr - startPtr);
            }

            public int getNumStrings() {
                return numStrings;
            }
        }


        /** Implementation of the ELFHashTable. */
        class ELFHashTableImpl implements ELFHashTable {
            private int num_buckets;
            private int num_chains;

            // These could probably be memoized.
            private int buckets[];
            private int chains[];

            ELFHashTableImpl(long offset, int length) throws ELFException {
                seek(offset);
                num_buckets = readInt();
                num_chains = readInt();

                buckets = new int[num_buckets];
                chains = new int[num_chains];
                // Read the bucket data.
                for (int i = 0; i < num_buckets; i++) {
                    buckets[i] = readInt();
                }

                // Read the chain data.
                for (int i = 0; i < num_chains; i++) {
                    chains[i] = readInt();
                }

                // Make sure that the amount of bytes we were supposed to read
                // was what we actually read.
                int actual = num_buckets * 4 + num_chains * 4 + 8;
                if (length != actual) {
                    throw new ELFException("Error reading string table (read " +
                                           actual + "bytes, expected to " +
                                           "read " + length + "bytes).");
                }
            }

            /**
             * This method doesn't work every time and is unreliable.  Use
             * ELFSection.getELFSymbol(String) to retrieve symbols by name.
             * NOTE: since this method is currently broken it will always
             * return null. */
            public ELFSymbol getSymbol(String symbolName) {
//                if (symbolName == null) {
//                    return null;
//                }
//
//                long hash = 0;
//                long g = 0;
//
//                for (int i = 0; i < symbolName.length(); i++) {
//                    hash = (hash << 4) + symbolName.charAt(i);
//                    if ((g = hash & 0xf0000000) != 0) {
//                        hash ^= g >>> 24;
//                    }
//                    hash &= ~g;
//                }
//
//                ELFSymbol symbol = null;
//                ELFSectionHeader dyn_sh =
//                    getHeader().getDynamicSymbolTableSection();
//                int index = (int)hash % num_buckets;
//                while(index != 0) {
//                    symbol = dyn_sh.getELFSymbol(index);
//                    if (symbolName.equals(symbol.getName())) {
//                        break;
//                    }
//                    symbol = null;
//                    index = chains[index];
//                }
//                return symbol;
                return null;
            }
        }


        public void close() throws ELFException {
            try {
                file.close();
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        void seek(long offset) throws ELFException {
            try {
                file.seek(offset);
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        long getFilePointer() throws ELFException {
            try {
                return file.getFilePointer();
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        byte readByte() throws ELFException {
            try {
                return file.readByte();
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        int readBytes(byte[] b) throws ELFException {
            try {
                return file.read(b);
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        short readShort() throws ELFException {
            try {
                short val;
                switch (ident[NDX_ENCODING]) {
                    case DATA_LSB:
                        val = byteSwap(file.readShort());
                        break;
                    case DATA_MSB:
                        val = file.readShort();
                        break;
                    default:
                        throw new ELFException("Invalid encoding.");
                }
                return val;
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        int readInt() throws ELFException {
            try {
                int val;
                switch (ident[NDX_ENCODING]) {
                    case DATA_LSB:
                        val = byteSwap(file.readInt());
                        break;
                    case DATA_MSB:
                        val = file.readInt();
                        break;
                    default:
                        throw new ELFException("Invalid encoding.");
                }
                return val;
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        long readLong() throws ELFException {
            try {
                long val;
                switch (ident[NDX_ENCODING]) {
                    case DATA_LSB:
                        val = byteSwap(file.readLong());
                        break;
                    case DATA_MSB:
                        val = file.readLong();
                        break;
                    default:
                        throw new ELFException("Invalid encoding.");
                }
                return val;
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        long readWord() throws ELFException {
            switch (getObjectSize()) {
                case CLASS_32:
                    return readInt();
                case CLASS_64:
                    return readLong();
                default:
                    throw new ELFException("Invalid Object Size.");
            }
        }

        /** Signed byte utility functions used for converting from big-endian
         * (MSB) to little-endian (LSB). */
        short byteSwap(short arg) {
          return (short) ((arg << 8) | ((arg >>> 8) & 0xFF));
        }

        int byteSwap(int arg) {
            return (((int) byteSwap((short) arg)) << 16) |
                   (((int) (byteSwap((short) (arg >>> 16)))) & 0xFFFF);
        }

        long byteSwap(long arg) {
            return ((((long) byteSwap((int) arg)) << 32) |
                   (((long) byteSwap((int) (arg >>> 32))) & 0xFFFFFFFF));
        }


        /* Unsigned byte utility functions.  Since java does not have unsigned
         * data types we must convert values manually and we must return
         * unsigned values in a larger data type.  Therefore we can only have
         * unsigned values for byte, short, and int. */
        short readUnsignedByte() throws ELFException {
            try {
                return unsignedByte(file.readByte());
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        int readUnsignedShort() throws ELFException {
            try {
                int val;
                switch (ident[NDX_ENCODING]) {
                    case DATA_LSB:
                        val = unsignedByteSwap(file.readShort());
                        break;
                    case DATA_MSB:
                        val = unsignedByte(file.readShort());
                        break;
                    default:
                        throw new ELFException("Invalid encoding.");
                }
                return val;
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        long readUnsignedInt() throws ELFException {
            try {
                long val;
                switch (ident[NDX_ENCODING]) {
                    case DATA_LSB:
                        val = unsignedByteSwap(file.readInt());
                        break;
                    case DATA_MSB:
                        val = unsignedByte(file.readInt());
                        break;
                    default:
                        throw new ELFException("Invalid encoding.");
                }
                return val;
            } catch (IOException e) {
                throw new ELFException(e);
            }
        }

        /** Returns the unsigned value of the byte. */
        short unsignedByte(byte arg) {
            return (short)(arg & 0x00FF);
        }

        /** Returns a big-endian unsigned representation of the short. */
        int unsignedByte(short arg) {
            int val;
            if (arg >= 0) {
                val = arg;
            } else {
                val = (int)(((int)unsignedByte((byte)(arg >>> 8)) << 8) |
                            ((byte)arg));
            }
            return val;
        }

        /** Returns a big-endian unsigned representation of the int. */
        long unsignedByte(int arg) {
            long val;
            if (arg >= 0) {
                val = arg;
            } else {
                val = (long)(((long)unsignedByte((short)(arg >>> 16)) << 16) |
                             ((short)arg));
            }
            return val;
        }

        /** Unsigned byte utility functions used for converting from big-endian
         * (MSB) to little-endian (LSB). */
        int unsignedByteSwap(short arg) {
            return (int)(((int)unsignedByte((byte)arg)) << 8) |
                         ((int)unsignedByte((byte)(arg >>> 8)));
        }

        long unsignedByteSwap(int arg) {
            return (long)(((long)unsignedByteSwap((short)arg)) << 16) |
                          ((long)unsignedByteSwap((short)(arg >>> 16)));
        }

        void assert32bitLong(long x) {
            if (x != (long)(int)x) {
                throw new ELFException("64-bit value does not fit in 32-bits: " + x);
            }
        }
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: java ELFFileParser <elf file>");
            System.exit(0);
        }

        // Parse the file.
        ELFFile elfFile = ELFFileParser.getParser().parse(args[0]);

        ELFHeader elfHeader = elfFile.getHeader();
        System.out.println("ELF File: " + args[0]);

        System.out.println("ELF object size: " +
                ((elfFile.getObjectSize() == 0) ? "Invalid Object Size" :
                (elfFile.getObjectSize() == 1) ? "32-bit" : "64-bit"));
        System.out.println("ELF data encoding: " +
                ((elfFile.getEncoding() == 0) ? "Invalid Data Encoding" :
                (elfFile.getEncoding() == 1) ? "LSB" : "MSB"));

        int h = elfHeader.getNumberOfSectionHeaders();
        System.out.println("--> Start: reading " + h + " section headers.");
        for (int i = 0; i < elfHeader.getNumberOfSectionHeaders(); i++) {
            ELFSectionHeader sh = elfHeader.getSectionHeader(i);
            String str = sh.getName();
            System.out.println("----> Start: Section (" + i + ") " + str);

            int num = 0;
            if ((num = sh.getNumberOfSymbols()) != 0) {
                System.out.println("------> Start: reading " + num + " symbols.");
                for (int j = 0; j < num ; j++) {
                    ELFSymbol sym = sh.getELFSymbol(j);
                    //String name = sym.getName();
                    //if (name != null) {
                    //    System.out.println(name);
                    //}
                }
                System.out.println("<------ End: reading " + num + " symbols.");
            }
            ELFStringTable st;
            if (sh.getType() == ELFSectionHeader.TYPE_STRTBL) {
                System.out.println("------> Start: reading string table.");
                st = sh.getStringTable();
                System.out.println("<------ End: reading string table.");
            }
            if (sh.getType() == ELFSectionHeader.TYPE_HASH) {
                System.out.println("------> Start: reading hash table.");
                sh.getHashTable();
                System.out.println("<------ End: reading hash table.");
            }
            System.out.println("<---- End: Section (" + i + ") " + str);
        }
        System.out.println("<-- End: reading " + h + " section headers.");
/*
        h = elfHeader.getNumberOfProgramHeaders();
        System.out.println("--> Start: reading " + h + " program headers.");
        for (int i = 0; i < elfHeader.getNumberOfProgramHeaders(); i++) {
            elfHeader.getProgramHeader(i);
        }
        System.out.println("<-- End: reading " + h + " program headers.");
*/
        elfFile.close();
    }
}
