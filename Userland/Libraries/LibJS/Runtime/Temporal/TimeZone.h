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

    // Needs to store values in the range -8.64 * 10^21 to 8.64 * 10^21
    using OffsetType = double;

public:
    explicit TimeZone(String identifier, Object& prototype);
    virtual ~TimeZone() override = default;

    String const& identifier() const { return m_identifier; }
    Optional<OffsetType> const& offset_nanoseconds() const { return m_offset_nanoseconds; }
    void set_offset_nanoseconds(u32 offset_nanoseconds) { m_offset_nanoseconds = offset_nanoseconds; };

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
Object* create_temporal_time_zone(GlobalObject&, String const& identifier, FunctionObject* new_target = nullptr);

}
