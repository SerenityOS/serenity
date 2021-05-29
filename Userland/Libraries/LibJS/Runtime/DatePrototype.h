/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class DatePrototype final : public Object {
    JS_OBJECT(DatePrototype, Object);

public:
    explicit DatePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~DatePrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(get_date);
    JS_DECLARE_NATIVE_FUNCTION(get_day);
    JS_DECLARE_NATIVE_FUNCTION(get_full_year);
    JS_DECLARE_NATIVE_FUNCTION(set_full_year);
    JS_DECLARE_NATIVE_FUNCTION(get_year);
    JS_DECLARE_NATIVE_FUNCTION(set_year);
    JS_DECLARE_NATIVE_FUNCTION(get_hours);
    JS_DECLARE_NATIVE_FUNCTION(set_hours);
    JS_DECLARE_NATIVE_FUNCTION(get_milliseconds);
    JS_DECLARE_NATIVE_FUNCTION(set_milliseconds);
    JS_DECLARE_NATIVE_FUNCTION(get_minutes);
    JS_DECLARE_NATIVE_FUNCTION(set_minutes);
    JS_DECLARE_NATIVE_FUNCTION(get_month);
    JS_DECLARE_NATIVE_FUNCTION(get_seconds);
    JS_DECLARE_NATIVE_FUNCTION(set_seconds);
    JS_DECLARE_NATIVE_FUNCTION(get_time);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_date);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_day);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_full_year);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_hours);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_milliseconds);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_minutes);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_month);
    JS_DECLARE_NATIVE_FUNCTION(get_utc_seconds);
    JS_DECLARE_NATIVE_FUNCTION(to_date_string);
    JS_DECLARE_NATIVE_FUNCTION(to_gmt_string);
    JS_DECLARE_NATIVE_FUNCTION(to_utc_string);
    JS_DECLARE_NATIVE_FUNCTION(to_iso_string);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_date_string);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_string);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_time_string);
    JS_DECLARE_NATIVE_FUNCTION(to_time_string);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
};

}
