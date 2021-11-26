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

        {
            InstructionStreamIterator it { entry.key->instruction_stream() };
            auto& first_instruction = *it;
            if (first_instruction.is_terminator()) {
                if (first_instruction.type() == Instruction::Type::Jump) {
                    auto replacing_block = &static_cast<Op::Jump const&>(first_instruction).true_target()->block();
                    if (replacing_block != entry.key)
                        blocks_to_replace.set(entry.key, replacing_block);
                    continue;
                }
            }
        }

        if (auto cfg_entry = inverted_cfg.get(*entry.value.begin()); cfg_entry.has_value()) {
            auto& predecssor_entry = *cfg_entry;
            if (predecssor_entry.size() != 1)
                continue;
        }

        // The two blocks are safe to merge.
        blocks_to_merge.set(entry.key);
    }

    for (auto& entry : blocks_to_replace) {
        auto replacement = entry.value;
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
            InstructionStreamIterator it { block.instruction_stream() };
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
        auto current_block = *it;
        blocks_to_merge.remove(it);
        Vector<BasicBlock const*> successors { current_block };
        for (;;) {
            auto last = successors.last();
            auto entry = cfg.get(last);
            if (!entry.has_value())
                break;
            auto& successor = *entry->begin();
            successors.append(successor);
            auto it = blocks_to_merge.find(successor);
            if (it == blocks_to_merge.end())
                break;
            blocks_to_merge.remove(it);
        }

        auto blocks_to_merge_copy = blocks_to_merge;
        for (auto& last : blocks_to_merge) {
            auto entry = cfg.get(last);
            if (!entry.has_value())
                continue;
            auto successor = *entry->begin();
            if (auto it = successors.find(successor); !it.is_end()) {
                successors.insert(it.index(), last);
                blocks_to_merge_copy.remove(last);
            }
        }

        blocks_to_merge = move(blocks_to_merge_copy);

        size_t size = 0;
        StringBuilder builder;
        builder.append("merge");
        for (auto& entry : successors) {
            size += entry->size();
            builder.append('.');
            builder.append(entry->name());
        }

        auto new_block = BasicBlock::create(builder.build(), size);
        auto& block = *new_block;

        size_t last_successor_index = successors.size() - 1;
        for (size_t i = 0; i < successors.size(); ++i) {
            auto& entry = successors[i];
            InstructionStreamIterator it { entry->instruction_stream() };
            size_t copy_end = 0;
            while (!it.at_end()) {
                auto& instruction = *it;
                ++it;
                if (instruction.is_terminator() && last_successor_index != i)
                    break;
                copy_end = it.offset();
            }
            __builtin_memcpy(block.next_slot(), entry->instruction_stream().data(), copy_end);
            block.grow(copy_end);
        }

        auto first_successor_position = replace_blocks(successors, *new_block);
        VERIFY(first_successor_position.has_value());
        executable.executable.basic_blocks.insert(*first_successor_position, move(new_block));
    }

    executable.executable.basic_blocks.remove_all_matching([&blocks_to_remove](auto& candidate) { return blocks_to_remove.contains_slow(candidate.ptr()); });

    finished();
}

}
