/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

class Calendar final : public Object {
    JS_OBJECT(Calendar, Object);

public:
    virtual ~Calendar() override = default;

    [[nodiscard]] DeprecatedString const& identifier() const { return m_identifier; }

private:
    Calendar(DeprecatedString identifier, Object& prototype);

    // 12.5 Properties of Temporal.Calendar Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-calendar-instances
    DeprecatedString m_identifier; // [[Identifier]]
};

// 14.2 The Year-Week Record Specification Type, https://tc39.es/proposal-temporal/#sec-year-week-record-specification-type
struct YearWeekRecord {
    u8 week { 0 };
    i32 year { 0 };
};

bool is_builtin_calendar(DeprecatedString const& identifier);
Span<StringView const> available_calendars();
ThrowCompletionOr<Calendar*> create_temporal_calendar(VM&, DeprecatedString const& identifier, FunctionObject const* new_target = nullptr);
ThrowCompletionOr<Calendar*> get_builtin_calendar(VM&, DeprecatedString const& identifier);
Calendar* get_iso8601_calendar(VM&);
ThrowCompletionOr<Vector<DeprecatedString>> calendar_fields(VM&, Object& calendar, Vector<StringView> const& field_names);
ThrowCompletionOr<Object*> calendar_merge_fields(VM&, Object& calendar, Object& fields, Object& additional_fields);
ThrowCompletionOr<PlainDate*> calendar_date_add(VM&, Object& calendar, Value date, Duration&, Object* options = nullptr, FunctionObject* date_add = nullptr);
ThrowCompletionOr<Duration*> calendar_date_until(VM&, Object& calendar, Value one, Value two, Object& options, FunctionObject* date_until = nullptr);
ThrowCompletionOr<double> calendar_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<double> calendar_month(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<DeprecatedString> calendar_month_code(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<double> calendar_day(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_day_of_week(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_day_of_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_week_of_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_year_of_week(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_days_in_week(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<double> calendar_days_in_month(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_days_in_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_months_in_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_in_leap_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_era(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_era_year(VM&, Object& calendar, Object& date_like);
ThrowCompletionOr<Object*> to_temporal_calendar(VM&, Value);
ThrowCompletionOr<Object*> to_temporal_calendar_with_iso_default(VM&, Value);
ThrowCompletionOr<Object*> get_temporal_calendar_with_iso_default(VM&, Object&);
ThrowCompletionOr<PlainDate*> calendar_date_from_fields(VM&, Object& calendar, Object const& fields, Object const* options = nullptr);
ThrowCompletionOr<PlainYearMonth*> calendar_year_month_from_fields(VM&, Object& calendar, Object const& fields, Object const* options = nullptr);
ThrowCompletionOr<PlainMonthDay*> calendar_month_day_from_fields(VM&, Object& calendar, Object const& fields, Object const* options = nullptr);
ThrowCompletionOr<DeprecatedString> maybe_format_calendar_annotation(VM&, Object const* calendar_object, StringView show_calendar);
DeprecatedString format_calendar_annotation(StringView id, StringView show_calendar);
ThrowCompletionOr<bool> calendar_equals(VM&, Object& one, Object& two);
ThrowCompletionOr<Object*> consolidate_calendars(VM&, Object& one, Object& two);
u8 iso_days_in_month(i32 year, u8 month);
YearWeekRecord to_iso_week_of_year(i32 year, u8 month, u8 day);
DeprecatedString iso_month_code(u8 month);
ThrowCompletionOr<double> resolve_iso_month(VM&, Object const& fields);
ThrowCompletionOr<ISODateRecord> iso_date_from_fields(VM&, Object const& fields, Object const& options);
ThrowCompletionOr<ISOYearMonth> iso_year_month_from_fields(VM&, Object const& fields, Object const& options);
ThrowCompletionOr<ISOMonthDay> iso_month_day_from_fields(VM&, Object const& fields, Object const& options);
ThrowCompletionOr<Object*> default_merge_calendar_fields(VM&, Object const& fields, Object const& additional_fields);
u16 to_iso_day_of_year(i32 year, u8 month, u8 day);
u8 to_iso_day_of_week(i32 year, u8 month, u8 day);

}
