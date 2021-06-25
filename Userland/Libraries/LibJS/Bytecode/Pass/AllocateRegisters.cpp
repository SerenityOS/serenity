/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <AK/QuickSort.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/PassManager.h>

// This pass implements a simple linear-pass register allocator to reduce the
// number of registers used in an executable, as described in the following paper:
// Poletto, Massimiliano; Sarkar, Vivek (1999). "Linear scan register allocation".

namespace JS::Bytecode::Passes {

// This pass assumes that the registers are in single static Assignment form

void AllocateRegisters::id_basic_blocks()
{
    m_basic_block_ids.clear();

    for (size_t i = 0; i < m_executable->executable.basic_blocks.size(); ++i) {
        m_basic_block_ids.set(&(m_executable->executable.basic_blocks[i]), i);
    }
}

void AllocateRegisters::find_block_range(const BasicBlock* basic_block, size_t max_bb)
{
    for (auto i = InstructionStreamIterator(basic_block->instruction_stream()); !i.at_end(); ++i) {
        auto& inst = *i;

        for (auto& reg : inst.write_registers()) {
            // Verify that our program is in SSA.
            VERIFY(!m_has_been_written[reg->index()]);
            m_has_been_written[reg->index()] = true;

            m_is_live[reg->index()] = true;
            m_live_ranges[reg->index()].reg = reg->index();
            m_live_ranges[reg->index()].start = { block_id(basic_block), i.offset() };
            m_live_ranges[reg->index()].finish = { 0, 0 };
        }

        for (auto& reg : inst.read_registers()) {
            if (max_bb < m_live_ranges[reg->index()].finish.basic_block)
                continue;

            if (block_id(basic_block) == max_bb) {
                m_live_ranges[reg->index()].finish = { block_id(basic_block), i.offset() };
            } else {
                // The maximum size_t is equivalent to the end of the basic block.
                m_live_ranges[reg->index()].finish = { max_bb, NumericLimits<size_t>::max() };
            }
        }
    }
}

void AllocateRegisters::find_live_ranges(const BasicBlock* basic_block, size_t max_bb)
{
    if (m_live_range_path.contains_slow(basic_block))
        return;

    m_live_range_path.append(basic_block);
    max_bb = max(block_id(basic_block), max_bb);

    find_block_range(basic_block, max_bb);

    const auto& children = m_executable->cfg.value().get(basic_block);
    if (children.has_value()) {
        for (auto& child : children.value()) {
            find_live_ranges(child, max_bb);
        }
    }

    m_live_range_path.take_last();
}

Vector<Register> AllocateRegisters::rename_registers()
{
    Vector<Register> rename;
    for (size_t i = 0; i < m_executable->executable.number_of_registers; ++i) {
        rename.append(Register(i));
    }

    Vector<size_t> active;
    active.resize(m_executable->executable.number_of_registers);
    for (auto& i : active) {
        i = 0;
    }

    quick_sort(m_live_ranges, [](LiveRange a, LiveRange b) { return a.start < b.start; });

    for (size_t i = 2; i < m_live_ranges.size(); ++i) {
        auto& timestamp = m_live_ranges[i].start;

        // Expire old intervals.
        for (size_t j = 2; j < i; ++j) {
            auto reg = rename[m_live_ranges[j].reg].index();
            if (m_live_ranges[j].finish < timestamp && active[reg] == j) {
                active[reg] = 0;
            }
        }

        // Select a register.
        for (size_t j = 2; j <= i; ++j) {
            if (!active[j]) {
                rename[m_live_ranges[i].reg] = Register(j);
                active[j] = i;
                break;
            }
        }
    }

    return rename;
}

void AllocateRegisters::apply_register_rename(Vector<Register> const& rename)
{
    for (auto& basic_block : m_executable->executable.basic_blocks) {
        for (auto i = InstructionStreamIterator(basic_block.instruction_stream()); !i.at_end(); ++i) {
            auto& inst = *i;
            for (auto reg : inst.write_registers()) {
                *reg = rename[reg->index()];
            }

            for (auto reg : inst.read_registers()) {
                *reg = rename[reg->index()];
            }
        }
    }

    u32 number_of_registers = 0;
    for (auto& reg : rename) {
        number_of_registers = max(number_of_registers, reg.index() + 1);
    }

    m_executable->executable.number_of_registers = number_of_registers;
}

void AllocateRegisters::perform(PassPipelineExecutable& executable)
{
    started();

    VERIFY(executable.cfg.has_value());
    m_executable = &executable;

    id_basic_blocks();

    m_is_live.resize(m_executable->executable.number_of_registers);
    m_has_been_written.resize(m_executable->executable.number_of_registers);
    m_live_ranges.resize(m_executable->executable.number_of_registers);

    for (auto& i : m_is_live) {
        i = false;
    }

    for (auto& i : m_has_been_written) {
        i = false;
    }

    find_live_ranges(&(m_executable->executable.basic_blocks.first()), 0);

    auto rename = rename_registers();

    apply_register_rename(rename);

    finished();
}

}
