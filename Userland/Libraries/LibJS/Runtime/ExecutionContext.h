/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/WeakPtr.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Module.h>
#include <LibJS/Runtime/PrivateEnvironment.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceRange.h>

namespace JS {

using ScriptOrModule = Variant<Empty, NonnullGCPtr<Script>, NonnullGCPtr<Module>>;

// 9.4 Execution Contexts, https://tc39.es/ecma262/#sec-execution-contexts
struct ExecutionContext {
    explicit ExecutionContext(Heap& heap);

    [[nodiscard]] ExecutionContext copy() const;

    void visit_edges(Cell::Visitor&);

private:
    explicit ExecutionContext(MarkedVector<Value> existing_arguments, MarkedVector<Value> existing_local_variables);

public:
    GCPtr<FunctionObject> function;                // [[Function]]
    GCPtr<Realm> realm;                            // [[Realm]]
    ScriptOrModule script_or_module;               // [[ScriptOrModule]]
    GCPtr<Environment> lexical_environment;        // [[LexicalEnvironment]]
    GCPtr<Environment> variable_environment;       // [[VariableEnvironment]]
    GCPtr<PrivateEnvironment> private_environment; // [[PrivateEnvironment]]

    // Non-standard: This points at something that owns this ExecutionContext, in case it needs to be protected from GC.
    GCPtr<Cell> context_owner;

    Optional<Bytecode::InstructionStreamIterator&> instruction_stream_iterator;
    DeprecatedFlyString function_name;
    Value this_value;
    MarkedVector<Value> arguments;
    MarkedVector<Value> local_variables;
    bool is_strict_mode { false };

    // https://html.spec.whatwg.org/multipage/webappapis.html#skip-when-determining-incumbent-counter
    // FIXME: Move this out of LibJS (e.g. by using the CustomData concept), as it's used exclusively by LibWeb.
    size_t skip_when_determining_incumbent_counter { 0 };
};

}
