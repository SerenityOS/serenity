/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Function.h"
#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"

namespace JSSpecCompiler {

FunctionDefinitionRef TranslationUnit::adopt_function(NonnullRefPtr<FunctionDefinition>&& function)
{
    function->m_translation_unit = this;
    function_index.set(function->m_name, make_ref_counted<FunctionPointer>(function));

    FunctionDefinitionRef result = function.ptr();
    function_definitions.append(move(function));
    return result;
}

FunctionDeclaration::FunctionDeclaration(StringView name)
    : m_name(name)
{
}

FunctionDefinition::FunctionDefinition(StringView name, Tree ast)
    : FunctionDeclaration(name)
    , m_ast(move(ast))
    , m_return_value(make_ref_counted<VariableDeclaration>("$return"sv))
{
}

}
