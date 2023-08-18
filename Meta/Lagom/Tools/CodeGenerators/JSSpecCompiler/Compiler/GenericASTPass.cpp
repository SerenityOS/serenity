/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Compiler/GenericASTPass.h"
#include "AST/AST.h"
#include "Function.h"

namespace JSSpecCompiler {

void RecursiveASTVisitor::run_in_const_subtree(NullableTree nullable_tree)
{
    if (nullable_tree) {
        auto tree = nullable_tree.release_nonnull();
        auto tree_copy = tree;
        NodeSubtreePointer pointer { &tree };
        recurse(tree, pointer);
        VERIFY(tree == tree_copy);
    }
}

void RecursiveASTVisitor::run_in_subtree(Tree& tree)
{
    NodeSubtreePointer pointer { &tree };
    recurse(tree, pointer);
}

void RecursiveASTVisitor::replace_current_node_with(NullableTree tree)
{
    m_current_subtree_pointer->replace_subtree({}, move(tree));
}

RecursionDecision RecursiveASTVisitor::recurse(Tree root, NodeSubtreePointer& pointer)
{
    RecursionDecision decision;

    m_current_subtree_pointer = &pointer;
    decision = on_entry(root);

    if (decision == RecursionDecision::Recurse) {
        for (auto& child : root->subtrees()) {
            if (recurse(child.get({}), child) == RecursionDecision::Break)
                return RecursionDecision::Break;
        }
    }

    m_current_subtree_pointer = &pointer;
    on_leave(root);

    return RecursionDecision::Continue;
}

void GenericASTPass::run()
{
    run_in_subtree(m_function->m_ast);
}

}
