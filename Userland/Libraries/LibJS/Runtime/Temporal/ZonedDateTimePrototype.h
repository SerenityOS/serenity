/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class ZonedDateTimePrototype final : public Object {
    JS_OBJECT(ZonedDateTimePrototype, Object);

public:
    explicit ZonedDateTimePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ZonedDateTimePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(calendar_getter);
    JS_DECLARE_NATIVE_FUNCTION(time_zone_getter);
    JS_DECLARE_NATIVE_FUNCTION(year_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_code_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_getter);
    JS_DECLARE_NATIVE_FUNCTION(hour_getter);
};

}
