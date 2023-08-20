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

class ExecutionContext {
public:
    HashMap<StringView, FunctionPointerRef> m_functions;
};

class Function : public RefCounted<Function> {
public:
    Function(ExecutionContext* context, StringView name, Tree ast);

    ExecutionContext* m_context;
    StringView m_name;
    Tree m_ast;
    VariableDeclarationRef m_return_value;
    HashMap<StringView, VariableDeclarationRef> m_local_variables;
};

}
