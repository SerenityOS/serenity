/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Function.h"
#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"

namespace JSSpecCompiler {

void TranslationUnit::adopt_declaration(NonnullRefPtr<FunctionDeclaration>&& declaration)
{
    declaration->m_translation_unit = this;
    function_index.set(declaration->m_name, declaration.ptr());
    declarations_owner.append(move(declaration));
}

FunctionDefinitionRef TranslationUnit::adopt_function(NonnullRefPtr<FunctionDefinition>&& function)
{
    FunctionDefinitionRef result = function.ptr();
    functions_to_compile.append(result);
    adopt_declaration(function);
    return result;
}

FunctionDeclaration::FunctionDeclaration(StringView name)
    : m_name(name)
{
}

FunctionDefinition::FunctionDefinition(StringView name, Tree ast, Vector<StringView>&& argument_names)
    : FunctionDeclaration(name)
    , m_ast(move(ast))
    , m_argument_names(move(argument_names))
    , m_named_return_value(make_ref_counted<NamedVariableDeclaration>("$return"sv))
{
}

void FunctionDefinition::reindex_ssa_variables()
{
    size_t index = 0;
    for (auto const& var : m_local_ssa_variables)
        var->m_index = index++;
}

}
