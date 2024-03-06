/*
 * Copyright (c) 2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

void Peephole::perform(PassPipelineExecutable& executable)
{
    started();

    // Fuse compare-followed-by-jump into a single compare-and-jump
    // This is a very common pattern in bytecode, and it's nice to have it as a single instruction
    // For example, LessThan + JumpIf => JumpLessThan

    HashMap<BasicBlock const*, BasicBlock const*> replacement_blocks;
    Vector<NonnullOwnPtr<BasicBlock>> replaced_blocks;

    for (size_t i = 0; i < executable.executable.basic_blocks.size(); ++i) {
        auto& block = executable.executable.basic_blocks[i];
        auto new_block = BasicBlock::create(block->name());
        if (block->handler())
            new_block->set_handler(*block->handler());
        if (block->finalizer())
            new_block->set_finalizer(*block->finalizer());
        replacement_blocks.set(block.ptr(), new_block.ptr());

        InstructionStreamIterator it { block->instruction_stream() };
        while (!it.at_end()) {
            auto const& instruction = *it;
            ++it;

            if (!it.at_end()) {
                auto const& next_instruction = *it;
                if (next_instruction.type() == Instruction::Type::JumpIf) {
                    auto const& jump = static_cast<Op::JumpIf const&>(next_instruction);

#define DO_FUSE_JUMP(PreOp, ...)                                          \
    if (instruction.type() == Instruction::Type::PreOp) {                 \
        auto const& compare = static_cast<Op::PreOp const&>(instruction); \
        VERIFY(jump.condition() == compare.dst());                        \
        new_block->append<Op::Jump##PreOp>(                               \
            compare.source_record().source_start_offset,                  \
            compare.source_record().source_end_offset,                    \
            compare.lhs(),                                                \
            compare.rhs(),                                                \
            *jump.true_target(),                                          \
            *jump.false_target());                                        \
        ++it;                                                             \
        VERIFY(it.at_end());                                              \
        continue;                                                         \
    }
                    JS_ENUMERATE_FUSABLE_BINARY_OPS(DO_FUSE_JUMP)
                }
            }

            auto slot_offset = new_block->size();
            new_block->grow(instruction.length());
            memcpy(new_block->data() + slot_offset, &instruction, instruction.length());
            if (instruction.is_terminator())
                new_block->terminate(slot_offset);
        }
        replaced_blocks.append(move(executable.executable.basic_blocks[i]));
        executable.executable.basic_blocks[i] = move(new_block);
    }

    auto update_block_references = [&](BasicBlock const& original, BasicBlock const& replacement) {
        for (auto& block : executable.executable.basic_blocks) {
            InstructionStreamIterator it { block->instruction_stream() };
            if (block->handler() == &original)
                block->set_handler(replacement);
            if (block->finalizer() == &original)
                block->set_finalizer(replacement);
            while (!it.at_end()) {
                auto const& instruction = *it;
                ++it;
                const_cast<Instruction&>(instruction).replace_references(original, replacement);
            }
        }
    };
    for (auto& entry : replacement_blocks)
        update_block_references(*entry.key, *entry.value);

    finished();
}

}
