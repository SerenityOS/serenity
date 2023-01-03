/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class WebAssemblyMemoryConstructor : public JS::NativeFunction {
    JS_OBJECT(WebAssemblyMemoryConstructor, JS::NativeFunction);

public:
    explicit WebAssemblyMemoryConstructor(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~WebAssemblyMemoryConstructor() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Object>> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
