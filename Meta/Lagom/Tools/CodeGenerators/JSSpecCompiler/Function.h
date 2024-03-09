/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

#include "DiagnosticEngine.h"
#include "Forward.h"

namespace JSSpecCompiler {

class TranslationUnit {
public:
    TranslationUnit(StringView filename);
    ~TranslationUnit();

    void adopt_declaration(NonnullRefPtr<FunctionDeclaration>&& declaration);
    void adopt_function(NonnullRefPtr<FunctionDefinition>&& definition);
    FunctionDeclarationRef find_abstract_operation_by_name(StringView name) const;

    StringView filename() const { return m_filename; }
    DiagnosticEngine& diag() { return m_diagnostic_engine; }
    Vector<FunctionDefinitionRef> functions_to_compile() const { return m_functions_to_compile; }

    EnumeratorRef get_node_for_enumerator_value(StringView value);

    Runtime::Realm* realm() const { return m_realm; }

private:
    StringView m_filename;
    DiagnosticEngine m_diagnostic_engine;
    Vector<FunctionDefinitionRef> m_functions_to_compile;
    Vector<NonnullRefPtr<FunctionDeclaration>> m_declarations_owner;
    HashMap<FlyString, FunctionDeclarationRef> m_abstract_operation_index;
    HashMap<StringView, EnumeratorRef> m_enumerator_nodes;

    NonnullOwnPtr<Runtime::Realm> m_realm;
};

struct FunctionArgument {
    StringView name;
    size_t optional_arguments_group;
};

class QualifiedName {
public:
    QualifiedName() { }

    QualifiedName(ReadonlySpan<StringView> parsed_name)
    {
        m_components.ensure_capacity(parsed_name.size());
        for (auto component : parsed_name)
            m_components.unchecked_append(MUST(FlyString::from_utf8(component)));
    }

    QualifiedName(ReadonlySpan<FlyString> parsed_name)
    {
        m_components.ensure_capacity(parsed_name.size());
        for (auto component : parsed_name)
            m_components.unchecked_append(component);
    }

    String to_string() const
    {
        return MUST(String::join("."sv, m_components));
    }

    Vector<FlyString> const& components() const
    {
        return m_components;
    }

    FlyString last_component() const
    {
        return m_components.last();
    }

    ReadonlySpan<FlyString> without_last_component() const
    {
        return components().span().slice(0, components().size() - 1);
    }

    QualifiedName slice(size_t start, size_t length) const
    {
        return { m_components.span().slice(start, length) };
    }

    QualifiedName with_appended(FlyString component) const
    {
        auto new_components = m_components;
        new_components.append(component);
        return { new_components };
    }

private:
    Vector<FlyString> m_components;
};

struct AbstractOperationDeclaration {
    FlyString name;
    Vector<FunctionArgument> arguments;
};

struct AccessorDeclaration {
    QualifiedName name;
};

struct MethodDeclaration {
    QualifiedName name;
    Vector<FunctionArgument> arguments;
};

using Declaration = Variant<AbstractOperationDeclaration, AccessorDeclaration, MethodDeclaration>;

class FunctionDeclaration : public RefCounted<FunctionDeclaration> {
public:
    FunctionDeclaration(Declaration&& declaration, Location location);

    virtual ~FunctionDeclaration() = default;

    Declaration const& declaration() const { return m_declaration; }
    Location location() const { return m_location; }
    String name() const;
    ReadonlySpan<FunctionArgument> arguments() const;

private:
    Declaration m_declaration;
    Location m_location;
};

class FunctionDefinition : public FunctionDeclaration {
public:
    FunctionDefinition(Declaration&& declaration, Location location, Tree ast);

    void reindex_ssa_variables();

    Tree m_ast;

    // Populates during reference resolving
    // NOTE: The hash map here is ordered since we do not want random hash changes to break our test
    //       expectations (looking at you, SipHash).
    OrderedHashMap<StringView, NamedVariableDeclarationRef> m_local_variables;

    // Fields populate during CFG building
    NamedVariableDeclarationRef m_named_return_value;
    RefPtr<ControlFlowGraph> m_cfg;

    // Fields populate during SSA building
    Vector<SSAVariableDeclarationRef> m_ssa_arguments;
    SSAVariableDeclarationRef m_return_value;
    Vector<SSAVariableDeclarationRef> m_local_ssa_variables;
};

}
