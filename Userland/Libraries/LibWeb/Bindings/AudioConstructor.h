/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class AudioConstructor final : public JS::NativeFunction {
public:
    explicit AudioConstructor(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~AudioConstructor() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
    virtual const char* class_name() const override { return "AudioConstructor"; }
};

}
