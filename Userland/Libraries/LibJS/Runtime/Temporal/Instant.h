/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Instant final : public Object {
    JS_OBJECT(Instant, Object);

public:
    explicit Instant(BigInt& nanoseconds, Object& prototype);
    virtual ~Instant() override = default;

    BigInt const& nanoseconds() const { return m_nanoseconds; }

private:
    virtual void visit_edges(Visitor&) override;

    // 8.4 Properties of Temporal.Instant Instances, https://tc39.es/proposal-temporal/#sec-properties-of-temporal-instant-instances

    // [[Nanoseconds]]
    BigInt& m_nanoseconds;
};

// -86400 * 10^17
const auto INSTANT_NANOSECONDS_MIN = "-8640000000000000000000"_sbigint;
// +86400 * 10^17
const auto INSTANT_NANOSECONDS_MAX = "8640000000000000000000"_sbigint;

bool is_valid_epoch_nanoseconds(BigInt const& epoch_nanoseconds);
Instant* create_temporal_instant(GlobalObject&, BigInt& nanoseconds, FunctionObject* new_target = nullptr);
Instant* to_temporal_instant(GlobalObject&, Value item);
BigInt* parse_temporal_instant(GlobalObject&, String const& iso_string);

}
