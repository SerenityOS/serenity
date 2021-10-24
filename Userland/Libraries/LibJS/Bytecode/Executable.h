/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/StringTable.h>

namespace JS::Bytecode {

struct Executable {
    NonnullOwnPtrVector<BasicBlock> basic_blocks;
    NonnullOwnPtr<StringTable> string_table;
    size_t number_of_registers { 0 };

    String const& get_string(StringTableIndex index) const { return string_table->get(index); }

    void dump() const;
};

}
