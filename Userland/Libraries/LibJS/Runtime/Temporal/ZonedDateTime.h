/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class ZonedDateTime final : public Object {
    JS_OBJECT(ZonedDateTime, Object);

public:
    ZonedDateTime(BigInt const& nanoseconds, Object& time_zone, Object& calendar, Object& prototype);
    virtual ~ZonedDateTime() override = default;

    [[nodiscard]] BigInt const& nanoseconds() const { return m_nanoseconds; }
    [[nodiscard]] Object const& time_zone() const { return m_time_zone; }
    [[nodiscard]] Object& time_zone() { return m_time_zone; }
    [[nodiscard]] Object const& calendar() const { return m_calendar; }
    [[nodiscard]] Object& calendar() { return m_calendar; }

private:
    virtual void visit_edges(Visitor&) override;

    // 6.4 Properties of Temporal.ZonedDateTime Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-zoneddatetime-instances
    BigInt const& m_nanoseconds; // [[Nanoseconds]]
    Object& m_time_zone;         // [[TimeZone]]
    Object& m_calendar;          // [[Calendar]]
};

struct NanosecondsToDaysResult {
    double days;
    Handle<BigInt> nanoseconds;
    double day_length;
};

enum class OffsetBehavior {
    Option,
    Exact,
    Wall,
};

enum class MatchBehavior {
    MatchExactly,
    MatchMinutes,
};

ThrowCompletionOr<BigInt const*> interpret_iso_date_time_offset(GlobalObject&, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, OffsetBehavior offset_behavior, double offset_nanoseconds, Value time_zone, StringView disambiguation, StringView offset_option, MatchBehavior match_behavior);
ThrowCompletionOr<ZonedDateTime*> to_temporal_zoned_date_time(GlobalObject&, Value item, Object* options = nullptr);
ThrowCompletionOr<ZonedDateTime*> create_temporal_zoned_date_time(GlobalObject&, BigInt const& epoch_nanoseconds, Object& time_zone, Object& calendar, FunctionObject const* new_target = nullptr);
ThrowCompletionOr<String> temporal_zoned_date_time_to_string(GlobalObject&, ZonedDateTime& zoned_date_time, Variant<StringView, u8> const& precision, StringView show_calendar, StringView show_time_zone, StringView show_offset, Optional<u64> increment = {}, Optional<StringView> unit = {}, Optional<StringView> rounding_mode = {});
ThrowCompletionOr<BigInt*> add_zoned_date_time(GlobalObject&, BigInt const& epoch_nanoseconds, Value time_zone, Object& calendar, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object* options = nullptr);
ThrowCompletionOr<TemporalDuration> difference_zoned_date_time(GlobalObject&, BigInt const& nanoseconds1, BigInt const& nanoseconds2, Object& time_zone, Object& calendar, StringView largest_unit, Object* options = nullptr);
ThrowCompletionOr<NanosecondsToDaysResult> nanoseconds_to_days(GlobalObject&, BigInt const& nanoseconds, Value relative_to);

}
