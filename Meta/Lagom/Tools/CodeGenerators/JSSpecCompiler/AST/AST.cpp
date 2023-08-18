/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST/AST.h"

namespace JSSpecCompiler {

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

Vector<NodeSubtreePointer> ReturnExpression::subtrees()
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
    if (m_condition.has_value())
        return { { &m_condition.value() }, { &m_branch } };
    return { { &m_branch } };
}

Vector<NodeSubtreePointer> TreeList::subtrees()
{
    Vector<NodeSubtreePointer> result;
    for (auto& expression : m_expressions)
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
