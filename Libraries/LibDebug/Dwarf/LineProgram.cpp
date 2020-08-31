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

#include "LineProgram.h"

#include <AK/String.h>

//#define DWARF_DEBUG

namespace Debug::Dwarf {

LineProgram::LineProgram(InputMemoryStream& stream)
    : m_stream(stream)
{
    m_unit_offset = m_stream.offset();
    parse_unit_header();
    parse_source_directories();
    parse_source_files();
    run_program();
}

void LineProgram::parse_unit_header()
{
    m_stream >> Bytes { &m_unit_header, sizeof(m_unit_header) };

    ASSERT(m_unit_header.version == DWARF_VERSION);
    ASSERT(m_unit_header.opcode_base == SPECIAL_OPCODES_BASE);

#ifdef DWARF_DEBUG
    dbg() << "unit length: " << m_unit_header.length;
#endif
}

void LineProgram::parse_source_directories()
{
    m_source_directories.append(".");

    while (m_stream.peek_or_error()) {
        String directory;
        m_stream >> directory;
#ifdef DWARF_DEBUG
        dbg() << "directory: " << directory;
#endif
        m_source_directories.append(move(directory));
    }
    m_stream.handle_recoverable_error();
    m_stream.discard_or_error(1);
    ASSERT(!m_stream.has_any_error());
}

void LineProgram::parse_source_files()
{
    m_source_files.append({ ".", 0 });
    while (!m_stream.eof() && m_stream.peek_or_error()) {
        String file_name;
        m_stream >> file_name;
        size_t directory_index = 0;
        m_stream.read_LEB128_unsigned(directory_index);
        size_t _unused = 0;
        m_stream.read_LEB128_unsigned(_unused); // skip modification time
        m_stream.read_LEB128_unsigned(_unused); // skip file size
#ifdef DWARF_DEBUG
        dbg() << "file: " << file_name << ", directory index: " << directory_index;
#endif
        m_source_files.append({ file_name, directory_index });
    }
    m_stream.discard_or_error(1);
    ASSERT(!m_stream.has_any_error());
}

void LineProgram::append_to_line_info()
{
#ifdef DWARF_DEBUG
    dbg() << "appending line info: " << (void*)m_address << ", " << m_source_files[m_file_index].name << ":" << m_line;
#endif
    if (!m_is_statement)
        return;

    String directory = m_source_directories[m_source_files[m_file_index].directory_index];
    String full_path = String::format("%s/%s", directory.characters(), m_source_files[m_file_index].name.characters());
    m_lines.append({ m_address, full_path, m_line });
}

void LineProgram::reset_registers()
{
    m_address = 0;
    m_line = 1;
    m_file_index = 1;
    m_is_statement = m_unit_header.default_is_stmt == 1;
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
        ASSERT(length == sizeof(size_t) + 1);
        m_stream >> m_address;
#ifdef DWARF_DEBUG
        dbg() << "SetAddress: " << (void*)m_address;
#endif
        break;
    }
    case ExtendedOpcodes::SetDiscriminator: {
#ifdef DWARF_DEBUG
        dbg() << "SetDiscriminator";
#endif
        m_stream.discard_or_error(1);
        break;
    }
    default:
#ifdef DWARF_DEBUG
        dbg() << "offset: " << (void*)m_stream.offset();
#endif
        ASSERT_NOT_REACHED();
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
        size_t delta = operand * m_unit_header.min_instruction_length;
#ifdef DWARF_DEBUG
        dbg() << "AdvnacePC by: " << delta << " to: " << (void*)(m_address + delta);
#endif
        m_address += delta;
        break;
    }
    case StandardOpcodes::SetFile: {
        size_t new_file_index = 0;
        m_stream.read_LEB128_unsigned(new_file_index);
#ifdef DWARF_DEBUG
        dbg() << "SetFile: new file index: " << new_file_index;
#endif
        m_file_index = new_file_index;
        break;
    }
    case StandardOpcodes::SetColumn: {
        // not implemented
#ifdef DWARF_DEBUG
        dbg() << "SetColumn";
#endif
        size_t new_column;
        m_stream.read_LEB128_unsigned(new_column);

        break;
    }
    case StandardOpcodes::AdvanceLine: {
        ssize_t line_delta;
        m_stream.read_LEB128_signed(line_delta);
        // dbg() << "line_delta: " << line_delta;
        ASSERT(line_delta >= 0 || m_line >= (size_t)(-line_delta));
        m_line += line_delta;
#ifdef DWARF_DEBUG
        dbg() << "AdvanceLine: " << m_line;
#endif
        break;
    }
    case StandardOpcodes::NegateStatement: {
#ifdef DWARF_DEBUG
        dbg() << "NegateStatement";
#endif
        m_is_statement = !m_is_statement;
        break;
    }
    case StandardOpcodes::ConstAddPc: {
        u8 adjusted_opcode = 255 - SPECIAL_OPCODES_BASE;
        ssize_t address_increment = (adjusted_opcode / m_unit_header.line_range) * m_unit_header.min_instruction_length;
        address_increment *= m_unit_header.min_instruction_length;
#ifdef DWARF_DEBUG
        dbg() << "ConstAddPc: advance pc by: " << address_increment << " to: " << (m_address + address_increment);
#endif
        m_address += address_increment;
        break;
    }
    default:
        ASSERT_NOT_REACHED();
    }
}
void LineProgram::handle_sepcial_opcode(u8 opcode)
{
    u8 adjusted_opcode = opcode - SPECIAL_OPCODES_BASE;
    ssize_t address_increment = (adjusted_opcode / m_unit_header.line_range) * m_unit_header.min_instruction_length;
    ssize_t line_increment = m_unit_header.line_base + (adjusted_opcode % m_unit_header.line_range);

    m_address += address_increment;
    m_line += line_increment;

#ifdef DWARF_DEBUG
    dbg() << "Special adjusted_opcode: " << adjusted_opcode << ", delta_address: " << address_increment << ", delta_line: " << line_increment;
    dbg() << "Address is now:" << (void*)m_address << ", and line is: " << m_source_files[m_file_index].name << ":" << m_line;
#endif

    append_to_line_info();
}

void LineProgram::run_program()
{
    reset_registers();

    while ((size_t)m_stream.offset() < m_unit_offset + sizeof(u32) + m_unit_header.length) {
        u8 opcode = 0;
        m_stream >> opcode;

#ifdef DWARF_DEBUG
        dbg() << (void*)(m_stream.offset() - 1) << ": opcode: " << opcode;
#endif

        if (opcode == 0) {
            handle_extended_opcode();
        } else if (opcode >= 1 && opcode <= 12) {
            handle_standard_opcode(opcode);
        } else {
            handle_sepcial_opcode(opcode);
        }
    }
}

}
