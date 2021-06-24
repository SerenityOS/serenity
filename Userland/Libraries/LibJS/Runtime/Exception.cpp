/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
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
    m_traceback.ensure_capacity(vm.execution_context_stack().size());
    for (ssize_t i = vm.execution_context_stack().size() - 1; i >= 0; i--) {
        auto* context = vm.execution_context_stack()[i];
        auto function_name = context->function_name;
        if (function_name.is_empty())
            function_name = "<anonymous>";
        m_traceback.empend(
            move(function_name),
            // We might not have an AST node associated with the execution context, e.g. in promise
            // reaction jobs (which aren't called anywhere from the source code).
            // They're not going to generate any _unhandled_ exceptions though, so a meaningless
            // source range is fine.
            context->current_node ? context->current_node->source_range() : SourceRange {});
    }
}

void Exception::visit_edges(Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_value);
}

}
