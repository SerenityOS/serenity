/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibDebug/Dwarf/DIE.h>
#include <LibDebug/Dwarf/DwarfInfo.h>
#include <LibDebug/Dwarf/LineProgram.h>
#include <LibELF/Image.h>
#include <sys/arch/regs.h>

namespace Debug {

class DebugInfo {
    AK_MAKE_NONCOPYABLE(DebugInfo);
    AK_MAKE_NONMOVABLE(DebugInfo);

public:
    explicit DebugInfo(ELF::Image const&, ByteString source_root = {}, FlatPtr base_address = 0);

    ELF::Image const& elf() const { return m_elf; }

    struct SourcePosition {
        DeprecatedFlyString file_path;
        size_t line_number { 0 };
        Optional<FlatPtr> address_of_first_statement;

        SourcePosition()
            : SourcePosition(ByteString::empty(), 0)
        {
        }
        SourcePosition(ByteString file_path, size_t line_number)
            : file_path(file_path)
            , line_number(line_number)
        {
        }
        SourcePosition(ByteString file_path, size_t line_number, FlatPtr address_of_first_statement)
            : file_path(file_path)
            , line_number(line_number)
            , address_of_first_statement(address_of_first_statement)
        {
        }

        bool operator==(SourcePosition const& other) const { return file_path == other.file_path && line_number == other.line_number; }

        static SourcePosition from_line_info(Dwarf::LineProgram::LineInfo const&);
    };

    struct VariableInfo {
        enum class LocationType {
            None,
            Address,
            Register,
        };
        ByteString name;
        ByteString type_name;
        LocationType location_type { LocationType::None };
        union {
            FlatPtr address;
        } location_data { 0 };

        union {
            u32 as_u32;
            u32 as_i32;
            char const* as_string;
        } constant_data { 0 };

        Dwarf::EntryTag type_tag;
        OwnPtr<VariableInfo> type;
        Vector<NonnullOwnPtr<VariableInfo>> members;
        VariableInfo* parent { nullptr };
        Vector<u32> dimension_sizes;

        bool is_enum_type() const { return type && type->type_tag == Dwarf::EntryTag::EnumerationType; }
    };

    struct VariablesScope {
        bool is_function { false };
        ByteString name;
        FlatPtr address_low { 0 };
        FlatPtr address_high { 0 }; // Non-inclusive - the lowest address after address_low that's not in this scope
        Vector<Dwarf::DIE> dies_of_variables;
    };

    ErrorOr<Vector<NonnullOwnPtr<VariableInfo>>> get_variables_in_current_scope(PtraceRegisters const&) const;

    Optional<SourcePosition> get_source_position(FlatPtr address) const;

    struct SourcePositionWithInlines {
        Optional<SourcePosition> source_position;
        Vector<SourcePosition> inline_chain;
    };
    ErrorOr<SourcePositionWithInlines> get_source_position_with_inlines(FlatPtr address) const;

    struct SourcePositionAndAddress {
        ByteString file;
        size_t line;
        FlatPtr address;
    };

    Optional<SourcePositionAndAddress> get_address_from_source_position(ByteString const& file, size_t line) const;

    ByteString name_of_containing_function(FlatPtr address) const;
    Vector<SourcePosition> source_lines_in_scope(VariablesScope const&) const;
    Optional<VariablesScope> get_containing_function(FlatPtr address) const;

private:
    ErrorOr<void> prepare_variable_scopes();
    ErrorOr<void> prepare_lines();
    ErrorOr<void> parse_scopes_impl(Dwarf::DIE const& die);
    ErrorOr<OwnPtr<VariableInfo>> create_variable_info(Dwarf::DIE const& variable_die, PtraceRegisters const&, u32 address_offset = 0) const;
    static bool is_variable_tag_supported(Dwarf::EntryTag const& tag);
    ErrorOr<void> add_type_info_to_variable(Dwarf::DIE const& type_die, PtraceRegisters const& regs, DebugInfo::VariableInfo* parent_variable) const;

    ErrorOr<Optional<Dwarf::LineProgram::DirectoryAndFile>> get_source_path_of_inline(Dwarf::DIE const&) const;
    ErrorOr<Optional<uint32_t>> get_line_of_inline(Dwarf::DIE const&) const;

    ELF::Image const& m_elf;
    ByteString m_source_root;
    FlatPtr m_base_address { 0 };
    Dwarf::DwarfInfo m_dwarf_info;

    Vector<VariablesScope> m_scopes;
    Vector<Dwarf::LineProgram::LineInfo> m_sorted_lines;
};

}
