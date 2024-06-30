/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Function.h"
#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"
#include "Runtime/Realm.h"

namespace JSSpecCompiler {

TranslationUnit::TranslationUnit(StringView filename)
    : m_filename(filename)
    , m_realm(make<Runtime::Realm>(m_diagnostic_engine))
{
}

TranslationUnit::~TranslationUnit() = default;

void TranslationUnit::adopt_declaration(NonnullRefPtr<FunctionDeclaration>&& declaration)
{
    if (auto decl_name = declaration->declaration(); decl_name.has<AbstractOperationDeclaration>())
        m_abstract_operation_index.set(decl_name.get<AbstractOperationDeclaration>().name, declaration.ptr());
    m_declarations_owner.append(move(declaration));
}

void TranslationUnit::adopt_function(NonnullRefPtr<FunctionDefinition>&& definition)
{
    m_functions_to_compile.append(definition);
    adopt_declaration(definition);
}

FunctionDeclarationRef TranslationUnit::find_abstract_operation_by_name(StringView name) const
{
    auto it = m_abstract_operation_index.find(name);
    if (it == m_abstract_operation_index.end())
        return nullptr;
    return it->value;
}

EnumeratorRef TranslationUnit::get_node_for_enumerator_value(StringView value)
{
    if (auto it = m_enumerator_nodes.find(value); it != m_enumerator_nodes.end())
        return it->value;

    auto enumerator = NonnullRefPtr(NonnullRefPtr<Enumerator>::Adopt, *new Enumerator { {}, value });
    m_enumerator_nodes.set(value, enumerator);
    return enumerator;
}

FunctionDeclaration::FunctionDeclaration(Declaration&& declaration, Location location)
    : m_declaration(move(declaration))
    , m_location(location)
{
}

String FunctionDeclaration::name() const
{
    return m_declaration.visit(
        [&](AbstractOperationDeclaration const& abstract_operation) {
            return abstract_operation.name.to_string();
        },
        [&](MethodDeclaration const& method) {
            return MUST(String::formatted("%{}%", method.name.to_string()));
        },
        [&](AccessorDeclaration const& accessor) {
            return MUST(String::formatted("%get {}%", accessor.name.to_string()));
        });
}

ReadonlySpan<FunctionArgument> FunctionDeclaration::arguments() const
{
    return m_declaration.visit(
        [&](AccessorDeclaration const&) {
            return ReadonlySpan<FunctionArgument> {};
        },
        [&](auto const& declaration) {
            return declaration.arguments.span();
        });
}

FunctionDefinition::FunctionDefinition(Declaration&& declaration, Location location, Tree ast)
    : FunctionDeclaration(move(declaration), location)
    , m_ast(move(ast))
    , m_named_return_value(make_ref_counted<NamedVariableDeclaration>("$return"sv))
{
}

FunctionDefinition::~FunctionDefinition() = default;

void FunctionDefinition::reindex_ssa_variables()
{
    size_t index = 0;
    for (auto const& var : m_local_ssa_variables)
        var->m_index = index++;
}

}
