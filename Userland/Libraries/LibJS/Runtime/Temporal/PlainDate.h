/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

class PlainDate final : public Object {
    JS_OBJECT(PlainDate, Object);

public:
    PlainDate(i32 iso_year, i32 iso_month, i32 iso_day, Object& calendar, Object& prototype);
    virtual ~PlainDate() override = default;

    [[nodiscard]] i32 iso_year() const { return m_iso_year; }
    [[nodiscard]] i32 iso_month() const { return m_iso_month; }
    [[nodiscard]] i32 iso_day() const { return m_iso_day; }
    [[nodiscard]] Object const& calendar() const { return m_calendar; }
    [[nodiscard]] Object& calendar() { return m_calendar; }

private:
    virtual void visit_edges(Visitor&) override;

    // 3.4 Properties of Temporal.PlainDate Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-plaindate-instances
    i32 m_iso_year { 0 };  // [[ISOYear]]
    i32 m_iso_month { 1 }; // [[ISOMonth]]
    i32 m_iso_day { 1 };   // [[ISODay]]
    Object& m_calendar;    // [[Calendar]]
};

PlainDate* create_temporal_date(GlobalObject&, i32 iso_year, i32 iso_month, i32 iso_day, Object& calendar, FunctionObject* new_target = nullptr);
PlainDate* to_temporal_date(GlobalObject&, Value item, Object* options = nullptr);
Optional<TemporalDate> regulate_iso_date(GlobalObject&, double year, double month, double day, String const& overflow);
bool is_valid_iso_date(i32 year, i32 month, i32 day);

}
