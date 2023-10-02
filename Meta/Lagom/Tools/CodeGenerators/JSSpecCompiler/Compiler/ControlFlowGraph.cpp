/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>

#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"

using namespace JSSpecCompiler;

ErrorOr<void> AK::Formatter<ControlFlowGraph>::format(FormatBuilder& format_builder, ControlFlowGraph const& control_flow_graph)
{
    auto& builder = format_builder.builder();

    for (auto const& block : control_flow_graph.blocks) {
        builder.appendff("{}:\n", block->m_index);
        for (auto const& expression : block->m_expressions)
            builder.appendff("{}", expression);
        builder.appendff("{}\n", Tree(block->m_continuation));
    }

    // Remove trailing \n
    builder.trim(1);

    return {};
}
