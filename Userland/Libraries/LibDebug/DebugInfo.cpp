/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DebugInfo.h"
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/MemoryStream.h>
#include <AK/QuickSort.h>
#include <LibDebug/Dwarf/CompilationUnit.h>
#include <LibDebug/Dwarf/DwarfInfo.h>
#include <LibDebug/Dwarf/Expression.h>

namespace Debug {

DebugInfo::DebugInfo(NonnullOwnPtr<const ELF::Image> elf, String source_root, FlatPtr base_address)
    : m_elf(move(elf))
    , m_source_root(move(source_root))
    , m_base_address(base_address)
    , m_dwarf_info(*m_elf)
{
    prepare_variable_scopes();
    prepare_lines();
}

void DebugInfo::prepare_variable_scopes()
{
    m_dwarf_info.for_each_compilation_unit([&](const Dwarf::CompilationUnit& unit) {
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
            dbgln_if(SPAM_DEBUG, "DWARF inlined functions are not supported");
            return;
        }
        if (child.get_attribute(Dwarf::Attribute::Ranges).has_value()) {
            dbgln_if(SPAM_DEBUG, "DWARF ranges are not supported");
            return;
        }
        auto name = child.get_attribute(Dwarf::Attribute::Name);

        VariablesScope scope {};
        scope.is_function = (child.tag() == Dwarf::EntryTag::SubProgram);
        if (name.has_value())
            scope.name = name.value().data.as_string;

        if (!child.get_attribute(Dwarf::Attribute::LowPc).has_value()) {
            dbgln_if(SPAM_DEBUG, "DWARF: Couldn't find attribute LowPc for scope");
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
    auto section = elf().lookup_section(".debug_line"sv);
    if (!section.has_value())
        return;

    InputMemoryStream stream { section->bytes() };

    Vector<Dwarf::LineProgram::LineInfo> all_lines;
    while (!stream.eof()) {
        Dwarf::LineProgram program(m_dwarf_info, stream);
        all_lines.append(program.lines());
    }

    HashMap<FlyString, Optional<String>> memoized_full_paths;
    auto compute_full_path = [&](FlyString const& file_path) -> Optional<String> {
        if (file_path.view().contains("Toolchain/"sv) || file_path.view().contains("libgcc"sv))
            return {};
        if (file_path.view().starts_with("./"sv) && !m_source_root.is_null())
            return LexicalPath::join(m_source_root, file_path).string();
        if (auto index_of_serenity_slash = file_path.view().find("serenity/"sv); index_of_serenity_slash.has_value()) {
            auto start_index = index_of_serenity_slash.value() + "serenity/"sv.length();
            return file_path.view().substring_view(start_index, file_path.length() - start_index);
        }
        return file_path;
    };

    m_sorted_lines.ensure_capacity(all_lines.size());

    for (auto const& line_info : all_lines) {
        auto it = memoized_full_paths.find(line_info.file);
        if (it == memoized_full_paths.end()) {
            memoized_full_paths.set(line_info.file, compute_full_path(line_info.file));
            it = memoized_full_paths.find(line_info.file);
        }
        if (!it->value.has_value())
            continue;
        m_sorted_lines.unchecked_append({ line_info.address, it->value.value(), line_info.line });
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

Optional<DebugInfo::SourcePositionAndAddress> DebugInfo::get_address_from_source_position(const String& file, size_t line) const
{
    String file_path = file;
    if (!file_path.starts_with("/"))
        file_path = String::formatted("/{}", file_path);

    constexpr char SERENITY_LIBS_PREFIX[] = "/usr/src/serenity";
    if (file.starts_with(SERENITY_LIBS_PREFIX)) {
        file_path = file.substring(sizeof(SERENITY_LIBS_PREFIX), file.length() - sizeof(SERENITY_LIBS_PREFIX));
        file_path = String::formatted("../{}", file_path);
    }

    Optional<SourcePositionAndAddress> result;
    for (const auto& line_entry : m_sorted_lines) {
        if (!line_entry.file.ends_with(file_path))
            continue;

        if (line_entry.line > line)
            continue;

        // We look for the source position that is closest to the desired position, and is not after it.
        // For example, get_address_of_source_position("main.cpp", 73) could return the address for an instruction whose location is ("main.cpp", 72)
        // as there might not be an instruction mapped for "main.cpp", 73.
        if (!result.has_value() || (line_entry.line > result.value().line)) {
            result = SourcePositionAndAddress { line_entry.file, line_entry.line, line_entry.address };
        }
    }
    return result;
}

NonnullOwnPtrVector<DebugInfo::VariableInfo> DebugInfo::get_variables_in_current_scope(const PtraceRegisters& regs) const
{
    NonnullOwnPtrVector<DebugInfo::VariableInfo> variables;

    // TODO: We can store the scopes in a better data structure
    for (const auto& scope : m_scopes) {
        if (regs.eip - m_base_address < scope.address_low || regs.eip - m_base_address >= scope.address_high)
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

    VERIFY(type_die_offset.value().type == Dwarf::AttributeValue::Type::DieReference);

    auto type_die = variable_die.get_die_at_offset(type_die_offset.value().data.as_u32);
    auto type_name = type_die.get_attribute(Dwarf::Attribute::Name);
    if (type_name.has_value()) {
        variable_info.type_name = type_name.value().data.as_string;
    } else {
        dbgln("Unnamed DWARF type at offset: {}", type_die.offset());
        variable_info.type_name = "[Unnamed Type]";
    }

    return type_die;
}

static void parse_variable_location(const Dwarf::DIE& variable_die, DebugInfo::VariableInfo& variable_info, const PtraceRegisters& regs)
{
    auto location_info = variable_die.get_attribute(Dwarf::Attribute::Location);
    if (!location_info.has_value()) {
        location_info = variable_die.get_attribute(Dwarf::Attribute::MemberLocation);
    }

    if (!location_info.has_value())
        return;

    switch (location_info.value().type) {
    case Dwarf::AttributeValue::Type::UnsignedNumber:
        variable_info.location_type = DebugInfo::VariableInfo::LocationType::Address;
        variable_info.location_data.address = location_info.value().data.as_u32;
        break;
    case Dwarf::AttributeValue::Type::DwarfExpression: {
        auto expression_bytes = ReadonlyBytes { location_info.value().data.as_raw_bytes.bytes, location_info.value().data.as_raw_bytes.length };
        auto value = Dwarf::Expression::evaluate(expression_bytes, regs);

        if (value.type != Dwarf::Expression::Type::None) {
            VERIFY(value.type == Dwarf::Expression::Type::UnsignedIntetger);
            variable_info.location_type = DebugInfo::VariableInfo::LocationType::Address;
            variable_info.location_data.address = value.data.as_u32;
        }
        break;
    }
    default:
        dbgln("Warning: unhandled Dwarf location type: {}", (int)location_info.value().type);
    }
}

OwnPtr<DebugInfo::VariableInfo> DebugInfo::create_variable_info(const Dwarf::DIE& variable_die, const PtraceRegisters& regs, u32 address_offset) const
{
    VERIFY(is_variable_tag_supported(variable_die.tag()));

    if (variable_die.tag() == Dwarf::EntryTag::FormalParameter
        && !variable_die.get_attribute(Dwarf::Attribute::Name).has_value()) {
        // We don't want to display info for unused parameters
        return {};
    }

    NonnullOwnPtr<VariableInfo> variable_info = make<VariableInfo>();
    auto name_attribute = variable_die.get_attribute(Dwarf::Attribute::Name);
    if (name_attribute.has_value())
        variable_info->name = name_attribute.value().data.as_string;

    auto type_die = parse_variable_type_die(variable_die, *variable_info);

    if (variable_die.tag() == Dwarf::EntryTag::Enumerator) {
        auto constant = variable_die.get_attribute(Dwarf::Attribute::ConstValue);
        VERIFY(constant.has_value());
        switch (constant.value().type) {
        case Dwarf::AttributeValue::Type::UnsignedNumber:
            variable_info->constant_data.as_u32 = constant.value().data.as_u32;
            break;
        case Dwarf::AttributeValue::Type::SignedNumber:
            variable_info->constant_data.as_i32 = constant.value().data.as_i32;
            break;
        case Dwarf::AttributeValue::Type::String:
            variable_info->constant_data.as_string = constant.value().data.as_string;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    } else {
        parse_variable_location(variable_die, *variable_info, regs);
        variable_info->location_data.address += address_offset;
    }

    if (type_die.has_value())
        add_type_info_to_variable(type_die.value(), regs, variable_info);

    return variable_info;
}

void DebugInfo::add_type_info_to_variable(const Dwarf::DIE& type_die, const PtraceRegisters& regs, DebugInfo::VariableInfo* parent_variable) const
{
    OwnPtr<VariableInfo> type_info;
    auto is_array_type = type_die.tag() == Dwarf::EntryTag::ArrayType;

    if (type_die.tag() == Dwarf::EntryTag::EnumerationType
        || type_die.tag() == Dwarf::EntryTag::StructureType
        || is_array_type) {
        type_info = create_variable_info(type_die, regs);
    }

    type_die.for_each_child([&](const Dwarf::DIE& member) {
        if (member.is_null())
            return;

        if (is_array_type && member.tag() == Dwarf::EntryTag::SubRangeType) {
            auto upper_bound = member.get_attribute(Dwarf::Attribute::UpperBound);
            VERIFY(upper_bound.has_value());
            auto size = upper_bound.value().data.as_u32 + 1;
            type_info->dimension_sizes.append(size);
            return;
        }

        if (!is_variable_tag_supported(member.tag()))
            return;

        auto member_variable = create_variable_info(member, regs, parent_variable->location_data.address);
        VERIFY(member_variable);

        if (type_die.tag() == Dwarf::EntryTag::EnumerationType) {
            member_variable->parent = type_info.ptr();
            type_info->members.append(member_variable.release_nonnull());
        } else {
            if (parent_variable->location_type != DebugInfo::VariableInfo::LocationType::Address)
                return;

            member_variable->parent = parent_variable;
            parent_variable->members.append(member_variable.release_nonnull());
        }
    });

    if (type_info) {
        if (is_array_type) {
            StringBuilder array_type_name;
            array_type_name.append(type_info->type_name);
            for (auto array_size : type_info->dimension_sizes) {
                array_type_name.append("[");
                array_type_name.append(String::formatted("{:d}", array_size));
                array_type_name.append("]");
            }
            parent_variable->type_name = array_type_name.to_string();
        }
        parent_variable->type = move(type_info);
        parent_variable->type->type_tag = type_die.tag();
    }
}

bool DebugInfo::is_variable_tag_supported(const Dwarf::EntryTag& tag)
{
    return tag == Dwarf::EntryTag::Variable
        || tag == Dwarf::EntryTag::Member
        || tag == Dwarf::EntryTag::FormalParameter
        || tag == Dwarf::EntryTag::EnumerationType
        || tag == Dwarf::EntryTag::Enumerator
        || tag == Dwarf::EntryTag::StructureType
        || tag == Dwarf::EntryTag::ArrayType;
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
    return { line.file, line.line, { line.address } };
}

}
