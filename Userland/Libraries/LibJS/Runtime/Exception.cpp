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
    m_traceback.ensure_capacity(vm.call_stack().size());
    for (auto* call_frame : vm.call_stack()) {
        auto function_name = call_frame->function_name;
        if (function_name.is_empty())
            function_name = "<anonymous>";
        m_traceback.prepend({
            .function_name = move(function_name),
            // We might not have an AST node associated with the call frame, e.g. in promise
            // reaction jobs (which aren't called anywhere from the source code).
            // They're not going to generate any _unhandled_ exceptions though, so a meaningless
            // source range is fine.
            .source_range = call_frame->current_node ? call_frame->current_node->source_range() : SourceRange {},
        });
    }
}

void Exception::visit_edges(Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_value);
}

}
