/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.win32.coff;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import sun.jvm.hotspot.utilities.memo.*;
import sun.jvm.hotspot.utilities.Assert;
import sun.jvm.hotspot.debugger.DataSource;
import sun.jvm.hotspot.debugger.MappedByteBufferDataSource;

/** Top-level factory which parses COFF files, including object files,
    Portable Executables and DLLs. Returns {@link
    sun.jvm.hotspot.debugger.win32.coff.COFFFile} objects. This class is a
    singleton. */

public class COFFFileParser {
  private static COFFFileParser soleInstance;

  // Constants from the file format documentation
  private static final int COFF_HEADER_SIZE = 20;
  private static final int SECTION_HEADER_SIZE = 40;
  private static final int SYMBOL_SIZE = 18;
  private static final int RELOCATION_SIZE = 10;
  private static final int LINE_NUMBER_SIZE = 6;

  private static final String US_ASCII = "US-ASCII";

  private COFFFileParser() {}

  /** This class is a singleton; returns the sole instance. */
  public static COFFFileParser getParser() {
    if (soleInstance == null) {
      soleInstance = new COFFFileParser();
    }
    return soleInstance;
  }

  public COFFFile parse(String filename) throws COFFException {
    try {
      File file = new File(filename);
      FileInputStream stream = new FileInputStream(file);
      MappedByteBuffer buf = stream.getChannel().map(FileChannel.MapMode.READ_ONLY,
                                                     0,
                                                     file.length());

      // This is pretty confusing. The file format is little-endian
      // and so is the CPU. In order for the multi-byte accessors to
      // work properly we must NOT change the endianness of the
      // MappedByteBuffer. Need to think about this some more and file
      // a bug if there is one. (FIXME)
      //   buf.order(ByteOrder.nativeOrder());
      return parse(new MappedByteBufferDataSource(buf));
    } catch (FileNotFoundException e) {
      throw new COFFException(e);
    } catch (IOException e) {
      throw new COFFException(e);
    }
  }

  public COFFFile parse(DataSource source) throws COFFException {
    return new COFFFileImpl(source);
  }

  class COFFFileImpl implements COFFFile {
    private DataSource file;
    private long       filePos;
    private boolean isImage;
    private long    imageHeaderOffset;
    private MemoizedObject header = new MemoizedObject() {
        public Object computeValue() {
          return new COFFHeaderImpl();
        }
      };

    COFFFileImpl(DataSource file) throws COFFException {
      this.file = file;
      initialize();
    }

    public boolean isImage() {
      return isImage;
    }

    public COFFHeader getHeader() {
      return (COFFHeaderImpl) header.getValue();
    }

    class COFFHeaderImpl implements COFFHeader {
      private short machine;
      private short numberOfSections;
      private int   timeDateStamp;
      private int   pointerToSymbolTable;
      private int   numberOfSymbols;
      private short sizeOfOptionalHeader;
      private short characteristics;
      private MemoizedObject[] sectionHeaders;
      private MemoizedObject[] symbols;

      // Init stringTable at decl time since other fields init'ed in the
      // constructor need the String Table.
      private MemoizedObject stringTable = new MemoizedObject() {
          public Object computeValue() {
            // the String Table follows the Symbol Table
            int ptr = getPointerToSymbolTable();
            if (ptr == 0) {
              // no Symbol Table so no String Table
              return new StringTable(0);
            } else {
              return new StringTable(ptr + SYMBOL_SIZE * getNumberOfSymbols());
            }
          }
        };

      COFFHeaderImpl() {
        seek(imageHeaderOffset);
        machine = readShort();
        numberOfSections = readShort();
        timeDateStamp = readInt();
        pointerToSymbolTable = readInt();
        numberOfSymbols = readInt();
        // String Table can be accessed at this point because
        // pointerToSymbolTable and numberOfSymbols fields are set.
        sizeOfOptionalHeader = readShort();
        characteristics = readShort();

        // Set up section headers
        sectionHeaders = new MemoizedObject[numberOfSections];
        for (int i = 0; i < numberOfSections; i++) {
          final int secHdrOffset = (int)
            (imageHeaderOffset + COFF_HEADER_SIZE + sizeOfOptionalHeader + i * SECTION_HEADER_SIZE);
          sectionHeaders[i] = new MemoizedObject() {
              public Object computeValue() {
                return new SectionHeaderImpl(secHdrOffset);
              }
            };
        }

        // Set up symbols
        symbols = new MemoizedObject[numberOfSymbols];
        for (int i = 0; i < numberOfSymbols; i++) {
          final int symbolOffset = pointerToSymbolTable + i * SYMBOL_SIZE;
          symbols[i] = new MemoizedObject() {
              public Object computeValue() {
                return new COFFSymbolImpl(symbolOffset);
              }
            };
        }
      }

      public short          getMachineType()          { return machine; }
      public short          getNumberOfSections()     { return numberOfSections; }
      public int            getTimeDateStamp()        { return timeDateStamp; }
      public int            getPointerToSymbolTable() { return pointerToSymbolTable; }
      public int            getNumberOfSymbols()      { return numberOfSymbols; }
      public short          getSizeOfOptionalHeader() { return sizeOfOptionalHeader; }
      public OptionalHeader getOptionalHeader() throws COFFException {
        if (getSizeOfOptionalHeader() == 0) {
          return null;
        }
        return new OptionalHeaderImpl((int) (imageHeaderOffset + COFF_HEADER_SIZE));
      }
      public short          getCharacteristics()      { return characteristics; }
      public boolean hasCharacteristic(short characteristic) {
        return ((characteristics & characteristic) != 0);
      }
      public SectionHeader getSectionHeader(int index) {
        // NOTE zero-basing of index
        return (SectionHeader) sectionHeaders[index - 1].getValue();
      }
      public COFFSymbol    getCOFFSymbol(int index)    {
        return (COFFSymbol) symbols[index].getValue();
      }
      public int getNumberOfStrings() {
        return getStringTable().getNum();
      }
      public String getString(int i) {
        return getStringTable().get(i);
      }

      StringTable          getStringTable() { return (StringTable) stringTable.getValue(); }

      // NOTE: can destroy current seek() position!
      int rvaToFileOffset(int rva) {
        if (rva == 0) return 0;
        // Search for section containing RVA
        for (int i = 1; i <= getNumberOfSections(); i++) {
          SectionHeader sec = getSectionHeader(i);
          int va = sec.getVirtualAddress();
          int sz = sec.getSize();
          if ((va <= rva) && (rva < (va + sz))) {
            return sec.getPointerToRawData() + (rva - va);
          }
        }
        throw new COFFException("Unable to find RVA 0x" +
                                Integer.toHexString(rva) +
                                " in any section");
      }

      class OptionalHeaderImpl implements OptionalHeader {
        private short magic;
        private MemoizedObject standardFields;
        private MemoizedObject windowsSpecificFields;
        private MemoizedObject dataDirectories;

        // We use an offset of 2 because OptionalHeaderStandardFieldsImpl doesn't
        // include the 'magic' field.
        private static final int STANDARD_FIELDS_OFFSET = 2;
        private static final int PE32_WINDOWS_SPECIFIC_FIELDS_OFFSET = 28;
        private static final int PE32_DATA_DIRECTORIES_OFFSET = 96;
        private static final int PE32_PLUS_WINDOWS_SPECIFIC_FIELDS_OFFSET = 24;
        private static final int PE32_PLUS_DATA_DIRECTORIES_OFFSET = 112;

        OptionalHeaderImpl(final int offset) {
          seek(offset);
          magic = readShort();

          final boolean isPE32Plus = (magic == MAGIC_PE32_PLUS);
          final int standardFieldsOffset = offset + STANDARD_FIELDS_OFFSET;
          final int windowsSpecificFieldsOffset = offset +
            (isPE32Plus
             ? PE32_PLUS_WINDOWS_SPECIFIC_FIELDS_OFFSET
             : PE32_WINDOWS_SPECIFIC_FIELDS_OFFSET);
          final int dataDirectoriesOffset = offset +
            (isPE32Plus
             ? PE32_PLUS_DATA_DIRECTORIES_OFFSET
             : PE32_DATA_DIRECTORIES_OFFSET);

          standardFields = new MemoizedObject() {
              public Object computeValue() {
                return new OptionalHeaderStandardFieldsImpl(standardFieldsOffset,
                                                            isPE32Plus);
              }
            };
          windowsSpecificFields = new MemoizedObject() {
              public Object computeValue() {
                return new OptionalHeaderWindowsSpecificFieldsImpl(windowsSpecificFieldsOffset,
                                                                   isPE32Plus);
              }
            };
          dataDirectories = new MemoizedObject() {
              public Object computeValue() {
                return new OptionalHeaderDataDirectoriesImpl(dataDirectoriesOffset,
                                                             getWindowsSpecificFields().getNumberOfRvaAndSizes());
              }
            };
        }

        public short getMagicNumber() {
          return magic;
        }

        public OptionalHeaderStandardFields getStandardFields() {
          return (OptionalHeaderStandardFields) standardFields.getValue();
        }

        public OptionalHeaderWindowsSpecificFields getWindowsSpecificFields() {
          return (OptionalHeaderWindowsSpecificFields) windowsSpecificFields.getValue();
        }
        public OptionalHeaderDataDirectories getDataDirectories() {
          return (OptionalHeaderDataDirectories) dataDirectories.getValue();
        }
      }

      class OptionalHeaderStandardFieldsImpl implements OptionalHeaderStandardFields {
        private boolean isPE32Plus;
        private byte majorLinkerVersion;
        private byte minorLinkerVersion;
        private int sizeOfCode;
        private int sizeOfInitializedData;
        private int sizeOfUninitializedData;
        private int addressOfEntryPoint;
        private int baseOfCode;
        private int baseOfData;  // only set in PE32

        OptionalHeaderStandardFieldsImpl(int offset,
                                         boolean isPE32Plus) {
          this.isPE32Plus = isPE32Plus;
          seek(offset);
          majorLinkerVersion = readByte();
          minorLinkerVersion = readByte();
          sizeOfCode = readInt();
          sizeOfInitializedData = readInt();
          sizeOfUninitializedData = readInt();
          addressOfEntryPoint = readInt();
          baseOfCode = readInt();
          if (!isPE32Plus) {
            // only available in PE32
            baseOfData = readInt();
          }
        }

        public byte getMajorLinkerVersion() { return majorLinkerVersion; }
        public byte getMinorLinkerVersion() { return minorLinkerVersion; }
        public int getSizeOfCode()              { return sizeOfCode; }
        public int getSizeOfInitializedData()   { return sizeOfInitializedData; }
        public int getSizeOfUninitializedData() { return sizeOfUninitializedData; }
        public int getAddressOfEntryPoint()     { return addressOfEntryPoint; }
        public int getBaseOfCode()              { return baseOfCode; }
        public int getBaseOfData() throws COFFException {
          if (isPE32Plus) {
            throw new COFFException("Not present in PE32+ files");
          }
          return baseOfData;
        }
      }

      class OptionalHeaderWindowsSpecificFieldsImpl implements OptionalHeaderWindowsSpecificFields {
        private long imageBase;
        private int sectionAlignment;
        private int fileAlignment;
        private short majorOperatingSystemVersion;
        private short minorOperatingSystemVersion;
        private short majorImageVersion;
        private short minorImageVersion;
        private short majorSubsystemVersion;
        private short minorSubsystemVersion;
        private int sizeOfImage;
        private int sizeOfHeaders;
        private int checkSum;
        private short subsystem;
        private short dllCharacteristics;
        private long sizeOfStackReserve;
        private long sizeOfStackCommit;
        private long sizeOfHeapReserve;
        private long sizeOfHeapCommit;
        private int loaderFlags;
        private int numberOfRvaAndSizes;

        OptionalHeaderWindowsSpecificFieldsImpl(int offset, boolean isPE32Plus) {
          seek(offset);

          if (!isPE32Plus) {
            imageBase = maskInt(readInt());
          } else {
            imageBase = readLong();
          }
          sectionAlignment = readInt();
          fileAlignment = readInt();
          majorOperatingSystemVersion = readShort();
          minorOperatingSystemVersion = readShort();
          majorImageVersion = readShort();
          minorImageVersion = readShort();
          majorSubsystemVersion = readShort();
          minorSubsystemVersion = readShort();
          readInt(); // Reserved
          sizeOfImage = readInt();
          sizeOfHeaders = readInt();
          checkSum = readInt();
          subsystem = readShort();
          dllCharacteristics = readShort();
          if (!isPE32Plus) {
            sizeOfStackReserve = maskInt(readInt());
            sizeOfStackCommit  = maskInt(readInt());
            sizeOfHeapReserve  = maskInt(readInt());
            sizeOfHeapCommit   = maskInt(readInt());
          } else {
            sizeOfStackReserve = readLong();
            sizeOfStackCommit  = readLong();
            sizeOfHeapReserve  = readLong();
            sizeOfHeapCommit   = readLong();
          }
          loaderFlags = readInt();
          numberOfRvaAndSizes = readInt();
        }

        public long getImageBase()              { return imageBase; }
        public int getSectionAlignment()        { return sectionAlignment; }
        public int getFileAlignment()           { return fileAlignment; }
        public short getMajorOperatingSystemVersion() { return majorOperatingSystemVersion; }
        public short getMinorOperatingSystemVersion() { return minorOperatingSystemVersion; }
        public short getMajorImageVersion()     { return majorImageVersion; }
        public short getMinorImageVersion()     { return minorImageVersion; }
        public short getMajorSubsystemVersion() { return majorSubsystemVersion; }
        public short getMinorSubsystemVersion() { return minorSubsystemVersion; }
        public int getSizeOfImage()             { return sizeOfImage; }
        public int getSizeOfHeaders()           { return sizeOfHeaders; }
        public int getCheckSum()                { return checkSum; }
        public short getSubsystem()             { return subsystem; }
        public short getDLLCharacteristics()    { return dllCharacteristics; }
        public long getSizeOfStackReserve()     { return sizeOfStackReserve; }
        public long getSizeOfStackCommit()      { return sizeOfStackCommit; }
        public long getSizeOfHeapReserve()      { return sizeOfHeapReserve; }
        public long getSizeOfHeapCommit()       { return sizeOfHeapCommit; }
        public int getLoaderFlags()             { return loaderFlags; }
        public int getNumberOfRvaAndSizes()     { return numberOfRvaAndSizes; }

        private long maskInt(long arg) {
          return (arg & 0x00000000FFFFFFFFL);
        }
      }

      class OptionalHeaderDataDirectoriesImpl implements OptionalHeaderDataDirectories {
        private int numberOfRvaAndSizes;
        private MemoizedObject[] dataDirectories;
        private MemoizedObject   exportDirectoryTable;
        private MemoizedObject   debugDirectory;

        private static final int DATA_DIRECTORY_SIZE = 8;

        OptionalHeaderDataDirectoriesImpl(int offset,
                                          int numberOfRvaAndSizes) {
          this.numberOfRvaAndSizes = numberOfRvaAndSizes;
          dataDirectories = new MemoizedObject[numberOfRvaAndSizes];
          for (int i = 0; i < numberOfRvaAndSizes; i++) {
            final int dirOffset = offset + (i * DATA_DIRECTORY_SIZE);
            dataDirectories[i] = new MemoizedObject() {
                public Object computeValue() {
                  return new DataDirectoryImpl(dirOffset);
                }
              };
          }

          exportDirectoryTable = new MemoizedObject() {
              public Object computeValue() {
                DataDirectory dir = getExportTable();
                if (dir.getRVA() == 0 || dir.getSize() == 0) {
                  return null;
                }
                // ExportDirectoryTableImpl needs both the RVA and the
                // RVA converted to a file offset.
                return new
                    ExportDirectoryTableImpl(dir.getRVA(), dir.getSize());
              }
            };

          debugDirectory = new MemoizedObject() {
              public Object computeValue() {
                DataDirectory dir = getDebug();
                if (dir.getRVA() == 0 || dir.getSize() == 0) {
                  return null;
                }
                return new DebugDirectoryImpl(rvaToFileOffset(dir.getRVA()), dir.getSize());
              }
            };
        }

        public DataDirectory getExportTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(0)].getValue();
        }
        public DataDirectory getImportTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(1)].getValue();
        }
        public DataDirectory getResourceTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(2)].getValue();
        }
        public DataDirectory getExceptionTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(3)].getValue();
        }
        public DataDirectory getCertificateTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(4)].getValue();
        }
        public DataDirectory getBaseRelocationTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(5)].getValue();
        }
        public DataDirectory getDebug() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(6)].getValue();
        }
        public DataDirectory getArchitecture() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(7)].getValue();
        }
        public DataDirectory getGlobalPtr() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(8)].getValue();
        }
        public DataDirectory getTLSTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(9)].getValue();
        }
        public DataDirectory getLoadConfigTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(10)].getValue();
        }
        public DataDirectory getBoundImportTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(11)].getValue();
        }
        public DataDirectory getImportAddressTable() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(12)].getValue();
        }
        public DataDirectory getDelayImportDescriptor() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(13)].getValue();
        }
        public DataDirectory getCOMPlusRuntimeHeader() throws COFFException {
          return (DataDirectory) dataDirectories[checkIndex(14)].getValue();
        }

        public ExportDirectoryTable getExportDirectoryTable() throws COFFException {
          return (ExportDirectoryTable) exportDirectoryTable.getValue();
        }

        public DebugDirectory getDebugDirectory() throws COFFException {
          return (DebugDirectory) debugDirectory.getValue();
        }

        private int checkIndex(int index) throws COFFException {
          if ((index < 0) || (index >= dataDirectories.length)) {
            throw new COFFException("Directory " + index + " unavailable (only " +
                                    numberOfRvaAndSizes + " tables present)");
          }
          return index;
        }
      }

      class DataDirectoryImpl implements DataDirectory {
        int rva;
        int size;

        DataDirectoryImpl(int offset) {
          seek(offset);
          rva  = readInt();
          size = readInt();
        }

        public int getRVA()  { return rva; }
        public int getSize() { return size; }
      }

      class ExportDirectoryTableImpl implements ExportDirectoryTable {
        private int exportDataDirRVA;
        private int offset;
        private int size;

        private int exportFlags;
        private int timeDateStamp;
        private short majorVersion;
        private short minorVersion;
        private int nameRVA;
        private int ordinalBase;
        private int addressTableEntries;
        private int numberOfNamePointers;
        private int exportAddressTableRVA;
        private int namePointerTableRVA;
        private int ordinalTableRVA;

        private MemoizedObject dllName;

        private MemoizedObject exportNameTable;
        private MemoizedObject exportNamePointerTable;
        private MemoizedObject exportOrdinalTable;
        private MemoizedObject exportAddressTable;

        ExportDirectoryTableImpl(int exportDataDirRVA, int size) {
          this.exportDataDirRVA = exportDataDirRVA;
          offset = rvaToFileOffset(exportDataDirRVA);
          this.size   = size;
          seek(offset);
          exportFlags = readInt();
          timeDateStamp = readInt();
          majorVersion = readShort();
          minorVersion = readShort();
          nameRVA = readInt();
          ordinalBase = readInt();
          addressTableEntries = readInt();
          numberOfNamePointers = readInt();
          exportAddressTableRVA = readInt();
          namePointerTableRVA = readInt();
          ordinalTableRVA = readInt();

          dllName = new MemoizedObject() {
              public Object computeValue() {
                seek(rvaToFileOffset(getNameRVA()));
                return readCString();
              }
            };

          exportNamePointerTable = new MemoizedObject() {
              public Object computeValue() {
                int[] pointers = new int[getNumberOfNamePointers()];
                seek(rvaToFileOffset(getNamePointerTableRVA()));
                // Must make two passes to avoid rvaToFileOffset
                // destroying seek() position
                for (int i = 0; i < pointers.length; i++) {
                  pointers[i] = readInt();
                }
                for (int i = 0; i < pointers.length; i++) {
                  pointers[i] = rvaToFileOffset(pointers[i]);
                }
                return pointers;
              }
            };

          exportNameTable = new MemoizedObject() {
              public Object computeValue() {
                return new ExportNameTable(getExportNamePointerTable());
              }
            };

          exportOrdinalTable = new MemoizedObject() {
              public Object computeValue() {
                // number of ordinals is same as the number of name pointers
                short[] ordinals = new short[getNumberOfNamePointers()];
                seek(rvaToFileOffset(getOrdinalTableRVA()));
                for (int i = 0; i < ordinals.length; i++) {
                  ordinals[i] = readShort();
                }
                return ordinals;
              }
            };

          exportAddressTable = new MemoizedObject() {
              public Object computeValue() {
                int[] addresses = new int[getNumberOfAddressTableEntries()];
                seek(rvaToFileOffset(getExportAddressTableRVA()));
                // The Export Address Table values are a union of two
                // possible values:
                //   Export RVA - The address of the exported symbol when
                //       loaded into memory, relative to the image base.
                //       This value doesn't get converted into a file offset.
                //   Forwarder RVA - The pointer to a null-terminated ASCII
                //       string in the export section. This value gets
                //       converted into a file offset because we have to
                //       fetch the string.
                for (int i = 0; i < addresses.length; i++) {
                  addresses[i] = readInt();
                }
                return addresses;
              }
            };
        }

        public int   getExportFlags()   { return exportFlags; }
        public int   getTimeDateStamp() { return timeDateStamp; }
        public short getMajorVersion()  { return majorVersion; }
        public short getMinorVersion()  { return minorVersion; }
        public int   getNameRVA()       { return nameRVA; }

        public String getDLLName() {
          return (String) dllName.getValue();
        }

        public int getOrdinalBase()                 { return ordinalBase; }
        public int getNumberOfAddressTableEntries() { return addressTableEntries; }
        public int getNumberOfNamePointers()        { return numberOfNamePointers; }
        public int getExportAddressTableRVA()       { return exportAddressTableRVA; }
        public int getNamePointerTableRVA()         { return namePointerTableRVA; }
        public int getOrdinalTableRVA()             { return ordinalTableRVA; }

        public String getExportName(int i) {
          return getExportNameTable().get(i);
        }

        public short  getExportOrdinal(int i) {
          return getExportOrdinalTable()[i];
        }

        public boolean isExportAddressForwarder(short ordinal) {
          int addr = getExportAddress(ordinal);
          return ((exportDataDirRVA <= addr) &&
              (addr < (exportDataDirRVA + size)));
        }

        public String getExportAddressForwarder(short ordinal) {
          seek(rvaToFileOffset(getExportAddress(ordinal)));
          return readCString();
        }

        public int    getExportAddress(short ordinal) {

          ///////////////////////
          // FIXME: MAJOR HACK //
          ///////////////////////

          // According to the documentation, the first line here is
          // correct. However, it doesn't seem to work. The second
          // one, however, does.

          // OK, it's probably due to using negative indices in the
          // export address table in "real life"...need to rethink
          // this when I'm more awake

          //          return getExportAddressTable()[ordinal - ordinalBase];
          return getExportAddressTable()[ordinal];
        }

        private ExportNameTable getExportNameTable() {
          return (ExportNameTable) exportNameTable.getValue();
        }

        private int[] getExportNamePointerTable() {
          return (int[]) exportNamePointerTable.getValue();
        }

        private short[] getExportOrdinalTable() {
          return (short[]) exportOrdinalTable.getValue();
        }

        private int[] getExportAddressTable() {
          return (int[]) exportAddressTable.getValue();
        }
      }

      class ExportNameTable {
        private MemoizedObject[] names;

        ExportNameTable(final int[] exportNamePointerTable) {
          names = new MemoizedObject[exportNamePointerTable.length];
          for (int i = 0; i < exportNamePointerTable.length; i++) {
            final int idx = i;
            names[idx] = new MemoizedObject() {
                public Object computeValue() {
                  seek(exportNamePointerTable[idx]);
                  return readCString();
                }
              };
            };
        }

        String get(int i) {
          return (String) names[i].getValue();
        }
      }

      class DebugDirectoryImpl implements DebugDirectory {
        private int offset;
        private int size;
        private int numEntries;

        private static final int DEBUG_DIRECTORY_ENTRY_SIZE = 28;

        DebugDirectoryImpl(int offset, int size) {
          this.offset = offset;
          this.size = size;

          if ((size % DEBUG_DIRECTORY_ENTRY_SIZE) != 0) {
            throw new COFFException("Corrupt DebugDirectory at offset 0x" +
                                    Integer.toHexString(offset));
          }

          numEntries = size / DEBUG_DIRECTORY_ENTRY_SIZE;
        }

        public int getNumEntries() { return numEntries; }
        public DebugDirectoryEntry getEntry(int i) {
          Objects.checkIndex(i, getNumEntries());
          return new DebugDirectoryEntryImpl(offset + i * DEBUG_DIRECTORY_ENTRY_SIZE);
        }
      }

      class DebugDirectoryEntryImpl implements DebugDirectoryEntry, DebugTypes {
        private int characteristics;
        private int timeDateStamp;
        private short majorVersion;
        private short minorVersion;
        private int type;
        private int sizeOfData;
        private int addressOfRawData;
        private int pointerToRawData;

        DebugDirectoryEntryImpl(int offset) {
          seek(offset);
          characteristics = readInt();
          timeDateStamp = readInt();
          majorVersion = readShort();
          minorVersion = readShort();
          type = readInt();
          sizeOfData = readInt();
          addressOfRawData = readInt();
          pointerToRawData = readInt();
        }

        public int   getCharacteristics()  { return characteristics; }
        public int   getTimeDateStamp()    { return timeDateStamp; }
        public short getMajorVersion()     { return majorVersion; }
        public short getMinorVersion()     { return minorVersion; }
        public int   getType()             { return type; }
        public int   getSizeOfData()       { return sizeOfData; }
        public int   getAddressOfRawData() { return addressOfRawData; }
        public int   getPointerToRawData() { return pointerToRawData; }

        public DebugVC50 getDebugVC50() {
          // See whether we can recognize VC++ 5.0 debug information.
          try {
            if (getType() != IMAGE_DEBUG_TYPE_CODEVIEW) return null;

            int offset = getPointerToRawData();
            seek(offset);
            if (readByte() == 'N' &&
                readByte() == 'B' &&
                readByte() == '1' &&
                readByte() == '1') {
              return new DebugVC50Impl(offset);
            }
          } catch (COFFException e) {
            e.printStackTrace();
          }
          return null;
        }

        public byte  getRawDataByte(int i) {
          Objects.checkIndex(i, getSizeOfData());
          seek(getPointerToRawData() + i);
          return readByte();
        }
      }

      class DebugVC50Impl implements DebugVC50, DebugVC50TypeLeafIndices {
        private int lfaBase;

        private int subsectionDirectoryOffset;
        private MemoizedObject subsectionDirectory;

        DebugVC50Impl(int offset) {
          lfaBase = offset;
          seek(offset);
          readInt();  // Discard NB11
          subsectionDirectoryOffset = globalOffset(readInt());

          // Ensure information is complete
          verify();

          subsectionDirectory = new MemoizedObject() {
              public Object computeValue() {
                return new DebugVC50SubsectionDirectoryImpl(getSubsectionDirectoryOffset());
              }
            };
        }

        public int getSubsectionDirectoryOffset() {
          return subsectionDirectoryOffset;
        }

        public DebugVC50SubsectionDirectory getSubsectionDirectory() {
          return (DebugVC50SubsectionDirectory) subsectionDirectory.getValue();
        }

        private int globalOffset(int offset) {
          return offset + lfaBase;
        }

        private void verify() {
          // Seek to subsection directory manually and look for
          // signature following it. This finishes validating that we
          // have VC++ 5.0 debug info. Throw COFFException if not
          // found; will cause caller to return null.
          seek(subsectionDirectoryOffset);
          int headerLength = readShort();
          int entryLength  = readShort();
          int numEntries   = readInt();
          int endOffset    = subsectionDirectoryOffset + headerLength + numEntries * entryLength;
          seek(endOffset);

          if (readByte() == 'N' &&
              readByte() == 'B' &&
              readByte() == '1' &&
              readByte() == '1') {
            return;
          }

          throw new COFFException("Did not find NB11 signature at end of debug info");
        }

        class DebugVC50SubsectionDirectoryImpl
          implements DebugVC50SubsectionDirectory,
                     DebugVC50SubsectionTypes {
          private int   offset;
          private short dirHeaderLength;
          private short dirEntryLength;
          private int   numEntries;

          DebugVC50SubsectionDirectoryImpl(int offset) {
            this.offset = offset;
            // Read header
            seek(offset);
            dirHeaderLength = readShort();
            dirEntryLength  = readShort();
            numEntries      = readInt();
          }

          public short getHeaderLength() { return dirHeaderLength; }
          public short getEntryLength()  { return dirEntryLength;  }
          public int   getNumEntries()   { return numEntries;      }

          public DebugVC50Subsection getSubsection(int i) {
            // Fetch the subsection type and instantiate the correct
            // type of subsection based on it
            seek(offset + dirHeaderLength + (i * dirEntryLength));
            short ssType = readShort();
            short iMod   = readShort(); // Unneeded?
            int   lfo    = globalOffset(readInt());
            int   cb     = readInt();
            switch (ssType) {
            case SST_MODULE:
              return new DebugVC50SSModuleImpl(ssType, iMod, cb, lfo);
            case SST_TYPES:
              return new DebugVC50SSTypesImpl(ssType, iMod, cb, lfo);
            case SST_PUBLIC:
              return new DebugVC50SSPublicImpl(ssType, iMod, cb, lfo);
            case SST_PUBLIC_SYM:
              return new DebugVC50SSPublicSymImpl(ssType, iMod, cb, lfo);
            case SST_SYMBOLS:
              return new DebugVC50SSSymbolsImpl(ssType, iMod, cb, lfo);
            case SST_ALIGN_SYM:
              return new DebugVC50SSAlignSymImpl(ssType, iMod, cb, lfo);
            case SST_SRC_LN_SEG:
              return new DebugVC50SSSrcLnSegImpl(ssType, iMod, cb, lfo);
            case SST_SRC_MODULE:
              return new DebugVC50SSSrcModuleImpl(ssType, iMod, cb, lfo);
            case SST_LIBRARIES:
              return new DebugVC50SSLibrariesImpl(ssType, iMod, cb, lfo);
            case SST_GLOBAL_SYM:
              return new DebugVC50SSGlobalSymImpl(ssType, iMod, cb, lfo);
            case SST_GLOBAL_PUB:
              return new DebugVC50SSGlobalPubImpl(ssType, iMod, cb, lfo);
            case SST_GLOBAL_TYPES:
              return new DebugVC50SSGlobalTypesImpl(ssType, iMod, cb, lfo);
            case SST_MPC:
              return new DebugVC50SSMPCImpl(ssType, iMod, cb, lfo);
            case SST_SEG_MAP:
              return new DebugVC50SSSegMapImpl(ssType, iMod, cb, lfo);
            case SST_SEG_NAME:
              return new DebugVC50SSSegNameImpl(ssType, iMod, cb, lfo);
            case SST_PRE_COMP:
              return new DebugVC50SSPreCompImpl(ssType, iMod, cb, lfo);
            case SST_UNUSED:
              return null;
            case SST_OFFSET_MAP_16:
              return new DebugVC50SSOffsetMap16Impl(ssType, iMod, cb, lfo);
            case SST_OFFSET_MAP_32:
              return new DebugVC50SSOffsetMap32Impl(ssType, iMod, cb, lfo);
            case SST_FILE_INDEX:
              return new DebugVC50SSFileIndexImpl(ssType, iMod, cb, lfo);
            case SST_STATIC_SYM:
              return new DebugVC50SSStaticSymImpl(ssType, iMod, cb, lfo);
            default:
              throw new COFFException("Unknown section type " + ssType);
            }
          }
        }

        ////////////////////////////////////
        //                                //
        // Implementations of subsections //
        //                                //
        ////////////////////////////////////

        class DebugVC50SubsectionImpl implements DebugVC50Subsection {
          private short ssType;
          private short iMod;
          private int   ssSize;

          DebugVC50SubsectionImpl(short ssType, short iMod, int ssSize, int offset) {
            this.ssType = ssType;
            this.iMod   = iMod;
            this.ssSize = ssSize;
          }

          public short getSubsectionType()        { return ssType; }
          public short getSubsectionModuleIndex() { return iMod; }
          public int   getSubsectionSize()        { return ssSize; }
        }

        class DebugVC50SSModuleImpl extends DebugVC50SubsectionImpl implements DebugVC50SSModule {
          private int offset;
          private short ovlNumber;
          private short iLib;
          private short cSeg;
          private short style;
          private MemoizedObject segInfo;
          private MemoizedObject name;

          private static final int HEADER_SIZE   = 8;
          private static final int SEG_INFO_SIZE = 12;

          DebugVC50SSModuleImpl(short ssType, short iMod, int ssSize, final int offset) {
            super(ssType, iMod, ssSize, offset);
            this.offset = offset;
            seek(offset);
            ovlNumber = readShort();
            iLib      = readShort();
            cSeg      = readShort();
            style     = readShort();
            segInfo   = new MemoizedObject() {
                public Object computeValue() {
                  int base = offset + HEADER_SIZE;
                  DebugVC50SegInfo[] res = new DebugVC50SegInfo[cSeg];
                  for (int i = 0; i < cSeg; i++) {
                    res[i] = new DebugVC50SegInfoImpl(base);
                    base += SEG_INFO_SIZE;
                  }
                  return res;
                }
              };
            name      = new MemoizedObject() {
                public Object computeValue() {
                  return readLengthPrefixedStringAt(offset + (HEADER_SIZE + cSeg * SEG_INFO_SIZE));
                }
              };
          }

          public short getOverlayNumber()   { return ovlNumber; }
          public short getLibrariesIndex()  { return iLib; }
          public short getNumCodeSegments() { return cSeg; }
          public short getDebuggingStyle()  { return style; }
          public DebugVC50SegInfo getSegInfo(int i) { return ((DebugVC50SegInfo[]) segInfo.getValue())[i]; }
          public String getName()           { return (String) name.getValue(); }
        }

        class DebugVC50SegInfoImpl implements DebugVC50SegInfo {
          private short seg;
          private int   offset;
          private int   cbSeg;

          DebugVC50SegInfoImpl(int offset) {
            seek(offset);
            seg = readShort();
            readShort(); // pad
            offset = readInt();
            cbSeg = readInt();
          }

          public short getSegment() { return seg; }
          public int getOffset() { return offset; }
          public int getSegmentCodeSize() { return cbSeg; }
        }

        class DebugVC50SSTypesImpl extends DebugVC50SubsectionImpl implements DebugVC50SSTypes {
          DebugVC50SSTypesImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSPublicImpl extends DebugVC50SubsectionImpl implements DebugVC50SSPublic {
          DebugVC50SSPublicImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSPublicSymImpl extends DebugVC50SubsectionImpl implements DebugVC50SSPublicSym {
          DebugVC50SSPublicSymImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSSymbolsImpl extends DebugVC50SubsectionImpl implements DebugVC50SSSymbols {
          DebugVC50SSSymbolsImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSAlignSymImpl extends DebugVC50SubsectionImpl implements DebugVC50SSAlignSym {
          private int offset;

          DebugVC50SSAlignSymImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
            this.offset = offset;
          }

          public DebugVC50SymbolIterator getSymbolIterator() {
            return new DebugVC50SymbolIteratorImpl(offset, getSubsectionSize());
          }
        }

        class DebugVC50SSSrcLnSegImpl extends DebugVC50SubsectionImpl implements DebugVC50SSSrcLnSeg {
          DebugVC50SSSrcLnSegImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSSrcModuleImpl extends DebugVC50SubsectionImpl implements DebugVC50SSSrcModule {
          private int offset;
          private short cFile;
          private short cSeg;
          private MemoizedObject baseSrcFiles;
          private MemoizedObject segOffsets;
          private MemoizedObject segs;

          DebugVC50SSSrcModuleImpl(short ssType, short iMod, int ssSize, final int offset) {
            super(ssType, iMod, ssSize, offset);

            this.offset = offset;
            seek(offset);
            cFile = readShort();
            cSeg  = readShort();

            baseSrcFiles = new MemoizedObject() {
                public Object computeValue() {
                  int[] offsets = new int[getNumSourceFiles()];
                  seek(offset + 4);
                  for (int i = 0; i < getNumSourceFiles(); i++) {
                    offsets[i] = offset + readInt();
                  }
                  DebugVC50SrcModFileDescImpl[] res = new DebugVC50SrcModFileDescImpl[offsets.length];
                  for (int i = 0; i < res.length; i++) {
                    res[i] = new DebugVC50SrcModFileDescImpl(offsets[i], offset);
                  }
                  return res;
                }
              };

            segOffsets = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4 * (getNumSourceFiles() + 1));
                  int[] res = new int[2 * getNumCodeSegments()];
                  for (int i = 0; i < 2 * getNumCodeSegments(); i++) {
                    res[i] = readInt();
                  }
                  return res;
                }
              };

            segs = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4 * (getNumSourceFiles() + 1) + 8 * getNumCodeSegments());
                  short[] res = new short[getNumCodeSegments()];
                  for (int i = 0; i < getNumCodeSegments(); i++) {
                    res[i] = readShort();
                  }
                  return res;
                }
              };
          }

          public int getNumSourceFiles()  { return cFile & 0xFFFF; }
          public int getNumCodeSegments() { return cSeg & 0xFFFF;  }
          public DebugVC50SrcModFileDesc getSourceFileDesc(int i) {
            return ((DebugVC50SrcModFileDescImpl[]) baseSrcFiles.getValue())[i];
          }

          public int getSegmentStartOffset(int i) {
            return ((int[]) segOffsets.getValue())[2*i];
          }

          public int getSegmentEndOffset(int i) {
            return ((int[]) segOffsets.getValue())[2*i+1];
          }

          public int getSegment(int i) {
            return ((short[]) segs.getValue())[i] & 0xFFFF;
          }
        }

        class DebugVC50SrcModFileDescImpl implements DebugVC50SrcModFileDesc {
          private short cSeg;
          private MemoizedObject baseSrcLn;
          private MemoizedObject segOffsets;
          private MemoizedObject name;

          DebugVC50SrcModFileDescImpl(final int offset, final int baseOffset) {
            seek(offset);
            cSeg = readShort();

            baseSrcLn = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4);
                  int[] offsets = new int[getNumCodeSegments()];
                  for (int i = 0; i < getNumCodeSegments(); i++) {
                    offsets[i] = baseOffset + readInt();
                  }
                  DebugVC50SrcModLineNumberMapImpl[] res =
                    new DebugVC50SrcModLineNumberMapImpl[getNumCodeSegments()];
                  for (int i = 0; i < getNumCodeSegments(); i++) {
                    res[i] = new DebugVC50SrcModLineNumberMapImpl(offsets[i]);
                  }
                  return res;
                }
              };

            segOffsets = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4 * (getNumCodeSegments() + 1));
                  int[] res = new int[2 * getNumCodeSegments()];
                  for (int i = 0; i < 2 * getNumCodeSegments(); i++) {
                    res[i] = readInt();
                  }
                  return res;
                }
              };

            name = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4 + 12 * getNumCodeSegments());
                  // NOTE: spec says name length is two bytes, but it's really one
                  int cbName = readByte() & 0xFF;
                  byte[] res = new byte[cbName];
                  readBytes(res);
                  try {
                    return new String(res, US_ASCII);
                  } catch (UnsupportedEncodingException e) {
                    throw new COFFException(e);
                  }
                }
              };
          }

          public int getNumCodeSegments() { return cSeg & 0xFFFF; }

          public DebugVC50SrcModLineNumberMap getLineNumberMap(int i) {
            return ((DebugVC50SrcModLineNumberMapImpl[]) baseSrcLn.getValue())[i];
          }

          public int getSegmentStartOffset(int i) {
            return ((int[]) segOffsets.getValue())[2*i];
          }

          public int getSegmentEndOffset(int i) {
            return ((int[]) segOffsets.getValue())[2*i+1];
          }

          public String getSourceFileName() {
            return (String) name.getValue();
          }
        }

        class DebugVC50SrcModLineNumberMapImpl implements DebugVC50SrcModLineNumberMap {
          private short seg;
          private short cPair;
          private MemoizedObject offsets;
          private MemoizedObject lineNumbers;

          DebugVC50SrcModLineNumberMapImpl(final int offset) {
            seek(offset);
            seg = readShort();
            cPair = readShort();
            offsets = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4);
                  int[] res = new int[getNumSourceLinePairs()];
                  for (int i = 0; i < getNumSourceLinePairs(); i++) {
                    res[i] = readInt();
                  }
                  return res;
                }
              };

            lineNumbers = new MemoizedObject() {
                public Object computeValue() {
                  seek(offset + 4 * (getNumSourceLinePairs() + 1));
                  short[] res = new short[getNumSourceLinePairs()];
                  for (int i = 0; i < getNumSourceLinePairs(); i++) {
                    res[i] = readShort();
                  }
                  return res;
                }
              };
          }

          public int getSegment() { return seg; }
          public int getNumSourceLinePairs() { return cPair; }
          public int getCodeOffset(int i) {
            return ((int[]) offsets.getValue())[i];
          }
          public int getLineNumber(int i) {
            return ((short[]) lineNumbers.getValue())[i] & 0xFFFF;
          }
        }

        class DebugVC50SSLibrariesImpl extends DebugVC50SubsectionImpl implements DebugVC50SSLibraries {
          DebugVC50SSLibrariesImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }

          // FIXME: NOT FINISHED
        }

        class DebugVC50SSSymbolBaseImpl extends DebugVC50SubsectionImpl implements DebugVC50SSSymbolBase {
          private int   offset;
          private short symHash;
          private short addrHash;
          private int   cbSymbol;
          private int   cbSymHash;
          private int   cbAddrHash;

          private static final int HEADER_SIZE = 16;

          DebugVC50SSSymbolBaseImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
            this.offset = offset;
            seek(offset);
            symHash    = readShort();
            addrHash   = readShort();
            cbSymbol   = readInt();
            cbSymHash  = readInt();
            cbAddrHash = readInt();
          }

          public short getSymHashIndex()  { return symHash; }
          public short getAddrHashIndex() { return addrHash; }
          public int getSymTabSize()      { return cbSymbol; }
          public int getSymHashSize()     { return cbSymHash; }
          public int getAddrHashSize()    { return cbAddrHash; }

          public DebugVC50SymbolIterator getSymbolIterator() {
            return new DebugVC50SymbolIteratorImpl(offset + HEADER_SIZE, cbSymbol);
          }
        }

        class DebugVC50SSGlobalSymImpl extends DebugVC50SSSymbolBaseImpl implements DebugVC50SSGlobalSym {
          DebugVC50SSGlobalSymImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }
        class DebugVC50SSGlobalPubImpl extends DebugVC50SSSymbolBaseImpl implements DebugVC50SSGlobalPub {
          DebugVC50SSGlobalPubImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSGlobalTypesImpl extends DebugVC50SubsectionImpl implements DebugVC50SSGlobalTypes {
          private int offset;
          private int cType;

          DebugVC50SSGlobalTypesImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
            this.offset = offset;
            seek(offset);
            readInt(); // Discard "flags"
            cType = readInt();
          }

          public int getNumTypes() { return cType; }
          // FIXME: should memoize these
          public int getTypeOffset(int i) {
            seek(offset + 4 * (i + 2));
            return readInt() + offsetOfFirstType();
          }

          public DebugVC50TypeIterator getTypeIterator() {
            return new DebugVC50TypeIteratorImpl(this,
                                                 offsetOfFirstType(),
                                                 cType);
          }

          private int offsetOfFirstType() {
            return offset + 4 * (getNumTypes() + 2);
          }
        }

        class DebugVC50SSMPCImpl extends DebugVC50SubsectionImpl implements DebugVC50SSMPC {
          DebugVC50SSMPCImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSSegMapImpl extends DebugVC50SubsectionImpl implements DebugVC50SSSegMap {
          private short cSeg;
          private short cSegLog;
          private MemoizedObject segDescs;

          DebugVC50SSSegMapImpl(short ssType, short iMod, int ssSize, final int offset) {
            super(ssType, iMod, ssSize, offset);
            seek(offset);
            cSeg = readShort();
            cSegLog = readShort();
            segDescs = new MemoizedObject() {
                public Object computeValue() {
                  DebugVC50SegDesc[] descs = new DebugVC50SegDesc[cSeg];
                  for (int i = 0; i < cSeg; i++) {
                    descs[i] = new DebugVC50SegDescImpl(offset + 4 + (20 * i));
                  }
                  return descs;
                }
              };
          }

          public short getNumSegDesc() { return cSeg; }
          public short getNumLogicalSegDesc() { return cSegLog; }
          public DebugVC50SegDesc getSegDesc(int i) { return ((DebugVC50SegDesc[]) segDescs.getValue())[i]; }
        }

        class DebugVC50SegDescImpl implements DebugVC50SegDesc {
          private short flags;
          private short ovl;
          private short group;
          private short frame;
          private short iSegName;
          private short iClassName;
          private int   offset;
          private int   cbSeg;

          DebugVC50SegDescImpl(int offset) {
            seek(offset);
            flags = readShort();
            ovl = readShort();
            group = readShort();
            frame = readShort();
            iSegName = readShort();
            iClassName = readShort();
            offset = readInt();
            cbSeg = readInt();
          }

          public short getFlags() { return flags; }
          public short getOverlayNum() { return ovl; }
          public short getGroup() { return group; }
          public short getFrame() { return frame; }
          public short getName() { return iSegName; }
          public short getClassName() { return iClassName; }
          public int   getOffset() { return offset; }
          public int   getSize() { return cbSeg; }
        }


        class DebugVC50SSSegNameImpl extends DebugVC50SubsectionImpl implements DebugVC50SSSegName {
          private int offset;
          private int size;
          private MemoizedObject names;

          DebugVC50SSSegNameImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
            this.offset = offset;
            this.size   = ssSize;
            seek(offset);
            names = new MemoizedObject() {
                public Object computeValue() {
                  int i = 0;
                  List<String> data = new ArrayList<>();
                  while (i < size) {
                    String s = readCString();
                    data.add(s);
                    i += s.length();
                  }
                  String[] res = new String[data.size()];
                  res = data.toArray(res);
                  return res;
                }
              };
          }

          public String getSegName(int i) {
            return ((String[]) names.getValue())[i];
          }
        }

        class DebugVC50SSPreCompImpl extends DebugVC50SubsectionImpl implements DebugVC50SSPreComp {
          DebugVC50SSPreCompImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSOffsetMap16Impl extends DebugVC50SubsectionImpl implements DebugVC50SSOffsetMap16 {
          DebugVC50SSOffsetMap16Impl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSOffsetMap32Impl extends DebugVC50SubsectionImpl implements DebugVC50SSOffsetMap32 {
          DebugVC50SSOffsetMap32Impl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        class DebugVC50SSFileIndexImpl extends DebugVC50SubsectionImpl implements DebugVC50SSFileIndex {
          private int offset;
          private short cMod; // Number of modules in the executable
          private short cRef; // Total number of file name references
          private MemoizedObject modStart;
          private MemoizedObject cRefCnt;
          // FIXME: probably useless; needs fixup to be converted into
          // indices rather than offsets
          private MemoizedObject nameRef;
          private MemoizedObject names;

          DebugVC50SSFileIndexImpl(short ssType, short iMod, int ssSize, final int offset) {
            super(ssType, iMod, ssSize, offset);
            this.offset = offset;
            seek(offset);
            cMod = readShort();
            cRef = readShort();
            modStart = new MemoizedObject() {
                public Object computeValue() {
                  short[] vals = new short[cMod];
                  seek(4 + offset);
                  for (int i = 0; i < cMod; i++) {
                    vals[i] = readShort();
                  }
                  return vals;
                }
              };
            cRefCnt = new MemoizedObject() {
                public Object computeValue() {
                  short[] vals = new short[cMod];
                  seek(4 + offset + (2 * cMod));
                  for (int i = 0; i < cMod; i++) {
                    vals[i] = readShort();
                  }
                  return vals;
                }
              };
            nameRef = new MemoizedObject() {
                public Object computeValue() {
                  int[] vals = new int[cRef];
                  seek(4 + offset + (4 * cMod));
                  for (int i = 0; i < cMod; i++) {
                    vals[i] = readInt();
                  }
                  return vals;
                }
              };
            names = new MemoizedObject() {
                public Object computeValue() {
                  String[] vals = new String[cRef];
                  for (int i = 0; i < cRef; i++) {
                    vals[i] = readCString();
                  }
                  return vals;
                }
              };
          }

          public short getNumModules()    { return cMod; }
          public short getNumReferences() { return cRef; }
          public short[] getModStart()    { return (short[]) modStart.getValue(); }
          public short[] getRefCount()    { return (short[]) cRefCnt.getValue(); }
          public int[] getNameRef()       { return (int[]) nameRef.getValue(); }
          public String[] getNames()      { return (String[]) names.getValue(); }
        }

        class DebugVC50SSStaticSymImpl extends DebugVC50SSSymbolBaseImpl implements DebugVC50SSStaticSym {
          DebugVC50SSStaticSymImpl(short ssType, short iMod, int ssSize, int offset) {
            super(ssType, iMod, ssSize, offset);
          }
        }

        //////////////////////////////////////////////////
        //                                              //
        // Implementations of symbol and type iterators //
        //                                              //
        //////////////////////////////////////////////////

        class DebugVC50SymbolIteratorImpl implements DebugVC50SymbolIterator {
          private int base;
          private int size;
          private int pos;
          private int curSymSize;
          private int curSymType;

          private static final int HEADER_SIZE = 4;

          DebugVC50SymbolIteratorImpl(int base, int size) {
            this(base, size, base);
          }

          private DebugVC50SymbolIteratorImpl(int base, int size, int pos) {
            this.base = base;
            this.size = size;
            this.pos = pos;
            seek(pos);
            curSymSize = readShort() & 0xFFFF;
            curSymType = readShort() & 0xFFFF;
          }

          public boolean done() {
            return (pos == (base + size));
          }

          public void next() throws NoSuchElementException {
            if (done()) throw new NoSuchElementException("No more symbols");
            pos += curSymSize + 2;
            seek(pos);
            curSymSize = readShort() & 0xFFFF;
            curSymType = readShort() & 0xFFFF;
          }

          public short getLength() {
            return (short) curSymSize;
          }

          public int getType() {
            return curSymType;
          }

          public int getOffset() {
            return pos + HEADER_SIZE;
          }

          /////////////////////////
          // S_COMPILE accessors //
          /////////////////////////

          public byte getCompilerTargetProcessor() {
            symSeek(0);
            return readByte();
          }

          public int getCompilerFlags() {
            symSeek(1);
            int res = 0;
            for (int i = 0; i < 3; i++) {
              int b = readByte() & 0xFF;
              res = (res << 8) | b;
            }
            return res;
          }

          public String getComplierVersion() {
            return readLengthPrefixedStringAt(4);
          }

          //////////////////////////
          // S_REGISTER accessors //
          //////////////////////////

          public int getRegisterSymbolType() {
            symSeek(0);
            return readInt();
          }

          public short getRegisterEnum() {
            symSeek(4);
            return readShort();
          }

          public String getRegisterSymbolName() {
            return readLengthPrefixedStringAt(6);
          }

          //////////////////////////
          // S_CONSTANT accessors //
          //////////////////////////

          public int getConstantType() {
            symSeek(0);
            return readInt();
          }

          public int getConstantValueAsInt() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(4);
          }

          public long getConstantValueAsLong() throws DebugVC50WrongNumericTypeException {
            return readLongNumericLeafAt(4);
          }

          public float getConstantValueAsFloat() throws DebugVC50WrongNumericTypeException {
            return readFloatNumericLeafAt(4);
          }

          public double getConstantValueAsDouble() throws DebugVC50WrongNumericTypeException {
            return readDoubleNumericLeafAt(4);
          }

          public String getConstantName() {
            return readLengthPrefixedStringAt(4 + numericLeafLengthAt(4));
          }

          /////////////////////
          // S_UDT accessors //
          /////////////////////

          public int getUDTType() {
            symSeek(0);
            return readInt();
          }

          public String getUDTName() {
            return readLengthPrefixedStringAt(4);
          }

          /////////////////////////
          // S_SSEARCH accessors //
          /////////////////////////

          public int getSearchSymbolOffset() {
            symSeek(0);
            return readInt();
          }

          public short getSearchSegment() {
            symSeek(4);
            return readShort();
          }

          /////////////////////
          // S_END accessors //
          /////////////////////

          // (No accessors)

          //////////////////////
          // S_SKIP accessors //
          //////////////////////

          // (No accessors)

          ///////////////////////////
          // S_CVRESERVE accessors //
          ///////////////////////////

          // (No accessors)

          /////////////////////////
          // S_OBJNAME accessors //
          /////////////////////////

          public int getObjectCodeViewSignature() {
            symSeek(0);
            return readInt();
          }

          public String getObjectName() {
            return readLengthPrefixedStringAt(4);
          }

          ////////////////////////
          // S_ENDARG accessors //
          ////////////////////////

          // (No accessors)

          //////////////////////////
          // S_COBOLUDT accessors //
          //////////////////////////

          // (Elided as they are irrelevant)

          /////////////////////////
          // S_MANYREG accessors //
          /////////////////////////

          public int getManyRegType() {
            symSeek(0);
            return readInt();
          }

          public byte getManyRegCount() {
            symSeek(4);
            return readByte();
          }

          public byte getManyRegRegister(int i) {
            symSeek(5 + i);
            return readByte();
          }

          public String getManyRegName() {
            return readLengthPrefixedStringAt(5 + getManyRegCount());
          }

          ////////////////////////
          // S_RETURN accessors //
          ////////////////////////

          public short getReturnFlags() {
            symSeek(0);
            return readShort();
          }

          public byte getReturnStyle() {
            symSeek(2);
            return readByte();
          }

          public byte getReturnRegisterCount() {
            symSeek(3);
            return readByte();
          }

          public byte getReturnRegister(int i) {
            symSeek(4 + i);
            return readByte();
          }

          ///////////////////////////
          // S_ENTRYTHIS accessors //
          ///////////////////////////

          public void advanceToEntryThisSymbol() {
            seek(pos + 4);
            int tmpSymSize = readShort();
            int tmpSymType = readShort();
            if (Assert.ASSERTS_ENABLED) {
              // Make sure that ends of inner and outer symbols line
              // up, otherwise need more work
              Assert.that(pos + curSymSize + 2 == pos + 4 + tmpSymSize,
                          "advanceToEntryThisSymbol needs more work");
            }
            pos += 4;
            curSymSize = tmpSymSize;
            curSymType = tmpSymType;
          }

          ///////////////////////////////////////////////////////////////////////
          //                                                                   //
          //                                                                   //
          // Symbols for (Intel) 16:32 Segmented and 32-bit Flat Architectures //
          //                                                                   //
          //                                                                   //
          ///////////////////////////////////////////////////////////////////////

          /////////////////////////
          // S_BPREL32 accessors //
          /////////////////////////

          public int getBPRelOffset() {
            symSeek(0);
            return readInt();
          }

          public int getBPRelType() {
            symSeek(4);
            return readInt();
          }

          public String getBPRelName() {
            return readLengthPrefixedStringAt(8);
          }

          ///////////////////////////////////////
          // S_LDATA32 and S_GDATA32 accessors //
          ///////////////////////////////////////

          public int getLGDataType() {
            symSeek(0);
            return readInt();
          }

          public int getLGDataOffset() {
            symSeek(4);
            return readInt();
          }

          public short getLGDataSegment() {
            symSeek(8);
            return readShort();
          }

          public String getLGDataName() {
            return readLengthPrefixedStringAt(10);
          }

          ///////////////////////
          // S_PUB32 accessors //
          ///////////////////////

          // FIXME: has the same format as the above; consider updating
          // documentation. No separate accessors provided.

          ///////////////////////////////////////
          // S_LPROC32 and S_GPROC32 accessors //
          ///////////////////////////////////////

          public DebugVC50SymbolIterator getLGProcParent() {
            int offs = getLGProcParentOffset();
            if (offs == 0) return null;
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getLGProcParentOffset() {
            symSeek(0);
            int offs = readInt();
            if (offs == 0) return 0;
            return base + offs;
          }

          public DebugVC50SymbolIterator getLGProcEnd() {
            int offs = getLGProcEndOffset();
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getLGProcEndOffset() {
            symSeek(4);
            int offs = readInt();
            if (Assert.ASSERTS_ENABLED) {
              Assert.that(offs != 0, "should not have null end offset for procedure symbols");
            }
            return base + offs;
          }

          public DebugVC50SymbolIterator getLGProcNext() {
            int offs = getLGProcNextOffset();
            if (offs == 0) return null;
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getLGProcNextOffset() {
            symSeek(8);
            int offs = readInt();
            if (offs == 0) return 0;
            return base + offs;
          }

          public int getLGProcLength() {
            symSeek(12);
            return readInt();
          }

          public int getLGProcDebugStart() {
            symSeek(16);
            return readInt();
          }

          public int getLGProcDebugEnd() {
            symSeek(20);
            return readInt();
          }

          public int getLGProcType() {
            symSeek(24);
            return readInt();
          }

          public int getLGProcOffset() {
            symSeek(28);
            return readInt();
          }

          public short getLGProcSegment() {
            symSeek(32);
            return readShort();
          }

          public byte getLGProcFlags() {
            symSeek(34);
            return readByte();
          }

          public String getLGProcName() {
            return readLengthPrefixedStringAt(35);
          }

          /////////////////////////
          // S_THUNK32 accessors //
          /////////////////////////

          public DebugVC50SymbolIterator getThunkParent() {
            int offs = getThunkParentOffset();
            if (offs == 0) return null;
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getThunkParentOffset() {
            symSeek(0);
            int offs = readInt();
            if (offs == 0) return 0;
            return base + offs;
          }

          public DebugVC50SymbolIterator getThunkEnd() {
            symSeek(4);
            int offs = readInt();
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getThunkEndOffset() {
            symSeek(4);
            int offs = readInt();
            if (Assert.ASSERTS_ENABLED) {
              Assert.that(offs != 0, "should not have null end offset for thunk symbols");
            }
            return base + offs;
          }

          public DebugVC50SymbolIterator getThunkNext() {
            int offs = getThunkNextOffset();
            if (offs == 0) return null;
            return new DebugVC50SymbolIteratorImpl(base, size, base + offs);
          }

          public int getThunkNextOffset() {
            symSeek(8);
            int offs = readInt();
            if (offs == 0) return 0;
            return base + offs;
          }

          public int getThunkOffset() {
            symSeek(12);
            return readInt();
          }

          public short getThunkSegment() {
            symSeek(16);
            return readShort();
          }

          public short getThunkLength() {
            symSeek(18);
            return readShort();
          }

          public byte getThunkType() {
            symSeek(20);
            return readByte();
          }

          public String getThunkName() {
            return readLengthPrefixedStringAt(21);
          }

          public short getThunkAdjustorThisDelta() {
            symSeek(21 + lengthPrefixedStringLengthAt(21));
            return readShort();
          }

          public String getThunkAdjustorTargetName() {
            return readLengthPrefixedStringAt(23 + lengthPrefixedStringLengthAt(21));
          }

          public short getThunkVCallDisplacement() {
            symSeek(21 + lengthPrefixedStringLengthAt(21));
            return readShort();
          }

          public int getThunkPCodeOffset() {
            symSeek(21 + lengthPrefixedStringLengthAt(21));
            return readInt();
          }

          public short getThunkPCodeSegment() {
            symSeek(25 + lengthPrefixedStringLengthAt(21));
            return readShort();
          }

          /////////////////////////
          // S_BLOCK32 accessors //
          /////////////////////////

          public DebugVC50SymbolIterator getBlockParent() {
            int offs = getBlockParentOffset();
            if (offs == 0) return null;
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getBlockParentOffset() {
            symSeek(0);
            int offs = readInt();
            if (offs == 0) return 0;
            return base + offs;
          }

          public DebugVC50SymbolIterator getBlockEnd() {
            symSeek(4);
            int offs = readInt();
            return new DebugVC50SymbolIteratorImpl(base, size, offs);
          }

          public int getBlockEndOffset() {
            symSeek(4);
            int offs = readInt();
            if (Assert.ASSERTS_ENABLED) {
              Assert.that(offs != 0, "should not have null end offset for block symbols");
            }
            return base + offs;
          }

          public int getBlockLength() {
            symSeek(8);
            return readInt();
          }

          public int getBlockOffset() {
            symSeek(12);
            return readInt();
          }

          public short getBlockSegment() {
            symSeek(16);
            return readShort();
          }

          public String getBlockName() {
            return readLengthPrefixedStringAt(18);
          }

          ////////////////////////
          // S_WITH32 accessors //
          ////////////////////////

          // FIXME: this is a Pascal construct; ignored for now

          /////////////////////////
          // S_LABEL32 accessors //
          /////////////////////////

          public int getLabelOffset() {
            symSeek(0);
            return readInt();
          }

          public short getLabelSegment() {
            symSeek(4);
            return readShort();
          }

          public byte getLabelFlags() {
            symSeek(6);
            return readByte();
          }

          public String getLabelName() {
            return readLengthPrefixedStringAt(7);
          }

          ////////////////////////////
          // S_CEXMODEL32 accessors //
          ////////////////////////////

          public int getChangeOffset() {
            symSeek(0);
            return readInt();
          }

          public short getChangeSegment() {
            symSeek(4);
            return readShort();
          }

          public short getChangeModel() {
            symSeek(6);
            return readShort();
          }

          ////////////////////////////
          // S_VFTTABLE32 accessors //
          ////////////////////////////

          public int getVTableRoot() {
            symSeek(0);
            return readInt();
          }

          public int getVTablePath() {
            symSeek(4);
            return readInt();
          }

          public int getVTableOffset() {
            symSeek(8);
            return readInt();
          }

          public short getVTableSegment() {
            symSeek(12);
            return readShort();
          }

          //////////////////////////
          // S_REGREL32 accessors //
          //////////////////////////

          public int getRegRelOffset() {
            symSeek(0);
            return readInt();
          }

          public int getRegRelType() {
            symSeek(4);
            return readInt();
          }

          public short getRegRelRegister() {
            symSeek(8);
            return readShort();
          }

          public String getRegRelName() {
            return readLengthPrefixedStringAt(10);
          }

          ///////////////////////////////////////////
          // S_LTHREAD32 and S_GTHREAD32 accessors //
          ///////////////////////////////////////////

          public int getLThreadType() {
            symSeek(0);
            return readInt();
          }

          public int getLThreadOffset() {
            symSeek(4);
            return readInt();
          }

          public short getLThreadSegment() {
            symSeek(8);
            return readShort();
          }

          public String getLThreadName() {
            return readLengthPrefixedStringAt(10);
          }

          //----------------------------------------------------------------------
          // Internals only below this point
          //

          private void symSeek(int offsetInSym) {
            seek(pos + HEADER_SIZE + offsetInSym);
          }

          private int numericLeafLengthAt(int offsetInSym) {
            return DebugVC50Impl.this.numericLeafLengthAt(pos + HEADER_SIZE + offsetInSym);
          }

          private int readIntNumericLeafAt(int offsetInSym) {
            return DebugVC50Impl.this.readIntNumericLeafAt(pos + HEADER_SIZE + offsetInSym);
          }

          private long readLongNumericLeafAt(int offsetInSym) {
            return DebugVC50Impl.this.readLongNumericLeafAt(pos + HEADER_SIZE + offsetInSym);
          }

          private float readFloatNumericLeafAt(int offsetInSym) {
            return DebugVC50Impl.this.readFloatNumericLeafAt(pos + HEADER_SIZE + offsetInSym);
          }

          private double readDoubleNumericLeafAt(int offsetInSym) {
            return DebugVC50Impl.this.readDoubleNumericLeafAt(pos + HEADER_SIZE + offsetInSym);
          }

          private int lengthPrefixedStringLengthAt(int offsetInSym) {
            return DebugVC50Impl.this.lengthPrefixedStringLengthAt(pos + HEADER_SIZE + offsetInSym);
          }

          private String readLengthPrefixedStringAt(int offsetInSym) {
            return DebugVC50Impl.this.readLengthPrefixedStringAt(pos + HEADER_SIZE + offsetInSym);
          }
        }

        class DebugVC50TypeIteratorImpl implements DebugVC50TypeIterator,
                        DebugVC50TypeLeafIndices, DebugVC50MemberAttributes, DebugVC50TypeEnums {
          private DebugVC50SSGlobalTypes parent;
          private int   base;
          private int   numTypes;
          private int   typeIndex;
          private int   typeRecordOffset;
          private int   typeStringOffset;
          private int   typeRecordSize;
          private int   typeStringLeaf;

          DebugVC50TypeIteratorImpl(DebugVC50SSGlobalTypes parent, int base, int numTypes) {
            this(parent, base, numTypes, 0, base);
          }

          private DebugVC50TypeIteratorImpl(DebugVC50SSGlobalTypes parent, int base, int numTypes, int curType, int offset) {
            this.parent = parent;
            this.base = base;
            this.numTypes = numTypes;
            this.typeIndex = curType;
            if (!done()) {
              typeRecordOffset = offset;
              loadTypeRecord();
            }
          }

          public boolean done() {
            return (typeIndex == numTypes);
          }

          public void next() throws NoSuchElementException {
            if (done()) throw new NoSuchElementException();
            ++typeIndex;
            if (!done()) {
              typeRecordOffset = parent.getTypeOffset(typeIndex);
              loadTypeRecord();
            }
          }

          public short getLength() {
            return (short) typeRecordSize;
          }

          public int getTypeIndex() {
            return biasTypeIndex(typeIndex);
          }

          public int getNumTypes() {
            return numTypes;
          }

          public boolean typeStringDone() {
            return (typeStringOffset - typeRecordOffset - 2) >= typeRecordSize;
          }

          public void typeStringNext() throws NoSuchElementException {
            if (typeStringDone()) throw new NoSuchElementException();
            typeStringOffset += typeStringLength();
            loadTypeString();
          }

          public int typeStringLeaf() {
            return typeStringLeaf;
          }

          public int typeStringOffset() {
            return typeStringOffset;
          }

          ///////////////////////////
          // LF_MODIFIER accessors //
          ///////////////////////////

          public int getModifierIndex() {
            typeSeek(2);
            return readInt();
          }

          public short getModifierAttribute() {
            typeSeek(6);
            return readShort();
          }

          //////////////////////////
          // LF_POINTER accessors //
          //////////////////////////

          public int getPointerType() {
            typeSeek(2);
            return readInt();
          }

          public int getPointerAttributes() {
            typeSeek(6);
            return readInt();
          }

          public int getPointerBasedOnTypeIndex() {
            typeSeek(10);
            return readInt();
          }

          public String getPointerBasedOnTypeName() {
            return readLengthPrefixedStringAt(14);
          }

          public int getPointerToMemberClass() {
            typeSeek(10);
            return readInt();
          }

          public short getPointerToMemberFormat() {
            typeSeek(14);
            return readShort();
          }

          ////////////////////////
          // LF_ARRAY accessors //
          ////////////////////////

          public int getArrayElementType() {
            typeSeek(2);
            return readInt();
          }

          public int getArrayIndexType() {
            typeSeek(6);
            return readInt();
          }

          public int getArrayLength() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(10);
          }

          public String getArrayName() {
            return readLengthPrefixedStringAt(10 + numericLeafLengthAt(10));
          }

          /////////////////////////////////////////
          // LF_CLASS and LF_STRUCTURE accessors //
          /////////////////////////////////////////

          public short getClassCount() {
            typeSeek(2);
            return readShort();
          }

          public short getClassProperty() {
            typeSeek(4);
            return readShort();
          }

          public int getClassFieldList() {
            typeSeek(6);
            return readInt();
          }

          public DebugVC50TypeIterator getClassFieldListIterator() {
            int index = unbiasTypeIndex(getClassFieldList());
            int offset = parent.getTypeOffset(index);
            return new DebugVC50TypeIteratorImpl(parent, base, numTypes, index, offset);
          }

          public int getClassDerivationList() {
            typeSeek(10);
            return readInt();
          }

          public int getClassVShape() {
            typeSeek(14);
            return readInt();
          }

          public int getClassSize() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(18);
          }

          public String getClassName() {
            return readLengthPrefixedStringAt(18 + numericLeafLengthAt(18));
          }

          ////////////////////////
          // LF_UNION accessors //
          ////////////////////////

          public short getUnionCount() {
            typeSeek(2);
            return readShort();
          }

          public short getUnionProperty() {
            typeSeek(4);
            return readShort();
          }

          public int getUnionFieldList() {
            typeSeek(6);
            return readInt();
          }

          public DebugVC50TypeIterator getUnionFieldListIterator() {
            int index = unbiasTypeIndex(getUnionFieldList());
            int offset = parent.getTypeOffset(index);
            return new DebugVC50TypeIteratorImpl(parent, base, numTypes, index, offset);
          }

          public int getUnionSize() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(10);
          }

          public String getUnionName() {
            return readLengthPrefixedStringAt(10 + numericLeafLengthAt(10));
          }

          ///////////////////////
          // LF_ENUM accessors //
          ///////////////////////

          public short getEnumCount() {
            typeSeek(2);
            return readShort();
          }

          public short getEnumProperty() {
            typeSeek(4);
            return readShort();
          }

          public int getEnumType() {
            typeSeek(6);
            return readInt();
          }

          public int getEnumFieldList() {
            typeSeek(10);
            return readInt();
          }

          public DebugVC50TypeIterator getEnumFieldListIterator() {
            int index = unbiasTypeIndex(getEnumFieldList());
            int offset = parent.getTypeOffset(index);
            return new DebugVC50TypeIteratorImpl(parent, base, numTypes, index, offset);
          }

          public String getEnumName() {
            return readLengthPrefixedStringAt(14);
          }

          ////////////////////////////
          // LF_PROCEDURE accessors //
          ////////////////////////////

          public int getProcedureReturnType() {
            typeSeek(2);
            return readInt();
          }

          public byte getProcedureCallingConvention() {
            typeSeek(6);
            return readByte();
          }

          public short getProcedureNumberOfParameters() {
            typeSeek(8);
            return readShort();
          }

          public int getProcedureArgumentList() {
            typeSeek(10);
            return readInt();
          }

          public DebugVC50TypeIterator getProcedureArgumentListIterator() {
            int index = unbiasTypeIndex(getProcedureArgumentList());
            int offset = parent.getTypeOffset(index);
            return new DebugVC50TypeIteratorImpl(parent, base, numTypes, index, offset);
          }

          ////////////////////////////
          // LF_MFUNCTION accessors //
          ////////////////////////////

          public int getMFunctionReturnType() {
            typeSeek(2);
            return readInt();
          }

          public int getMFunctionContainingClass() {
            typeSeek(6);
            return readInt();
          }

          public int getMFunctionThis() {
            typeSeek(10);
            return readInt();
          }

          public byte getMFunctionCallingConvention() {
            typeSeek(14);
            return readByte();
          }

          public short getMFunctionNumberOfParameters() {
            typeSeek(16);
            return readShort();
          }

          public int getMFunctionArgumentList() {
            typeSeek(18);
            return readInt();
          }

          public DebugVC50TypeIterator getMFunctionArgumentListIterator() {
            int index = unbiasTypeIndex(getMFunctionArgumentList());
            int offset = parent.getTypeOffset(index);
            return new DebugVC50TypeIteratorImpl(parent, base, numTypes, index, offset);
          }

          public int getMFunctionThisAdjust() {
            typeSeek(22);
            return readInt();
          }

          //////////////////////////
          // LF_VTSHAPE accessors //
          //////////////////////////

          public short getVTShapeCount() {
            typeSeek(2);
            return readShort();
          }

          public int getVTShapeDescriptor(int i) {
            typeSeek(4 + (i / 2));
            int val = readByte() & 0xFF;
            if ((i % 2) != 0) {
              val = val >> 4;
            }
            return val;
          }

          /////////////////////////
          // LF_BARRAY accessors //
          /////////////////////////

          public int getBasicArrayType() {
            typeSeek(2);
            return readInt();
          }

          ////////////////////////
          // LF_LABEL accessors //
          ////////////////////////

          public short getLabelAddressMode() {
            typeSeek(2);
            return readShort();
          }

          ///////////////////////////
          // LF_DIMARRAY accessors //
          ///////////////////////////

          public int getDimArrayType() {
            typeSeek(2);
            return readInt();
          }

          public int getDimArrayDimInfo() {
            typeSeek(6);
            return readInt();
          }

          public String getDimArrayName() {
            return readLengthPrefixedStringAt(10);
          }

          //////////////////////////
          // LF_VFTPATH accessors //
          //////////////////////////

          public int getVFTPathCount() {
            typeSeek(2);
            return readInt();
          }

          public int getVFTPathBase(int i) {
            typeSeek(6 + (4 * i));
            return readInt();
          }

          ///////////////////////
          // LF_SKIP accessors //
          ///////////////////////

          public int getSkipIndex() {
            typeSeek(2);
            return readInt();
          }

          //////////////////////////
          // LF_ARGLIST accessors //
          //////////////////////////

          public int getArgListCount() {
            typeSeek(2);
            return readInt();
          }

          public int getArgListType(int i) {
            typeSeek(6 + (4 * i));
            return readInt();
          }

          /////////////////////////
          // LF_DEFARG accessors //
          /////////////////////////

          public int getDefaultArgType() {
            typeSeek(2);
            return readInt();
          }

          public String getDefaultArgExpression() {
            return readLengthPrefixedStringAt(6);
          }

          //////////////////////////
          // LF_DERIVED accessors //
          //////////////////////////

          public int getDerivedCount() {
            typeSeek(2);
            return readInt();
          }

          public int getDerivedType(int i) {
            typeSeek(6);
            return readInt();
          }

          ///////////////////////////
          // LF_BITFIELD accessors //
          ///////////////////////////

          public int getBitfieldFieldType() {
            typeSeek(2);
            return readInt();
          }

          public byte getBitfieldLength() {
            typeSeek(6);
            return readByte();
          }

          public byte getBitfieldPosition() {
            typeSeek(7);
            return readByte();
          }

          ////////////////////////
          // LF_MLIST accessors //
          ////////////////////////

          public short getMListAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getMListLength() {
            return (getLength() - 6 - (isMListIntroducingVirtual() ? 4 : 0)) / 4;
          }

          public int getMListType(int i) {
            typeSeek(6 + 4 * i);
            return readInt();
          }

          public boolean isMListIntroducingVirtual() {
            return isIntroducingVirtual(getMListAttribute());
          }

          public int getMListVtabOffset() {
            typeSeek(6 + 4 * getMListLength());
            return readInt();
          }

          /////////////////////////
          // LF_REFSYM accessors //
          /////////////////////////

          public DebugVC50SymbolIterator getRefSym() {
            typeSeek(2);
            int len = readShort() & 0xFFFF;
            return new DebugVC50SymbolIteratorImpl(typeStringOffset + 2, len);
          }

          /////////////////////////
          // LF_BCLASS accessors //
          /////////////////////////

          public short getBClassAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getBClassType() {
            typeSeek(4);
            return readInt();
          }

          public int getBClassOffset() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(8);
          }

          //////////////////////////
          // LF_VBCLASS accessors //
          //////////////////////////

          public short getVBClassAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getVBClassBaseClassType() {
            typeSeek(4);
            return readInt();
          }

          public int getVBClassVirtualBaseClassType() {
            typeSeek(8);
            return readInt();
          }

          public int getVBClassVBPOff() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(12);
          }

          public int getVBClassVBOff() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(12 + numericLeafLengthAt(12));
          }

          ///////////////////////////
          // LF_IVBCLASS accessors //
          ///////////////////////////

          public short getIVBClassAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getIVBClassBType() {
            typeSeek(4);
            return readInt();
          }

          public int getIVBClassVBPType() {
            typeSeek(8);
            return readInt();
          }

          public int getIVBClassVBPOff() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(12);
          }

          public int getIVBClassVBOff() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(12 + numericLeafLengthAt(12));
          }

          ////////////////////////////
          // LF_ENUMERATE accessors //
          ////////////////////////////

          public short getEnumerateAttribute() {
            typeSeek(2);
            return readShort();
          }

          public long getEnumerateValue() {
            return readIntNumericLeafAt(4);
          }

          public String getEnumerateName() {
            return readLengthPrefixedStringAt(4 + numericLeafLengthAt(4));
          }

          ////////////////////////////
          // LF_FRIENDFCN accessors //
          ////////////////////////////

          public int getFriendFcnType() {
            typeSeek(4);
            return readInt();
          }

          public String getFriendFcnName() {
            return readLengthPrefixedStringAt(8);
          }

          ////////////////////////
          // LF_INDEX accessors //
          ////////////////////////

          public int getIndexValue() {
            typeSeek(4);
            return readInt();
          }

          public DebugVC50TypeIterator getIndexIterator() {
            int index = unbiasTypeIndex(getIndexValue());
            int offset = parent.getTypeOffset(index);
            return new DebugVC50TypeIteratorImpl(parent, base, numTypes, index, offset);
          }

          /////////////////////////
          // LF_MEMBER accessors //
          /////////////////////////

          public short getMemberAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getMemberType() {
            typeSeek(4);
            return readInt();
          }

          public int getMemberOffset() throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(8);
          }

          public String getMemberName() {
            return readLengthPrefixedStringAt(8 + numericLeafLengthAt(8));
          }

          ///////////////////////////
          // LF_STMEMBER accessors //
          ///////////////////////////

          public short getStaticAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getStaticType() {
            typeSeek(4);
            return readInt();
          }

          public String getStaticName() {
            return readLengthPrefixedStringAt(8);
          }

          /////////////////////////
          // LF_METHOD accessors //
          /////////////////////////

          public short getMethodCount() {
            typeSeek(2);
            return readShort();
          }

          public int getMethodList() {
            typeSeek(4);
            return readInt();
          }

          public String getMethodName() {
            return readLengthPrefixedStringAt(8);
          }

          /////////////////////////////
          // LF_NESTEDTYPE accessors //
          /////////////////////////////

          public int getNestedType() {
            typeSeek(4);
            return readInt();
          }

          public String getNestedName() {
            return readLengthPrefixedStringAt(8);
          }

          ///////////////////////////
          // LF_VFUNCTAB accessors //
          ///////////////////////////

          public int getVFuncTabType() {
            typeSeek(4);
            return readInt();
          }

          ////////////////////////////
          // LF_FRIENDCLS accessors //
          ////////////////////////////

          public int getFriendClsType() {
            typeSeek(4);
            return readInt();
          }

          ////////////////////////////
          // LF_ONEMETHOD accessors //
          ////////////////////////////

          public short getOneMethodAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getOneMethodType() {
            typeSeek(4);
            return readInt();
          }

          public boolean isOneMethodIntroducingVirtual() {
            return isIntroducingVirtual(getOneMethodAttribute());
          }

          public int getOneMethodVBaseOff() {
            typeSeek(8);
            return readInt();
          }

          public String getOneMethodName() {
            int baseLen = 8 + (isOneMethodIntroducingVirtual() ? 4 : 0);
            return readLengthPrefixedStringAt(baseLen);
          }

          ///////////////////////////
          // LF_VFUNCOFF accessors //
          ///////////////////////////

          public int getVFuncOffType() {
            typeSeek(4);
            return readInt();
          }

          public int getVFuncOffOffset() {
            typeSeek(8);
            return readInt();
          }

          ///////////////////////////////
          // LF_NESTEDTYPEEX accessors //
          ///////////////////////////////

          public short getNestedExAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getNestedExType() {
            typeSeek(4);
            return readInt();
          }

          public String getNestedExName() {
            return readLengthPrefixedStringAt(8);
          }

          ///////////////////////////////
          // LF_MEMBERMODIFY accessors //
          ///////////////////////////////

          public short getMemberModifyAttribute() {
            typeSeek(2);
            return readShort();
          }

          public int getMemberModifyType() {
            typeSeek(4);
            return readInt();
          }

          public String getMemberModifyName() {
            return readLengthPrefixedStringAt(8);
          }

          ////////////////////////////
          // Numeric Leaf accessors //
          ////////////////////////////

          public short getNumericTypeAt(int byteOffset) {
            typeSeek(byteOffset);
            return readShort();
          }

          public int getNumericLengthAt(int byteOffset)
            throws DebugVC50WrongNumericTypeException {
            return numericLeafLengthAt(byteOffset);
          }

          public int getNumericIntAt(int byteOffset)
            throws DebugVC50WrongNumericTypeException {
            return readIntNumericLeafAt(byteOffset);
          }

          public long getNumericLongAt(int byteOffset)
            throws DebugVC50WrongNumericTypeException {
            // FIXME
            throw new RuntimeException("Unimplemented");
          }

          public float getNumericFloatAt(int byteOffset)
            throws DebugVC50WrongNumericTypeException {
            // FIXME
            throw new RuntimeException("Unimplemented");
          }

          public double getNumericDoubleAt(int byteOffset)
            throws DebugVC50WrongNumericTypeException {
            // FIXME
            throw new RuntimeException("Unimplemented");
          }

          public byte[] getNumericDataAt(int byteOffset)
            throws DebugVC50WrongNumericTypeException {
            // FIXME
            throw new RuntimeException("Unimplemented");
          }

          //----------------------------------------------------------------------
          // Internals only below this point
          //

          private void loadTypeRecord() {
            seek(typeRecordOffset);
            typeRecordSize = readShort() & 0xFFFF;
            typeStringOffset = typeRecordOffset + 2;
            loadTypeString();
          }

          private void loadTypeString() {
            seek(typeStringOffset);
            int lo = readByte() & 0xFF;
            // See if it is one of the single-byte leaves
            if (lo >= LF_PAD0) {
              typeStringLeaf = lo;
            } else {
              int hi = readByte() & 0xFF;
              typeStringLeaf = (hi << 8) | lo;
            }
          }

          private void typeSeek(int offset) {
            seek(typeStringOffset + offset);
          }

          private int typeStringLength() {
            // LF_PAD
            if (typeStringLeaf >= 0xF0 && typeStringLeaf <= 0xFF) {
              return (typeStringLeaf - 0xF0);
            }

            switch (typeStringLeaf) {

              // Leaf indices for type records that can be referenced
              // from symbols:
            case LF_MODIFIER: return 8;
            case LF_POINTER: {
              int extraLen = 0;
              int attr = (getPointerAttributes() & POINTER_PTRTYPE_MASK) >> POINTER_PTRTYPE_SHIFT;
              int mode = (getPointerAttributes() & POINTER_PTRMODE_MASK) >> POINTER_PTRMODE_SHIFT;
              if (attr == POINTER_PTRTYPE_BASED_ON_TYPE) {
                extraLen = 4 + numericLeafLengthAt(typeStringOffset + 14);
              } else if (mode == POINTER_PTRMODE_PTR_TO_DATA_MEMBER ||
                         mode == POINTER_PTRMODE_PTR_TO_METHOD) {
                extraLen = 6;
              }
              return 10 + extraLen;
            }
            case LF_ARRAY: {
              int temp = 10 + numericLeafLengthAt(10);
              return temp + lengthPrefixedStringLengthAt(temp);
            }
            case LF_CLASS:
            case LF_STRUCTURE: {
              int temp = 18 + numericLeafLengthAt(18);
              return temp + lengthPrefixedStringLengthAt(temp);
            }
            case LF_UNION: {
              int temp = 10 + numericLeafLengthAt(10);
              return temp + lengthPrefixedStringLengthAt(temp);
            }
            case LF_ENUM: {
              return 14 + lengthPrefixedStringLengthAt(14);
            }
            case LF_PROCEDURE: return 14;
            case LF_MFUNCTION: return 26;
            case LF_VTSHAPE:   return 4 + ((getVTShapeCount() + 1) / 2);
            case LF_COBOL0:
            case LF_COBOL1:    throw new COFFException("COBOL symbols unimplemented");
            case LF_BARRAY:    return 6;
            case LF_LABEL:     return 4;
            case LF_NULL:      return 2;
            case LF_NOTTRAN:   return 2;
            case LF_DIMARRAY:  return 10 + lengthPrefixedStringLengthAt(10);
            case LF_VFTPATH:   return 6 + 4 * getVFTPathCount();
            case LF_PRECOMP:   return 14 + lengthPrefixedStringLengthAt(14);
            case LF_ENDPRECOMP: return 6;
            case LF_OEM:       throw new COFFException("OEM symbols unimplemented");
            case LF_TYPESERVER: return 10 + lengthPrefixedStringLengthAt(10);

            case LF_SKIP:      return 6 + numericLeafLengthAt(6);
            case LF_ARGLIST:   return 6 + 4 * getArgListCount();
            case LF_DEFARG:    return 6 + lengthPrefixedStringLengthAt(6);
              // case LF_FIELDLIST: throw new COFFException("Should not see LF_FIELDLIST leaf");
            case LF_FIELDLIST: return 2;
            case LF_DERIVED:   return 6 + 4 * getDerivedCount();
            case LF_BITFIELD:  return 8;
            case LF_METHODLIST: {
              return 6 + 4 * getMListLength() + (isMListIntroducingVirtual() ? 4 : 0);
            }
            case LF_DIMCONU:
            case LF_DIMCONLU:
            case LF_DIMVARU:
            case LF_DIMVARLU:  throw new COFFException("LF_DIMCONU, LF_DIMCONLU, LF_DIMVARU, and LF_DIMVARLU unsupported");
            case LF_REFSYM: {
              seek(typeStringOffset + 2);
              return 4 + readShort();
            }

            case LF_BCLASS:  return 8 + numericLeafLengthAt(8);
            case LF_VBCLASS:
            case LF_IVBCLASS: {
              int temp = 12 + numericLeafLengthAt(12);
              return temp + numericLeafLengthAt(temp);
            }
            case LF_ENUMERATE: {
              int temp = 4 + numericLeafLengthAt(4);
              return temp + lengthPrefixedStringLengthAt(temp);
            }
            case LF_FRIENDFCN: return 8 + lengthPrefixedStringLengthAt(8);
            case LF_INDEX: return 8;
            case LF_MEMBER: {
              int temp = 8 + numericLeafLengthAt(8);
              return temp + lengthPrefixedStringLengthAt(temp);
            }
            case LF_STMEMBER: return 8 + lengthPrefixedStringLengthAt(8);
            case LF_METHOD:   return 8 + lengthPrefixedStringLengthAt(8);
            case LF_NESTTYPE: return 8 + lengthPrefixedStringLengthAt(8);
            case LF_VFUNCTAB: return 8;
            case LF_FRIENDCLS: return 8;
            case LF_ONEMETHOD: {
              int baseLen = 8 + (isOneMethodIntroducingVirtual() ? 4 : 0);
              return baseLen + lengthPrefixedStringLengthAt(baseLen);
            }
            case LF_VFUNCOFF:  return 12;
            case LF_NESTTYPEEX: return 8 + lengthPrefixedStringLengthAt(8);
            case LF_MEMBERMODIFY: return 8 + lengthPrefixedStringLengthAt(8);

            // Should not encounter numeric leaves with this routine
            case LF_CHAR:
            case LF_SHORT:
            case LF_USHORT:
            case LF_LONG:
            case LF_ULONG:
            case LF_REAL32:
            case LF_REAL64:
            case LF_REAL80:
            case LF_REAL128:
            case LF_QUADWORD:
            case LF_UQUADWORD:
            case LF_REAL48:
            case LF_COMPLEX32:
            case LF_COMPLEX64:
            case LF_COMPLEX80:
            case LF_COMPLEX128:
            case LF_VARSTRING:  throw new RuntimeException("Unexpected numeric leaf " + typeStringLeaf +
                                                           "in type string");
            default:
              throw new COFFException("Unrecognized leaf " + typeStringLeaf + " in type string at offset " +
                                      typeStringOffset);
            }
          }

          private boolean isIntroducingVirtual(int mprop) {
            int masked = mprop & MEMATTR_MPROP_MASK;
            return ((masked == MEMATTR_MPROP_INTRODUCING_VIRTUAL) ||
                    (masked == MEMATTR_MPROP_PURE_INTRODUCING_VIRTUAL));
          }

          private int numericLeafLengthAt(int offset) {
            return DebugVC50Impl.this.numericLeafLengthAt(typeStringOffset + offset);
          }

          private int readIntNumericLeafAt(int offset) {
            return DebugVC50Impl.this.readIntNumericLeafAt(typeStringOffset + offset);
          }

          private int lengthPrefixedStringLengthAt(int offset) {
            return DebugVC50Impl.this.lengthPrefixedStringLengthAt(typeStringOffset + offset);
          }

          private String readLengthPrefixedStringAt(int offset) {
            return DebugVC50Impl.this.readLengthPrefixedStringAt(typeStringOffset + offset);
          }
        }

        private int numericLeafLengthAt(int absoluteOffset) throws DebugVC50WrongNumericTypeException {
          seek(absoluteOffset);
          int leaf = readShort() & 0xFFFF;
          if (leaf < 0x8000) return 2;
          switch (leaf) {
          case LF_CHAR:       return 3;
          case LF_SHORT:
          case LF_USHORT:     return 4;
          case LF_LONG:
          case LF_ULONG:      return 6;
          case LF_REAL32:     return 6;
          case LF_REAL64:     return 10;
          case LF_REAL80:     return 12;
          case LF_REAL128:    return 18;
          case LF_QUADWORD:
          case LF_UQUADWORD:  return 18;
          case LF_REAL48:     return 8;
          case LF_COMPLEX32:  return 10;
          case LF_COMPLEX64:  return 18;
          case LF_COMPLEX80:  return 26;
          case LF_COMPLEX128: return 66;
            // FIXME: figure out format of variable-length strings
          case LF_VARSTRING:  return 4 + readIntNumericLeafAt(absoluteOffset + 2);

          default:
            throw new DebugVC50WrongNumericTypeException("Illegal numeric leaf index " + leaf +
                                                         " at offset " + absoluteOffset);
          }
        }

        private int readIntNumericLeafAt(int absoluteOffset) throws DebugVC50WrongNumericTypeException {
          seek(absoluteOffset);
          int leaf = readShort() & 0xFFFF;
          if (leaf < 0x8000) return leaf;
          switch (leaf) {
          case LF_CHAR:       return readByte() & 0xFF;
          case LF_SHORT:
          case LF_USHORT:     return readShort() & 0xFFFF;
          case LF_LONG:
          case LF_ULONG:      return readInt();

          default:
            throw new DebugVC50WrongNumericTypeException("Illegal numeric leaf index " + leaf);
          }
        }

        private long readLongNumericLeafAt(int absoluteOffset) throws DebugVC50WrongNumericTypeException {
          seek(absoluteOffset);
          int leaf = readShort() & 0xFFFF;
          if (leaf < 0x8000) return leaf;
          switch (leaf) {
          case LF_CHAR:       return readByte() & 0xFF;
          case LF_SHORT:
          case LF_USHORT:     return readShort() & 0xFFFF;
          case LF_LONG:
          case LF_ULONG:      return readInt() & 0xFFFFFFFF;
          case LF_QUADWORD:
          case LF_UQUADWORD:  return readLong();

          default:
            throw new DebugVC50WrongNumericTypeException("Illegal numeric leaf index " + leaf);
          }
        }

        private float readFloatNumericLeafAt(int absoluteOffset) throws DebugVC50WrongNumericTypeException {
          seek(absoluteOffset);
          int leaf = readShort() & 0xFFFF;
          if (leaf != LF_REAL32) {
            throw new DebugVC50WrongNumericTypeException("Illegal numeric leaf index " + leaf);
          }
          return readFloat();
        }

        private double readDoubleNumericLeafAt(int absoluteOffset) throws DebugVC50WrongNumericTypeException {
          seek(absoluteOffset);
          int leaf = readShort() & 0xFFFF;
          if (leaf != LF_REAL64) {
            throw new DebugVC50WrongNumericTypeException("Illegal numeric leaf index " + leaf);
          }
          return readDouble();
        }

        private int lengthPrefixedStringLengthAt(int absoluteOffset) {
          // NOTE: the format of length-prefixed strings is not well
          // specified. There is a LF_VARSTRING numeric leaf (the
          // format of which is also not specified), but it seems that
          // most length-prefixed strings are comprised of a single
          // byte length followed by that many bytes of data.
          seek(absoluteOffset);
          int len = readByte() & 0xFF;
          return 1 + len;
        }

        private String readLengthPrefixedStringAt(int absoluteOffset) {
          // NOTE: it isn't clear whether LF_VARSTRING numeric leaves
          // ever show up, or in general what happens when the length
          // of the string is > 255 (FIXME)
          seek(absoluteOffset);
          int len = readByte() & 0xFF;
          byte[] res = new byte[len];
          int numRead = readBytes(res);
          if (numRead != len) {
            throw new COFFException("Error reading length prefixed string in symbol at offset " +
                                    absoluteOffset);
          }
          try {
            return new String(res, US_ASCII);
          } catch (UnsupportedEncodingException e) {
            throw new COFFException(e);
          }
        }

        private int unbiasTypeIndex(int index) {
          return index - 0x1000;
        }

        private int biasTypeIndex(int index) {
          return index + 0x1000;
        }
      } // Class DebugVC50Impl

      class SectionHeaderImpl implements SectionHeader {
        private String name;
        private int    virtualSize;
        private int    virtualAddress;
        private int    sizeOfRawData;
        private int    pointerToRawData;
        private int    pointerToRelocations;
        private int    pointerToLineNumbers;
        private short  numberOfRelocations;
        private short  numberOfLineNumbers;
        private int    characteristics;
        private MemoizedObject[] relocations;
        private MemoizedObject[] lineNumbers;

        public SectionHeaderImpl(int offset) throws COFFException {
          seek(offset);

          // FIXME: compute name lazily

          // Read name
          byte[] tmpName = new byte[8];
          int numRead = readBytes(tmpName);
          if (numRead != 8) {
            throw new COFFException("Error reading name of section header at offset " + offset);
          }
          if (tmpName[0] == (byte) '/') {
            // Long name; must find real value in string table
            int index = 0;
            try {
              index = Integer.parseInt(new String(tmpName, 1, tmpName.length - 1, US_ASCII));
            } catch (NumberFormatException e) {
              throw new COFFException("Error parsing string table index of name of section header " +
                                      "at offset " + offset);
            } catch (UnsupportedEncodingException e) {
              throw new COFFException(e);
            }
            // Look up in string table
            // FIXME: this index value is assumed to be in the valid range
            name = getStringTable().get(index);
          } else {
            try {
              int length = 0;
              // find last non-NULL
              for (; length < tmpName.length && tmpName[length] != '\0';) {
                length++;
              }
              // don't include NULL chars in returned name String
              name = new String(tmpName, 0, length, US_ASCII);
            } catch (UnsupportedEncodingException e) {
              throw new COFFException(e);
            }
          }
          virtualSize          = readInt();
          virtualAddress       = readInt();
          sizeOfRawData        = readInt();
          pointerToRawData     = readInt();
          pointerToRelocations = readInt();
          pointerToLineNumbers = readInt();
          numberOfRelocations  = readShort();
          numberOfLineNumbers  = readShort();
          characteristics      = readInt();

          // Set up relocations
          relocations = new MemoizedObject[numberOfRelocations];
          for (int i = 0; i < numberOfRelocations; i++) {
            final int relocOffset = pointerToRelocations + i * RELOCATION_SIZE;
            relocations[i] = new MemoizedObject() {
                public Object computeValue() {
                  return new COFFRelocationImpl(relocOffset);
                }
              };
          }

          // Set up line numbers
          lineNumbers = new MemoizedObject[numberOfLineNumbers];
          for (int i = 0; i < numberOfLineNumbers; i++) {
            final int lineNoOffset = pointerToLineNumbers + i * LINE_NUMBER_SIZE;
            lineNumbers[i] = new MemoizedObject() {
                public Object computeValue() {
                  return new COFFLineNumberImpl(lineNoOffset);
                }
              };
          }
        }

        public String getName() { return name; }
        public int getSize() { return virtualSize; }
        public int getVirtualAddress() { return virtualAddress; }
        public int getSizeOfRawData() { return sizeOfRawData; }
        public int getPointerToRawData() { return pointerToRawData; }
        public int getPointerToRelocations() { return pointerToRelocations; }
        public int getPointerToLineNumbers() { return pointerToLineNumbers; }
        public short getNumberOfRelocations() { return numberOfRelocations; }
        public short getNumberOfLineNumbers() { return numberOfLineNumbers; }
        public int getSectionFlags() { return characteristics; }
        public boolean hasSectionFlag(int flag ) {
          return ((characteristics & flag) != 0);
        }
        public COFFRelocation getCOFFRelocation(int index) {
          return (COFFRelocation) relocations[index].getValue();
        }
        public COFFLineNumber getCOFFLineNumber(int index) {
          return (COFFLineNumber) lineNumbers[index];
        }
      }

      class COFFSymbolImpl implements COFFSymbol, COFFSymbolConstants {
        private int    offset;
        private String name;
        private int    value;
        private short  sectionNumber;
        private short  type;
        private byte   storageClass;
        private byte   numberOfAuxSymbols;
        private MemoizedObject auxFunctionDefinitionRecord = new MemoizedObject() {
            public Object computeValue() {
              return new AuxFunctionDefinitionRecordImpl(offset + SYMBOL_SIZE);
            }
          };
        private MemoizedObject auxBfEfRecord = new MemoizedObject() {
            public Object computeValue() {
              return new AuxBfEfRecordImpl(offset + SYMBOL_SIZE);
            }
          };
        private MemoizedObject auxWeakExternalRecord = new MemoizedObject() {
            public Object computeValue() {
              return new AuxWeakExternalRecordImpl(offset + SYMBOL_SIZE);
            }
          };
        private MemoizedObject auxFileRecord = new MemoizedObject() {
            public Object computeValue() {
              return new AuxFileRecordImpl(offset + SYMBOL_SIZE);
            }
          };
        private MemoizedObject auxSectionDefinitionsRecord = new MemoizedObject() {
            public Object computeValue() {
              return new AuxSectionDefinitionsRecordImpl(offset + SYMBOL_SIZE);
            }
          };

        public COFFSymbolImpl(int offset) throws COFFException {
          this.offset = offset;
          seek(offset);

          // Parse name
          byte[] tmpName = new byte[8];
          int numRead = readBytes(tmpName);
          if (numRead != 8) {
            throw new COFFException("Error reading name of symbol at offset " + offset);
          }
          if ((tmpName[0] == 0) &&
              (tmpName[1] == 0) &&
              (tmpName[2] == 0) &&
              (tmpName[3] == 0)) {
            // It's an offset into the string table.
            // FIXME: not sure about byte ordering...
            int stringOffset = (tmpName[4] << 24 |
                                tmpName[5] << 16 |
                                tmpName[6] <<  8 |
                                tmpName[7]);
            // FIXME: stringOffset is assumed to be in the valid range
            name = getStringTable().getAtOffset(stringOffset);
          }

          value = readInt();
          sectionNumber = readShort();
          type = readShort();
          storageClass = readByte();
          numberOfAuxSymbols = readByte();
        }

        public int getOffset()              { return offset; }
        public String getName()             { return name; }
        public int getValue()               { return value; }
        public short getSectionNumber()     { return sectionNumber; }
        public short getType()              { return type; }
        public byte getStorageClass()       { return storageClass; }
        public byte getNumberOfAuxSymbols() { return numberOfAuxSymbols; }
        public boolean isFunctionDefinition() {
          return ((getStorageClass() == IMAGE_SYM_CLASS_EXTERNAL) &&
                  ((getType() >>> 8) == IMAGE_SYM_DTYPE_FUNCTION) &&
                  (getSectionNumber() > 0));
        }
        public AuxFunctionDefinitionRecord getAuxFunctionDefinitionRecord() {
          return (AuxFunctionDefinitionRecord) auxFunctionDefinitionRecord.getValue();
        }
        public boolean isBfOrEfSymbol() {
          return ((getName().equals(".bf") || getName().equals(".ef")) &&
                  (getStorageClass() == IMAGE_SYM_CLASS_FUNCTION));
        }
        public AuxBfEfRecord getAuxBfEfRecord() {
          return (AuxBfEfRecord) auxBfEfRecord.getValue();
        }
        public boolean isWeakExternal() {
          return ((getStorageClass() == IMAGE_SYM_CLASS_EXTERNAL) &&
                  (getSectionNumber() == IMAGE_SYM_UNDEFINED) &&
                  (getValue() == 0));
        }
        public AuxWeakExternalRecord getAuxWeakExternalRecord() {
          return (AuxWeakExternalRecord) auxWeakExternalRecord.getValue();
        }
        public boolean isFile() {
          return ((getName().equals(".file")) &&
                  (getStorageClass() == IMAGE_SYM_CLASS_FILE));
        }
        public AuxFileRecord getAuxFileRecord() {
          return (AuxFileRecord) auxFileRecord.getValue();
        }
        public boolean isSectionDefinition() {
          // FIXME: not sure how to ensure that symbol name is the
          // name of a section.
          return ((getName().charAt(0) == '.') &&
                  (getStorageClass() == IMAGE_SYM_CLASS_STATIC));
        }
        public AuxSectionDefinitionsRecord getAuxSectionDefinitionsRecord() {
          return (AuxSectionDefinitionsRecord) auxSectionDefinitionsRecord.getValue();
        }
      }

      class AuxFunctionDefinitionRecordImpl implements AuxFunctionDefinitionRecord {
        private int tagIndex;
        private int totalSize;
        private int pointerToLineNumber;
        private int pointerToNextFunction;

        AuxFunctionDefinitionRecordImpl(int offset) {
          seek(offset);
          tagIndex              = readInt();
          totalSize             = readInt();
          // NOTE zero-basing of this index
          pointerToLineNumber   = readInt() - 1;
          pointerToNextFunction = readInt();
        }

        public int getTagIndex()              { return tagIndex; }
        public int getTotalSize()             { return totalSize; }
        public int getPointerToLineNumber()   { return pointerToLineNumber; }
        public int getPointerToNextFunction() { return pointerToNextFunction; }
        public int getType()                  { return FUNCTION_DEFINITION; }
      }

      class AuxBfEfRecordImpl implements AuxBfEfRecord {
        private short lineNumber;
        private int   pointerToNextFunction;

        AuxBfEfRecordImpl(int offset) {
          seek(offset);
          readInt();
          lineNumber = readShort();
          readInt();
          readShort();
          pointerToNextFunction = readInt();
        }

        public short getLineNumber()          { return lineNumber; }
        public int getPointerToNextFunction() { return pointerToNextFunction; }
        public int getType()                  { return BF_EF_RECORD; }
      }

      class AuxWeakExternalRecordImpl implements AuxWeakExternalRecord {
        private int tagIndex;
        private int characteristics;

        AuxWeakExternalRecordImpl(int offset) {
          seek(offset);
          tagIndex = readInt();
          characteristics = readInt();
        }

        public int getTagIndex()        { return tagIndex; }
        public int getCharacteristics() { return characteristics; }
        public int getType()            { return WEAK_EXTERNAL; }
      }

      class AuxFileRecordImpl implements AuxFileRecord {
        private String name;

        AuxFileRecordImpl(int offset) {
          seek(offset);
          byte[] tmpName = new byte[18];
          int numRead = readBytes(tmpName);
          if (numRead != 18) {
            throw new COFFException("Error reading auxiliary file record at offset " + offset);
          }
          try {
            name = new String(tmpName, US_ASCII);
          } catch (UnsupportedEncodingException e) {
            throw new COFFException(e);
          }
        }

        public String getName() { return name; }
        public int getType()    { return FILE; }
      }

      class AuxSectionDefinitionsRecordImpl implements AuxSectionDefinitionsRecord {
        private int length;
        private short numberOfRelocations;
        private short numberOfLineNumbers;
        private int checkSum;
        private short number;
        private byte selection;

        AuxSectionDefinitionsRecordImpl(int offset) {
          seek(offset);
          length = readInt();
          numberOfRelocations = readShort();
          numberOfLineNumbers = readShort();
          checkSum = readInt();
          number = readShort();
          selection = readByte();
        }

        public int   getLength()              { return length; }
        public short getNumberOfRelocations() { return numberOfRelocations; }
        public short getNumberOfLineNumbers() { return numberOfLineNumbers; }
        public int   getCheckSum()            { return checkSum; }
        public short getNumber()              { return number; }
        public byte  getSelection()           { return selection; }
        public int getType()                  { return SECTION_DEFINITION; }
      }

      class COFFRelocationImpl implements COFFRelocation {
        private int virtualAddress;
        private int symbolTableIndex;
        private short type;

        COFFRelocationImpl(int offset) {
          seek(offset);
          virtualAddress   = readInt();
          symbolTableIndex = readInt();
          type             = readShort();
        }

        public int   getVirtualAddress()     { return virtualAddress; }
        public int   getSymbolTableIndex()   { return symbolTableIndex; }
        public short getType()               { return type; }
      }

      class COFFLineNumberImpl implements COFFLineNumber {
        private int   type;
        private short lineNumber;

        COFFLineNumberImpl(int offset) {
          seek(offset);
          type       = readInt();
          lineNumber = readShort();
        }

        public int getType() {
          return type;
        }

        public short getLineNumber() {
          return lineNumber;
        }
      }

      class StringTable {
        class COFFString {
          String str;
          int    offset;

          COFFString(String str, int offset) {
            this.str = str; this.offset = offset;
          }
        }

        COFFString[] strings;

        StringTable(int offset) {
          if (offset == 0) {
            // no String Table
            strings = new COFFString[0];
            return;
          }

          seek(offset);
          int length = readInt();  // length includes itself
          byte[] data = new byte[length - 4];
          int numBytesRead = readBytes(data);
          if (numBytesRead != data.length) {
            throw new COFFException("Error reading string table (read " +
                                    numBytesRead + " bytes, expected to read " + data.length + ")");
          }
          int numStrings = 0;
          int ptr = 0;
          for (ptr = 0; ptr < data.length; ptr++) {
            if (data[ptr] == 0) {
              numStrings++;
            }
          }
          strings = new COFFString[numStrings];
          int lastPtr = 0;
          ptr = 0;
          for (int i = 0; i < numStrings; i++) {
            while (data[ptr] != 0) {
              ptr++;
            }
            try {
              strings[i] = new COFFString(new String(data, lastPtr, ptr - lastPtr, US_ASCII),
                                          offset + ptr + 4);
            } catch (UnsupportedEncodingException e) {
              throw new COFFException(e);
            }
            ptr++;
            lastPtr = ptr;
          }
        }

        int getNum() {
          return strings.length;
        }

        String get(int i) {
          return strings[i].str;
        }

        /** This version takes an absolute offset in the file */
        String getAtOffset(int offset) {
          int i = Arrays.binarySearch(strings, new COFFString(null, offset),
                                      new Comparator<>() {
                                          public int compare(COFFString s1, COFFString s2) {
                                            if (s1.offset == s2.offset) {
                                              return 0;
                                            } else if (s1.offset < s2.offset) {
                                              return -1;
                                            } else {
                                              return 1;
                                            }
                                          }
                                        });
          if (i < 0) {
            throw new COFFException("No string found at file offset " + offset);
          }
          return strings[i].str;
        }
      }
    }

    void initialize() throws COFFException {
      // Figure out whether this file is an object file or an image
      // (either executable or DLL).
      seek(0x3c); // Error here probably indicates file format error
      try {
        int peOffset = readInt();
        seek(peOffset);
        if ((readByte() == (byte) 'P') &&
            (readByte() == (byte) 'E') &&
            (readByte() == (byte) 0) &&
            (readByte() == (byte) 0)) {
          isImage = true;
          imageHeaderOffset = getFilePointer();
        }
      }
      catch (COFFException e) {
        // Expect failures here if not image file.
      }
    }

    byte readByteAt(long offset) throws COFFException {
      seek(offset);
      return readByte();
    }

    byte readByte() throws COFFException {
      try {
        return file.readByte();
      } catch (IOException e) {
        throw new COFFException(e.toString() + " at offset 0x" +
                                Long.toHexString(filePos), e);
      }
    }

    int readBytesAt(long offset, byte[] b) throws COFFException {
      seek(offset);
      return readBytes(b);
    }

    int readBytes(byte[] b) throws COFFException {
      try {
        return file.read(b);
      } catch (IOException e) {
        throw new COFFException(e.toString() + " at offset 0x" +
                                Long.toHexString(filePos), e);
      }
    }

    /** NOTE: reads little-endian short */
    short readShortAt(long offset) throws COFFException {
      seek(offset);
      return readShort();
    }

    /** NOTE: reads little-endian short */
    short readShort() throws COFFException {
      try {
        return byteSwap(file.readShort());
      } catch (IOException e) {
        throw new COFFException(e.toString() + " at offset 0x" +
                                Long.toHexString(filePos), e);
      }
    }

    /** NOTE: reads little-endian int */
    int readIntAt(long offset) throws COFFException {
      seek(offset);
      return readInt();
    }

    /** NOTE: reads little-endian int */
    int readInt() throws COFFException {
      try {
        return byteSwap(file.readInt());
      } catch (IOException e) {
        throw new COFFException(e.toString() + " at offset 0x" +
                                Long.toHexString(filePos), e);
      }
    }

    /** NOTE: reads little-endian long */
    long readLongAt(long offset) throws COFFException {
      seek(offset);
      return readLong();
    }

    /** NOTE: reads little-endian long */
    long readLong() throws COFFException {
      try {
        return byteSwap(file.readLong());
      } catch (IOException e) {
        throw new COFFException(e.toString() + " at offset 0x" +
                                Long.toHexString(filePos), e);
      }
    }

    /** NOTE: reads little-endian float */
    float readFloat() throws COFFException {
      int i = readInt();
      return Float.intBitsToFloat(i);
    }

    /** NOTE: reads little-endian double */
    double readDouble() throws COFFException {
      long l = readLong();
      return Double.longBitsToDouble(l);
    }

    String readCString() throws COFFException {
      List<Byte> data = new ArrayList<>();
      byte b = 0;
      while ((b = readByte()) != 0) {
        data.add(b);
      }
      byte[] bytes = new byte[data.size()];
      for (int i = 0; i < data.size(); i++) {
        bytes[i] = (data.get(i)).byteValue();
      }
      try {
        return new String(bytes, US_ASCII);
      } catch (UnsupportedEncodingException e) {
        throw new COFFException(e);
      }
    }

    void seek(long offset) throws COFFException {
      try {
        filePos = offset;
        file.seek(offset);
      } catch (IOException e) {
        throw new COFFException(e.toString() + " at offset 0x" +
                                Long.toHexString(offset), e);
      }
    }

    long getFilePointer() throws COFFException {
      try {
        return file.getFilePointer();
      } catch (IOException e) {
        throw new COFFException(e);
      }
    }

    short byteSwap(short arg) {
      return (short) ((arg << 8) | ((arg >>> 8) & 0xFF));
    }

    int byteSwap(int arg) {
      return (((int) byteSwap((short) arg)) << 16) | (((int) (byteSwap((short) (arg >>> 16)))) & 0xFFFF);
    }

    long byteSwap(long arg) {
      return ((((long) byteSwap((int) arg)) << 32) | (((long) byteSwap((int) (arg >>> 32))) & 0xFFFFFFFF));
    }

    public void close() throws COFFException {
      try {
        file.close();
      } catch (IOException e) {
        throw new COFFException(e);
      }
    }
  }
}
