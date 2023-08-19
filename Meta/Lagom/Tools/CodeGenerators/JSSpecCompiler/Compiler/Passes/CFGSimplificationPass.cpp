/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Compiler/Passes/CFGSimplificationPass.h"
#include "AST/AST.h"
#include "Function.h"

namespace JSSpecCompiler {

void CFGSimplificationPass::process_function()
{
    auto& graph = *m_function->m_cfg;

    m_replacement.clear();
    m_replacement.resize(graph.blocks_count());
    m_state.clear();
    m_state.resize(graph.blocks_count());

    for (auto const& block : graph.blocks) {
        m_replacement[block->m_index] = block;
        if (block->m_expressions.size() == 0)
            if (auto jump = as<ControlFlowJump>(block->m_continuation); jump)
                m_replacement[block->m_index] = jump->m_block;
    }

    for (size_t i = 0; i < graph.blocks_count(); ++i)
        if (m_state[i] == State::NotUsed)
            VERIFY(compute_replacement_block(i));

    // Fixing references
    graph.start_block = m_replacement[graph.start_block->m_index];
    for (auto const& block : graph.blocks) {
        for (auto* next_block : block->m_continuation->references())
            *next_block = m_replacement[(*next_block)->m_index];
    }

    // Removing unused nodes
    m_state.span().fill(State::NotUsed);
    compute_referenced_blocks(graph.start_block);

    size_t j = 0;
    for (size_t i = 0; i < graph.blocks_count(); ++i) {
        if (m_state[graph.blocks[i]->m_index] == State::Used) {
            graph.blocks[j] = graph.blocks[i];
            graph.blocks[j]->m_index = j;
            ++j;
        }
    }
    graph.blocks.shrink(j);
}

bool CFGSimplificationPass::compute_replacement_block(size_t i)
{
    if (m_state[i] == State::CurrentlyInside)
        return false;
    VERIFY(m_state[i] == State::NotUsed);
    m_state[i] = State::CurrentlyInside;

    size_t j = m_replacement[i]->m_index;

    if (i == j)
        return true;
    if (m_state[j] == State::NotUsed)
        if (!compute_replacement_block(j))
            return false;
    m_replacement[i] = m_replacement[j];

    m_state[i] = State::Used;
    return true;
}

void CFGSimplificationPass::compute_referenced_blocks(BasicBlockRef block)
{
    if (m_state[block->m_index] == State::Used)
        return;
    m_state[block->m_index] = State::Used;

    for (auto* next : block->m_continuation->references())
        compute_referenced_blocks(*next);
}

}
