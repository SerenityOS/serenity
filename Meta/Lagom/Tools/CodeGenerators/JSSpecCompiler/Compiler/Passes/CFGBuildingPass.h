/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>

#include "Compiler/ControlFlowGraph.h"
#include "Compiler/GenericASTPass.h"

namespace JSSpecCompiler {

class CFGBuildingPass
    : public IntraproceduralCompilerPass
    , private RecursiveASTVisitor {
public:
    inline static constexpr StringView name = "cfg-building"sv;

    using IntraproceduralCompilerPass::IntraproceduralCompilerPass;

protected:
    void process_function() override;
    RecursionDecision on_entry(Tree tree) override;
    void on_leave(Tree tree) override;

private:
    BasicBlockRef create_empty_block();
    BasicBlockRef exchange_current_with_empty();
    void will_be_used_as_expression(Tree const& tree);

    ControlFlowGraph* m_cfg;
    BasicBlockRef m_current_block;
    Vector<bool> m_is_expression_stack;
};

}
