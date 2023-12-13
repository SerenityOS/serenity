/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

NonnullOwnPtr<ExecutionContext> ExecutionContext::create(Heap& heap)
{
    return adopt_own(*new ExecutionContext(heap));
}

ExecutionContext::ExecutionContext(Heap& heap)
    : m_heap(heap)
{
}

ExecutionContext::~ExecutionContext()
{
}

NonnullOwnPtr<ExecutionContext> ExecutionContext::copy() const
{
    auto copy = create(m_heap);
    copy->function = function;
    copy->realm = realm;
    copy->script_or_module = script_or_module;
    copy->lexical_environment = lexical_environment;
    copy->variable_environment = variable_environment;
    copy->private_environment = private_environment;
    copy->instruction_stream_iterator = instruction_stream_iterator;
    copy->function_name = function_name;
    copy->this_value = this_value;
    copy->is_strict_mode = is_strict_mode;
    copy->executable = executable;
    copy->arguments = arguments;
    copy->locals = locals;
    return copy;
}

void ExecutionContext::visit_edges(Cell::Visitor& visitor)
{
    visitor.visit(function);
    visitor.visit(realm);
    visitor.visit(variable_environment);
    visitor.visit(lexical_environment);
    visitor.visit(private_environment);
    visitor.visit(context_owner);
    visitor.visit(this_value);
    visitor.visit(executable);
    if (instruction_stream_iterator.has_value())
        visitor.visit(const_cast<Bytecode::Executable*>(instruction_stream_iterator.value().executable()));
    visitor.visit(function_name);
    for (auto argument : arguments)
        visitor.visit(argument);
    for (auto local : locals)
        visitor.visit(local);
    script_or_module.visit(
        [](Empty) {},
        [&](auto& script_or_module) {
            visitor.visit(script_or_module);
        });
}

}
