/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class ImageConstructor final : public JS::NativeFunction {
public:
    explicit ImageConstructor(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~ImageConstructor() override;

    virtual JS::Value call() override;
    virtual JS::Value construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
    virtual char const* class_name() const override { return "ImageConstructor"; }
};

}
