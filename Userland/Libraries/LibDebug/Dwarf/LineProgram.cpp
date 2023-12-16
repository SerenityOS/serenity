/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LineProgram.h"
#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/LEB128.h>
#include <AK/StringBuilder.h>
#include <LibDebug/Dwarf/DwarfInfo.h>

namespace Debug::Dwarf {

LineProgram::LineProgram(DwarfInfo& dwarf_info, SeekableStream& stream, size_t unit_offset)
    : m_dwarf_info(dwarf_info)
    , m_stream(stream)
    , m_unit_offset(unit_offset)
{
}

ErrorOr<NonnullOwnPtr<LineProgram>> LineProgram::create(DwarfInfo& dwarf_info, SeekableStream& stream)
{
    auto offset = TRY(stream.tell());
    auto program = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LineProgram(dwarf_info, stream, offset)));
    TRY(program->parse_unit_header());
    TRY(program->parse_source_directories());
    TRY(program->parse_source_files());
    TRY(program->run_program());
    return program;
}

ErrorOr<void> LineProgram::parse_unit_header()
{
    m_unit_header = TRY(m_stream.read_value<LineProgramUnitHeader32>());

    VERIFY(m_unit_header.version() >= MIN_DWARF_VERSION && m_unit_header.version() <= MAX_DWARF_VERSION);
    VERIFY(m_unit_header.opcode_base() <= sizeof(m_unit_header.std_opcode_lengths) / sizeof(m_unit_header.std_opcode_lengths[0]) + 1);

    dbgln_if(DWARF_DEBUG, "unit length: {}", m_unit_header.length());
    return {};
}

ErrorOr<void> LineProgram::parse_path_entries(Function<void(PathEntry& entry)> callback, PathListType list_type)
{
    if (m_unit_header.version() >= 5) {
        auto path_entry_format_count = TRY(m_stream.read_value<u8>());

        Vector<PathEntryFormat> format_descriptions;

        for (u8 i = 0; i < path_entry_format_count; i++) {
            UnderlyingType<ContentType> content_type = TRY(m_stream.read_value<LEB128<UnderlyingType<ContentType>>>());

            UnderlyingType<AttributeDataForm> data_form = TRY(m_stream.read_value<LEB128<UnderlyingType<AttributeDataForm>>>());

            format_descriptions.empend(static_cast<ContentType>(content_type), static_cast<AttributeDataForm>(data_form));
        }

        size_t paths_count = TRY(m_stream.read_value<LEB128<size_t>>());

        for (size_t i = 0; i < paths_count; i++) {
            PathEntry entry;
            for (auto& format_description : format_descriptions) {
                auto value = TRY(m_dwarf_info.get_attribute_value(format_description.form, 0, m_stream));
                switch (format_description.type) {
                case ContentType::Path:
                    entry.path = TRY(value.as_string());
                    break;
                case ContentType::DirectoryIndex:
                    entry.directory_index = value.as_unsigned();
                    break;
                default:
                    dbgln_if(DWARF_DEBUG, "Unhandled path list attribute: {}", to_underlying(format_description.type));
                }
            }
            callback(entry);
        }
    } else {
        while (true) {
            StringBuilder builder;
            while (auto c = TRY(m_stream.read_value<char>()))
                TRY(builder.try_append(c));
            auto path = builder.to_byte_string();
            if (path.length() == 0)
                break;
            dbgln_if(DWARF_DEBUG, "path: {}", path);
            PathEntry entry;
            entry.path = path;
            if (list_type == PathListType::Filenames) {
                size_t directory_index = TRY(m_stream.read_value<LEB128<size_t>>());
                TRY(m_stream.read_value<LEB128<size_t>>()); // skip modification time
                TRY(m_stream.read_value<LEB128<size_t>>()); // skip file size
                entry.directory_index = directory_index;
                dbgln_if(DWARF_DEBUG, "file: {}, directory index: {}", path, directory_index);
            }
            callback(entry);
        }
    }

    return {};
}

ErrorOr<void> LineProgram::parse_source_directories()
{
    if (m_unit_header.version() < 5) {
        m_source_directories.append(".");
    }

    TRY(parse_path_entries([this](PathEntry& entry) {
        m_source_directories.append(entry.path);
    },
        PathListType::Directories));

    return {};
}

ErrorOr<void> LineProgram::parse_source_files()
{
    if (m_unit_header.version() < 5) {
        m_source_files.append({ ".", 0 });
    }

    TRY(parse_path_entries([this](PathEntry& entry) {
        m_source_files.append({ entry.path, entry.directory_index });
    },
        PathListType::Filenames));

    return {};
}

void LineProgram::append_to_line_info()
{
    dbgln_if(DWARF_DEBUG, "appending line info: {:p}, {}:{}", m_address, m_source_files[m_file_index].name, m_line);
    if (!m_is_statement)
        return;

    if (m_file_index >= m_source_files.size())
        return;

    auto const& directory = m_source_directories[m_source_files[m_file_index].directory_index];

    StringBuilder full_path(directory.length() + m_source_files[m_file_index].name.length() + 1);
    full_path.append(directory);
    full_path.append('/');
    full_path.append(m_source_files[m_file_index].name);

    m_lines.append({ m_address, DeprecatedFlyString { full_path.string_view() }, m_line });
}

void LineProgram::reset_registers()
{
    m_address = 0;
    m_line = 1;
    m_file_index = 1;
    m_is_statement = m_unit_header.default_is_stmt() == 1;
}

ErrorOr<void> LineProgram::handle_extended_opcode()
{
    size_t length = TRY(m_stream.read_value<LEB128<size_t>>());

    auto sub_opcode = TRY(m_stream.read_value<u8>());

    switch (sub_opcode) {
    case ExtendedOpcodes::EndSequence: {
        append_to_line_info();
        reset_registers();
        break;
    }
    case ExtendedOpcodes::SetAddress: {
        VERIFY(length == sizeof(size_t) + 1);
        m_address = TRY(m_stream.read_value<FlatPtr>());
        dbgln_if(DWARF_DEBUG, "SetAddress: {:p}", m_address);
        break;
    }
    case ExtendedOpcodes::SetDiscriminator: {
        dbgln_if(DWARF_DEBUG, "SetDiscriminator");
        [[maybe_unused]] size_t discriminator = TRY(m_stream.read_value<LEB128<size_t>>());
        break;
    }
    default:
        dbgln("Encountered unknown sub opcode {} at stream offset {:p}", sub_opcode, TRY(m_stream.tell()));
        VERIFY_NOT_REACHED();
    }

    return {};
}
ErrorOr<void> LineProgram::handle_standard_opcode(u8 opcode)
{
    switch (opcode) {
    case StandardOpcodes::Copy: {
        append_to_line_info();
        break;
    }
    case StandardOpcodes::AdvancePc: {
        size_t operand = TRY(m_stream.read_value<LEB128<size_t>>());
        size_t delta = operand * m_unit_header.min_instruction_length();
        dbgln_if(DWARF_DEBUG, "AdvancePC by: {} to: {:p}", delta, m_address + delta);
        m_address += delta;
        break;
    }
    case StandardOpcodes::SetFile: {
        size_t new_file_index = TRY(m_stream.read_value<LEB128<size_t>>());
        dbgln_if(DWARF_DEBUG, "SetFile: new file index: {}", new_file_index);
        m_file_index = new_file_index;
        break;
    }
    case StandardOpcodes::SetColumn: {
        // not implemented
        dbgln_if(DWARF_DEBUG, "SetColumn");
        [[maybe_unused]] size_t new_column = TRY(m_stream.read_value<LEB128<size_t>>());

        break;
    }
    case StandardOpcodes::AdvanceLine: {
        ssize_t line_delta = TRY(m_stream.read_value<LEB128<ssize_t>>());
        VERIFY(line_delta >= 0 || m_line >= (size_t)(-line_delta));
        m_line += line_delta;
        dbgln_if(DWARF_DEBUG, "AdvanceLine: {}", m_line);
        break;
    }
    case StandardOpcodes::NegateStatement: {
        dbgln_if(DWARF_DEBUG, "NegateStatement");
        m_is_statement = !m_is_statement;
        break;
    }
    case StandardOpcodes::ConstAddPc: {
        u8 adjusted_opcode = 255 - m_unit_header.opcode_base();
        ssize_t address_increment = (adjusted_opcode / m_unit_header.line_range()) * m_unit_header.min_instruction_length();
        address_increment *= m_unit_header.min_instruction_length();
        dbgln_if(DWARF_DEBUG, "ConstAddPc: advance pc by: {} to: {}", address_increment, (m_address + address_increment));
        m_address += address_increment;
        break;
    }
    case StandardOpcodes::SetIsa: {
        size_t isa = TRY(m_stream.read_value<LEB128<size_t>>());
        dbgln_if(DWARF_DEBUG, "SetIsa: {}", isa);
        break;
    }
    case StandardOpcodes::FixAdvancePc: {
        auto delta = TRY(m_stream.read_value<u16>());
        dbgln_if(DWARF_DEBUG, "FixAdvancePC by: {} to: {:p}", delta, m_address + delta);
        m_address += delta;
        break;
    }
    case StandardOpcodes::SetBasicBlock: {
        m_basic_block = true;
        break;
    }
    case StandardOpcodes::SetPrologueEnd: {
        m_prologue_end = true;
        break;
    }
    case StandardOpcodes::SetEpilogueBegin: {
        m_epilogue_begin = true;
        break;
    }
    default:
        dbgln("Unhandled LineProgram opcode {}", opcode);
        VERIFY_NOT_REACHED();
    }

    return {};
}
void LineProgram::handle_special_opcode(u8 opcode)
{
    u8 adjusted_opcode = opcode - m_unit_header.opcode_base();
    ssize_t address_increment = (adjusted_opcode / m_unit_header.line_range()) * m_unit_header.min_instruction_length();
    ssize_t line_increment = m_unit_header.line_base() + (adjusted_opcode % m_unit_header.line_range());

    m_address += address_increment;
    m_line += line_increment;

    if constexpr (DWARF_DEBUG) {
        dbgln("Special adjusted_opcode: {}, address_increment: {}, line_increment: {}", adjusted_opcode, address_increment, line_increment);
        dbgln("Address is now: {:p}, and line is: {}:{}", m_address, m_source_files[m_file_index].name, m_line);
    }

    append_to_line_info();

    m_basic_block = false;
    m_prologue_end = false;
}

ErrorOr<void> LineProgram::run_program()
{
    reset_registers();

    while (TRY(m_stream.tell()) < m_unit_offset + sizeof(u32) + m_unit_header.length()) {
        auto opcode = TRY(m_stream.read_value<u8>());

        dbgln_if(DWARF_DEBUG, "{:p}: opcode: {}", TRY(m_stream.tell()) - 1, opcode);

        if (opcode == 0) {
            TRY(handle_extended_opcode());
        } else if (opcode >= 1 && opcode <= 12) {
            TRY(handle_standard_opcode(opcode));
        } else {
            handle_special_opcode(opcode);
        }
    }

    return {};
}

LineProgram::DirectoryAndFile LineProgram::get_directory_and_file(size_t file_index) const
{
    VERIFY(file_index < m_source_files.size());
    auto file_entry = m_source_files[file_index];
    VERIFY(file_entry.directory_index < m_source_directories.size());
    auto directory_entry = m_source_directories[file_entry.directory_index];
    return { directory_entry, file_entry.name };
}

bool LineProgram::looks_like_embedded_resource() const
{
    if (source_files().size() == 1)
        return source_files()[0].name.view().contains("serenity_icon_"sv);

    if (source_files().size() == 2 && source_files()[0].name.view() == "."sv)
        return source_files()[1].name.view().contains("serenity_icon_"sv);

    return false;
}

}
