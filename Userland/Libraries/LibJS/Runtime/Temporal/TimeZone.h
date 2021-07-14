/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class TimeZone final : public Object {
    JS_OBJECT(TimeZone, Object);

    // Needs to store values in the range -8.64 * 10^13 to 8.64 * 10^13
    using OffsetType = double;

public:
    explicit TimeZone(String identifier, Object& prototype);
    virtual ~TimeZone() override = default;

    String const& identifier() const { return m_identifier; }
    Optional<OffsetType> const& offset_nanoseconds() const { return m_offset_nanoseconds; }
    void set_offset_nanoseconds(OffsetType offset_nanoseconds) { m_offset_nanoseconds = offset_nanoseconds; };

private:
    // 11.5 Properties of Temporal.TimeZone Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-timezone-instances

    // [[Identifier]]
    String m_identifier;

    // [[OffsetNanoseconds]]
    Optional<OffsetType> m_offset_nanoseconds;
};

bool is_valid_time_zone_name(String const& time_zone);
String canonicalize_time_zone_name(String const& time_zone);
String default_time_zone();
TimeZone* create_temporal_time_zone(GlobalObject&, String const& identifier, FunctionObject* new_target = nullptr);
double parse_time_zone_offset_string(GlobalObject&, String const&);
String format_time_zone_offset_string(double offset_nanoseconds);

bool is_valid_time_zone_numeric_utc_offset_syntax(String const&);

}
