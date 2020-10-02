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
#include <AK/MemoryStream.h>
#include <AK/QuickSort.h>
#include <LibDebug/Dwarf/CompilationUnit.h>
#include <LibDebug/Dwarf/DwarfInfo.h>
#include <LibDebug/Dwarf/Expression.h>

//#define DEBUG_SPAM

namespace Debug {

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
#ifdef DEBUG_SPAM
            dbg() << "DWARF inlined functions are not supported";
#endif
            return;
        }
        if (child.get_attribute(Dwarf::Attribute::Ranges).has_value()) {
#ifdef DEBUG_SPAM
            dbg() << "DWARF ranges are not supported";
#endif
            return;
        }
        auto name = child.get_attribute(Dwarf::Attribute::Name);

        VariablesScope scope {};
        scope.is_function = (child.tag() == Dwarf::EntryTag::SubProgram);
        if (name.has_value())
            scope.name = name.value().data.as_string;

        if (!child.get_attribute(Dwarf::Attribute::LowPc).has_value()) {
#ifdef DEBUG_SPAM
            dbg() << "DWARF: Couldn't find attribute LowPc for scope";
#endif
            return;
        }
        scope.address_low = child.get_attribute(Dwarf::Attribute::LowPc).value().data.as_u32;
        // The attribute name HighPc is confusing. In this context, it seems to actually be a positive offset from LowPc
        scope.address_high = scope.address_low + child.get_attribute(Dwarf::Attribute::HighPc).value().data.as_u32;

        child.for_each_child([&](const Dwarf::DIE& variable_entry) {
            if (!(variable_entry.tag() == Dwarf::EntryTag::Variable
                    || variable_entry.tag() == Dwarf::EntryTag::FormalParameter))
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
    if (section.is_undefined())
        return;

    auto buffer = section.wrapping_byte_buffer();
    InputMemoryStream stream { buffer };

    Vector<Dwarf::LineProgram::LineInfo> all_lines;
    while (!stream.eof()) {
        Dwarf::LineProgram program(stream);
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
            return SourcePosition::from_line_info(m_sorted_lines[i]);
        }
    }
    return {};
}

Optional<u32> DebugInfo::get_instruction_from_source(const String& file, size_t line) const
{
    String file_path = file;
    constexpr char SERENITY_LIBS_PREFIX[] = "/usr/src/serenity";
    if (file.starts_with(SERENITY_LIBS_PREFIX)) {
        file_path = file.substring(sizeof(SERENITY_LIBS_PREFIX), file.length() - sizeof(SERENITY_LIBS_PREFIX));
        file_path = String::format("../%s", file_path.characters());
    }
    for (const auto& line_entry : m_sorted_lines) {
        if (line_entry.file == file_path && line_entry.line == line)
            return Optional<u32>(line_entry.address);
    }
    return {};
}

NonnullOwnPtrVector<DebugInfo::VariableInfo> DebugInfo::get_variables_in_current_scope(const PtraceRegisters& regs) const
{
    NonnullOwnPtrVector<DebugInfo::VariableInfo> variables;

    // TODO: We can store the scopes in a better data structure
    for (const auto& scope : m_scopes) {
        if (regs.eip < scope.address_low || regs.eip >= scope.address_high)
            continue;

        for (const auto& die_entry : scope.dies_of_variables) {
            auto variable_info = create_variable_info(die_entry, regs);
            if (!variable_info)
                continue;
            variables.append(variable_info.release_nonnull());
        }
    }
    return variables;
}

static Optional<Dwarf::DIE> parse_variable_type_die(const Dwarf::DIE& variable_die, DebugInfo::VariableInfo& variable_info)
{
    auto type_die_offset = variable_die.get_attribute(Dwarf::Attribute::Type);
    if (!type_die_offset.has_value())
        return {};

    ASSERT(type_die_offset.value().type == Dwarf::DIE::AttributeValue::Type::DieReference);

    auto type_die = variable_die.get_die_at_offset(type_die_offset.value().data.as_u32);
    auto type_name = type_die.get_attribute(Dwarf::Attribute::Name);
    if (type_name.has_value()) {
        variable_info.type_name = type_name.value().data.as_string;
    } else {
        dbg() << "Unnamed DWARF type at offset: " << type_die.offset();
        variable_info.name = "[Unnamed Type]";
    }

    return type_die;
}

static void parse_variable_location(const Dwarf::DIE& variable_die, DebugInfo::VariableInfo& variable_info, const PtraceRegisters& regs)
{
    auto location_info = variable_die.get_attribute(Dwarf::Attribute::Location);
    if (!location_info.has_value())
        location_info = variable_die.get_attribute(Dwarf::Attribute::MemberLocation);

    if (location_info.has_value()) {
        if (location_info.value().type == Dwarf::DIE::AttributeValue::Type::UnsignedNumber) {
            variable_info.location_type = DebugInfo::VariableInfo::LocationType::Address;
            variable_info.location_data.address = location_info.value().data.as_u32;
        }

        if (location_info.value().type == Dwarf::DIE::AttributeValue::Type::DwarfExpression) {
            auto expression_bytes = ReadonlyBytes { location_info.value().data.as_raw_bytes.bytes, location_info.value().data.as_raw_bytes.length };
            auto value = Dwarf::Expression::evaluate(expression_bytes, regs);

            if (value.type != Dwarf::Expression::Type::None) {
                ASSERT(value.type == Dwarf::Expression::Type::UnsignedIntetger);
                variable_info.location_type = DebugInfo::VariableInfo::LocationType::Address;
                variable_info.location_data.address = value.data.as_u32;
            }
        }
    }
}

OwnPtr<DebugInfo::VariableInfo> DebugInfo::create_variable_info(const Dwarf::DIE& variable_die, const PtraceRegisters& regs) const
{
    ASSERT(variable_die.tag() == Dwarf::EntryTag::Variable
        || variable_die.tag() == Dwarf::EntryTag::Member
        || variable_die.tag() == Dwarf::EntryTag::FormalParameter
        || variable_die.tag() == Dwarf::EntryTag::EnumerationType
        || variable_die.tag() == Dwarf::EntryTag::Enumerator
        || variable_die.tag() == Dwarf::EntryTag::StructureType);

    if (variable_die.tag() == Dwarf::EntryTag::FormalParameter
        && !variable_die.get_attribute(Dwarf::Attribute::Name).has_value()) {
        // We don't want to display info for unused parameters
        return {};
    }

    NonnullOwnPtr<VariableInfo> variable_info = make<VariableInfo>();
    variable_info->name = variable_die.get_attribute(Dwarf::Attribute::Name).value().data.as_string;

    auto type_die = parse_variable_type_die(variable_die, *variable_info);

    if (variable_die.tag() == Dwarf::EntryTag::Enumerator) {
        auto constant = variable_die.get_attribute(Dwarf::Attribute::ConstValue);
        ASSERT(constant.has_value());
        switch (constant.value().type) {
        case Dwarf::DIE::AttributeValue::Type::UnsignedNumber:
            variable_info->constant_data.as_u32 = constant.value().data.as_u32;
            break;
        case Dwarf::DIE::AttributeValue::Type::SignedNumber:
            variable_info->constant_data.as_i32 = constant.value().data.as_i32;
            break;
        case Dwarf::DIE::AttributeValue::Type::String:
            variable_info->constant_data.as_string = constant.value().data.as_string;
            break;
        default:
            ASSERT_NOT_REACHED();
        }
    } else {
        parse_variable_location(variable_die, *variable_info, regs);
    }

    if (type_die.has_value()) {
        OwnPtr<VariableInfo> type_info;
        if (type_die.value().tag() == Dwarf::EntryTag::EnumerationType || type_die.value().tag() == Dwarf::EntryTag::StructureType) {
            type_info = create_variable_info(type_die.value(), regs);
        }

        type_die.value().for_each_child([&](const Dwarf::DIE& member) {
            if (member.is_null())
                return;
            auto member_variable = create_variable_info(member, regs);

            ASSERT(member_variable);

            if (type_die.value().tag() == Dwarf::EntryTag::EnumerationType) {
                member_variable->parent = type_info.ptr();
                type_info->members.append(member_variable.release_nonnull());
            } else {
                ASSERT(variable_info->location_type == DebugInfo::VariableInfo::LocationType::Address);

                if (member_variable->location_type == DebugInfo::VariableInfo::LocationType::Address)
                    member_variable->location_data.address += variable_info->location_data.address;

                member_variable->parent = variable_info.ptr();
                variable_info->members.append(member_variable.release_nonnull());
            }
        });

        if (type_info) {
            variable_info->type = move(type_info);
            variable_info->type->type_tag = type_die.value().tag();
        }
    }

    return variable_info;
}

String DebugInfo::name_of_containing_function(u32 address) const
{
    auto function = get_containing_function(address);
    if (!function.has_value())
        return {};
    return function.value().name;
}

Optional<DebugInfo::VariablesScope> DebugInfo::get_containing_function(u32 address) const
{
    for (const auto& scope : m_scopes) {
        if (!scope.is_function || address < scope.address_low || address >= scope.address_high)
            continue;
        return scope;
    }
    return {};
}

Vector<DebugInfo::SourcePosition> DebugInfo::source_lines_in_scope(const VariablesScope& scope) const
{
    Vector<DebugInfo::SourcePosition> source_lines;
    for (const auto& line : m_sorted_lines) {
        if (line.address < scope.address_low)
            continue;

        if (line.address >= scope.address_high)
            break;
        source_lines.append(SourcePosition::from_line_info(line));
    }
    return source_lines;
}

DebugInfo::SourcePosition DebugInfo::SourcePosition::from_line_info(const Dwarf::LineProgram::LineInfo& line)
{
    return { line.file, line.line, line.address };
}

}
