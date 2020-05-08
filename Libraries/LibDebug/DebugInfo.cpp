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

#include "DebugInfo.h"
#include <AK/QuickSort.h>
#include <LibDebug/Dwarf/CompilationUnit.h>
#include <LibDebug/Dwarf/DwarfInfo.h>
#include <LibDebug/Dwarf/Expression.h>

DebugInfo::DebugInfo(NonnullRefPtr<const ELF::Loader> elf)
    : m_elf(elf)
    , m_dwarf_info(Dwarf::DwarfInfo::create(m_elf))
{
    prepare_variable_scopes();
    prepare_lines();
}

void DebugInfo::prepare_variable_scopes()
{
    m_dwarf_info->for_each_compilation_unit([&](const Dwarf::CompilationUnit& unit) {
        auto root = unit.root_die();
        parse_scopes_impl(root);
    });
}

void DebugInfo::parse_scopes_impl(const Dwarf::DIE& die)
{
    die.for_each_child([&](const Dwarf::DIE& child) {
        if (child.is_null())
            return;
        if (!(child.tag() == Dwarf::EntryTag::SubProgram || child.tag() == Dwarf::EntryTag::LexicalBlock))
            return;

        if (child.get_attribute(Dwarf::Attribute::Inline).has_value()) {
            dbg() << "DWARF inlined functions are not supported";
            return;
        }
        if (child.get_attribute(Dwarf::Attribute::Ranges).has_value()) {
            dbg() << "DWARF ranges are not supported";
            return;
        }
        auto name = child.get_attribute(Dwarf::Attribute::Name);

        VariablesScope scope {};
        scope.is_function = (child.tag() == Dwarf::EntryTag::SubProgram);
        if (name.has_value())
            scope.name = name.value().data.as_string;

        scope.address_low = child.get_attribute(Dwarf::Attribute::LowPc).value().data.as_u32;
        // The attribute name HighPc is confusing. In this context, it seems to actually be a positive offset from LowPc
        scope.address_high = scope.address_low + child.get_attribute(Dwarf::Attribute::HighPc).value().data.as_u32;

        child.for_each_child([&](const Dwarf::DIE& variable_entry) {
            if (variable_entry.tag() != Dwarf::EntryTag::Variable)
                return;
            scope.dies_of_variables.append(variable_entry);
        });
        m_scopes.append(scope);

        parse_scopes_impl(child);
    });
}

void DebugInfo::prepare_lines()
{
    auto section = m_elf->image().lookup_section(".debug_line");
    ASSERT(!section.is_undefined());

    auto buffer = ByteBuffer::wrap(reinterpret_cast<const u8*>(section.raw_data()), section.size());
    BufferStream stream(buffer);

    Vector<LineProgram::LineInfo> all_lines;
    while (!stream.at_end()) {
        LineProgram program(stream);
        all_lines.append(program.lines());
    }

    for (auto& line_info : all_lines) {
        String file_path = line_info.file;
        if (file_path.contains("Toolchain/") || file_path.contains("libgcc"))
            continue;
        if (file_path.contains("serenity/")) {
            auto start_index = file_path.index_of("serenity/").value() + String("serenity/").length();
            file_path = file_path.substring(start_index, file_path.length() - start_index);
        }
        m_sorted_lines.append({ line_info.address, file_path, line_info.line });
    }
    quick_sort(m_sorted_lines, [](auto& a, auto& b) {
        return a.address < b.address;
    });
}

Optional<DebugInfo::SourcePosition> DebugInfo::get_source_position(u32 target_address) const
{
    if (m_sorted_lines.is_empty())
        return {};
    if (target_address < m_sorted_lines[0].address)
        return {};

    // TODO: We can do a binray search here
    for (size_t i = 0; i < m_sorted_lines.size() - 1; ++i) {
        if (m_sorted_lines[i + 1].address > target_address) {
            return Optional<SourcePosition>({ m_sorted_lines[i].file, m_sorted_lines[i].line, m_sorted_lines[i].address });
        }
    }
    return {};
}

Optional<u32> DebugInfo::get_instruction_from_source(const String& file, size_t line) const
{
    for (const auto& line_entry : m_sorted_lines) {
        if (line_entry.file == file && line_entry.line == line)
            return Optional<u32>(line_entry.address);
    }
    return {};
}

NonnullOwnPtrVector<DebugInfo::VariableInfo> DebugInfo::get_variables_in_current_scope(const PtraceRegisters& regs) const
{
    NonnullOwnPtrVector<DebugInfo::VariableInfo> variables;

    // TODO: We can store the scopes in a better data strucutre
    for (const auto& scope : m_scopes) {
        if (regs.eip < scope.address_low || regs.eip >= scope.address_high)
            continue;

        for (const auto& die_entry : scope.dies_of_variables) {
            variables.append(create_variable_info(die_entry, regs));
        }
    }
    return variables;
}

NonnullOwnPtr<DebugInfo::VariableInfo> DebugInfo::create_variable_info(const Dwarf::DIE& variable_die, const PtraceRegisters& regs) const
{
    ASSERT(variable_die.tag() == Dwarf::EntryTag::Variable || variable_die.tag() == Dwarf::EntryTag::Member);

    NonnullOwnPtr<VariableInfo> variable_info = make<VariableInfo>();

    variable_info->name = variable_die.get_attribute(Dwarf::Attribute::Name).value().data.as_string;
    auto type_die_offset = variable_die.get_attribute(Dwarf::Attribute::Type);
    ASSERT(type_die_offset.has_value());
    ASSERT(type_die_offset.value().type == Dwarf::DIE::AttributeValue::Type::DieReference);

    auto type_die = variable_die.get_die_at_offset(type_die_offset.value().data.as_u32);
    auto type_name = type_die.get_attribute(Dwarf::Attribute::Name);
    if (type_name.has_value()) {
        variable_info->type = type_name.value().data.as_string;
    } else {
        dbg() << "Unnamed DWARF type at offset: " << type_die.offset();
        variable_info->name = "[Unnamed Type]";
    }

    auto location_info = variable_die.get_attribute(Dwarf::Attribute::Location);
    if (!location_info.has_value()) {
        location_info = variable_die.get_attribute(Dwarf::Attribute::MemberLocation);
    }

    if (location_info.has_value()) {
        if (location_info.value().type == Dwarf::DIE::AttributeValue::Type::UnsignedNumber) {
            variable_info->location_type = VariableInfo::LocationType::Address;
            variable_info->location_data.address = location_info.value().data.as_u32;
        }

        if (location_info.value().type == Dwarf::DIE::AttributeValue::Type::DwarfExpression) {
            auto expression_bytes = ByteBuffer::wrap(location_info.value().data.as_dwarf_expression.bytes, location_info.value().data.as_dwarf_expression.length);
            auto value = Dwarf::Expression::evaluate(expression_bytes, regs);

            if (value.type != Dwarf::Expression::Type::None) {
                ASSERT(value.type == Dwarf::Expression::Type::UnsignedIntetger);
                variable_info->location_type = VariableInfo::LocationType::Address;
                variable_info->location_data.address = value.data.as_u32;
            }
        }
    }

    type_die.for_each_child([&](const Dwarf::DIE& member) {
        if (member.is_null())
            return;
        auto member_variable = create_variable_info(member, regs);
        ASSERT(member_variable->location_type == DebugInfo::VariableInfo::LocationType::Address);
        ASSERT(variable_info->location_type == DebugInfo::VariableInfo::LocationType::Address);

        member_variable->location_data.address += variable_info->location_data.address;
        member_variable->parent = variable_info.ptr();

        variable_info->members.append(move(member_variable));
    });

    return variable_info;
}

String DebugInfo::name_of_containing_function(u32 address) const
{
    for (const auto& scope : m_scopes) {
        if (!scope.is_function || address < scope.address_low || address >= scope.address_high)
            continue;
        return scope.name;
    }
    return {};
}
