/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>

#include "AST/AST.h"
#include "Compiler/IfBranchMergingPass.h"

namespace JSSpecCompiler {

RecursionDecision IfBranchMergingPass::on_entry(Tree tree)
{
    if (auto list = as<TreeList>(tree); list) {
        Vector<Tree> result;
        Vector<Tree> unmerged_branches;

        auto merge_if_needed = [&] {
            if (!unmerged_branches.is_empty()) {
                result.append(merge_branches(unmerged_branches));
                unmerged_branches.clear();
            }
        };

        for (auto const& node : list->m_expressions) {
            if (is<IfBranch>(node.ptr())) {
                merge_if_needed();
                unmerged_branches.append(node);
            } else if (is<ElseIfBranch>(node.ptr())) {
                unmerged_branches.append(node);
            } else {
                merge_if_needed();
                result.append(node);
            }
        }
        merge_if_needed();

        list->m_expressions = move(result);
    }
    return RecursionDecision::Recurse;
}

Tree IfBranchMergingPass::merge_branches(Vector<Tree> const& unmerged_branches)
{
    static const Tree error = make_ref_counted<ErrorNode>("Cannot make sense of if-elseif-else chain"sv);

    VERIFY(unmerged_branches.size() >= 1);

    Vector<Tree> conditions;
    Vector<Tree> branches;
    NullableTree else_branch;

    if (auto if_branch = as<IfBranch>(unmerged_branches[0]); if_branch) {
        conditions.append(if_branch->m_condition);
        branches.append(if_branch->m_branch);
    } else {
        return error;
    }

    for (size_t i = 1; i < unmerged_branches.size(); ++i) {
        auto branch = as<ElseIfBranch>(unmerged_branches[i]);

        if (!branch)
            return error;

        if (!branch->m_condition) {
            // There might be situation like:
            //   1. If <condition>, then
            //      ...
            //   2. Else,
            //      a. If <condition>, then
            //         ...
            //   3. Else,
            //      ...
            auto substep_list = as<TreeList>(branch->m_branch);
            if (substep_list && substep_list->m_expressions.size() == 1) {
                if (auto nested_if = as<IfBranch>(substep_list->m_expressions[0]); nested_if)
                    branch = make_ref_counted<ElseIfBranch>(nested_if->m_condition, nested_if->m_branch);
            }
        }

        if (branch->m_condition) {
            conditions.append(branch->m_condition.release_nonnull());
            branches.append(branch->m_branch);
        } else {
            if (i + 1 != unmerged_branches.size())
                return error;
            else_branch = branch->m_branch;
        }
    }

    return make_ref_counted<IfElseIfChain>(move(conditions), move(branches), else_branch);
}

}
