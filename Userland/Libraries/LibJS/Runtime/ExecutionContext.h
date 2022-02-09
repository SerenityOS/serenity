/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/WeakPtr.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Module.h>
#include <LibJS/Runtime/PrivateEnvironment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

using ScriptOrModule = Variant<Empty, WeakPtr<Script>, WeakPtr<Module>>;

// 9.4 Execution Contexts, https://tc39.es/ecma262/#sec-execution-contexts
struct ExecutionContext {
    explicit ExecutionContext(Heap& heap)
        : arguments(heap)
    {
    }

    [[nodiscard]] ExecutionContext copy() const
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

private:
    explicit ExecutionContext(MarkedVector<Value> existing_arguments)
        : arguments(move(existing_arguments))
    {
    }

public:
    FunctionObject* function { nullptr };                // [[Function]]
    Realm* realm { nullptr };                            // [[Realm]]
    ScriptOrModule script_or_module;                     // [[ScriptOrModule]]
    Environment* lexical_environment { nullptr };        // [[LexicalEnvironment]]
    Environment* variable_environment { nullptr };       // [[VariableEnvironment]]
    PrivateEnvironment* private_environment { nullptr }; // [[PrivateEnvironment]]

    ASTNode const* current_node { nullptr };
    FlyString function_name;
    Value this_value;
    MarkedVector<Value> arguments;
    bool is_strict_mode { false };

    // https://html.spec.whatwg.org/multipage/webappapis.html#skip-when-determining-incumbent-counter
    // FIXME: Move this out of LibJS (e.g. by using the CustomData concept), as it's used exclusively by LibWeb.
    size_t skip_when_determining_incumbent_counter { 0 };
};

}
