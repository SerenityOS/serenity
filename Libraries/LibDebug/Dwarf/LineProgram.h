/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/MemoryStream.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Debug::Dwarf {

class LineProgram {
public:
    explicit LineProgram(InputMemoryStream& stream);

    struct LineInfo {
        u32 address { 0 };
        String file;
        size_t line { 0 };
    };

    const Vector<LineInfo>& lines() const { return m_lines; }

private:
    void parse_unit_header();
    void parse_source_directories();
    void parse_source_files();
    void run_program();

    void append_to_line_info();
    void reset_registers();

    void handle_extended_opcode();
    void handle_standard_opcode(u8 opcode);
    void handle_sepcial_opcode(u8 opcode);

    struct [[gnu::packed]] UnitHeader32
    {
        u32 length;
        u16 version;
        u32 header_length;
        u8 min_instruction_length;
        u8 default_is_stmt;
        i8 line_base;
        u8 line_range;
        u8 opcode_base;
        u8 std_opcode_lengths[12];
    };

    enum StandardOpcodes {
        Copy = 1,
        AdvancePc,
        AdvanceLine,
        SetFile,
        SetColumn,
        NegateStatement,
        SetBasicBlock,
        ConstAddPc,
        FixAdvancePc,
        SetProlougeEnd,
        SetEpilogueBegin,
        SetIsa
    };

    enum ExtendedOpcodes {
        EndSequence = 1,
        SetAddress,
        DefineFile,
        SetDiscriminator,
    };

    struct FileEntry {
        String name;
        size_t directory_index { 0 };
    };

    static constexpr u16 DWARF_VERSION = 3;
    static constexpr u8 SPECIAL_OPCODES_BASE = 13;

    InputMemoryStream& m_stream;

    size_t m_unit_offset { 0 };
    UnitHeader32 m_unit_header {};
    Vector<String> m_source_directories;
    Vector<FileEntry> m_source_files;

    // The registers of the "line program" virtual machine
    u32 m_address { 0 };
    size_t m_line { 0 };
    size_t m_file_index { 0 };
    bool m_is_statement { false };

    Vector<LineInfo> m_lines;
};

}
