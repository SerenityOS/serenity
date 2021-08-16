/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_ELFFILE_HPP
#define SHARE_UTILITIES_ELFFILE_HPP

#if !defined(_WINDOWS) && !defined(__APPLE__) && !defined(_AIX)

#if defined(__OpenBSD__)
#include <sys/exec_elf.h>
#else
#include <elf.h>
#endif
#include <stdio.h>

#ifdef _LP64

typedef Elf64_Half      Elf_Half;
typedef Elf64_Word      Elf_Word;
typedef Elf64_Off       Elf_Off;
typedef Elf64_Addr      Elf_Addr;

typedef Elf64_Ehdr      Elf_Ehdr;
typedef Elf64_Shdr      Elf_Shdr;
typedef Elf64_Phdr      Elf_Phdr;
typedef Elf64_Sym       Elf_Sym;

#if !defined(_ALLBSD_SOURCE) || defined(__APPLE__)
#define ELF_ST_TYPE ELF64_ST_TYPE
#endif

#else

typedef Elf32_Half      Elf_Half;
typedef Elf32_Word      Elf_Word;
typedef Elf32_Off       Elf_Off;
typedef Elf32_Addr      Elf_Addr;

typedef Elf32_Ehdr      Elf_Ehdr;
typedef Elf32_Shdr      Elf_Shdr;
typedef Elf32_Phdr      Elf_Phdr;
typedef Elf32_Sym       Elf_Sym;

#if !defined(_ALLBSD_SOURCE) || defined(__APPLE__)
#define ELF_ST_TYPE ELF32_ST_TYPE
#endif
#endif

#include "globalDefinitions.hpp"
#include "memory/allocation.hpp"
#include "utilities/decoder.hpp"

class ElfStringTable;
class ElfSymbolTable;
class ElfFuncDescTable;

// ELF section, may or may not have cached data
class ElfSection {
private:
  Elf_Shdr      _section_hdr;
  void*         _section_data;
  NullDecoder::decoder_status _stat;
public:
  ElfSection(FILE* fd, const Elf_Shdr& hdr);
  ~ElfSection();

  NullDecoder::decoder_status status() const { return _stat; }

  const Elf_Shdr* section_header() const { return &_section_hdr; }
  const void*     section_data()   const { return (const void*)_section_data; }
private:
  // load this section.
  // it return no_error, when it fails to cache the section data due to lack of memory
  NullDecoder::decoder_status load_section(FILE* const file, const Elf_Shdr& hdr);
};

class FileReader : public StackObj {
protected:
  FILE* const _fd;
public:
  FileReader(FILE* const fd) : _fd(fd) {};
  bool read(void* buf, size_t size);
  int  read_buffer(void* buf, size_t size);
  bool set_position(long offset);
};

// Mark current position, so we can get back to it after
// reads.
class MarkedFileReader : public FileReader {
private:
  long  _marked_pos;
public:
  MarkedFileReader(FILE* const fd);
  ~MarkedFileReader();

  bool has_mark() const { return _marked_pos >= 0; }
};

// ElfFile is basically an elf file parser, which can lookup the symbol
// that is the nearest to the given address.
// Beware, this code is called from vm error reporting code, when vm is already
// in "error" state, so there are scenarios, lookup will fail. We want this
// part of code to be very defensive, and bait out if anything went wrong.
class ElfFile: public CHeapObj<mtInternal> {
  friend class ElfDecoder;

private:
  // link ElfFiles
  ElfFile*          _next;

  // Elf file
  char*             _filepath;
  FILE*             _file;

  // Elf header
  Elf_Ehdr          _elfHdr;

  // symbol tables
  ElfSymbolTable*   _symbol_tables;

  // regular string tables
  ElfStringTable*   _string_tables;

  // section header string table, used for finding section name
  ElfStringTable*   _shdr_string_table;

  // function descriptors table
  ElfFuncDescTable* _funcDesc_table;

  NullDecoder::decoder_status  _status;

public:
  ElfFile(const char* filepath);
  ~ElfFile();

  bool decode(address addr, char* buf, int buflen, int* offset);

  const char* filepath() const {
    return _filepath;
  }

  bool same_elf_file(const char* filepath) const {
    assert(filepath != NULL, "null file path");
    return (_filepath != NULL && !strcmp(filepath, _filepath));
  }

  NullDecoder::decoder_status get_status() const {
    return _status;
  }

  // Returns true if the elf file is marked NOT to require an executable stack,
  // or if the file could not be opened.
  // Returns false if the elf file requires an executable stack, the stack flag
  // is not set at all, or if the file can not be read.
  // On systems other than linux it always returns false.
  static bool specifies_noexecstack(const char* filepath) NOT_LINUX({ return false; });
private:
  // sanity check, if the file is a real elf file
  static bool is_elf_file(Elf_Ehdr&);

  // parse this elf file
  NullDecoder::decoder_status parse_elf(const char* filename);

  // load string, symbol and function descriptor tables from the elf file
  NullDecoder::decoder_status load_tables();

  ElfFile*  next() const { return _next; }
  void set_next(ElfFile* file) { _next = file; }

  // find a section by name, return section index
  // if there is no such section, return -1
  int section_by_name(const char* name, Elf_Shdr& hdr);

  // string tables are stored in a linked list
  void add_string_table(ElfStringTable* table);

  // symbol tables are stored in a linked list
  void add_symbol_table(ElfSymbolTable* table);

  // return a string table at specified section index
  ElfStringTable* get_string_table(int index);


  FILE* const fd() const { return _file; }

  // Cleanup string, symbol and function descriptor tables
  void cleanup_tables();

public:
  // For whitebox test
  static bool _do_not_cache_elf_section;
};

#endif // !_WINDOWS && !__APPLE__

#endif // SHARE_UTILITIES_ELFFILE_HPP
