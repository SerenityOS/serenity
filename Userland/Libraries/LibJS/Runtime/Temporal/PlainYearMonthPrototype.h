/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainYearMonthPrototype final : public Object {
    JS_OBJECT(PlainYearMonthPrototype, Object);

public:
    explicit PlainYearMonthPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainYearMonthPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(calendar_getter);
    JS_DECLARE_NATIVE_FUNCTION(year_getter);
};

}
