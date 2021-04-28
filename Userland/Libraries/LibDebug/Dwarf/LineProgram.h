/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>

namespace Debug::Dwarf {

struct [[gnu::packed]] LineProgramUnitHeader32 {
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

class LineProgram {
public:
    explicit LineProgram(InputMemoryStream& stream);

    struct LineInfo {
        u32 address { 0 };
        FlyString file;
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
    void handle_special_opcode(u8 opcode);

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
        FlyString name;
        size_t directory_index { 0 };
    };

    static constexpr u16 DWARF_VERSION = 3;
    static constexpr u8 SPECIAL_OPCODES_BASE = 13;

    InputMemoryStream& m_stream;

    size_t m_unit_offset { 0 };
    LineProgramUnitHeader32 m_unit_header {};
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
