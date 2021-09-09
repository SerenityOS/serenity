/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Duration final : public Object {
    JS_OBJECT(Duration, Object);

public:
    Duration(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object& prototype);
    virtual ~Duration() override = default;

    [[nodiscard]] double years() const { return m_years; }
    [[nodiscard]] double months() const { return m_months; }
    [[nodiscard]] double weeks() const { return m_weeks; }
    [[nodiscard]] double days() const { return m_days; }
    [[nodiscard]] double hours() const { return m_hours; }
    [[nodiscard]] double minutes() const { return m_minutes; }
    [[nodiscard]] double seconds() const { return m_seconds; }
    [[nodiscard]] double milliseconds() const { return m_milliseconds; }
    [[nodiscard]] double microseconds() const { return m_microseconds; }
    [[nodiscard]] double nanoseconds() const { return m_nanoseconds; }

private:
    // 7.4 Properties of Temporal.Duration Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-duration-instances
    double m_years;        // [[Years]]
    double m_months;       // [[Months]]
    double m_weeks;        // [[Weeks]]
    double m_days;         // [[Days]]
    double m_hours;        // [[Hours]]
    double m_minutes;      // [[Minutes]]
    double m_seconds;      // [[Seconds]]
    double m_milliseconds; // [[Milliseconds]]
    double m_microseconds; // [[Microseconds]]
    double m_nanoseconds;  // [[Nanoseconds]]
};

// Used by ToTemporalDurationRecord to temporarily hold values
struct TemporalDuration {
    double years;
    double months;
    double weeks;
    double days;
    double hours;
    double minutes;
    double seconds;
    double milliseconds;
    double microseconds;
    double nanoseconds;
};

// Used by ToPartialDuration to temporarily hold values
struct PartialDuration {
    Optional<double> years;
    Optional<double> months;
    Optional<double> weeks;
    Optional<double> days;
    Optional<double> hours;
    Optional<double> minutes;
    Optional<double> seconds;
    Optional<double> milliseconds;
    Optional<double> microseconds;
    Optional<double> nanoseconds;
};

// Used by BalanceDuration to temporarily hold values
struct BalancedDuration {
    double days;
    double hours;
    double minutes;
    double seconds;
    double milliseconds;
    double microseconds;
    double nanoseconds;
};

// Table 7: Properties of a TemporalDurationLike, https://tc39.es/proposal-temporal/#table-temporal-temporaldurationlike-properties

template<typename StructT, typename ValueT>
struct TemporalDurationLikeProperty {
    ValueT StructT::*internal_slot { nullptr };
    PropertyName property;
};

template<typename StructT, typename ValueT>
auto temporal_duration_like_properties = [](VM& vm) {
    using PropertyT = TemporalDurationLikeProperty<StructT, ValueT>;
    return AK::Array<PropertyT, 10> {
        PropertyT { &StructT::days, vm.names.days },
        PropertyT { &StructT::hours, vm.names.hours },
        PropertyT { &StructT::microseconds, vm.names.microseconds },
        PropertyT { &StructT::milliseconds, vm.names.milliseconds },
        PropertyT { &StructT::minutes, vm.names.minutes },
        PropertyT { &StructT::months, vm.names.months },
        PropertyT { &StructT::nanoseconds, vm.names.nanoseconds },
        PropertyT { &StructT::seconds, vm.names.seconds },
        PropertyT { &StructT::weeks, vm.names.weeks },
        PropertyT { &StructT::years, vm.names.years },
    };
};

Duration* to_temporal_duration(GlobalObject&, Value item);
TemporalDuration to_temporal_duration_record(GlobalObject&, Object& temporal_duration_like);
i8 duration_sign(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds);
bool is_valid_duration(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds);
PartialDuration to_partial_duration(GlobalObject&, Value temporal_duration_like);
Duration* create_temporal_duration(GlobalObject&, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, FunctionObject const* new_target = nullptr);
Duration* create_negated_temporal_duration(GlobalObject& global_object, Duration const& duration);
BigInt* total_duration_nanoseconds(GlobalObject&, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, BigInt const& nanoseconds, double offset_shift);
Optional<BalancedDuration> balance_duration(GlobalObject&, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, BigInt const& nanoseconds, String const& largest_unit, Object* relative_to = nullptr);
Optional<TemporalDuration> to_limited_temporal_duration(GlobalObject&, Value temporal_duration_like, Vector<StringView> const& disallowed_fields);

}
