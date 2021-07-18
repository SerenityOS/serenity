/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>

namespace JS::Temporal {

// 5.5.1 GetEpochFromISOParts ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-getepochfromisoparts
BigInt* get_epoch_from_iso_parts(GlobalObject& global_object, i32 year, i32 month, i32 day, i32 hour, i32 minute, i32 second, i32 millisecond, i32 microsecond, i32 nanosecond)
{
    auto& vm = global_object.vm();

    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Assert: ! IsValidISODate(year, month, day) is true.
    VERIFY(is_valid_iso_date(year, month, day));

    // 3. Assert: ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is true.
    VERIFY(is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond));

    // 4. Let date be ! MakeDay(𝔽(year), 𝔽(month − 1), 𝔽(day)).
    auto date = make_day(global_object, Value(year), Value(month - 1), Value(day));

    // 5. Let time be ! MakeTime(𝔽(hour), 𝔽(minute), 𝔽(second), 𝔽(millisecond)).
    auto time = make_time(global_object, Value(hour), Value(minute), Value(second), Value(millisecond));

    // 6. Let ms be ! MakeDate(date, time).
    auto ms = make_date(date, time);

    // 7. Assert: ms is finite.
    VERIFY(ms.is_finite_number());

    // 8. Return ℝ(ms) × 10^6 + microsecond × 10^3 + nanosecond.
    return js_bigint(vm.heap(), Crypto::SignedBigInteger::create_from(static_cast<i64>(ms.as_double())).multiplied_by(Crypto::UnsignedBigInteger { 1'000'000 }).plus(Crypto::SignedBigInteger::create_from((i64)microsecond * 1000)).plus(Crypto::SignedBigInteger(nanosecond)));
}

// -864 * 10^19 - 864 * 10^14
const auto DATETIME_NANOSECONDS_MIN = "-8640086400000000000000"_sbigint;
// +864 * 10^19 + 864 * 10^14
const auto DATETIME_NANOSECONDS_MAX = "8640086400000000000000"_sbigint;

// 5.5.2 ISODateTimeWithinLimits ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-isodatetimewithinlimits
bool iso_date_time_within_limits(GlobalObject& global_object, i32 year, i32 month, i32 day, i32 hour, i32 minute, i32 second, i32 millisecond, i32 microsecond, i32 nanosecond)
{
    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let ns be ! GetEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond).
    auto ns = get_epoch_from_iso_parts(global_object, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

    // 3. If ns ≤ -8.64 × 10^21 - 8.64 × 10^16, then
    if (ns->big_integer() <= DATETIME_NANOSECONDS_MIN) {
        // a. Return false.
        return false;
    }

    // 4. If ns ≥ 8.64 × 10^21 + 8.64 × 10^16, then
    if (ns->big_integer() >= DATETIME_NANOSECONDS_MAX) {
        // a. Return false.
        return false;
    }
    // 5. Return true.
    return true;
}

}
