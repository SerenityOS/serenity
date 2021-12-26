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
    JS_DECLARE_NATIVE_FUNCTION(minute_getter);
    JS_DECLARE_NATIVE_FUNCTION(second_getter);
    JS_DECLARE_NATIVE_FUNCTION(millisecond_getter);
    JS_DECLARE_NATIVE_FUNCTION(microsecond_getter);
    JS_DECLARE_NATIVE_FUNCTION(nanosecond_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_seconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_milliseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_microseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_nanoseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_of_week_getter);
    JS_DECLARE_NATIVE_FUNCTION(day_of_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(week_of_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(days_in_week_getter);
    JS_DECLARE_NATIVE_FUNCTION(days_in_month_getter);
    JS_DECLARE_NATIVE_FUNCTION(days_in_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(months_in_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(in_leap_year_getter);
    JS_DECLARE_NATIVE_FUNCTION(offset_nanoseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(offset_getter);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
    JS_DECLARE_NATIVE_FUNCTION(to_instant);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_date);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_time);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_date_time);
};

}
