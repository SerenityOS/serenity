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
    FunctionDefinitionRef adopt_function(NonnullRefPtr<FunctionDefinition>&& function);

    StringView filename;
    Vector<NonnullRefPtr<FunctionDefinition>> function_definitions;
    HashMap<StringView, FunctionPointerRef> function_index;
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

    Tree m_ast;
    VariableDeclarationRef m_return_value;
    HashMap<StringView, VariableDeclarationRef> m_local_variables;
    RefPtr<ControlFlowGraph> m_cfg;
};

}
