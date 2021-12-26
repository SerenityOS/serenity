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
    explicit WebAssemblyMemoryConstructor(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WebAssemblyMemoryConstructor() override;

    virtual JS::Value call() override;
    virtual JS::Value construct(JS::Function& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
