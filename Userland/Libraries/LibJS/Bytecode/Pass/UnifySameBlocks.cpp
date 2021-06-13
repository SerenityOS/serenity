/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>
#include <string.h>

namespace JS::Bytecode::Passes {

void UnifySameBlocks::perform(PassPipelineExecutable& executable)
{
    started();

    VERIFY(executable.cfg.has_value());
    VERIFY(executable.inverted_cfg.has_value());
    auto cfg = executable.cfg.release_value();
    auto inverted_cfg = executable.inverted_cfg.release_value();

    HashMap<BasicBlock const*, BasicBlock const*> equal_blocks;

    for (size_t i = 0; i < executable.executable.basic_blocks.size(); ++i) {
        auto& block = executable.executable.basic_blocks[i];
        auto block_bytes = block.instruction_stream();
        for (auto& candidate_block : executable.executable.basic_blocks.span().slice(i + 1)) {
            // FIXME: This can probably be relaxed a bit...
            if (candidate_block->size() != block.size())
                continue;

            auto candidate_bytes = candidate_block->instruction_stream();
            if (memcmp(candidate_bytes.data(), block_bytes.data(), candidate_block->size()) == 0)
                equal_blocks.set(&*candidate_block, &block);
        }
    }

    auto replace_blocks = [&](auto& match, auto& replacement) {
        Optional<size_t> first_successor_position;
        auto it = executable.executable.basic_blocks.find_if([match](auto& block) { return match == block; });
        VERIFY(!it.is_end());
        executable.executable.basic_blocks.remove(it.index());
        if (!first_successor_position.has_value())
            first_successor_position = it.index();

        for (auto& block : executable.executable.basic_blocks) {
            InstructionStreamIterator it { block.instruction_stream() };
            while (!it.at_end()) {
                auto& instruction = *it;
                ++it;
                const_cast<Instruction&>(instruction).replace_references(*match, replacement);
            }
        }
        return first_successor_position;
    };

    for (auto& entry : equal_blocks)
        (void)replace_blocks(entry.key, *entry.value);

    finished();
}

}
