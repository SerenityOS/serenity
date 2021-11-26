/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Promise.h>

namespace JS {

class AsyncFunctionDriverWrapper final : public Promise {
    JS_OBJECT(AsyncFunctionDriverWrapper, Promise);

public:
    static ThrowCompletionOr<Value> create(GlobalObject&, GeneratorObject*);
    explicit AsyncFunctionDriverWrapper(GlobalObject&, GeneratorObject*);

    virtual ~AsyncFunctionDriverWrapper() override;
    void visit_edges(Cell::Visitor&) override;

    ThrowCompletionOr<Value> react_to_async_task_completion(VM&, GlobalObject&, Value, bool is_successful);

private:
    GeneratorObject* m_generator_object { nullptr };
    NativeFunction* m_on_fulfillment { nullptr };
    NativeFunction* m_on_rejection { nullptr };
};

}
