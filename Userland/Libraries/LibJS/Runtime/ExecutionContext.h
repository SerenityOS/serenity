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
#include <LibJS/Module.h>
#include <LibJS/Runtime/PrivateEnvironment.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceRange.h>

namespace JS {

using ScriptOrModule = Variant<Empty, NonnullGCPtr<Script>, NonnullGCPtr<Module>>;

// 9.4 Execution Contexts, https://tc39.es/ecma262/#sec-execution-contexts
struct ExecutionContext {
    static NonnullOwnPtr<ExecutionContext> create(Heap&);
    [[nodiscard]] NonnullOwnPtr<ExecutionContext> copy() const;

    ~ExecutionContext();

    void visit_edges(Cell::Visitor&);

private:
    ExecutionContext(Heap&);

    IntrusiveListNode<ExecutionContext> m_list_node;

public:
    Heap& m_heap;

    using List = IntrusiveList<&ExecutionContext::m_list_node>;

    GCPtr<FunctionObject> function;                // [[Function]]
    GCPtr<Realm> realm;                            // [[Realm]]
    ScriptOrModule script_or_module;               // [[ScriptOrModule]]
    GCPtr<Environment> lexical_environment;        // [[LexicalEnvironment]]
    GCPtr<Environment> variable_environment;       // [[VariableEnvironment]]
    GCPtr<PrivateEnvironment> private_environment; // [[PrivateEnvironment]]

    // Non-standard: This points at something that owns this ExecutionContext, in case it needs to be protected from GC.
    GCPtr<Cell> context_owner;

    Optional<Bytecode::InstructionStreamIterator> instruction_stream_iterator;
    GCPtr<PrimitiveString> function_name;
    Value this_value;
    bool is_strict_mode { false };

    GCPtr<Bytecode::Executable> executable;

    // https://html.spec.whatwg.org/multipage/webappapis.html#skip-when-determining-incumbent-counter
    // FIXME: Move this out of LibJS (e.g. by using the CustomData concept), as it's used exclusively by LibWeb.
    size_t skip_when_determining_incumbent_counter { 0 };

    Value argument(size_t index) const
    {
        if (index >= arguments.size()) [[unlikely]]
            return js_undefined();
        return arguments[index];
    }

    Value& local(size_t index)
    {
        return locals[index];
    }

    Vector<Value> arguments;
    Vector<Value> locals;
};

struct StackTraceElement {
    ExecutionContext* execution_context;
    Optional<UnrealizedSourceRange> source_range;
};

}
