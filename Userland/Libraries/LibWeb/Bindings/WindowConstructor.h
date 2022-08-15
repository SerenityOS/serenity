/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class WindowConstructor : public JS::NativeFunction {
    JS_OBJECT(WindowConstructor, JS::NativeFunction);

public:
    explicit WindowConstructor(JS::Realm&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WindowConstructor() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
