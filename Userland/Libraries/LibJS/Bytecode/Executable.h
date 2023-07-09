/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/StringTable.h>

namespace JS::Bytecode {

struct PropertyLookupCache {
    WeakPtr<Shape> shape;
    Optional<u32> property_offset;
};

struct Executable {
    DeprecatedFlyString name;
    Vector<PropertyLookupCache> property_lookup_caches;
    Vector<NonnullOwnPtr<BasicBlock>> basic_blocks;
    NonnullOwnPtr<StringTable> string_table;
    NonnullOwnPtr<IdentifierTable> identifier_table;
    size_t number_of_registers { 0 };
    bool is_strict_mode { false };

    DeprecatedString const& get_string(StringTableIndex index) const { return string_table->get(index); }
    DeprecatedFlyString const& get_identifier(IdentifierTableIndex index) const { return identifier_table->get(index); }

    void dump() const;
};

}
