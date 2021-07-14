/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/Now.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <time.h>

namespace JS::Temporal {

// 2 The Temporal.now Object, https://tc39.es/proposal-temporal/#sec-temporal-now-object
Now::Now(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Now::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.timeZone, time_zone, 0, attr);
    define_native_function(vm.names.instant, instant, 0, attr);
}

// 2.1.1 Temporal.now.timeZone ( ), https://tc39.es/proposal-temporal/#sec-temporal.now.timezone
JS_DEFINE_NATIVE_FUNCTION(Now::time_zone)
{
    // 1. Return ? SystemTimeZone().
    return system_time_zone(global_object);
}

// 2.1.2 Temporal.now.instant ( ), https://tc39.es/proposal-temporal/#sec-temporal.now.instant
JS_DEFINE_NATIVE_FUNCTION(Now::instant)
{
    // 1. Return ? SystemInstant().
    return system_instant(global_object);
}

// 2.2.1 SystemTimeZone ( ), https://tc39.es/proposal-temporal/#sec-temporal-systemtimezone
TimeZone* system_time_zone(GlobalObject& global_object)
{
    // 1. Let identifier be ! DefaultTimeZone().
    auto identifier = default_time_zone();

    // 2. Return ? CreateTemporalTimeZone(identifier).
    return create_temporal_time_zone(global_object, identifier);
}

// 2.2.2 SystemUTCEpochNanoseconds ( )
BigInt* system_utc_epoch_nanoseconds(GlobalObject& global_object)
{
    // 1. Let ns be the approximate current UTC date and time, in nanoseconds since the epoch.
    struct timespec now;
    clock_gettime(CLOCK_REALTIME, &now);
    Checked<i64> ns_timestamp;
    ns_timestamp += now.tv_sec;
    ns_timestamp *= 1'000'000'000;
    ns_timestamp += now.tv_nsec;
    if (ns_timestamp.has_overflow()) {
        // TODO: Deal with this before 2262-04-21T00:47:16Z.
        VERIFY_NOT_REACHED();
    }
    auto ns = Crypto::SignedBigInteger::create_from(ns_timestamp.value());

    // 2. Set ns to the result of clamping ns between −8.64 × 10^21 and 8.64 × 10^21.
    // Uhh, these don't even fit in an i64... ¯\_(ツ)_/¯

    // 3. Return ℤ(ns).
    return js_bigint(global_object.heap(), move(ns));
}

// 2.2.3 SystemInstant ( )
Instant* system_instant(GlobalObject& global_object)
{
    // 1. Let ns be ! SystemUTCEpochNanoseconds().
    auto* ns = system_utc_epoch_nanoseconds(global_object);

    // 2. Return ? CreateTemporalInstant(ns).
    return create_temporal_instant(global_object, *ns);
}

}
