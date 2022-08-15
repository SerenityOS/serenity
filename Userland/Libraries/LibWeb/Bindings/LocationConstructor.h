/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class LocationConstructor : public JS::NativeFunction {
    JS_OBJECT(LocationConstructor, JS::NativeFunction);

public:
    explicit LocationConstructor(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~LocationConstructor() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
