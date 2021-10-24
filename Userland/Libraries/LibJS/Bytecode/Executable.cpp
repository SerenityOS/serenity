/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Executable.h>

namespace JS::Bytecode {

void Executable::dump() const
{
    dbgln("\033[33;1mJS::Bytecode::Executable\033[0m ({})", name);
    for (auto& block : basic_blocks)
        block.dump(*this);
    if (!string_table->is_empty()) {
        outln();
        string_table->dump();
    }
    if (!identifier_table->is_empty()) {
        outln();
        identifier_table->dump();
    }
}

}
