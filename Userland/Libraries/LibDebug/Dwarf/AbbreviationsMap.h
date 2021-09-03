/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DwarfTypes.h"
#include <YAK/HashMap.h>
#include <YAK/Optional.h>
#include <YAK/Types.h>

namespace Debug::Dwarf {

class DwarfInfo;

class AbbreviationsMap {
public:
    AbbreviationsMap(DwarfInfo const& dwarf_info, u32 offset);

    struct AbbreviationEntry {

        EntryTag tag;
        bool has_children;

        Vector<AttributeSpecification> attribute_specifications;
    };

    Optional<AbbreviationEntry> get(u32 code) const;

private:
    void populate_map();

    DwarfInfo const& m_dwarf_info;
    u32 m_offset { 0 };
    HashMap<u32, AbbreviationEntry> m_entries;
};

}
