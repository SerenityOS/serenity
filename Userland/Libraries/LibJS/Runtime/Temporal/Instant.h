/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Optional.h>
#include <YAK/Variant.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Instant final : public Object {
    JS_OBJECT(Instant, Object);

public:
    Instant(BigInt& nanoseconds, Object& prototype);
    virtual ~Instant() override = default;

    [[nodiscard]] BigInt const& nanoseconds() const { return m_nanoseconds; }

private:
    virtual void visit_edges(Visitor&) override;

    // 8.4 Properties of Temporal.Instant Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-instant-instances
    BigInt& m_nanoseconds; // [[Nanoseconds]]
};

// -86400 * 10^17
const auto INSTANT_NANOSECONDS_MIN = "-8640000000000000000000"_sbigint;
// +86400 * 10^17
const auto INSTANT_NANOSECONDS_MAX = "8640000000000000000000"_sbigint;

bool is_valid_epoch_nanoseconds(BigInt const& epoch_nanoseconds);
Instant* create_temporal_instant(GlobalObject&, BigInt& nanoseconds, FunctionObject* new_target = nullptr);
Instant* to_temporal_instant(GlobalObject&, Value item);
BigInt* parse_temporal_instant(GlobalObject&, String const& iso_string);
i32 compare_epoch_nanoseconds(BigInt const&, BigInt const&);
BigInt* add_instant(GlobalObject&, BigInt const& epoch_nanoseconds, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds);
BigInt* round_temporal_instant(GlobalObject&, BigInt const& nanoseconds, u64 increment, String const& unit, String const& rounding_mode);
Optional<String> temporal_instant_to_string(GlobalObject&, Instant&, Value time_zone, Variant<String, u8> const& precision);

}
