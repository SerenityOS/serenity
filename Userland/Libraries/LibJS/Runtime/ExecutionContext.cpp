/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ExecutionContext.h>

namespace JS {

ExecutionContext::ExecutionContext(Heap& heap)
    : arguments(heap)
{
}

ExecutionContext::ExecutionContext(MarkedVector<Value> existing_arguments)
    : arguments(move(existing_arguments))
{
}

ExecutionContext ExecutionContext::copy() const
{
    ExecutionContext copy { arguments };

    copy.function = function;
    copy.realm = realm;
    copy.script_or_module = script_or_module;
    copy.lexical_environment = lexical_environment;
    copy.variable_environment = variable_environment;
    copy.private_environment = private_environment;
    copy.current_node = current_node;
    copy.function_name = function_name;
    copy.this_value = this_value;
    copy.is_strict_mode = is_strict_mode;

    return copy;
}

}
