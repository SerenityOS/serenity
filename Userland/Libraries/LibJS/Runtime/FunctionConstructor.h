/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class FunctionConstructor final : public NativeFunction {
    JS_OBJECT(FunctionConstructor, NativeFunction);

public:
    explicit FunctionConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~FunctionConstructor() override;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
