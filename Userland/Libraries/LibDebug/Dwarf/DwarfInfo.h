/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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
    explicit DwarfInfo(const ELF::Image&);

    ReadonlyBytes debug_info_data() const { return m_debug_info_data; }
    ReadonlyBytes abbreviation_data() const { return m_abbreviation_data; }
    ReadonlyBytes debug_strings_data() const { return m_debug_strings_data; }
    ReadonlyBytes debug_line_strings_data() const { return m_debug_line_strings_data; }

    template<typename Callback>
    void for_each_compilation_unit(Callback) const;

    AttributeValue get_attribute_value(AttributeDataForm form, ssize_t implicit_const_value,
        InputMemoryStream& debug_info_stream, const CompilationUnit* unit = nullptr) const;

private:
    void populate_compilation_units();

    ReadonlyBytes section_data(const StringView& section_name) const;

    const ELF::Image& m_elf;
    ReadonlyBytes m_debug_info_data;
    ReadonlyBytes m_abbreviation_data;
    ReadonlyBytes m_debug_strings_data;
    ReadonlyBytes m_debug_line_data;
    ReadonlyBytes m_debug_line_strings_data;

    NonnullOwnPtrVector<Dwarf::CompilationUnit> m_compilation_units;
};

template<typename Callback>
void DwarfInfo::for_each_compilation_unit(Callback callback) const
{
    for (const auto& unit : m_compilation_units) {
        callback(unit);
    }
}

}
