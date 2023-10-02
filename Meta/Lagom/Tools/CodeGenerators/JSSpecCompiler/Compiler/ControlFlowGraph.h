/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>

#include "Forward.h"

namespace JSSpecCompiler {

class BasicBlock : public RefCounted<BasicBlock> {
public:
    BasicBlock(size_t index, NonnullRefPtr<ControlFlowOperator> continuation)
        : m_index(index)
        , m_continuation(move(continuation))
    {
    }

    size_t m_index;
    Vector<Tree> m_expressions;
    NonnullRefPtr<ControlFlowOperator> m_continuation;
};

class ControlFlowGraph : public RefCounted<ControlFlowGraph> {
public:
    ControlFlowGraph() { }

    size_t blocks_count() const { return blocks.size(); }

    Vector<NonnullRefPtr<BasicBlock>> blocks;
    BasicBlockRef start_block;
    BasicBlockRef end_block;
};

}

namespace AK {

template<>
struct Formatter<JSSpecCompiler::ControlFlowGraph> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, JSSpecCompiler::ControlFlowGraph const& control_flow_graph);
};

}
