/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

Exception::Exception(Value value)
    : m_value(value)
{
    auto& vm = this->vm();
    auto& call_stack = vm.call_stack();
    for (ssize_t i = call_stack.size() - 1; i >= 0; --i) {
        String function_name = call_stack[i]->function_name;
        if (function_name.is_empty())
            function_name = "<anonymous>";
        m_trace.append(function_name);
    }

    if (auto* interpreter = vm.interpreter_if_exists()) {
        for (auto* node_chain = interpreter->executing_ast_node_chain(); node_chain; node_chain = node_chain->previous) {
            m_source_ranges.append(node_chain->node.source_range());
        }
    }
}

Exception::~Exception()
{
}

void Exception::visit_edges(Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_value);
}

}
