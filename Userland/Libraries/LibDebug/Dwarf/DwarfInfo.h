/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AttributeValue.h"
#include "CompilationUnit.h"
#include "DwarfTypes.h"
#include <AK/ByteBuffer.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RedBlackTree.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibELF/Image.h>

namespace Debug::Dwarf {

class DwarfInfo {
    AK_MAKE_NONCOPYABLE(DwarfInfo);
    AK_MAKE_NONMOVABLE(DwarfInfo);

public:
    explicit DwarfInfo(ELF::Image const&);

    ReadonlyBytes debug_info_data() const { return m_debug_info_data; }
    ReadonlyBytes abbreviation_data() const { return m_abbreviation_data; }
    ReadonlyBytes debug_strings_data() const { return m_debug_strings_data; }
    ReadonlyBytes debug_line_strings_data() const { return m_debug_line_strings_data; }
    ReadonlyBytes debug_range_lists_data() const { return m_debug_range_lists_data; }
    ReadonlyBytes debug_str_offsets_data() const { return m_debug_str_offsets_data; }
    ReadonlyBytes debug_addr_data() const { return m_debug_addr_data; }

    template<typename Callback>
    void for_each_compilation_unit(Callback) const;

    AttributeValue get_attribute_value(AttributeDataForm form, ssize_t implicit_const_value,
        InputMemoryStream& debug_info_stream, const CompilationUnit* unit = nullptr) const;

    Optional<DIE> get_die_at_address(FlatPtr) const;

    // Note that even if there is a DIE at the given offset,
    // but it does not exist in the DIE cache (because for example
    // it does not contain an address range), then this function will not return it.
    // To get any DIE object at a given offset in a compilation unit,
    // use CompilationUnit::get_die_at_offset.
    Optional<DIE> get_cached_die_at_offset(FlatPtr) const;

private:
    void populate_compilation_units();
    void build_cached_dies() const;

    ReadonlyBytes section_data(StringView section_name) const;

    ELF::Image const& m_elf;
    ReadonlyBytes m_debug_info_data;
    ReadonlyBytes m_abbreviation_data;
    ReadonlyBytes m_debug_strings_data;
    ReadonlyBytes m_debug_line_data;
    ReadonlyBytes m_debug_line_strings_data;
    ReadonlyBytes m_debug_range_lists_data;
    ReadonlyBytes m_debug_str_offsets_data;
    ReadonlyBytes m_debug_addr_data;

    NonnullOwnPtrVector<Dwarf::CompilationUnit> m_compilation_units;

    struct DIERange {
        FlatPtr start_address { 0 };
        FlatPtr end_address { 0 };
    };

    struct DIEAndRange {
        DIE die;
        DIERange range;
    };

    using DIEStartAddress = FlatPtr;

    mutable RedBlackTree<DIEStartAddress, DIEAndRange> m_cached_dies_by_range;
    mutable RedBlackTree<FlatPtr, DIE> m_cached_dies_by_offset;
    mutable bool m_built_cached_dies { false };
};

template<typename Callback>
void DwarfInfo::for_each_compilation_unit(Callback callback) const
{
    for (const auto& unit : m_compilation_units) {
        callback(unit);
    }
}

}
