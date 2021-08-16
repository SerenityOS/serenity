/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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

#ifndef CPU_S390_RELOCINFO_S390_HPP
#define CPU_S390_RELOCINFO_S390_HPP

//----------------------------
//  relocInfo layout
//----------------------------

// This description should be contained in code/relocInfo.hpp
// but was put here to minimize shared code diffs.

// Relocation information for a nmethod is stored in compressed
// form in an array of element type short int (16 bits).
// Each array element constitutes one relocInfo record.
// The layout of one such record is described here.

// +------------+---+---+------------------------------+
// |    type    |  fmt  |      offset/offset_unit      |
// +------------+---+---+------------------------------+
//
// |<-- value_width (16) ----------------------------->|
// |<type_width>|<-- nontype_width (12) -------------->|
//      (4)
// |            |<--+-->|<-- offset_width (10) ------->|
//              /       \
//             /   (2)   \
//            /<--format->\
//            |    width  |


// only for type == data_prefix_tag:
// +------------+---+---+------------------------------+
// |    type    |   |              data                |
// +------------+---+---+------------------------------+
// |     15     |<->|<-- datalen_width (11) ---------->|
//                |
//                +--datalen_tag (1)

// relocType
//   The type field holds a value of relocType (which is
//   an enum of all possible relocation types). Currently,
//   there are 16 distinct relocation types, requiring
//   type_width to be (at least) 4.
// relocFormat
//   The format field holds a value of relocFormat (which is
//   an enum of all possible relocation formats). Currently,
//   there are 4 distinct relocation formats, requiring
//   format_width to be (at least) 2.
// offset
//   Each relocInfo is related to one specific address in the CodeBlob.
//   The address always points to the first byte of the target instruction.
//   It does NOT refer directly to the relocation subfield or embedded constant.
//   offset contains the distance of this relocInfo from the previous one.
//   offset is scaled by offset_unit (the platform-specific instruction
//   alignment requirement) to maximize the encodable distance.
//   To obtain the absolute address in the CodeBlob the relocInfo is
//   related to, you have to iterate over all relocInfos from the
//   beginning, and then use RelocIterator::addr() to get the address.

// relocType == data_prefix_tag
//   These are relocInfo records containing inline data that belongs to
//   the next non-data relocInfo record. Usage of that inline data is
//   specific and private to that relocInfo record.
//   For details refer to code/relocInfo.hpp


  // machine-dependent parts of class relocInfo
 private:
  enum {
    // Instructions are HW (2-byte) aligned on z/Architecture.
    offset_unit        =  2,

    // Encodes Assembler::disp32_operand vs. Assembler::imm64_operand.
    // (Assembler::call32_operand is used on call instructions only.)
    format_width       =  2
  };

 public:

  enum relocFormat {
    no_format           = 0,
    uncompressed_format = 0,  // Relocation is for a regular oop.
    compressed_format   = 1,  // Relocation is for a narrow (compressed) oop or klass.
                              // Similar to relocInfo::narrow_oop_in_const.
    pcrel_addr_format   = 2,  // Relocation is for the target LOCATION of a pc-relative instruction.
    pcrel_data_format   = 3   // Relocation is for the target data of a pc-relative instruction.
  };

  // This platform has no oops in the code that are not also
  // listed in the oop section.
  static bool mustIterateImmediateOopsInCode() { return false; }

#endif // CPU_S390_RELOCINFO_S390_HPP
