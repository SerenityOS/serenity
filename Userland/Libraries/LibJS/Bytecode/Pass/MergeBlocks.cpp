/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

void MergeBlocks::perform(PassPipelineExecutable& executable)
{
    started();

    VERIFY(executable.cfg.has_value());
    VERIFY(executable.inverted_cfg.has_value());
    auto cfg = executable.cfg.release_value();
    auto inverted_cfg = executable.inverted_cfg.release_value();

    // Figure out which blocks can be merged
    HashTable<BasicBlock const*> blocks_to_merge;
    HashMap<BasicBlock const*, BasicBlock const*> blocks_to_replace;
    Vector<BasicBlock const*> blocks_to_remove;
    Vector<size_t> boundaries;

    for (auto& entry : cfg) {
        if (entry.value.size() != 1)
            continue;

        if (executable.exported_blocks->contains(*entry.value.begin()))
            continue;

        if (!entry.key->is_terminated())
            continue;

        if (entry.key->terminator()->type() != Instruction::Type::Jump)
            continue;

        {
            InstructionStreamIterator it { entry.key->instruction_stream() };
            auto& first_instruction = *it;
            if (first_instruction.type() == Instruction::Type::Jump) {
                auto const* replacing_block = &static_cast<Op::Jump const&>(first_instruction).true_target()->block();
                if (replacing_block != entry.key) {
                    blocks_to_replace.set(entry.key, replacing_block);
                }
                continue;
            }
        }

        if (auto cfg_iter = inverted_cfg.find(*entry.value.begin()); cfg_iter != inverted_cfg.end()) {
            auto& predecessor_entry = cfg_iter->value;
            if (predecessor_entry.size() != 1)
                continue;
        }

        // The two blocks are safe to merge.
        blocks_to_merge.set(entry.key);
    }

    for (auto& entry : blocks_to_replace) {
        auto const* replacement = entry.value;
        for (;;) {
            auto lookup = blocks_to_replace.get(replacement);
            if (!lookup.has_value())
                break;
            if (replacement == *lookup)
                break;
            replacement = *lookup;
        }
        entry.value = replacement;
    }

    auto replace_blocks = [&](auto& blocks, auto& replacement) {
        Optional<size_t> first_successor_position;
        for (auto& entry : blocks) {
            blocks_to_remove.append(entry);
            auto it = executable.executable.basic_blocks.find_if([entry](auto& block) { return entry == block; });
            VERIFY(!it.is_end());
            if (!first_successor_position.has_value())
                first_successor_position = it.index();
        }
        for (auto& block : executable.executable.basic_blocks) {
            InstructionStreamIterator it { block->instruction_stream() };
            while (!it.at_end()) {
                auto& instruction = *it;
                ++it;
                for (auto& entry : blocks)
                    const_cast<Instruction&>(instruction).replace_references(*entry, replacement);
            }
        }
        return first_successor_position;
    };

    for (auto& entry : blocks_to_replace) {
        AK::Array candidates { entry.key };
        (void)replace_blocks(candidates, *entry.value);
    }

    while (!blocks_to_merge.is_empty()) {
        auto it = blocks_to_merge.begin();
        auto const* current_block = *it;
        blocks_to_merge.remove(it);

        Vector<BasicBlock const*> successors { current_block };
        for (;;) {
            auto const* last = successors.last();
            auto entry = cfg.find(last);
            if (entry == cfg.end())
                break;
            auto const* successor = *entry->value.begin();
            successors.append(successor);

            if (!blocks_to_merge.remove(successor))
                break;
        }

        auto blocks_to_merge_copy = blocks_to_merge;
        // We need to do the following multiple times, due to it not being
        // guaranteed, that the blocks are in sequential order
        bool did_prepend = true;
        while (did_prepend) {
            did_prepend = false;
            for (auto const* last : blocks_to_merge) {
                auto entry = cfg.find(last);
                if (entry == cfg.end())
                    continue;
                auto const* successor = *entry->value.begin();
                if (successor == successors.first()) {
                    successors.prepend(last);
                    blocks_to_merge_copy.remove(last);
                    did_prepend = true;
                }
            }
        }

        blocks_to_merge = move(blocks_to_merge_copy);

        StringBuilder builder;
        builder.append("merge"sv);
        for (auto& entry : successors) {
            builder.append('.');
            builder.append(entry->name());
        }

        auto new_block = BasicBlock::create(MUST(builder.to_string()));
        auto& block = *new_block;
        auto first_successor_position = replace_blocks(successors, *new_block);
        VERIFY(first_successor_position.has_value());

        size_t last_successor_index = successors.size() - 1;
        for (size_t i = 0; i < successors.size(); ++i) {
            auto& entry = successors[i];
            InstructionStreamIterator it { entry->instruction_stream() };
            while (!it.at_end()) {
                auto& instruction = *it;
                ++it;
                if (instruction.is_terminator() && last_successor_index != i)
                    break;
                // FIXME: Use a single memcpy to copy the whole block at once.
                auto instruction_size = instruction.length();
                size_t slot_offset = block.size();
                block.grow(instruction_size);
                auto* next_slot = block.data() + slot_offset;
                memcpy(next_slot, &instruction, instruction_size);
            }
        }

        executable.executable.basic_blocks.insert(*first_successor_position, move(new_block));
    }

    executable.executable.basic_blocks.remove_all_matching([&blocks_to_remove](auto& candidate) { return blocks_to_remove.contains_slow(candidate.ptr()); });

    finished();
}

}
