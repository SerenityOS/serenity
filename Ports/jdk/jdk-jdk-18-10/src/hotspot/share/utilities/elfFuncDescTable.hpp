/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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

#ifndef SHARE_UTILITIES_ELFFUNCDESCTABLE_HPP
#define SHARE_UTILITIES_ELFFUNCDESCTABLE_HPP

#if !defined(_WINDOWS) && !defined(__APPLE__)


#include "memory/allocation.hpp"
#include "utilities/decoder.hpp"
#include "utilities/elfFile.hpp"

/*

On PowerPC-64 (and other architectures like for example IA64) a pointer to a
function is not just a plain code address, but instead a pointer to a so called
function descriptor (which is simply a structure containing 3 pointers).
This fact is also reflected in the ELF ABI for PowerPC-64.

On architectures like x86 or SPARC, the ELF symbol table contains the start
address and size of an object. So for example for a function object (i.e. type
'STT_FUNC') the symbol table's 'st_value' and 'st_size' fields directly
represent the starting address and size of that function. On PPC64 however, the
symbol table's 'st_value' field only contains an index into another, PPC64
specific '.opd' (official procedure descriptors) section, while the 'st_size'
field still holds the size of the corresponding function. In order to get the
actual start address of a function, it is necessary to read the corresponding
function descriptor entry in the '.opd' section at the corresponding index and
extract the start address from there.

That's exactly what this 'ElfFuncDescTable' class is used for. If the HotSpot
runs on a PPC64 machine, and the corresponding ELF files contains an '.opd'
section (which is actually mandatory on PPC64) it will be read into an object
of type 'ElfFuncDescTable' just like the string and symbol table sections.
Later on, during symbol lookup in 'ElfSymbolTable::lookup()' this function
descriptor table will be used if available to find the real function address.

All this is how things work today (2013) on contemporary Linux distributions
(i.e. SLES 10) and new version of GCC (i.e. > 4.0). However there is a history,
and it goes like this:

In SLES 9 times (sometimes before GCC 3.4) gcc/ld on PPC64 generated two
entries in the symbol table for every function. The value of the symbol with
the name of the function was the address of the function descriptor while the
dot '.' prefixed name was reserved to hold the actual address of that function
(http://refspecs.linuxfoundation.org/ELF/ppc64/PPC-elf64abi-1.9.html#FUNC-DES).

For a C-function 'foo' this resulted in two symbol table entries like this
(extracted from the output of 'readelf -a <lib.so>'):

Section Headers:
  [ 9] .text             PROGBITS         0000000000000a20  00000a20
       00000000000005a0  0000000000000000  AX       0     0     16
  [21] .opd              PROGBITS         00000000000113b8  000013b8
       0000000000000138  0000000000000000  WA       0     0     8

Symbol table '.symtab' contains 86 entries:
   Num:    Value          Size Type    Bind   Vis      Ndx Name
    76: 00000000000114c0    24 FUNC    GLOBAL DEFAULT   21 foo
    78: 0000000000000bb0    76 FUNC    GLOBAL DEFAULT    9 .foo

You can see now that the '.foo' entry actually points into the '.text' segment
('Ndx'=9) and its value and size fields represent the functions actual address
and size. On the other hand, the entry for plain 'foo' points into the '.opd'
section ('Ndx'=21) and its value and size fields are the index into the '.opd'
section and the size of the corresponding '.opd' section entry (3 pointers on
PPC64).

These so called 'dot symbols' were dropped around gcc 3.4 from GCC and BINUTILS,
see http://gcc.gnu.org/ml/gcc-patches/2004-08/msg00557.html.
But nevertheless it may still be necessary to support both formats because we
either run on an old system or because it is possible at any time that functions
appear in the stack trace which come from old-style libraries.

Therefore we not only have to check for the presence of the function descriptor
table during symbol lookup in 'ElfSymbolTable::lookup()'. We additionally have
to check that the symbol table entry references the '.opd' section. Only in
that case we can resolve the actual function address from there. Otherwise we
use the plain 'st_value' field from the symbol table as function address. This
way we can also lookup the symbols in old-style ELF libraries (although we get
the 'dotted' versions in that case). However, if present, the 'dot' will be
conditionally removed on PPC64 from the symbol in 'ElfDecoder::demangle()' in
decoder_linux.cpp.

Notice that we can not reliably get the function address from old-style
libraries because the 'st_value' field of the symbol table entries which point
into the '.opd' section denote the size of the corresponding '.opd' entry and
not that of the corresponding function. This has changed for the symbol table
entries in new-style libraries as described at the beginning of this
documentation.

*/

class ElfFuncDescTable: public CHeapObj<mtInternal> {
  friend class ElfFile;
private:
  // holds the complete function descriptor section if
  // we can allocate enough memory
  ElfSection          _section;

  // file contains string table
  FILE* const         _file;

  // The section index of this function descriptor (i.e. '.opd') section in the ELF file
  const int           _index;

  NullDecoder::decoder_status  _status;
public:
  ElfFuncDescTable(FILE* file, Elf_Shdr shdr, int index);
  ~ElfFuncDescTable();

  // return the function address for the function descriptor at 'index' or NULL on error
  address lookup(Elf_Word index);

  int get_index() const { return _index; };

  NullDecoder::decoder_status get_status() const { return _status; };

private:
  address* cached_func_descs() const { return (address*)_section.section_data(); }
};

#endif // !_WINDOWS && !__APPLE__

#endif // SHARE_UTILITIES_ELFFUNCDESCTABLE_HPP
