/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/InstantConstructor.h>

namespace JS::Temporal {

// 8 Temporal.Instant Objects, https://tc39.es/proposal-temporal/#sec-temporal-instant-objects
Instant::Instant(BigInt& nanoseconds, Object& prototype)
    : Object(prototype)
    , m_nanoseconds(nanoseconds)
{
}

void Instant::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_nanoseconds);
}

// 8.5.1 IsValidEpochNanoseconds ( epochNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidepochnanoseconds
bool is_valid_epoch_nanoseconds(BigInt const& epoch_nanoseconds)
{
    // 1. Assert: Type(epochNanoseconds) is BigInt.

    // 2. If epochNanoseconds < −86400ℤ × 10^17ℤ or epochNanoseconds > 86400ℤ × 10^17ℤ, then
    if (epoch_nanoseconds.big_integer() < INSTANT_NANOSECONDS_MIN || epoch_nanoseconds.big_integer() > INSTANT_NANOSECONDS_MAX) {
        // a. Return false.
        return false;
    }

    // 3. Return true.
    return true;
}

// 8.5.2 CreateTemporalInstant ( epochNanoseconds [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalinstant
Object* create_temporal_instant(GlobalObject& global_object, BigInt& epoch_nanoseconds, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(epochNanoseconds) is BigInt.

    // 2. Assert: ! IsValidEpochNanoseconds(epochNanoseconds) is true.
    VERIFY(is_valid_epoch_nanoseconds(epoch_nanoseconds));

    // 3. If newTarget is not present, set it to %Temporal.Instant%.
    if (!new_target)
        new_target = global_object.temporal_instant_constructor();

    // 4. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.Instant.prototype%", « [[InitializedTemporalInstant]], [[Nanoseconds]] »).
    // 5. Set object.[[Nanoseconds]] to epochNanoseconds.
    auto* object = ordinary_create_from_constructor<Instant>(global_object, *new_target, &GlobalObject::temporal_instant_prototype, epoch_nanoseconds);
    if (vm.exception())
        return {};

    // 6. Return object.
    return object;
}

}
