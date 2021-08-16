/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainMonthDay final : public Object {
    JS_OBJECT(PlainMonthDay, Object);

public:
    PlainMonthDay(u8 iso_month, u8 iso_day, i32 iso_year, Object& calendar, Object& prototype);
    virtual ~PlainMonthDay() override = default;

    [[nodiscard]] i32 iso_year() const { return m_iso_year; }
    [[nodiscard]] u8 iso_month() const { return m_iso_month; }
    [[nodiscard]] u8 iso_day() const { return m_iso_day; }
    [[nodiscard]] Object const& calendar() const { return m_calendar; }
    [[nodiscard]] Object& calendar() { return m_calendar; }

private:
    virtual void visit_edges(Visitor&) override;

    // 10.4 Properties of Temporal.PlainMonthDay Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-plainmonthday-instances
    i32 m_iso_year { 0 }; // [[ISOYear]]
    u8 m_iso_month { 0 }; // [[ISOMonth]]
    u8 m_iso_day { 0 };   // [[ISODay]]
    Object& m_calendar;   // [[Calendar]]
};

struct ISOMonthDay {
    u8 month;
    u8 day;
    i32 reference_iso_year;
};

PlainMonthDay* create_temporal_month_day(GlobalObject&, u8 iso_month, u8 iso_day, Object& calendar, i32 reference_iso_year, FunctionObject* new_target = nullptr);

}
