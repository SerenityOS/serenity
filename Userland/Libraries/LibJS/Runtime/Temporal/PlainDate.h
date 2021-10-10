/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainDate final : public Object {
    JS_OBJECT(PlainDate, Object);

public:
    PlainDate(i32 iso_year, u8 iso_month, u8 iso_day, Object& calendar, Object& prototype);
    virtual ~PlainDate() override = default;

    [[nodiscard]] i32 iso_year() const { return m_iso_year; }
    [[nodiscard]] u8 iso_month() const { return m_iso_month; }
    [[nodiscard]] u8 iso_day() const { return m_iso_day; }
    [[nodiscard]] Object const& calendar() const { return m_calendar; }
    [[nodiscard]] Object& calendar() { return m_calendar; }

private:
    virtual void visit_edges(Visitor&) override;

    // 3.4 Properties of Temporal.PlainDate Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-plaindate-instances
    i32 m_iso_year { 0 }; // [[ISOYear]]
    u8 m_iso_month { 1 }; // [[ISOMonth]]
    u8 m_iso_day { 1 };   // [[ISODay]]
    Object& m_calendar;   // [[Calendar]]
};

struct ISODate {
    i32 year;
    u8 month;
    u8 day;
};

struct DifferenceISODateResult {
    double years;
    double months;
    double weeks;
    double days;
};

ThrowCompletionOr<PlainDate*> create_temporal_date(GlobalObject&, i32 iso_year, u8 iso_month, u8 iso_day, Object& calendar, FunctionObject const* new_target = nullptr);
ThrowCompletionOr<PlainDate*> to_temporal_date(GlobalObject&, Value item, Object* options = nullptr);
DifferenceISODateResult difference_iso_date(GlobalObject&, i32 year1, u8 month1, u8 day1, i32 year2, u8 month2, u8 day2, StringView largest_unit);
ThrowCompletionOr<ISODate> regulate_iso_date(GlobalObject&, double year, double month, double day, StringView overflow);
bool is_valid_iso_date(i32 year, u8 month, u8 day);
ISODate balance_iso_date(double year, double month, double day);
String pad_iso_year(i32 y);
ThrowCompletionOr<String> temporal_date_to_string(GlobalObject&, PlainDate&, StringView show_calendar);
ThrowCompletionOr<ISODate> add_iso_date(GlobalObject&, i32 year, u8 month, u8 day, double years, double months, double weeks, double days, StringView overflow);
i8 compare_iso_date(i32 year1, u8 month1, u8 day1, i32 year2, u8 month2, u8 day2);

}
