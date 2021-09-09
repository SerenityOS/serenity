/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class ZonedDateTime final : public Object {
    JS_OBJECT(ZonedDateTime, Object);

public:
    ZonedDateTime(BigInt& nanoseconds, Object& time_zone, Object& calendar, Object& prototype);
    virtual ~ZonedDateTime() override = default;

    [[nodiscard]] BigInt const& nanoseconds() const { return m_nanoseconds; }
    [[nodiscard]] BigInt& nanoseconds() { return m_nanoseconds; }
    [[nodiscard]] Object const& time_zone() const { return m_time_zone; }
    [[nodiscard]] Object& time_zone() { return m_time_zone; }
    [[nodiscard]] Object const& calendar() const { return m_calendar; }
    [[nodiscard]] Object& calendar() { return m_calendar; }

private:
    virtual void visit_edges(Visitor&) override;

    // 6.4 Properties of Temporal.ZonedDateTime Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-zoneddatetime-instances
    BigInt& m_nanoseconds; // [[Nanoseconds]]
    Object& m_time_zone;   // [[TimeZone]]
    Object& m_calendar;    // [[Calendar]]
};

ZonedDateTime* create_temporal_zoned_date_time(GlobalObject&, BigInt& epoch_nanoseconds, Object& time_zone, Object& calendar, FunctionObject const* new_target = nullptr);

}
