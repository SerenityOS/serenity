/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>

namespace JS::Temporal {

class TimeZone final : public Object {
    JS_OBJECT(TimeZone, Object);

public:
    // Needs to store values in the range -8.64 * 10^13 to 8.64 * 10^13
    using OffsetType = double;

    TimeZone(String identifier, Object& prototype);
    virtual ~TimeZone() override = default;

    [[nodiscard]] String const& identifier() const { return m_identifier; }
    [[nodiscard]] Optional<OffsetType> const& offset_nanoseconds() const { return m_offset_nanoseconds; }
    void set_offset_nanoseconds(OffsetType offset_nanoseconds) { m_offset_nanoseconds = offset_nanoseconds; };

private:
    // 11.5 Properties of Temporal.TimeZone Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-timezone-instances
    String m_identifier;                       // [[Identifier]]
    Optional<OffsetType> m_offset_nanoseconds; // [[OffsetNanoseconds]]
};

bool is_valid_time_zone_name(String const& time_zone);
String canonicalize_time_zone_name(String const& time_zone);
String default_time_zone();
String parse_temporal_time_zone(GlobalObject&, String const&);
TimeZone* create_temporal_time_zone(GlobalObject&, String const& identifier, FunctionObject const* new_target = nullptr);
Optional<ISODateTime> get_iso_parts_from_epoch(BigInt const& epoch_nanoseconds);
i64 get_iana_time_zone_offset_nanoseconds(BigInt const& epoch_nanoseconds, String const& time_zone_identifier);
double parse_time_zone_offset_string(GlobalObject&, String const&);
String format_time_zone_offset_string(double offset_nanoseconds);
Object* to_temporal_time_zone(GlobalObject&, Value temporal_time_zone_like);
double get_offset_nanoseconds_for(GlobalObject&, Value time_zone, Instant&);
Optional<String> builtin_time_zone_get_offset_string_for(GlobalObject&, Value time_zone, Instant&);
PlainDateTime* builtin_time_zone_get_plain_date_time_for(GlobalObject&, Value time_zone, Instant&, Object& calendar);

bool is_valid_time_zone_numeric_utc_offset_syntax(String const&);

}
