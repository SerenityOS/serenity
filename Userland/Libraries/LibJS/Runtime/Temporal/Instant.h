/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>

namespace JS::Temporal {

class Instant final : public Object {
    JS_OBJECT(Instant, Object);

public:
    Instant(BigInt const& nanoseconds, Object& prototype);
    virtual ~Instant() override = default;

    [[nodiscard]] BigInt const& nanoseconds() const { return m_nanoseconds; }

private:
    virtual void visit_edges(Visitor&) override;

    // 8.4 Properties of Temporal.Instant Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-instant-instances
    BigInt const& m_nanoseconds; // [[Nanoseconds]]
};

// https://tc39.es/proposal-temporal/#eqn-nsMaxInstant
// nsMaxInstant = 10^8 × nsPerDay = 8.64 × 10^21
static auto const ns_max_instant = "8640000000000000000000"_sbigint;

// https://tc39.es/proposal-temporal/#eqn-nsMinInstant
// nsMinInstant = -nsMaxInstant = -8.64 × 10^21
static auto const ns_min_instant = "-8640000000000000000000"_sbigint;

bool is_valid_epoch_nanoseconds(BigInt const& epoch_nanoseconds);
ThrowCompletionOr<Instant*> create_temporal_instant(GlobalObject&, BigInt const& nanoseconds, FunctionObject const* new_target = nullptr);
ThrowCompletionOr<Instant*> to_temporal_instant(GlobalObject&, Value item);
ThrowCompletionOr<BigInt*> parse_temporal_instant(GlobalObject&, String const& iso_string);
i32 compare_epoch_nanoseconds(BigInt const&, BigInt const&);
ThrowCompletionOr<BigInt*> add_instant(GlobalObject&, BigInt const& epoch_nanoseconds, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds);
BigInt* difference_instant(GlobalObject&, BigInt const& nanoseconds1, BigInt const& nanoseconds2, u64 rounding_increment, StringView smallest_unit, StringView rounding_mode);
BigInt* round_temporal_instant(GlobalObject&, BigInt const& nanoseconds, u64 increment, StringView unit, StringView rounding_mode);
ThrowCompletionOr<String> temporal_instant_to_string(GlobalObject&, Instant&, Value time_zone, Variant<StringView, u8> const& precision);
ThrowCompletionOr<Duration*> difference_temporal_instant(GlobalObject&, DifferenceOperation, Instant const&, Value other, Value options);
ThrowCompletionOr<Instant*> add_duration_to_or_subtract_duration_from_instant(GlobalObject&, ArithmeticOperation, Instant const&, Value temporal_duration_like);

}
