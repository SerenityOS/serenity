/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"

#if !defined(_WINDOWS) && !defined(__APPLE__)

#include <string.h>
#include <stdio.h>
#include <limits.h>
#include <new>

#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "utilities/decoder.hpp"
#include "utilities/elfFile.hpp"
#include "utilities/elfFuncDescTable.hpp"
#include "utilities/elfStringTable.hpp"
#include "utilities/elfSymbolTable.hpp"
#include "utilities/ostream.hpp"

// For test only, disable elf section cache and force to read from file directly.
bool ElfFile::_do_not_cache_elf_section = false;

ElfSection::ElfSection(FILE* fd, const Elf_Shdr& hdr) : _section_data(NULL) {
  _stat = load_section(fd, hdr);
}

ElfSection::~ElfSection() {
  if (_section_data != NULL) {
    os::free(_section_data);
  }
}

NullDecoder::decoder_status ElfSection::load_section(FILE* const fd, const Elf_Shdr& shdr) {
  memcpy((void*)&_section_hdr, (const void*)&shdr, sizeof(shdr));

  if (ElfFile::_do_not_cache_elf_section) {
    log_debug(decoder)("Elf section cache is disabled");
    return NullDecoder::no_error;
  }

  _section_data = os::malloc(shdr.sh_size, mtInternal);
  // No enough memory for caching. It is okay, we can try to read from
  // file instead.
  if (_section_data == NULL) return NullDecoder::no_error;

  MarkedFileReader mfd(fd);
  if (mfd.has_mark() &&
      mfd.set_position(shdr.sh_offset) &&
      mfd.read(_section_data, shdr.sh_size)) {
    return NullDecoder::no_error;
  } else {
    os::free(_section_data);
    _section_data = NULL;
    return NullDecoder::file_invalid;
  }
}

bool FileReader::read(void* buf, size_t size) {
  assert(buf != NULL, "no buffer");
  assert(size > 0, "no space");
  return fread(buf, size, 1, _fd) == 1;
}

int FileReader::read_buffer(void* buf, size_t size) {
  assert(buf != NULL, "no buffer");
  assert(size > 0, "no space");
  return fread(buf, 1, size, _fd);
}

bool FileReader::set_position(long offset) {
  return fseek(_fd, offset, SEEK_SET) == 0;
}

MarkedFileReader::MarkedFileReader(FILE* fd) : FileReader(fd) {
  _marked_pos = ftell(fd);
}

MarkedFileReader::~MarkedFileReader() {
  if (_marked_pos != -1) {
    set_position(_marked_pos);
  }
}

ElfFile::ElfFile(const char* filepath) :
  _next(NULL), _filepath(NULL), _file(NULL),
  _symbol_tables(NULL), _string_tables(NULL), _shdr_string_table(NULL), _funcDesc_table(NULL),
  _status(NullDecoder::no_error) {
  memset(&_elfHdr, 0, sizeof(_elfHdr));

  int len = strlen(filepath) + 1;
  _filepath = (char*)os::malloc(len * sizeof(char), mtInternal);
  if (_filepath == NULL) {
    _status = NullDecoder::out_of_memory;
    return;
  }
  strcpy(_filepath, filepath);

  _status = parse_elf(filepath);

  // we no longer need section header string table
  if (_shdr_string_table != NULL) {
    delete _shdr_string_table;
    _shdr_string_table = NULL;
  }
}

ElfFile::~ElfFile() {
  if (_shdr_string_table != NULL) {
    delete _shdr_string_table;
  }

  cleanup_tables();

  if (_file != NULL) {
    fclose(_file);
  }

  if (_filepath != NULL) {
    os::free((void*)_filepath);
  }

  if (_next != NULL) {
    delete _next;
  }
}

void ElfFile::cleanup_tables() {
  if (_string_tables != NULL) {
    delete _string_tables;
    _string_tables = NULL;
  }

  if (_symbol_tables != NULL) {
    delete _symbol_tables;
    _symbol_tables = NULL;
  }

  if (_funcDesc_table != NULL) {
    delete _funcDesc_table;
    _funcDesc_table = NULL;
  }
}

NullDecoder::decoder_status ElfFile::parse_elf(const char* filepath) {
  assert(filepath, "null file path");

  _file = fopen(filepath, "r");
  if (_file != NULL) {
    return load_tables();
  } else {
    return NullDecoder::file_not_found;
  }
}

//Check elf header to ensure the file is valid.
bool ElfFile::is_elf_file(Elf_Ehdr& hdr) {
  return (ELFMAG0 == hdr.e_ident[EI_MAG0] &&
      ELFMAG1 == hdr.e_ident[EI_MAG1] &&
      ELFMAG2 == hdr.e_ident[EI_MAG2] &&
      ELFMAG3 == hdr.e_ident[EI_MAG3] &&
      ELFCLASSNONE != hdr.e_ident[EI_CLASS] &&
      ELFDATANONE != hdr.e_ident[EI_DATA]);
}

NullDecoder::decoder_status ElfFile::load_tables() {
  assert(_file, "file not open");
  assert(!NullDecoder::is_error(_status), "already in error");

  FileReader freader(fd());
  // read elf file header
  if (!freader.read(&_elfHdr, sizeof(_elfHdr))) {
    return NullDecoder::file_invalid;
  }

  // Check signature
  if (!is_elf_file(_elfHdr)) {
    return NullDecoder::file_invalid;
  }

  // walk elf file's section headers, and load string tables
  Elf_Shdr shdr;
  if (!freader.set_position(_elfHdr.e_shoff)) {
    return NullDecoder::file_invalid;
  }

  for (int index = 0; index < _elfHdr.e_shnum; index ++) {
    if (!freader.read(&shdr, sizeof(shdr))) {
      return NullDecoder::file_invalid;
    }

    if (shdr.sh_type == SHT_STRTAB) {
      // string tables
      ElfStringTable* table = new (std::nothrow) ElfStringTable(fd(), shdr, index);
      if (table == NULL) {
        return NullDecoder::out_of_memory;
      }
      if (index == _elfHdr.e_shstrndx) {
        assert(_shdr_string_table == NULL, "Only set once");
        _shdr_string_table = table;
      } else {
        add_string_table(table);
      }
    } else if (shdr.sh_type == SHT_SYMTAB || shdr.sh_type == SHT_DYNSYM) {
      // symbol tables
      ElfSymbolTable* table = new (std::nothrow) ElfSymbolTable(fd(), shdr);
      if (table == NULL) {
        return NullDecoder::out_of_memory;
      }
      add_symbol_table(table);
    }
  }
#if defined(PPC64) && !defined(ABI_ELFv2)
  // Now read the .opd section wich contains the PPC64 function descriptor table.
  // The .opd section is only available on PPC64 (see for example:
  // http://refspecs.linuxfoundation.org/LSB_3.1.1/LSB-Core-PPC64/LSB-Core-PPC64/specialsections.html)
  // so this code should do no harm on other platforms but because of performance reasons we only
  // execute it on PPC64 platforms.
  // Notice that we can only find the .opd section after we have successfully read in the string
  // tables in the previous loop, because we need to query the name of each section which is
  // contained in one of the string tables (i.e. the one with the index m_elfHdr.e_shstrndx).

  // Reset the file pointer
  int sect_index = section_by_name(".opd", shdr);

  if (sect_index == -1) {
    return NullDecoder::file_invalid;
  }

  _funcDesc_table = new (std::nothrow) ElfFuncDescTable(_file, shdr, sect_index);
  if (_funcDesc_table == NULL) {
      return NullDecoder::out_of_memory;
  }
#endif
  return NullDecoder::no_error;
}

int ElfFile::section_by_name(const char* name, Elf_Shdr& hdr) {
  assert(name != NULL, "No section name");
  size_t len = strlen(name) + 1;
  ResourceMark rm;
  char* buf = NEW_RESOURCE_ARRAY(char, len);
  if (buf == NULL) {
    return -1;
  }

  assert(_shdr_string_table != NULL, "Section header string table should be loaded");
  ElfStringTable* const table = _shdr_string_table;
  MarkedFileReader mfd(fd());
  if (!mfd.has_mark() || !mfd.set_position(_elfHdr.e_shoff)) return -1;

  int sect_index = -1;
  for (int index = 0; index < _elfHdr.e_shnum; index ++) {
    if (!mfd.read((void*)&hdr, sizeof(hdr))) {
      break;
    }
    if (table->string_at(hdr.sh_name, buf, len)) {
      if (strncmp(buf, name, len) == 0) {
        sect_index = index;
        break;
      }
    }
  }
  return sect_index;
}

bool ElfFile::decode(address addr, char* buf, int buflen, int* offset) {
  // something already went wrong, just give up
  if (NullDecoder::is_error(_status)) {
    return false;
  }

  int string_table_index;
  int pos_in_string_table;
  int off = INT_MAX;
  bool found_symbol = false;
  ElfSymbolTable* symbol_table = _symbol_tables;

  while (symbol_table != NULL) {
    if (symbol_table->lookup(addr, &string_table_index, &pos_in_string_table, &off, _funcDesc_table)) {
      found_symbol = true;
      break;
    }
    symbol_table = symbol_table->next();
  }
  if (!found_symbol) {
    return false;
  }

  ElfStringTable* string_table = get_string_table(string_table_index);

  if (string_table == NULL) {
    _status = NullDecoder::file_invalid;
    return false;
  }
  if (offset) *offset = off;

  return string_table->string_at(pos_in_string_table, buf, buflen);
}

void ElfFile::add_symbol_table(ElfSymbolTable* table) {
  if (_symbol_tables == NULL) {
    _symbol_tables = table;
  } else {
    table->set_next(_symbol_tables);
    _symbol_tables = table;
  }
}

void ElfFile::add_string_table(ElfStringTable* table) {
  if (_string_tables == NULL) {
    _string_tables = table;
  } else {
    table->set_next(_string_tables);
    _string_tables = table;
  }
}

ElfStringTable* ElfFile::get_string_table(int index) {
  ElfStringTable* p = _string_tables;
  while (p != NULL) {
    if (p->index() == index) return p;
    p = p->next();
  }
  return NULL;
}

#endif // !_WINDOWS && !__APPLE__
