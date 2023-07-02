/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Bitmap.h>
#include <AK/TypeCasts.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

static NonnullOwnPtr<BasicBlock> eliminate_loads(BasicBlock const& block, size_t number_of_registers)
{
    auto array_ranges = Bitmap::create(number_of_registers, false).release_value_but_fixme_should_propagate_errors();

    for (auto it = InstructionStreamIterator(block.instruction_stream()); !it.at_end(); ++it) {
        if ((*it).type() == Instruction::Type::NewArray) {
            Op::NewArray const& array_instruction = static_cast<Op::NewArray const&>(*it);
            if (size_t element_count = array_instruction.element_count())
                array_ranges.set_range<true, false>(array_instruction.start().index(), element_count);
        } else if ((*it).type() == Instruction::Type::Call) {
            auto const& call_instruction = static_cast<Op::Call const&>(*it);
            if (size_t element_count = call_instruction.argument_count())
                array_ranges.set_range<true, false>(call_instruction.first_argument().index(), element_count);
        }
    }

    auto new_block = BasicBlock::create(block.name(), block.size());
    HashMap<size_t, Register> identifier_table {};
    HashMap<u32, Register> register_rerouting_table {};

    for (auto it = InstructionStreamIterator(block.instruction_stream()); !it.at_end();) {
        using enum Instruction::Type;

        // Note: When creating a variable, we technically purge the cache of any
        //       variables of the same name;
        //       In practice, we always generate a coinciding SetVariable, which
        //       does the same
        switch ((*it).type()) {
        case GetVariable: {
            auto const& get_variable = static_cast<Op::GetVariable const&>(*it);
            ++it;
            auto const& next_instruction = *it;

            if (auto reg = identifier_table.find(get_variable.identifier().value()); reg != identifier_table.end()) {
                // If we have already seen a variable, we can replace its GetVariable with a simple Load
                // knowing that it was already stored in a register
                new (new_block->next_slot()) Op::Load(reg->value);
                new_block->grow(sizeof(Op::Load));

                if (next_instruction.type() == Instruction::Type::Store) {
                    // If the next instruction is a Store, that is not meant to
                    // construct an array, we can simply elide that store and reroute
                    // all further references to the stores destination to the cached
                    // instance of variable.
                    // FIXME: We might be able to elide the previous load in the non-array case,
                    //        because we do not yet reuse the accumulator
                    auto const& store = static_cast<Op::Store const&>(next_instruction);

                    if (array_ranges.get(store.dst().index())) {
                        // re-emit the store
                        new (new_block->next_slot()) Op::Store(store);
                        new_block->grow(sizeof(Op::Store));
                    } else {
                        register_rerouting_table.set(store.dst().index(), reg->value);
                    }

                    ++it;
                }

                continue;
            }
            // Otherwise we need to emit the GetVariable
            new (new_block->next_slot()) Op::GetVariable(get_variable);
            new_block->grow(sizeof(Op::GetVariable));

            // And if the next instruction is a Store, we can cache it's destination
            if (next_instruction.type() == Instruction::Type::Store) {
                auto const& store = static_cast<Op::Store const&>(next_instruction);
                identifier_table.set(get_variable.identifier().value(), store.dst());

                new (new_block->next_slot()) Op::Store(store);
                new_block->grow(sizeof(Op::Store));
                ++it;
            }

            continue;
        }
        case SetVariable: {
            // When a variable is set we need to remove it from the cache, because
            // we don't have an accurate view on it anymore
            // FIXME: If the previous instruction was a `Load $reg`, we could
            //        update the cache instead
            auto const& set_variable = static_cast<Op::SetVariable const&>(*it);

            identifier_table.remove(set_variable.identifier().value());

            break;
        }
        case DeleteVariable: {
            // When a variable is deleted we need to remove it from the cache, it does not
            // exist anymore, although a variable of the same name may exist in upper scopes
            auto const& set_variable = static_cast<Op::DeleteVariable const&>(*it);

            identifier_table.remove(set_variable.identifier().value());

            break;
        }
        case Store: {
            // If we store to a position that we have are rerouting from,
            // we need to remove it from the routeing table
            // FIXME: This may be redundant due to us assigning to registers only once
            auto const& store = static_cast<Op::Store const&>(*it);
            register_rerouting_table.remove(store.dst().index());

            break;
        }
        case DeleteById:
        case DeleteByValue:
            // These can trigger proxies, which call into user code
            // So these are treated like calls
        case GetByValue:
        case GetByValueWithThis:
        case GetById:
        case GetByIdWithThis:
        case PutByValue:
        case PutByValueWithThis:
        case PutById:
        case PutByIdWithThis:
            // Attribute accesses (`a.o` or `a[o]`) may result in calls to getters or setters
            // or may trigger proxies
            // So these are treated like calls
        case Call:
        case CallWithArgumentArray:
            // Calls, especially to local functions and eval, may poison visible and
            // cached variables, hence we need to clear the lookup cache after emitting them
            // FIXME: In strict mode and with better identifier metrics, we might be able
            //        to safe some caching with a more fine-grained identifier table
            // FIXME: We might be able to save some lookups to objects like `this`
            //        which should not change their pointer
            memcpy(new_block->next_slot(), &*it, (*it).length());
            for (auto route : register_rerouting_table)
                reinterpret_cast<Instruction*>(new_block->next_slot())->replace_references(Register { route.key }, route.value);
            new_block->grow((*it).length());

            identifier_table.clear_with_capacity();

            ++it;
            continue;
        case NewBigInt:
            // FIXME: This is the only non trivially copyable Instruction,
            //        so we need to do some extra work here
            new (new_block->next_slot()) Op::NewBigInt(static_cast<Op::NewBigInt const&>(*it));
            new_block->grow(sizeof(Op::NewBigInt));
            ++it;
            continue;
        default:
            break;
        }

        memcpy(new_block->next_slot(), &*it, (*it).length());
        for (auto route : register_rerouting_table) {
            // rerouting from key to value
            reinterpret_cast<Instruction*>(new_block->next_slot())->replace_references(Register { route.key }, route.value);
        }
        // because we are replacing the current block, we need to replace references
        // to ourselves here
        reinterpret_cast<Instruction*>(new_block->next_slot())->replace_references(block, *new_block);

        new_block->grow((*it).length());

        ++it;
    }
    return new_block;
}

void EliminateLoads::perform(PassPipelineExecutable& executable)
{
    started();

    // FIXME: If we walk the CFG instead of the block list, we might be able to
    //        save some work between blocks
    for (auto it = executable.executable.basic_blocks.begin(); it != executable.executable.basic_blocks.end(); ++it) {
        auto const& old_block = *it;
        auto new_block = eliminate_loads(*old_block, executable.executable.number_of_registers);

        // We will replace the old block, with a new one, so we need to replace all references,
        // to the old one with the new one
        for (auto& block : executable.executable.basic_blocks) {
            InstructionStreamIterator it { block->instruction_stream() };
            while (!it.at_end()) {
                auto& instruction = *it;
                ++it;
                const_cast<Instruction&>(instruction).replace_references(*old_block, *new_block);
            }
        }

        executable.executable.basic_blocks[it.index()] = move(new_block);
    }

    finished();
}

}
