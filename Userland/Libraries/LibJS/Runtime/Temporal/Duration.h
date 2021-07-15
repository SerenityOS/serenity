/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Duration final : public Object {
    JS_OBJECT(Duration, Object);

public:
    explicit Duration(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object& prototype);
    virtual ~Duration() override = default;

    double years() const { return m_years; }
    double months() const { return m_months; }
    double weeks() const { return m_weeks; }
    double days() const { return m_days; }
    double hours() const { return m_hours; }
    double minutes() const { return m_minutes; }
    double seconds() const { return m_seconds; }
    double milliseconds() const { return m_milliseconds; }
    double microseconds() const { return m_microseconds; }
    double nanoseconds() const { return m_nanoseconds; }

private:
    // 7.4 Properties of Temporal.Duration Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-duration-instances

    double m_years;        // [[Years]]
    double m_months;       // [[Months]]
    double m_weeks;        // [[Weeks]]
    double m_days;         // [[Days]]
    double m_hours;        // [[Hours]]
    double m_minutes;      // [[Minutes]]
    double m_seconds;      // [[Seconds]]
    double m_milliseconds; // [[Milliseconds]]
    double m_microseconds; // [[Microseconds]]
    double m_nanoseconds;  // [[Nanoseconds]]
};

i8 duration_sign(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds);
bool is_valid_duration(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds);
Duration* create_temporal_duration(GlobalObject&, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, FunctionObject* new_target = nullptr);

}
