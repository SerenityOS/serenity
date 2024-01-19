/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

#include "Forward.h"

namespace JSSpecCompiler {

class TranslationUnit {
public:
    TranslationUnit(StringView filename);
    ~TranslationUnit();

    void adopt_declaration(NonnullRefPtr<FunctionDeclaration>&& declaration);
    FunctionDefinitionRef adopt_function(NonnullRefPtr<FunctionDefinition>&& definition);

    FunctionDeclarationRef find_declaration_by_name(StringView name) const;

    StringView filename() const { return m_filename; }
    Vector<FunctionDefinitionRef> functions_to_compile() const { return m_functions_to_compile; }

private:
    StringView m_filename;
    Vector<FunctionDefinitionRef> m_functions_to_compile;
    Vector<NonnullRefPtr<FunctionDeclaration>> m_declarations_owner;
    HashMap<StringView, FunctionDeclarationRef> m_function_index;
};

class FunctionDeclaration : public RefCounted<FunctionDeclaration> {
public:
    FunctionDeclaration(StringView name);

    virtual ~FunctionDeclaration() = default;

    TranslationUnitRef m_translation_unit = nullptr;
    StringView m_name;
};

class FunctionDefinition : public FunctionDeclaration {
public:
    FunctionDefinition(StringView name, Tree ast, Vector<StringView>&& argument_names);

    void reindex_ssa_variables();

    Tree m_ast;
    Vector<StringView> m_argument_names;

    // Populates during reference resolving
    // NOTE: The hash map here is ordered since we do not want random hash changes to break our test
    //       expectations (looking at you, SipHash).
    OrderedHashMap<StringView, NamedVariableDeclarationRef> m_local_variables;

    // Fields populate during CFG building
    NamedVariableDeclarationRef m_named_return_value;
    RefPtr<ControlFlowGraph> m_cfg;

    // Fields populate during SSA building
    Vector<SSAVariableDeclarationRef> m_arguments;
    SSAVariableDeclarationRef m_return_value;
    Vector<SSAVariableDeclarationRef> m_local_ssa_variables;
};

}
