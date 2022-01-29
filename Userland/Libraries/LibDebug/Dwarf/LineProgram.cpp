/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LineProgram.h"
#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace Debug::Dwarf {

LineProgram::LineProgram(DwarfInfo& dwarf_info, InputMemoryStream& stream)
    : m_dwarf_info(dwarf_info)
    , m_stream(stream)
{
    m_unit_offset = m_stream.offset();
    parse_unit_header();
    parse_source_directories();
    parse_source_files();
    run_program();
}

void LineProgram::parse_unit_header()
{
    m_stream >> m_unit_header;

    VERIFY(m_unit_header.version() >= MIN_DWARF_VERSION && m_unit_header.version() <= MAX_DWARF_VERSION);
    VERIFY(m_unit_header.opcode_base() <= sizeof(m_unit_header.std_opcode_lengths) / sizeof(m_unit_header.std_opcode_lengths[0]) + 1);

    dbgln_if(DWARF_DEBUG, "unit length: {}", m_unit_header.length());
}

void LineProgram::parse_path_entries(Function<void(PathEntry& entry)> callback, PathListType list_type)
{
    if (m_unit_header.version() >= 5) {
        u8 path_entry_format_count = 0;
        m_stream >> path_entry_format_count;

        Vector<PathEntryFormat> format_descriptions;

        for (u8 i = 0; i < path_entry_format_count; i++) {
            UnderlyingType<ContentType> content_type;
            m_stream.read_LEB128_unsigned(content_type);

            UnderlyingType<AttributeDataForm> data_form;
            m_stream.read_LEB128_unsigned(data_form);

            format_descriptions.empend(static_cast<ContentType>(content_type), static_cast<AttributeDataForm>(data_form));
        }

        size_t paths_count = 0;
        m_stream.read_LEB128_unsigned(paths_count);

        for (size_t i = 0; i < paths_count; i++) {
            PathEntry entry;
            for (auto& format_description : format_descriptions) {
                auto value = m_dwarf_info.get_attribute_value(format_description.form, 0, m_stream);
                switch (format_description.type) {
                case ContentType::Path:
                    entry.path = value.as_string();
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
        while (m_stream.peek_or_error()) {
            String path;
            m_stream >> path;
            dbgln_if(DWARF_DEBUG, "path: {}", path);
            PathEntry entry;
            entry.path = path;
            if (list_type == PathListType::Filenames) {
                size_t directory_index = 0;
                m_stream.read_LEB128_unsigned(directory_index);
                size_t _unused = 0;
                m_stream.read_LEB128_unsigned(_unused); // skip modification time
                m_stream.read_LEB128_unsigned(_unused); // skip file size
                entry.directory_index = directory_index;
                dbgln_if(DWARF_DEBUG, "file: {}, directory index: {}", path, directory_index);
            }
            callback(entry);
        }

        m_stream.handle_recoverable_error();
        m_stream.discard_or_error(1);
    }

    VERIFY(!m_stream.has_any_error());
}

void LineProgram::parse_source_directories()
{
    if (m_unit_header.version() < 5) {
        m_source_directories.append(".");
    }

    parse_path_entries([this](PathEntry& entry) {
        m_source_directories.append(entry.path);
    },
        PathListType::Directories);
}

void LineProgram::parse_source_files()
{
    if (m_unit_header.version() < 5) {
        m_source_files.append({ ".", 0 });
    }

    parse_path_entries([this](PathEntry& entry) {
        m_source_files.append({ entry.path, entry.directory_index });
    },
        PathListType::Filenames);
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

    m_lines.append({ m_address, FlyString { full_path.string_view() }, m_line });
}

void LineProgram::reset_registers()
{
    m_address = 0;
    m_line = 1;
    m_file_index = 1;
    m_is_statement = m_unit_header.default_is_stmt() == 1;
}

void LineProgram::handle_extended_opcode()
{
    size_t length = 0;
    m_stream.read_LEB128_unsigned(length);

    u8 sub_opcode = 0;
    m_stream >> sub_opcode;

    switch (sub_opcode) {
    case ExtendedOpcodes::EndSequence: {
        append_to_line_info();
        reset_registers();
        break;
    }
    case ExtendedOpcodes::SetAddress: {
        VERIFY(length == sizeof(size_t) + 1);
        m_stream >> m_address;
        dbgln_if(DWARF_DEBUG, "SetAddress: {:p}", m_address);
        break;
    }
    case ExtendedOpcodes::SetDiscriminator: {
        dbgln_if(DWARF_DEBUG, "SetDiscriminator");
        size_t discriminator;
        m_stream.read_LEB128_unsigned(discriminator);
        break;
    }
    default:
        dbgln("Encountered unknown sub opcode {} at stream offset {:p}", sub_opcode, m_stream.offset());
        VERIFY_NOT_REACHED();
    }
}
void LineProgram::handle_standard_opcode(u8 opcode)
{
    switch (opcode) {
    case StandardOpcodes::Copy: {
        append_to_line_info();
        break;
    }
    case StandardOpcodes::AdvancePc: {
        size_t operand = 0;
        m_stream.read_LEB128_unsigned(operand);
        size_t delta = operand * m_unit_header.min_instruction_length();
        dbgln_if(DWARF_DEBUG, "AdvancePC by: {} to: {:p}", delta, m_address + delta);
        m_address += delta;
        break;
    }
    case StandardOpcodes::SetFile: {
        size_t new_file_index = 0;
        m_stream.read_LEB128_unsigned(new_file_index);
        dbgln_if(DWARF_DEBUG, "SetFile: new file index: {}", new_file_index);
        m_file_index = new_file_index;
        break;
    }
    case StandardOpcodes::SetColumn: {
        // not implemented
        dbgln_if(DWARF_DEBUG, "SetColumn");
        size_t new_column;
        m_stream.read_LEB128_unsigned(new_column);

        break;
    }
    case StandardOpcodes::AdvanceLine: {
        ssize_t line_delta;
        m_stream.read_LEB128_signed(line_delta);
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
        size_t isa;
        m_stream.read_LEB128_unsigned(isa);
        dbgln_if(DWARF_DEBUG, "SetIsa: {}", isa);
        break;
    }
    case StandardOpcodes::FixAdvancePc: {
        u16 delta = 0;
        m_stream >> delta;
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
    default:
        dbgln("Unhandled LineProgram opcode {}", opcode);
        VERIFY_NOT_REACHED();
    }
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

void LineProgram::run_program()
{
    reset_registers();

    while (m_stream.offset() < m_unit_offset + sizeof(u32) + m_unit_header.length()) {
        u8 opcode = 0;
        m_stream >> opcode;

        dbgln_if(DWARF_DEBUG, "{:p}: opcode: {}", m_stream.offset() - 1, opcode);

        if (opcode == 0) {
            handle_extended_opcode();
        } else if (opcode >= 1 && opcode <= 12) {
            handle_standard_opcode(opcode);
        } else {
            handle_special_opcode(opcode);
        }
    }
}

LineProgram::DirectoryAndFile LineProgram::get_directory_and_file(size_t file_index) const
{
    VERIFY(file_index < m_source_files.size());
    auto file_entry = m_source_files[file_index];
    VERIFY(file_entry.directory_index < m_source_directories.size());
    auto directory_entry = m_source_directories[file_entry.directory_index];
    return { directory_entry, file_entry.name };
}

}
