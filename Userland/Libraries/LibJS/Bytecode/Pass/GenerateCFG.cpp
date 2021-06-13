/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

void GenerateCFG::perform(PassPipelineExecutable& executable)
{
    started();

    executable.cfg = HashMap<BasicBlock const*, HashTable<BasicBlock const*>> {};
    executable.inverted_cfg = HashMap<BasicBlock const*, HashTable<BasicBlock const*>> {};
    executable.exported_blocks = HashTable<BasicBlock const*> {};
    Vector<InstructionStreamIterator> iterators;
    Vector<BasicBlock const&> entered_blocks;
    HashTable<BasicBlock const*> seen_blocks;

    auto enter_label = [&](auto const& label, auto& entering_block, bool exported = false) {
        auto& entry = executable.cfg->ensure(&entering_block);
        entry.set(&label->block());
        auto& inverse_entry = executable.inverted_cfg->ensure(&label->block());
        inverse_entry.set(&entering_block);

        if (exported)
            executable.exported_blocks->set(&label->block());

        if (!seen_blocks.contains(&label->block())) {
            seen_blocks.set(&label->block());
            entered_blocks.append(label->block());
            iterators.empend(label->block().instruction_stream());
        }
    };

    seen_blocks.set(&executable.executable.basic_blocks.first());
    entered_blocks.append(executable.executable.basic_blocks.first());
    iterators.empend(executable.executable.basic_blocks.first().instruction_stream());

    while (!entered_blocks.is_empty()) {
        if (iterators.last().at_end()) {
            entered_blocks.take_last();
            iterators.take_last();
            continue;
        }
        auto& instruction = *iterators.last();
        ++iterators.last();
        if (!instruction.is_terminator())
            continue;

        auto& current_block = entered_blocks.last();

        if (instruction.type() == Instruction::Type::Jump) {
            auto& true_target = static_cast<Op::Jump const&>(instruction).true_target();
            enter_label(true_target, current_block);
            continue;
        }

        if (instruction.type() == Instruction::Type::JumpConditional || instruction.type() == Instruction::Type::JumpNullish || instruction.type() == Instruction::Type::JumpUndefined) {
            auto& true_target = static_cast<Op::Jump const&>(instruction).true_target();
            enter_label(true_target, current_block);
            auto& false_target = static_cast<Op::Jump const&>(instruction).false_target();
            enter_label(false_target, current_block);
            continue;
        }

        if (instruction.type() == Instruction::Type::Yield) {
            auto& continuation = static_cast<Op::Yield const&>(instruction).continuation();
            if (continuation.has_value())
                enter_label(continuation, current_block, true);
            continue;
        }

        if (instruction.type() == Instruction::Type::EnterUnwindContext) {
            auto& entry_point = static_cast<Op::EnterUnwindContext const&>(instruction).entry_point();
            auto& handler_target = static_cast<Op::EnterUnwindContext const&>(instruction).handler_target();
            auto& finalizer_target = static_cast<Op::EnterUnwindContext const&>(instruction).finalizer_target();
            enter_label(&entry_point, current_block);
            if (handler_target.has_value())
                enter_label(handler_target, current_block);
            if (finalizer_target.has_value())
                enter_label(finalizer_target, current_block);
            continue;
        }

        if (instruction.type() == Instruction::Type::ContinuePendingUnwind) {
            auto& resume_target = static_cast<Op::ContinuePendingUnwind const&>(instruction).resume_target();
            enter_label(&resume_target, current_block);
            continue;
        }

        // Otherwise, pop the current block off, it doesn't jump anywhere.
        iterators.take_last();
        entered_blocks.take_last();
    }

    finished();
}

}
