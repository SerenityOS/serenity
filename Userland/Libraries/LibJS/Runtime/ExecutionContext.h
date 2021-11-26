/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/PrivateEnvironment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 9.4 Execution Contexts, https://tc39.es/ecma262/#sec-execution-contexts
struct ExecutionContext {
    explicit ExecutionContext(Heap& heap)
        : arguments(heap)
    {
    }

    [[nodiscard]] ExecutionContext copy() const
    {
        ExecutionContext copy { arguments.copy() };

        copy.function = function;
        copy.realm = realm;
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
    explicit ExecutionContext(MarkedValueList existing_arguments)
        : arguments(move(existing_arguments))
    {
    }

public:
    FunctionObject* function { nullptr };                // [[Function]]
    Realm* realm { nullptr };                            // [[Realm]]
    Environment* lexical_environment { nullptr };        // [[LexicalEnvironment]]
    Environment* variable_environment { nullptr };       // [[VariableEnvironment]]
    PrivateEnvironment* private_environment { nullptr }; // [[PrivateEnvironment]]

    ASTNode const* current_node { nullptr };
    FlyString function_name;
    Value this_value;
    MarkedValueList arguments;
    bool is_strict_mode { false };
};

}
