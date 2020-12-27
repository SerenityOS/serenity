/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
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
#include <LibMiddleEnd/Utils.h>

namespace Cpp {

static void add_node_to_body(Cpp::ASTNode& node, NonnullRefPtrVector<SIR::ASTNode>& new_body, NonnullRefPtrVector<SIR::Variable>& parameters);

static void add_binary_operation_to_body(Cpp::BinaryExpression& binary_expression, NonnullRefPtrVector<SIR::ASTNode>& new_body, NonnullRefPtrVector<SIR::Variable>& parameters)
{
    add_node_to_body(binary_expression.left(), new_body, parameters);
    add_node_to_body(binary_expression.right(), new_body, parameters);

    new_body.append(binary_expression);
}

static void add_expression_to_body(Cpp::ASTNode& expression, NonnullRefPtrVector<SIR::ASTNode>& new_body, NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (expression.is_binary_expression()) {
        add_binary_operation_to_body(reinterpret_cast<SIR::BinaryExpression&>(expression), new_body, parameters);
    } else if (expression.is_identifier_expression()) {
    } else {
        ASSERT_NOT_REACHED();
    }
}

static void add_statement_to_body(Cpp::ASTNode& statement, NonnullRefPtrVector<SIR::ASTNode>& new_body, NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (statement.is_return_statement()) {
        auto& return_statement = reinterpret_cast<SIR::ReturnStatement&>(statement);
        if (return_statement.expression()) {
            add_expression_to_body(return_statement.expression().release_nonnull(), new_body, parameters);
            return_statement.set_expression(new_body.ptr_at(new_body.size() - 1));
        } else {
            TODO();
        }
        new_body.append(return_statement);
    } else
        ASSERT_NOT_REACHED();
}

static void add_node_to_body(Cpp::ASTNode& node, NonnullRefPtrVector<SIR::ASTNode>& new_body, NonnullRefPtrVector<SIR::Variable>& parameters)
{
    if (node.is_expression())
        add_expression_to_body(node, new_body, parameters);
    else if (node.is_statement())
        add_statement_to_body(node, new_body, parameters);
    else
        ASSERT_NOT_REACHED();
}

SIR::TranslationUnit IR::to_internal_representation(Cpp::TranslationUnit& tu)
{
    NonnullRefPtrVector<SIR::Function> functions;

    for (auto& fun : tu.functions()) {
        NonnullRefPtrVector<SIR::ASTNode> new_body;

        for (size_t i = 0; i < fun.parameters().size(); i++) {
            auto var = fun.parameters().ptr_at(i);
            new_body.append(MiddleEnd::Utils::create_store(var->node_type(), var->name()));
        }
        for (size_t i = 0; i < fun.body().size(); i++)
            add_node_to_body(fun.body().ptr_at(i), new_body, fun.parameters());

        fun.body().clear();
        fun.body().append(new_body);
        functions.append(fun);
    }
    return { functions };
}
}