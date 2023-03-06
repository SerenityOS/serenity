/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/StringTable.h>

namespace JS::Bytecode {

struct Executable {
    DeprecatedFlyString name;
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
