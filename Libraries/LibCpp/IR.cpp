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

static void add_node_to_body(ASTNode&, Scope&, const NonnullRefPtrVector<SIR::Variable>&);
static void add_scope_to_body(Scope&, Scope&, const NonnullRefPtrVector<Variable>&);

static NonnullRefPtr<Expression> create_comparison_operation(Expression& left, Scope& new_scope, SIR::BinaryExpression::Kind comparison, int i)
{
    auto right = create_ast_node<SIR::ConstantExpression>(left.end(), left.end(), i);
    auto result = create_ast_node<Variable>(left.end(), left.end(), create_ast_node<SIR::BooleanType>(left.end(), left.end()));
    auto expression = create_ast_node<BinaryExpression>(left.start(), left.end(), comparison, left, right, result);
    new_scope.add_node(expression);
    return expression;
}

static NonnullRefPtr<SIR::Expression> add_binary_operation_to_body(BinaryExpression& binary_expression, Scope& new_scope, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    add_node_to_body(binary_expression.left(), new_scope, parameters);
    add_node_to_body(binary_expression.right(), new_scope, parameters);

    new_scope.add_node(binary_expression);
    return binary_expression;
}

static NonnullRefPtr<SIR::Expression> add_expression_to_body(ASTNode& expression, Scope& new_scope, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (expression.is_binary_expression()) {
        return add_binary_operation_to_body(reinterpret_cast<SIR::BinaryExpression&>(expression), new_scope, parameters);
    } else if (expression.is_identifier_expression()) {
        return reinterpret_cast<SIR::Expression&>(expression);
    } else {
        ASSERT_NOT_REACHED();
    }
}

static void add_statement_to_body(ASTNode& statement, Scope& new_scope, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (statement.is_return_statement()) {
        auto& return_statement = reinterpret_cast<SIR::ReturnStatement&>(statement);
        if (return_statement.expression()) {
            auto inserted = add_expression_to_body(return_statement.expression().release_nonnull(), new_scope, parameters);
            return_statement.set_expression(inserted);
        } else {
            TODO();
        }
        new_scope.add_node(return_statement);
    } else if (statement.is_jump_statement()) {
        auto& jump_statement = reinterpret_cast<SIR::JumpStatement&>(statement);
        auto inserted = add_expression_to_body(jump_statement.condition(), new_scope, parameters);

        ASSERT(inserted && inserted->is_expression());
        auto variable = reinterpret_cast<NonnullRefPtr<Expression>&>(inserted);
        ASSERT(variable->result()->node_type()->size_in_bits() != 1 && variable->result()->node_type()->size_in_bytes() != 1);

        auto bool_expression = create_comparison_operation(variable, new_scope, SIR::BinaryExpression::Kind::NotEqual, 0);
        jump_statement.set_condition(bool_expression);

        auto if_true = create_ast_node<SIR::LabelExpression>(jump_statement.start(), jump_statement.end());
        auto if_false = create_ast_node<SIR::LabelExpression>(jump_statement.start(), jump_statement.end());

        new_scope.add_node(jump_statement);
        new_scope.add_node(if_true);
        add_scope_to_body(jump_statement.if_true(), new_scope, parameters);
        new_scope.add_node(if_false);
        if (jump_statement.if_false().has_value())
            add_scope_to_body(jump_statement.if_false().value(), new_scope, parameters);

        auto scope_if_true = create_ast_node<Scope>(if_true->start(), if_true->end());
        scope_if_true->add_node(if_true);
        jump_statement.set_if_true(scope_if_true);

        auto scope_if_false = create_ast_node<Scope>(if_false->start(), if_false->end());
        scope_if_false->add_node(if_false);
        jump_statement.set_if_false(scope_if_false);
    } else
        ASSERT_NOT_REACHED();
}

static void add_node_to_body(ASTNode& node, Scope& new_scope, const NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (node.is_expression())
        add_expression_to_body(node, new_scope, parameters);
    else if (node.is_statement())
        add_statement_to_body(node, new_scope, parameters);
    else if (node.is_scope())
        add_scope_to_body(reinterpret_cast<Scope&>(node), new_scope, parameters);
    else
        ASSERT_NOT_REACHED();
}

static void add_scope_to_body(Scope& scope, Scope& new_scope, const NonnullRefPtrVector<Variable>& parameters)
{
    for (auto& node : scope.body())
        add_node_to_body(node, new_scope, parameters);
}

void IR::to_internal_representation(TranslationUnit& tu)
{
    NonnullRefPtrVector<SIR::Function> functions;

    for (auto& fun : tu.functions()) {
        auto new_scope = create_ast_node<Scope>(fun.start(), fun.end());

        for (auto& param : fun.parameters())
            new_scope->add_node(LibIntermediate::Utils::create_store(param.node_type(), param.name()));
        add_scope_to_body(fun.body(), new_scope, fun.parameters());

        fun.set_body(new_scope);
        functions.append(fun);
        new_scope->set_parent(tu);
    }
}
}
