/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Function.h"
#include "AST/AST.h"

namespace JSSpecCompiler {

Function::Function(ExecutionContext* context, StringView name, Tree ast)
    : m_context(context)
    , m_name(name)
    , m_ast(move(ast))
{
}

}
