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

struct TranslationUnit {
    void adopt_declaration(NonnullRefPtr<FunctionDeclaration>&& declaration);
    FunctionDefinitionRef adopt_function(NonnullRefPtr<FunctionDefinition>&& definition);

    StringView filename;
    Vector<FunctionDefinitionRef> functions_to_compile;
    Vector<NonnullRefPtr<FunctionDeclaration>> declarations_owner;
    HashMap<StringView, FunctionDeclarationRef> function_index;
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
    FunctionDefinition(StringView name, Tree ast);

    void reindex_ssa_variables();

    Tree m_ast;
    NamedVariableDeclarationRef m_return_value;
    // NOTE: The hash map here is ordered since we do not want random hash changes to break our test
    //       expectations (looking at you, SipHash).
    OrderedHashMap<StringView, NamedVariableDeclarationRef> m_local_variables;
    Vector<SSAVariableDeclarationRef> m_local_ssa_variables;
    RefPtr<ControlFlowGraph> m_cfg;
};

}
