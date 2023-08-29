/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Compiler/Passes/DeadCodeEliminationPass.h"
#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"
#include "Compiler/StronglyConnectedComponents.h"
#include "Function.h"

namespace JSSpecCompiler {

void DeadCodeEliminationPass::process_function()
{
    with_graph(m_function->m_local_ssa_variables.size(), [&] {
        remove_unused_phi_nodes();
    });
    m_function->reindex_ssa_variables();
}

DeadCodeEliminationPass::Vertex DeadCodeEliminationPass::as_vertex(Variable* variable)
{
    return Vertex(variable->m_ssa);
}

RecursionDecision DeadCodeEliminationPass::on_entry(Tree tree)
{
    if (tree->is_statement())
        TODO();
    return RecursionDecision::Recurse;
}

void DeadCodeEliminationPass::on_leave(Tree tree)
{
    if (auto variable = as<Variable>(tree); variable)
        as_vertex(variable)->is_referenced = true;
}

void DeadCodeEliminationPass::remove_unused_phi_nodes()
{
    for (auto const& block : m_function->m_cfg->blocks) {
        for (auto const& phi_node : block->m_phi_nodes) {
            auto to = as_vertex(phi_node.var);
            for (auto const& branch : phi_node.branches) {
                auto from = as_vertex(branch.value);

                from->outgoing_edges.append(to);
                to->incoming_edges.append(from);
            }
        }

        for (auto& expr : block->m_expressions)
            run_in_subtree(expr);
        run_in_const_subtree(block->m_continuation);
    }

    // FIXME?: There surely must be a way to do this in a linear time without finding strongly
    //         connected components.
    for (auto const& component : find_strongly_connected_components(m_nodes)) {
        bool is_referenced = false;

        for (Vertex u : component)
            for (Vertex v : u->outgoing_edges)
                is_referenced |= v->is_referenced;

        if (is_referenced)
            for (Vertex u : component)
                u->is_referenced = true;
    }

    for (auto const& block : m_function->m_cfg->blocks) {
        block->m_phi_nodes.remove_all_matching([&](auto const& node) {
            return !as_vertex(node.var)->is_referenced;
        });
    }

    m_function->m_local_ssa_variables.remove_all_matching([&](auto const& variable) {
        return !Vertex(variable)->is_referenced;
    });
}

}
