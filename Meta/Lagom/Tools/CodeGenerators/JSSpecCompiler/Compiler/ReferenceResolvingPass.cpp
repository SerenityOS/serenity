/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>

#include "AST/AST.h"
#include "Compiler/ReferenceResolvingPass.h"
#include "Function.h"

namespace JSSpecCompiler {

RecursionDecision ReferenceResolvingPass::on_entry(Tree tree)
{
    if (auto binary_operation = as<BinaryOperation>(tree); binary_operation) {
        if (binary_operation->m_operation != BinaryOperator::Declaration)
            return RecursionDecision::Recurse;

        if (auto variable_name = as<UnresolvedReference>(binary_operation->m_left); variable_name) {
            auto name = variable_name->m_name;
            if (!m_function->m_local_variables.contains(name))
                m_function->m_local_variables.set(name, make_ref_counted<VariableDeclaration>(name));
        }
    }
    return RecursionDecision::Recurse;
}

void ReferenceResolvingPass::on_leave(Tree tree)
{
    auto& functions = m_function->m_context->m_functions;

    if (auto reference = as<UnresolvedReference>(tree); reference) {
        auto name = reference->m_name;

        if (name.starts_with("[["sv) && name.ends_with("]]"sv)) {
            replace_current_node_with(make_ref_counted<SlotName>(name.substring_view(2, name.length() - 4)));
            return;
        }

        if (auto it = m_function->m_local_variables.find(name); it != m_function->m_local_variables.end()) {
            replace_current_node_with(make_ref_counted<Variable>(it->value));
            return;
        }

        if (auto it = functions.find(name); it != functions.end()) {
            replace_current_node_with(it->value);
            return;
        }
    }
}

}
