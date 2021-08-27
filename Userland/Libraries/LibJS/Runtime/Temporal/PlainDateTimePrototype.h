/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainDateTimePrototype final : public Object {
    JS_OBJECT(PlainDateTimePrototype, Object);

public:
    explicit PlainDateTimePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainDateTimePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(calendar_getter);
    JS_DECLARE_NATIVE_FUNCTION(year_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_code_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_getter);
    JS_DECLARE_NATIVE_FUNCTION(hour_getter);
    JS_DECLARE_NATIVE_FUNCTION(minute_getter);
    JS_DECLARE_NATIVE_FUNCTION(second_getter);
    JS_DECLARE_NATIVE_FUNCTION(millisecond_getter);
    JS_DECLARE_NATIVE_FUNCTION(microsecond_getter);
    JS_DECLARE_NATIVE_FUNCTION(nanosecond_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_of_week_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_of_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(week_of_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(days_in_week_getter);
    JS_DECLARE_NATIVE_FUNCTION(days_in_month_getter);
    JS_DECLARE_NATIVE_FUNCTION(days_in_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(months_in_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(in_leap_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(era_getter);
    JS_DECLARE_NATIVE_FUNCTION(era_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(with_plain_time);
    JS_DECLARE_NATIVE_FUNCTION(with_plain_date);
    JS_DECLARE_NATIVE_FUNCTION(with_calendar);
    JS_DECLARE_NATIVE_FUNCTION(equals);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_date);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_year_month);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_month_day);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_time);
    JS_DECLARE_NATIVE_FUNCTION(get_iso_fields);
};

}
