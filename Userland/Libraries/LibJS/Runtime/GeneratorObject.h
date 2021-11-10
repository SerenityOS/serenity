/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class GeneratorObject final : public Object {
    JS_OBJECT(GeneratorObject, Object);

public:
    static ThrowCompletionOr<GeneratorObject*> create(GlobalObject&, Value, ECMAScriptFunctionObject*, ExecutionContext, Bytecode::RegisterWindow);
    GeneratorObject(GlobalObject&, Object& prototype, ExecutionContext);
    virtual void initialize(GlobalObject&) override;
    virtual ~GeneratorObject() override;
    void visit_edges(Cell::Visitor&) override;

    ThrowCompletionOr<Value> next_impl(VM&, GlobalObject&, Optional<Value> next_argument, Optional<Value> value_to_throw);
    void set_done() { m_done = true; }

private:
    ExecutionContext m_execution_context;
    ECMAScriptFunctionObject* m_generating_function { nullptr };
    Value m_previous_value;
    Bytecode::RegisterWindow m_frame;
    bool m_done { false };
};

}
