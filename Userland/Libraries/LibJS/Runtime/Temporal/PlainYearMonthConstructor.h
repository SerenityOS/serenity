/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Temporal {

class PlainYearMonthConstructor final : public NativeFunction {
    JS_OBJECT(PlainYearMonthConstructor, NativeFunction);

public:
    explicit PlainYearMonthConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainYearMonthConstructor() override = default;

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_OLD_NATIVE_FUNCTION(from);
    JS_DECLARE_OLD_NATIVE_FUNCTION(compare);
};

}
