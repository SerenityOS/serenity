/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainMonthDayPrototype final : public Object {
    JS_OBJECT(PlainMonthDayPrototype, Object);

public:
    explicit PlainMonthDayPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainMonthDayPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(calendar_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_code_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_getter);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
    JS_DECLARE_NATIVE_FUNCTION(get_iso_fields);
};

}
