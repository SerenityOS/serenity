/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
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
    Calendar(String identifier, Object& prototype);
    virtual ~Calendar() override = default;

    [[nodiscard]] String const& identifier() const { return m_identifier; }

private:
    // 12.5 Properties of Temporal.Calendar Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-calendar-instances
    String m_identifier; // [[Identifier]]
};

ThrowCompletionOr<Calendar*> create_temporal_calendar(GlobalObject&, String const& identifier, FunctionObject const* new_target = nullptr);
bool is_builtin_calendar(String const& identifier);
ThrowCompletionOr<Calendar*> get_builtin_calendar(GlobalObject&, String const& identifier);
Calendar* get_iso8601_calendar(GlobalObject&);
ThrowCompletionOr<Vector<String>> calendar_fields(GlobalObject&, Object& calendar, Vector<StringView> const& field_names);
ThrowCompletionOr<Object*> calendar_merge_fields(GlobalObject&, Object& calendar, Object& fields, Object& additional_fields);
ThrowCompletionOr<PlainDate*> calendar_date_add(GlobalObject&, Object& calendar, Value date, Duration&, Object* options, FunctionObject* date_add = nullptr);
ThrowCompletionOr<Duration*> calendar_date_until(GlobalObject&, Object& calendar, Value one, Value two, Object& options, FunctionObject* date_until = nullptr);
ThrowCompletionOr<double> calendar_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<double> calendar_month(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<String> calendar_month_code(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<double> calendar_day(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_day_of_week(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_day_of_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_week_of_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_days_in_week(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_days_in_month(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_days_in_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_months_in_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_in_leap_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_era(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Value> calendar_era_year(GlobalObject&, Object& calendar, Object& date_like);
ThrowCompletionOr<Object*> to_temporal_calendar(GlobalObject&, Value);
ThrowCompletionOr<Object*> to_temporal_calendar_with_iso_default(GlobalObject&, Value);
ThrowCompletionOr<Object*> get_temporal_calendar_with_iso_default(GlobalObject&, Object&);
ThrowCompletionOr<PlainDate*> date_from_fields(GlobalObject&, Object& calendar, Object const& fields, Object const& options);
ThrowCompletionOr<PlainYearMonth*> year_month_from_fields(GlobalObject&, Object& calendar, Object const& fields, Object const* options = nullptr);
ThrowCompletionOr<PlainMonthDay*> month_day_from_fields(GlobalObject& global_object, Object& calendar, Object const& fields, Object const* options = nullptr);
String format_calendar_annotation(StringView id, StringView show_calendar);
ThrowCompletionOr<bool> calendar_equals(GlobalObject&, Object& one, Object& two);
ThrowCompletionOr<Object*> consolidate_calendars(GlobalObject&, Object& one, Object& two);
bool is_iso_leap_year(i32 year);
u16 iso_days_in_year(i32 year);
u8 iso_days_in_month(i32 year, u8 month);
u8 to_iso_day_of_week(i32 year, u8 month, u8 day);
u16 to_iso_day_of_year(i32 year, u8 month, u8 day);
u8 to_iso_week_of_year(i32 year, u8 month, u8 day);
String build_iso_month_code(u8 month);
ThrowCompletionOr<double> resolve_iso_month(GlobalObject&, Object const& fields);
ThrowCompletionOr<ISODate> iso_date_from_fields(GlobalObject&, Object const& fields, Object const& options);
ThrowCompletionOr<ISOYearMonth> iso_year_month_from_fields(GlobalObject&, Object const& fields, Object const& options);
ThrowCompletionOr<ISOMonthDay> iso_month_day_from_fields(GlobalObject&, Object const& fields, Object const& options);
i32 iso_year(Object& temporal_object);
u8 iso_month(Object& temporal_object);
String iso_month_code(Object& temporal_object);
u8 iso_day(Object& temporal_object);
ThrowCompletionOr<Object*> default_merge_fields(GlobalObject&, Object const& fields, Object const& additional_fields);

}
