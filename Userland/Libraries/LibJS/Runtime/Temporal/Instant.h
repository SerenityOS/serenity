/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
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
const auto INSTANT_NANOSECONDS_MIN = Crypto::SignedBigInteger::from_base(10, "-8640000000000000000000");
// +86400 * 10^17
const auto INSTANT_NANOSECONDS_MAX = Crypto::SignedBigInteger::from_base(10, "8640000000000000000000");

bool is_valid_epoch_nanoseconds(BigInt const& epoch_nanoseconds);
Object* create_temporal_instant(GlobalObject&, BigInt& nanoseconds, FunctionObject* new_target = nullptr);

}
