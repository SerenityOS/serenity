/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>

#include "AST/AST.h"
#include "Compiler/Passes/CFGBuildingPass.h"
#include "Function.h"

namespace JSSpecCompiler {

void CFGBuildingPass::process_function()
{
    m_cfg = m_function->m_cfg = make_ref_counted<ControlFlowGraph>();
    m_current_block = m_cfg->start_block = create_empty_block();
    m_cfg->end_block = create_empty_block();
    m_cfg->end_block->m_continuation = make_ref_counted<ControlFlowFunctionReturn>(
        make_ref_counted<Variable>(m_function->m_named_return_value));
    m_is_expression_stack = { false };

    run_in_subtree(m_function->m_ast);

    // FIXME: What should we do if control flow reached the end of the function? Returning
    //        error_tree will 100% confuse future passes.
    m_current_block->m_expressions.append(make_ref_counted<BinaryOperation>(
        BinaryOperator::Assignment,
        make_ref_counted<Variable>(m_function->m_named_return_value),
        error_tree));
    m_current_block->m_continuation = make_ref_counted<ControlFlowJump>(m_cfg->end_block);
}

RecursionDecision CFGBuildingPass::on_entry(Tree tree)
{
    m_is_expression_stack.append(!as<Expression>(tree).is_null());

    if (auto if_else_if_chain = as<IfElseIfChain>(tree); if_else_if_chain) {
        auto* end_block = create_empty_block();

        for (auto [i, current_condition] : enumerate(if_else_if_chain->m_conditions)) {
            run_in_subtree(current_condition);
            will_be_used_as_expression(current_condition);
            auto* condition_block = exchange_current_with_empty();

            auto* branch_entry = m_current_block;
            run_in_subtree(if_else_if_chain->m_branches[i]);
            auto* branch_return = exchange_current_with_empty();
            branch_return->m_continuation = make_ref_counted<ControlFlowJump>(end_block);

            condition_block->m_continuation = make_ref_counted<ControlFlowBranch>(current_condition, branch_entry, m_current_block);
        }

        if (if_else_if_chain->m_else_branch)
            run_in_const_subtree(if_else_if_chain->m_else_branch);
        m_current_block->m_continuation = make_ref_counted<ControlFlowJump>(end_block);

        m_current_block = end_block;
        return RecursionDecision::Continue;
    }

    if (auto return_node = as<ReturnNode>(tree); return_node) {
        Tree return_assignment = make_ref_counted<BinaryOperation>(
            BinaryOperator::Assignment,
            make_ref_counted<Variable>(m_function->m_named_return_value),
            return_node->m_return_value);
        run_in_subtree(return_assignment);
        auto* return_block = exchange_current_with_empty();
        return_block->m_continuation = make_ref_counted<ControlFlowJump>(m_cfg->end_block);

        return RecursionDecision::Continue;
    }

    return RecursionDecision::Recurse;
}

void CFGBuildingPass::on_leave(Tree tree)
{
    (void)m_is_expression_stack.take_last();

    if (!m_is_expression_stack.last() && as<Expression>(tree))
        m_current_block->m_expressions.append(tree);
}

BasicBlockRef CFGBuildingPass::create_empty_block()
{
    m_cfg->blocks.append(make_ref_counted<BasicBlock>(m_cfg->blocks_count(), invalid_continuation));
    return m_cfg->blocks.last();
}

BasicBlockRef CFGBuildingPass::exchange_current_with_empty()
{
    auto* new_block = create_empty_block();
    swap(new_block, m_current_block);
    return new_block;
}

void CFGBuildingPass::will_be_used_as_expression(Tree const& tree)
{
    if (m_current_block->m_expressions.is_empty())
        VERIFY(is<Statement>(tree.ptr()));
    else
        VERIFY(m_current_block->m_expressions.take_last() == tree);
}

}
