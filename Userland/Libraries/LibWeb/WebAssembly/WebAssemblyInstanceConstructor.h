/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class WebAssemblyInstanceConstructor : public JS::NativeFunction {
    JS_OBJECT(WebAssemblyInstanceConstructor, JS::NativeFunction);

public:
    explicit WebAssemblyInstanceConstructor(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WebAssemblyInstanceConstructor() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
