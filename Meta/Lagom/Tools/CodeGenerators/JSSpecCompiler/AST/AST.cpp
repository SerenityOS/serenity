/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST/AST.h"

namespace JSSpecCompiler {

Tree NodeSubtreePointer::get(Badge<RecursiveASTVisitor>)
{
    return m_tree_ptr.visit(
        [&](NullableTree* nullable_tree) -> Tree {
            NullableTree copy = *nullable_tree;
            return copy.release_nonnull();
        },
        [&](Tree* tree) -> Tree {
            return *tree;
        });
}

void NodeSubtreePointer::replace_subtree(Badge<RecursiveASTVisitor>, NullableTree replacement)
{
    m_tree_ptr.visit(
        [&](NullableTree* nullable_tree) {
            *nullable_tree = replacement;
        },
        [&](Tree* tree) {
            *tree = replacement.release_nonnull();
        });
}

Vector<BasicBlockRef*> ControlFlowJump::references()
{
    return { &m_block };
}

Vector<BasicBlockRef*> ControlFlowBranch::references()
{
    return { &m_then, &m_else };
}

Vector<NodeSubtreePointer> BinaryOperation::subtrees()
{
    return { { &m_left }, { &m_right } };
}

Vector<NodeSubtreePointer> UnaryOperation::subtrees()
{
    return { { &m_operand } };
}

Vector<NodeSubtreePointer> IsOneOfOperation::subtrees()
{
    Vector<NodeSubtreePointer> result = { { &m_operand } };
    for (auto& child : m_compare_values)
        result.append({ &child });
    return result;
}

Vector<NodeSubtreePointer> ReturnNode::subtrees()
{
    return { { &m_return_value } };
}

Vector<NodeSubtreePointer> AssertExpression::subtrees()
{
    return { { &m_condition } };
}

Vector<NodeSubtreePointer> IfBranch::subtrees()
{
    return { { &m_condition }, { &m_branch } };
}

Vector<NodeSubtreePointer> ElseIfBranch::subtrees()
{
    if (m_condition)
        return { { &m_condition }, { &m_branch } };
    return { { &m_branch } };
}

Vector<NodeSubtreePointer> IfElseIfChain::subtrees()
{
    Vector<NodeSubtreePointer> result;
    for (size_t i = 0; i < branches_count(); ++i) {
        result.append({ &m_conditions[i] });
        result.append({ &m_branches[i] });
    }
    if (m_else_branch)
        result.append({ &m_else_branch });
    return result;
}

TreeList::TreeList(Vector<Tree>&& trees)
{
    for (auto const& tree : trees) {
        if (tree->is_list()) {
            for (auto const& nested_tree : as<TreeList>(tree)->m_trees)
                m_trees.append(nested_tree);
        } else {
            m_trees.append(tree);
        }
    }
}

Vector<NodeSubtreePointer> TreeList::subtrees()
{
    Vector<NodeSubtreePointer> result;
    for (auto& expression : m_trees)
        result.append({ &expression });
    return result;
}

Vector<NodeSubtreePointer> RecordDirectListInitialization::subtrees()
{
    Vector<NodeSubtreePointer> result { &m_type_reference };
    for (auto& argument : m_arguments) {
        result.append({ &argument.name });
        result.append({ &argument.value });
    }
    return result;
}

Vector<NodeSubtreePointer> FunctionCall::subtrees()
{
    Vector<NodeSubtreePointer> result = { { &m_name } };
    for (auto& child : m_arguments)
        result.append({ &child });
    return result;
}

}
