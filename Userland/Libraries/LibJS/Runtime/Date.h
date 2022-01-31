/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class Date final : public Object {
    JS_OBJECT(Date, Object);

public:
    static Date* create(GlobalObject&, double date_value);
    static Date* now(GlobalObject&);

    Date(double date_value, Object& prototype);
    virtual ~Date() override;

    double date_value() const { return m_date_value; }
    void set_date_value(double value) { m_date_value = value; }

    String iso_date_string() const;

    // https://tc39.es/ecma262/#eqn-HoursPerDay
    static constexpr double hours_per_day = 24;
    // https://tc39.es/ecma262/#eqn-MinutesPerHour
    static constexpr double minutes_per_hour = 60;
    // https://tc39.es/ecma262/#eqn-SecondsPerMinute
    static constexpr double seconds_per_minute = 60;
    // https://tc39.es/ecma262/#eqn-msPerSecond
    static constexpr double ms_per_second = 1'000;
    // https://tc39.es/ecma262/#eqn-msPerMinute
    static constexpr double ms_per_minute = 60'000;
    // https://tc39.es/ecma262/#eqn-msPerHour
    static constexpr double ms_per_hour = 3'600'000;
    // https://tc39.es/ecma262/#eqn-msPerDay
    static constexpr double ms_per_day = 86'400'000;

private:
    double m_date_value { 0 }; // [[DateValue]]
};

u16 day_within_year(double);
u8 date_from_time(double);
u16 days_in_year(i32);
double day_from_year(i32);
double time_from_year(i32);
i32 year_from_time(double);
bool in_leap_year(double);
u8 month_from_time(double);
u8 hour_from_time(double);
u8 min_from_time(double);
u8 sec_from_time(double);
u16 ms_from_time(double);
u8 week_day(double);
double local_tza(double time, bool is_utc, Optional<StringView> time_zone_override = {});
double local_time(double time);
double utc_time(double time);
double day(double);
double time_within_day(double);
Value make_time(GlobalObject& global_object, Value hour, Value min, Value sec, Value ms);
Value make_day(GlobalObject& global_object, Value year, Value month, Value date);
Value make_date(Value day, Value time);
Value time_clip(GlobalObject& global_object, Value time);

}
