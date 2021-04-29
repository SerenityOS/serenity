/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class ArrayBufferConstructor final : public NativeFunction {
    JS_OBJECT(ArrayBufferConstructor, NativeFunction);

public:
    explicit ArrayBufferConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ArrayBufferConstructor() override;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(is_view);
};

}
