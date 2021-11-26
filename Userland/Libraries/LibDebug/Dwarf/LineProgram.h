/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DwarfInfo.h"
#include <AK/FlyString.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>

namespace Debug::Dwarf {

struct [[gnu::packed]] LineProgramUnitHeader32Common {
    u32 length;
    u16 version;
};

struct [[gnu::packed]] LineProgramUnitHeader32V4Ext {
    u32 header_length;
    u8 min_instruction_length;
    u8 max_instruction_length;
    u8 default_is_stmt;
    i8 line_base;
    u8 line_range;
    u8 opcode_base;
};

struct [[gnu::packed]] LineProgramUnitHeader32V5Ext {
    u8 address_size;
    u8 segment_selector_size;
    u32 header_length;
    u8 min_instruction_length;
    u8 max_instruction_length;
    u8 default_is_stmt;
    i8 line_base;
    u8 line_range;
    u8 opcode_base;
};

struct [[gnu::packed]] LineProgramUnitHeader32 {
    LineProgramUnitHeader32Common common;

    union {
        LineProgramUnitHeader32V4Ext v4;
        LineProgramUnitHeader32V5Ext v5;
    };

    u8 std_opcode_lengths[13];

    size_t header_size() const
    {
        return sizeof(common) + ((common.version <= 4) ? sizeof(v4) : sizeof(v5)) + (opcode_base() - 1) * sizeof(std_opcode_lengths[0]);
    }

    u32 length() const { return common.length; }
    u16 version() const { return common.version; }
    u32 header_length() const { return (common.version <= 4) ? v4.header_length : v5.header_length; }
    u8 min_instruction_length() const { return (common.version <= 4) ? v4.min_instruction_length : v5.min_instruction_length; }
    u8 default_is_stmt() const { return (common.version <= 4) ? v4.default_is_stmt : v5.default_is_stmt; }
    i8 line_base() const { return (common.version <= 4) ? v4.line_base : v5.line_base; }
    u8 line_range() const { return (common.version <= 4) ? v4.line_range : v5.line_range; }
    u8 opcode_base() const { return (common.version <= 4) ? v4.opcode_base : v5.opcode_base; }
};

enum class ContentType {
    Path = 1,
    DirectoryIndex = 2,
    Timestamp = 3,
    Size = 4,
    MD5 = 5,
    LoUser = 0x2000,
    HiUser = 0x3fff,
};

struct PathEntryFormat {
    ContentType type;
    AttributeDataForm form;
};

struct PathEntry {
    String path;
    size_t directory_index { 0 };
};

enum class PathListType {
    Directories,
    Filenames,
};

inline InputStream& operator>>(InputStream& stream, LineProgramUnitHeader32& header)
{
    stream.read_or_error(Bytes { &header.common, sizeof(header.common) });
    if (header.common.version <= 4)
        stream.read_or_error(Bytes { &header.v4, sizeof(header.v4) });
    else
        stream.read_or_error(Bytes { &header.v5, sizeof(header.v5) });
    stream.read_or_error(Bytes { &header.std_opcode_lengths, min(sizeof(header.std_opcode_lengths), (header.opcode_base() - 1) * sizeof(header.std_opcode_lengths[0])) });
    return stream;
}

class LineProgram {
    AK_MAKE_NONCOPYABLE(LineProgram);
    AK_MAKE_NONMOVABLE(LineProgram);

public:
    explicit LineProgram(DwarfInfo& dwarf_info, InputMemoryStream& stream);

    struct LineInfo {
        FlatPtr address { 0 };
        FlyString file;
        size_t line { 0 };
    };

    Vector<LineInfo> const& lines() const { return m_lines; }

    struct DirectoryAndFile {
        FlyString directory;
        FlyString filename;
    };
    DirectoryAndFile get_directory_and_file(size_t file_index) const;

    struct FileEntry {
        FlyString name;
        size_t directory_index { 0 };
    };
    Vector<FileEntry> const& source_files() const { return m_source_files; }

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

    void parse_path_entries(Function<void(PathEntry& entry)> callback, PathListType list_type);

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

    static constexpr u16 MIN_DWARF_VERSION = 3;
    static constexpr u16 MAX_DWARF_VERSION = 5;

    DwarfInfo& m_dwarf_info;
    InputMemoryStream& m_stream;

    size_t m_unit_offset { 0 };
    LineProgramUnitHeader32 m_unit_header {};
    Vector<String> m_source_directories;
    Vector<FileEntry> m_source_files;

    // The registers of the "line program" virtual machine
    FlatPtr m_address { 0 };
    size_t m_line { 0 };
    size_t m_file_index { 0 };
    bool m_is_statement { false };
    bool m_basic_block { false };
    bool m_prologue_end { false };

    Vector<LineInfo> m_lines;
};

}
