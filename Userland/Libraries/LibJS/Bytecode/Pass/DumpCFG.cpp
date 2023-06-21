/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

void DumpCFG::perform(PassPipelineExecutable& executable)
{
    started();

    VERIFY(executable.cfg.has_value());
    outln(m_file, "CFG Dump for {} basic blocks:", executable.executable.basic_blocks.size());
    for (auto& entry : executable.cfg.value()) {
        for (auto& value : entry.value)
            outln(m_file, "{} -> {}", entry.key->name(), value->name());
    }
    outln(m_file);

    finished();
}

}
