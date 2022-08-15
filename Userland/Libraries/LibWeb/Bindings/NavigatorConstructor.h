/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class NavigatorConstructor : public JS::NativeFunction {
    JS_OBJECT(NavigatorConstructor, JS::NativeFunction);

public:
    explicit NavigatorConstructor(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~NavigatorConstructor() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
