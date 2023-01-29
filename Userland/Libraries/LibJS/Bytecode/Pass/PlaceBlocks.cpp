/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

void PlaceBlocks::perform(PassPipelineExecutable& executable)
{
    started();

    VERIFY(executable.cfg.has_value());
    auto cfg = executable.cfg.release_value();

    Vector<BasicBlock&> replaced_blocks;
    HashTable<BasicBlock const*> reachable_blocks;

    // Visit the blocks in CFG order
    Function<void(BasicBlock const*)> visit = [&](auto* block) {
        if (reachable_blocks.contains(block))
            return;

        reachable_blocks.set(block);
        replaced_blocks.append(*const_cast<BasicBlock*>(block));

        auto children = cfg.find(block);
        if (children == cfg.end())
            return;

        for (auto& entry : children->value)
            visit(entry);
    };

    // Make sure to visit the entry block first
    visit(&executable.executable.basic_blocks.first());

    for (auto& entry : cfg)
        visit(entry.key);

    // Put the unreferenced blocks back in at the end
    for (auto& entry : static_cast<Vector<NonnullOwnPtr<BasicBlock>>&>(executable.executable.basic_blocks)) {
        if (reachable_blocks.contains(entry.ptr()))
            (void)entry.leak_ptr();
        else
            replaced_blocks.append(*entry.leak_ptr()); // Don't try to do DCE here.
    }

    executable.executable.basic_blocks.clear();
    for (auto& block : replaced_blocks)
        executable.executable.basic_blocks.append(adopt_own(block));

    finished();
}

}
