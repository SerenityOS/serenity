/*
 * Copyright (c) 2020-2021, Denis Campredon <deni_@hotmail.fr>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "IR.h"
#include "AST.h"
#include "LibIntermediate/Utils.h"

namespace Cpp {

static void add_node_to_body(ASTNode&, NonnullRefPtrVector<SIR::ASTNode>&, const NonnullRefPtrVector<SIR::Variable>&);
static void add_scope_to_body(NonnullRefPtrVector<ASTNode>&, NonnullRefPtrVector<ASTNode>&, const NonnullRefPtrVector<Variable>&);

static NonnullRefPtr<Expression> create_comparison_operation(Expression& left, NonnullRefPtrVector<SIR::ASTNode>& new_body, SIR::BinaryExpression::Kind comparison, int i)
{
    auto right = create_ast_node<SIR::ConstantExpression>(left.end(), left.end(), i);
    auto result = create_ast_node<Variable>(left.end(), left.end(), create_ast_node<SIR::BooleanType>(left.end(), left.end()));
    auto expression = create_ast_node<BinaryExpression>(left.start(), left.end(), comparison, left, right, result);
    new_body.append(expression);
    return expression;
}

static NonnullRefPtr<SIR::Expression> add_binary_operation_to_body(BinaryExpression& binary_expression, NonnullRefPtrVector<SIR::ASTNode>& new_body, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    add_node_to_body(binary_expression.left(), new_body, parameters);
    add_node_to_body(binary_expression.right(), new_body, parameters);

    new_body.append(binary_expression);
    return binary_expression;
}

static NonnullRefPtr<SIR::Expression> add_expression_to_body(ASTNode& expression, NonnullRefPtrVector<SIR::ASTNode>& new_body, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (expression.is_binary_expression()) {
        return add_binary_operation_to_body(reinterpret_cast<SIR::BinaryExpression&>(expression), new_body, parameters);
    } else if (expression.is_identifier_expression()) {
        return reinterpret_cast<SIR::Expression&>(expression);
    } else {
        ASSERT_NOT_REACHED();
    }
}

static void add_statement_to_body(ASTNode& statement, NonnullRefPtrVector<SIR::ASTNode>& new_body, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (statement.is_return_statement()) {
        auto& return_statement = reinterpret_cast<SIR::ReturnStatement&>(statement);
        if (return_statement.expression()) {
            auto inserted = add_expression_to_body(return_statement.expression().release_nonnull(), new_body, parameters);
            return_statement.set_expression(inserted);
        } else {
            TODO();
        }
        new_body.append(return_statement);
    } else if (statement.is_jump_statement()) {
        auto& jump_statement = reinterpret_cast<SIR::JumpStatement&>(statement);
        auto inserted = add_expression_to_body(jump_statement.condition(), new_body, parameters);

        ASSERT(inserted && inserted->is_expression());
        auto variable = reinterpret_cast<NonnullRefPtr<Expression>&>(inserted);
        ASSERT(variable->result()->node_type()->size_in_bits() != 1 && variable->result()->node_type()->size_in_bytes() != 1);

        auto bool_expression = create_comparison_operation(variable, new_body, SIR::BinaryExpression::Kind::NotEqual, 0);
        jump_statement.set_condition(bool_expression);

        auto if_true = create_ast_node<SIR::LabelExpression>(jump_statement.start(), jump_statement.end());
        auto if_false = create_ast_node<SIR::LabelExpression>(jump_statement.start(), jump_statement.end());

        new_body.append(jump_statement);
        new_body.append(if_true);
        add_scope_to_body(jump_statement.if_true(), new_body, parameters);
        new_body.append(if_false);
        {
            NonnullRefPtrVector<ASTNode> vec_if_true;
            vec_if_true.append(if_true);
            jump_statement.set_if_true(vec_if_true);
        }
        jump_statement.set_if_false(if_false);

    } else
        ASSERT_NOT_REACHED();
}

static void add_node_to_body(ASTNode& node, NonnullRefPtrVector<SIR::ASTNode>& new_body, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (node.is_expression())
        add_expression_to_body(node, new_body, parameters);
    else if (node.is_statement())
        add_statement_to_body(node, new_body, parameters);
    else
        ASSERT_NOT_REACHED();
}

static void add_scope_to_body(NonnullRefPtrVector<ASTNode>& body, NonnullRefPtrVector<ASTNode>& new_body, const NonnullRefPtrVector<Variable>& parameters)
{
    for (size_t i = 0; i < body.size(); i++)
        add_node_to_body(body.at(i), new_body, parameters);
}

SIR::TranslationUnit IR::to_internal_representation(TranslationUnit& tu)
{
    NonnullRefPtrVector<SIR::Function> functions;

    for (auto& fun : tu.functions()) {
        NonnullRefPtrVector<SIR::ASTNode> new_body;

        for (size_t i = 0; i < fun.parameters().size(); i++) {
            auto var = fun.parameters().ptr_at(i);
            new_body.append(LibIntermediate::Utils::create_store(var->node_type(), var->name()));
        }
        add_scope_to_body(fun.body(), new_body, fun.parameters());

        fun.body().clear();
        fun.body().append(new_body);
        functions.append(fun);
    }
    return { functions };
}
}
