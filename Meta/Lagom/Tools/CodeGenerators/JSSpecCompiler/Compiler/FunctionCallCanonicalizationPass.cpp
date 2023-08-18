/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Compiler/FunctionCallCanonicalizationPass.h"
#include "AST/AST.h"

namespace JSSpecCompiler {

RecursionDecision FunctionCallCanonicalizationPass::on_entry(Tree tree)
{
    if (auto binary_operation = as<BinaryOperation>(tree); binary_operation) {
        if (binary_operation->m_operation == BinaryOperator::FunctionCall) {
            Vector<Tree> arguments;

            auto current_tree = binary_operation->m_right;
            while (true) {
                auto argument_tree = as<BinaryOperation>(current_tree);
                if (!argument_tree || argument_tree->m_operation != BinaryOperator::Comma)
                    break;
                arguments.append(argument_tree->m_left);
                current_tree = argument_tree->m_right;
            }
            arguments.append(current_tree);

            replace_current_node_with(make_ref_counted<FunctionCall>(binary_operation->m_left, move(arguments)));
        }
    }
    return RecursionDecision::Recurse;
}

}
