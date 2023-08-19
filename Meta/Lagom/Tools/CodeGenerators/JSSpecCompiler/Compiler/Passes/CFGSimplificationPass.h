/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Compiler/CompilerPass.h"
#include "Compiler/ControlFlowGraph.h"

namespace JSSpecCompiler {

// CFGSimplificationPass removes empty `BasicBlock`s with an unconditional jump continuation. It
// also removes unreferenced blocks from the graph.
class CFGSimplificationPass : public IntraproceduralCompilerPass {
public:
    inline static constexpr StringView name = "cfg-simplification"sv;

    using IntraproceduralCompilerPass::IntraproceduralCompilerPass;

protected:
    void process_function() override;

private:
    enum class State : char {
        NotUsed,
        CurrentlyInside,
        Used,
    };

    bool compute_replacement_block(size_t i);
    void compute_referenced_blocks(BasicBlockRef block);

    Vector<BasicBlockRef> m_replacement;
    Vector<State> m_state;
};

}
