/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class StringConstructor final : public NativeFunction {
    JS_OBJECT(StringConstructor, NativeFunction);

public:
    explicit StringConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~StringConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(raw);
    JS_DECLARE_NATIVE_FUNCTION(from_char_code);
    JS_DECLARE_NATIVE_FUNCTION(from_code_point);
};

}
