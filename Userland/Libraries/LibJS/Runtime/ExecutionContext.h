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
#include <LibJS/Runtime/Value.h>

namespace JS {

// 9.4 Execution Contexts, https://tc39.es/ecma262/#sec-execution-contexts
struct ExecutionContext {
    explicit ExecutionContext(Heap& heap)
        : arguments(heap)
    {
    }

    FunctionObject* function { nullptr };          // [[Function]]
    Realm* realm { nullptr };                      // [[Realm]]
    Environment* lexical_environment { nullptr };  // [[LexicalEnvironment]]
    Environment* variable_environment { nullptr }; // [[VariableEnvironment]]

    ASTNode const* current_node { nullptr };
    FlyString function_name;
    Value this_value;
    MarkedValueList arguments;
    bool is_strict_mode { false };
};

}
