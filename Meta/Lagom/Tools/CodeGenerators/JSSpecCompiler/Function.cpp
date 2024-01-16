/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Function.h"
#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"

namespace JSSpecCompiler {

TranslationUnit::TranslationUnit(StringView filename)
    : m_filename(filename)
{
}

TranslationUnit::~TranslationUnit() = default;

void TranslationUnit::adopt_declaration(NonnullRefPtr<FunctionDeclaration>&& declaration)
{
    declaration->m_translation_unit = this;
    m_function_index.set(declaration->m_name, declaration.ptr());
    m_declarations_owner.append(move(declaration));
}

FunctionDefinitionRef TranslationUnit::adopt_function(NonnullRefPtr<FunctionDefinition>&& definition)
{
    FunctionDefinitionRef result = definition.ptr();
    m_functions_to_compile.append(result);
    adopt_declaration(definition);
    return result;
}

FunctionDeclarationRef TranslationUnit::find_declaration_by_name(StringView name) const
{
    auto it = m_function_index.find(name);
    if (it == m_function_index.end())
        return nullptr;
    return it->value;
}

FunctionDeclaration::FunctionDeclaration(StringView name, Vector<FunctionArgument>&& arguments)
    : m_name(name)
    , m_arguments(arguments)
{
}

FunctionDefinition::FunctionDefinition(StringView name, Tree ast, Vector<FunctionArgument>&& arguments)
    : FunctionDeclaration(name, move(arguments))
    , m_ast(move(ast))
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
